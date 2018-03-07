package tcl.lang.channel;

import tcl.lang.AssocData;
import tcl.lang.Interp;
import tcl.lang.TclObject;

/**
 * AssocData structure that keeps the script for one fileevent
 * command.  The existence of a FileEventScript object indicates
 * the existence of a particular 'fileevent' command instance.
 * Once the 'fileevent' command is called to delete the script,
 * or if the interpreter is destroyed, the associated FileEventScript
 * is disposed which signals FileEvents associated with this script
 * to discontinue.
 *
 */
public class FileEventScript implements AssocData {
	TclObject script;
	
	/**
	 * 
	 * @param script The script to associate with this FileEventScript
	 */
	private FileEventScript(TclObject script) {
		this.script = script;
		this.script.preserve();
	}
	
	/**
	 * Create a FileEventScript, and register it in the interpreter,
	 * and queue the first FileEvent
	 * 
	 * @param interp
	 *            Interpreter  for the fileevent
	 * @param channel
	 *            name of channel that the fileEvent is registered on
	 * @param type
	 *            either FileEvent.READABLE or FileEvent.WRITEABLE
	 * @param script script to register          
	 * 
	 */
	public static void register(Interp interp, Channel channel, int type,  TclObject script) {
		FileEventScript fes = new FileEventScript(script);
		interp.setAssocData(getName(channel, type), fes);
		FileEvent.queueFileEvent(interp, channel, type);
	}

	/**
	 * @return the script associated with this FileEventScript
	 */
	public TclObject getScript() {
		return script;
	}
	
	public void disposeAssocData(Interp interp) {
		this.script.release();
	}


	/**
	 * Return a formatted name to use for the interpreter's assocData hash key to locate
	 * a FileEventScript
	 * 
	 * @param channelName
	 *            name of the channel containing the fileevent
	 * @param type
	 *            either FileEvent.READABLE or FileEvent.WRITEABLE
	 * @return string to use as the key for the assocData hash
	 */
	private static String getName(Channel channel, int type) {
		String channelName = channel.getChanName();
		return channelName + (type == FileEvent.READABLE ? " readable " : " writeable ") + FileEventScript.class.getName();
	}

	/**
	 * Find a FileEventScript if it exists
	 * 
	 * @param interp
	 *            Interpreter to search for the fileevent
	 * @param channel
	 *            name of channel that the fileEvent is registered on
	 * @param type
	 *            either FileEvent.READABLE or FileEvent.WRITEABLE
	 * 
	 * @return FileEvent object, or null if it does not exist
	 */
	public static FileEventScript find(Interp interp, Channel channel, int type) {
		String name = getName(channel, type);
		AssocData data = interp.getAssocData(name);
		if (data != null && data instanceof FileEventScript) {
			return (FileEventScript) data;
		} else
			return null;
	}
	
	/**
	 * Remove a FileEventScript, and hence all FileEvents, if it exists
	 * @param interp
	 *            Interpreter to search for the fileevent
	 * @param channel
	 *            name of channel that the fileEvent is registered on
	 * @param type
	 *            either FileEvent.READABLE or FileEvent.WRITEABLE
	 * 
	 */
	public static void dispose(Interp interp, Channel channel, int type) {
		String name = getName(channel, type);
		interp.deleteAssocData(name);
	}
	
}
