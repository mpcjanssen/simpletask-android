package tcl.lang.channel;

import java.io.IOException;

import tcl.lang.Interp;
import tcl.lang.Notifier;
import tcl.lang.TCL;
import tcl.lang.TclEvent;
import tcl.lang.TclException;
import tcl.lang.TimerHandler;

/**
 * This class is the handles the transient event that executes a 'fileevent'
 * script exactly once. It schedules a duplicate of itself for the next
 * fileevent script execution. It is closely tied with FileEventScript.
 * 
 * @author Dan Bodoh
 * 
 */
public class FileEvent extends TclEvent {
	
	/**
	 * When input is not available, wait this long to requeue.
	 */
	private final static long FILE_EVENT_DELAY_MS = 30;
	
	/**
	 * Indicates a READ file event
	 */
	public final static int READABLE = 0;
	/**
	 * Indicates a WRITE file event
	 */
	public final static int WRITABLE = 1;

	/**
	 * set to true if stdin is being used for command input and filevents should
	 * not be fired
	 */
	static boolean stdinUsedForCommandInput = false;

	/**
	 * The interpreter in which this FileEvent is registered
	 */
	Interp interp;
	/**
	 * The type of this FileEvent, either READABLE or WRITEABLE
	 */
	int type;

	/**
	 * The Channel associated with this FileEvent
	 */
	Channel channel;

	/**
	 * Create a new FileEvent
	 * 
	 * @param interp
	 *            interpreter in which to create the file event
	 * @param channel
	 *            Channel on which to create the file event
	 * @param type
	 *            either READABLE or WRITEABLE
	 */
	private FileEvent(Interp interp, Channel channel, int type) {
		super();
		this.interp = interp;
		this.channel = channel;
		this.type = type;
	}

	/**
	 * Create a new FileEvent and add it to the TclEvent queue.
	 * 
	 * @param interp
	 *            interpreter in which to create the file event
	 * @param channel
	 *            Channel on which to create the file event
	 * @param type
	 *            either READABLE or WRITABLE
	 */
	public static void queueFileEvent(Interp interp, Channel channel, int type) {
		Notifier notifier = interp.getNotifier();
		if (notifier != null) {
			TclEvent ev = new FileEvent(interp, channel, type);
			notifier.queueEvent(ev, TCL.QUEUE_TAIL);
		}
	}

	/**
	 * Put a duplicate FileEvent onto the queue
	 */
	private void requeue() {
		queueFileEvent(this.interp, this.channel, this.type);
	}
	
	/**
	 * Put a duplicate FileEvent onto the queue after FILE_EVENT_DELAY_MS millis.
	 */
	private void requeueLater() {
		new FileEventTimer(FILE_EVENT_DELAY_MS, this.interp, this.channel, this.type);
	}

	/**
	 * Permanently remove the FileEventScript for this FileEvent from the
	 * interpreter
	 */
	void dispose() {
		FileEventScript.dispose(interp, channel, type);
	}

	@Override
	public int processEvent(int flags) {
		FileEventScript script = FileEventScript.find(interp, channel, type);
		if (script == null) {
			return 1; // event was disposed
		}
		/*
		 * Don't fire fileevents on stdin when it is being used for command
		 * input ConsoleThread sets stdinUsedForCommandInput; some commands like
		 * vwait reset while executing
		 */
		if (channel instanceof StdChannel && "stdin".equals(channel.getChanName()) && isStdinUsedForCommandInput()) {
			requeue();
			return 1;
		}
		if (type == READABLE && !channel.isReadable()) {
			try {
				channel.fillInputBuffer();
				requeueLater();
			} catch (IOException e) {
				new TclException(interp, e.getMessage());
				interp.backgroundError();
				dispose();
			}
			return 1;
		}
		if (type == WRITABLE && !channel.isWritable()) {
			requeueLater();
			return 1;
		}

		/*
		 * Run the user's fileevent script
		 */
		try {
			interp.eval(script.getScript(), TCL.GLOBAL_ONLY);
		} catch (TclException e) {
			interp.backgroundError();
			dispose();
			return 1;
		}

		/*
		 * Put a duplicate FileEvent on the queue
		 */
		requeue();
		return 1;
	}

	/**
	 * @return true if stdin is currently being used for command input and file
	 *         events should not be fired on stdin
	 */
	public synchronized static boolean isStdinUsedForCommandInput() {
		return stdinUsedForCommandInput;
	}

	/**
	 * @param stdinUsedForCommandInput
	 *            the stdinUsedForCommandInput to set
	 * @return previous value of flag
	 */
	public synchronized static boolean setStdinUsedForCommandInput(boolean stdinUsedForCommandInput) {
		boolean rv = FileEvent.stdinUsedForCommandInput;
		FileEvent.stdinUsedForCommandInput = stdinUsedForCommandInput;
		return rv;
	}

	/**
	 * Create a TimerHandler to requeue a FileEvent
	 * @author tpoindex
	 *
	 */
	class FileEventTimer extends TimerHandler {

		private Interp interp;
		private Channel channel;
		private int type;
		
		public FileEventTimer(long milliseconds, Interp interp, Channel channel, int type) {
			super(interp.getNotifier(), milliseconds);
			this.interp = interp;
			this.channel = channel;
			this.type = type;
		}

		@Override
		public void processTimerEvent() {
			queueFileEvent(this.interp, this.channel, this.type);
		}
		
	}
}
