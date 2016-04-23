package tcl.lang.cmd;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TclException;
import tcl.lang.TclIO;
import tcl.lang.TclIndex;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;
import tcl.lang.channel.Channel;
import tcl.lang.channel.FileEvent;
import tcl.lang.channel.FileEventScript;

/**
 * This class implements Tcl's 'fileevent' command. Most of the work is done by
 * FileEvent and FileEventScript.
 */
public class FileeventCmd implements Command {

	String[] eventType = { "readable", "writable" };
	static final int READABLE_TYPE = 0;
	static final int WRITABLE_TYPE = 1;

	public void cmdProc(Interp interp, TclObject[] objv) throws TclException {

		if (objv.length < 3 || objv.length > 4) {
			throw new TclNumArgsException(interp, 1, objv, "channelId event ?script?");
		}

		int type = TclIndex.get(interp, objv[2], eventType, "event name", 0);
		if (type == READABLE_TYPE) {
			type = FileEvent.READABLE;
		} else {
			type = FileEvent.WRITABLE;
		}

		Channel channel = TclIO.getChannel(interp, objv[1].toString());
		if (channel == null) {
			throw new TclException(interp, "can not find channel named \"" + objv[1] + "\"");
		}

		if (objv.length == 3) {
			// return the script
			FileEventScript script = FileEventScript.find(interp, channel, type);
			if (script == null) {
				interp.setResult("");
			} else {
				interp.setResult(script.getScript());
			}
		} else {
			// create a new fileevent, or delete it if the script is empty
			if (objv[3].toString().length() == 0) {
				FileEventScript.dispose(interp, channel, type);
			} else {
				FileEventScript.register(interp, channel, type, objv[3]);
			}
			interp.setResult("");
		}
	}
}
