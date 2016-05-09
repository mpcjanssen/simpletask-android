/*
 * TclIO.java --
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: TclIO.java,v 1.11 2009/07/16 22:12:18 rszulgo Exp $
 *
 */

package tcl.lang;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import tcl.lang.channel.Channel;
import tcl.lang.channel.FileEvent;
import tcl.lang.channel.FileEventScript;
import tcl.lang.channel.StdChannel;

public class TclIO {

	/**
	 * Read all available input, until EOF
	 */
	public static final int READ_ALL = 1;
	/**
	 * Read up to end of line
	 */
	public static final int READ_LINE = 2;
	/**
	 * Read a specified number of bytes
	 */
	public static final int READ_N_BYTES = 3;

	/**
	 * Seek relative to the beginning of the file
	 */
	public static final int SEEK_SET = 1;
	/**
	 * Seek relative to the current position in the file
	 */
	public static final int SEEK_CUR = 2;
	/**
	 * Seek relative to the end of the file
	 */
	public static final int SEEK_END = 3;

	/**
	 * Open read-only
	 */
	public static final int RDONLY = 1;
	/**
	 * Open write-only
	 */
	public static final int WRONLY = 2;
	/**
	 * Open for reading and writing
	 */
	public static final int RDWR = 4;
	/**
	 * Append to end of file when writing
	 */
	public static final int APPEND = 8;
	/**
	 * Create a new file
	 */
	public static final int CREAT = 16;
	public static final int EXCL = 32;
	public static final int TRUNC = 64;

	/**
	 * Do full buffering up to size of buffer
	 */
	public static final int BUFF_FULL = 0;
	/**
	 * Flush at end of line; buffer only one line on input
	 */
	public static final int BUFF_LINE = 1;
	/**
	 * Flush after every write; don't buffer any input
	 */
	public static final int BUFF_NONE = 2;

	/**
	 * Translate \n, \r, \r\n to \n on input; translate \n to platform-specific
	 * end of line on output
	 */
	public static final int TRANS_AUTO = 0;
	/**
	 * Don't translate end of line characters
	 */
	public static final int TRANS_BINARY = 1;
	/**
	 * Don't translate end of line characters
	 */
	public static final int TRANS_LF = 2;
	/**
	 * \n -> \r on output; \r -> \n on input
	 */
	public static final int TRANS_CR = 3;
	/**
	 * \n to \r\n on output; \r\n -> \n on input
	 */
	public static final int TRANS_CRLF = 4;

	/**
	 * End-of-line translation for the current platform
	 */
	public static int TRANS_PLATFORM;

	static {
		if (Util.isWindows())
			TRANS_PLATFORM = TRANS_CRLF;
		else if (Util.isMac())
			TRANS_PLATFORM = TRANS_CR;
		else
			TRANS_PLATFORM = TRANS_LF;
	}

	/**
	 * Table of channels currently registered for all interps. The
	 * interpChanTable has "virtual" references into this table that stores the
	 * registered channels for the individual interp.
	 */

	private static StdChannel stdinChan = null;
	private static StdChannel stdoutChan = null;
	private static StdChannel stderrChan = null;

	/**
	 * Return a registered Channel object, given its name.
	 * 
	 * @param interp
	 *            Interpreter context
	 * @param chanName
	 *            Name of channel
	 * @return Channel or null if chanName does not exist
	 */
	public static Channel getChannel(Interp interp, String chanName) {
		HashMap<String, Channel> chanTable = getInterpChanTable(interp);

		/* Once we request stdin/stderr, [system encoding VALUE] can't change its encoding */
		if (interp.systemEncodingChangesStdoutStderr && ("stdout".equals(chanName) || "stderr".equals(chanName))) {
			interp.systemEncodingChangesStdoutStderr = false;
		}
		if (chanTable.containsKey(chanName)) {
			return chanTable.get(chanName);
		} else {
			// channel may have been registered as one of the standard channels
			for (String name : new String[] { "stdin", "stdout", "stderr" }) {
				Channel std = chanTable.get(name);
				if (std != null && std.getChanName().equals(chanName)) {
					return std;
				}
			}
		}
		return null;
	}

	/**
	 * Put all the channel names into the interpreter's result, as a list
	 * 
	 * @param interp
	 *            this interpreter
	 * @param pattern
	 *            Return only channel names that match this patter, as in
	 *            'string match', can be null to match all channels
	 * @throws TclException
	 */
	public static void getChannelNames(Interp interp, TclObject pattern) throws TclException {
		Iterator<String> it = getInterpChanTable(interp).keySet().iterator();

		/* Once we request stdin/stderr, [system encoding VALUE] can't change its encoding */
		interp.systemEncodingChangesStdoutStderr = false;
		
		while (it.hasNext()) {
			String chanName = it.next();

			try {
				if (pattern == null) {
					interp.appendElement(chanName);
				} else if (Util.stringMatch(chanName, pattern.toString())) {
					interp.appendElement(chanName);
				}
			} catch (TclException e) {
				throw e;
			}
		}
	}

	/**
	 * Register a channel in this interpreter's channel table. Any channel
	 * opening or manipulation must be done by the caller. If one of stdin,
	 * stdout or stderr is not defined in the channel table, the channel is
	 * registered as the first non-defined standard channel.
	 * 
	 * @param interp
	 *            This interpreter
	 * @param chan
	 *            channel to register
	 */
	public static void registerChannel(Interp interp, Channel chan) {

		if (interp != null) {
			HashMap<String, Channel> chanTable = getInterpChanTable(interp);
			String registerName;

			if (!chanTable.containsKey("stdin")) {
				registerName = "stdin";
			} else if (!chanTable.containsKey("stdout")) {
				registerName = "stdout";
			} else if (!chanTable.containsKey("stderr")) {
				registerName = "stderr";
			} else {
				registerName = chan.getChanName();
			}

			chanTable.put(registerName, chan);
			chan.refCount++;
		}
	}

	/**
	 * Call flush() for each currently registered open channel, ignoring
	 * exceptions. Non-blocking channels are guaranteed to be flushed after this
	 * call.
	 * 
	 * @param interp
	 *            the current interpreter
	 */
	public static void flushAllOpenChannels(Interp interp) {
		HashMap<String, Channel> chanTable = getInterpChanTable(interp);

		for (Channel channel : chanTable.values()) {
			if (channel.isWriteOnly() || channel.isReadWrite()) {
				boolean blockingMode = channel.getBlocking();
				if (!blockingMode)
					channel.setBlocking(true);
				try {
					channel.flush(interp);
				} catch (Exception ignored) {
				}
				if (!blockingMode)
					channel.setBlocking(false);
			}
		}
	}

	/**
	 * Give a channel to another interpreter, in exactly the same form it exists
	 * in the master
	 * 
	 * @param master
	 *            Interpreter giving the channel
	 * @param slave
	 *            Interpreter receiving the channel
	 * @param chanName
	 *            Name of channel to transfer
	 * @param removeFromMaster
	 *            If true, channel is unregistered from the master after being
	 *            transferred
	 * @throws TclException
	 *             if channel cannot be found
	 */
	public static void giveChannel(Interp master, Interp slave, String chanName, boolean removeFromMaster)
			throws TclException {

		
		HashMap<String, Channel> masterTable = getInterpChanTable(master);
		HashMap<String, Channel> slaveTable = getInterpChanTable(slave);
		
		slave.systemEncodingChangesStdoutStderr = false;

		Channel channel = masterTable.get(chanName);
		if (channel != null) {
			/* Transfer channel directly */
			slaveTable.put(chanName, channel);
		} else {
			/* May be posing as a std channel, so transfer likewise */
			for (String name : new String[] { "stdin", "stdout", "stderr" }) {
				channel = masterTable.get(name);
				if (channel != null && channel.getChanName().equals(chanName)) {
					slaveTable.put(name, channel);
					break;
				}
				channel = null;
			}

		}
		if (channel == null) {
			throw new TclException(master, "can not find channel named \"" + chanName + "\"");
		} else {
			channel.refCount++;
			if (removeFromMaster)
				unregisterChannel(master, channel);
		}

	}

	/**
	 * Unregister a channel and it's FileEventScripts in this interpreter's channel table, and call
	 * close() on that channel.
	 * 
	 * @param interp
	 *            this interpreter
	 * @param chan
	 *            channel to unregister
	 */
	public static void unregisterChannel(Interp interp, Channel chan) {
		HashMap<String, Channel> chanTable = getInterpChanTable(interp);
		
		FileEventScript.dispose(interp, chan, FileEvent.READABLE);
		FileEventScript.dispose(interp, chan, FileEvent.WRITABLE);
		
		if (chanTable.containsKey(chan.getChanName())) {
			chanTable.remove(chan.getChanName());
		} else {
			// channel may have been registered as one of the standard channels
			for (String name : new String[] { "stdin", "stdout", "stderr" }) {
				Channel std = chanTable.get(name);
				if (std != null && std.getChanName().equals(chan.getChanName())) {
					chanTable.remove(name);
					break;
				}
			}
		}

		if (--chan.refCount <= 0) {
			try {
				chan.close();
			} catch (IOException e) {
				// e.printStackTrace(System.err);
				throw new TclRuntimeError("TclIO.unregisterChannel() Error: IOException when closing "
						+ chan.getChanName() + ": " + e.getMessage());
			}
		}
	}

	/**
	 * Initialize this interpreter's channel table if it has not already been
	 * initialized.
	 * 
	 * @param interp
	 *            this interpreter
	 * @return the interpreter's channel table
	 */
	static HashMap<String, Channel> getInterpChanTable(Interp interp) {
		Channel chan;
		
		if (interp.interpChanTable == null) {
			interp.interpChanTable = new HashMap<>();

			chan = getStdChannel(StdChannel.STDIN);
			registerChannel(interp, chan);

			chan = getStdChannel(StdChannel.STDOUT);
			registerChannel(interp, chan);

			chan = getStdChannel(StdChannel.STDERR);
			registerChannel(interp, chan);
		}

		return interp.interpChanTable;
	}

	/**
	 * @param type
	 *            one of StdChannel.STDIN, StdChannel.STDOUT, StdChannel.STDERR
	 * @return the requested standard channel, creating it if required
	 */
	public static Channel getStdChannel(int type) {
		Channel chan = null;

		switch (type) {
		case StdChannel.STDIN:
			if (stdinChan == null) {
				stdinChan = new StdChannel(StdChannel.STDIN);
			}
			chan = stdinChan;
			break;
		case StdChannel.STDOUT:
			if (stdoutChan == null) {
				stdoutChan = new StdChannel(StdChannel.STDOUT);
			}
			chan = stdoutChan;
			break;
		case StdChannel.STDERR:
			if (stderrChan == null) {
				stderrChan = new StdChannel(StdChannel.STDERR);
			}
			chan = stderrChan;
			break;
		default:
			throw new TclRuntimeError("Invalid type for StdChannel");
		}

		return (chan);
	}

	/**
	 * Given a prefix for a channel name (such as "file") returns the next
	 * available channel name (such as "file5").
	 * 
	 * @param interp
	 *            current interpreter.
	 * @param prefix
	 *            string portion of the channel name
	 * @return the next name to use for a channel
	 */
	public static String getNextDescriptor(Interp interp, String prefix) {
		int i;
		HashMap<String, Channel> htbl = getInterpChanTable(interp);

		// The first available file identifier in Tcl is "file3"
		if (prefix.equals("file"))
			i = 3;
		else
			i = 0;

		for (; (htbl.get(prefix + i)) != null; i++) {
			// Do nothing...
		}
		return prefix + i;
	}

	/**
	 * @param translation
	 *            one of the TRANS_* constances
	 * @return a string description for a translation id defined above.
	 */

	public static String getTranslationString(int translation) {
		switch (translation) {
		case TRANS_AUTO:
			return "auto";
		case TRANS_CR:
			return "cr";
		case TRANS_CRLF:
			return "crlf";
		case TRANS_LF:
			return "lf";
		case TRANS_BINARY:
			return "lf";
		default:
			throw new TclRuntimeError("bad translation id");
		}
	}

	/**
	 * @param translation
	 *            one the fconfigure -translation strings
	 * @return a numerical identifier for the given -translation string.
	 */

	public static int getTranslationID(String translation) {
		switch (translation) {
			case "auto":
				return TRANS_AUTO;
			case "cr":
				return TRANS_CR;
			case "crlf":
				return TRANS_CRLF;
			case "lf":
				return TRANS_LF;
			case "binary":
				return TRANS_LF;
			case "platform":
				return TRANS_PLATFORM;
			default:
				return -1;
		}
	}

	/**
	 * @param buffering
	 *            one of TclIO.BUFF_FULL, TclIO.BUFF_LINE, TclIO.BUFF_NONE
	 * @return a string description for a -buffering id defined above.
	 */

	public static String getBufferingString(int buffering) {
		switch (buffering) {
		case BUFF_FULL:
			return "full";
		case BUFF_LINE:
			return "line";
		case BUFF_NONE:
			return "none";
		default:
			throw new TclRuntimeError("bad buffering id");
		}
	}

	/**
	 * @param buffering
	 *            a buffering string used in fconfigure
	 * @return a numerical identifier for the given -buffering string.
	 */

	public static int getBufferingID(String buffering) {
		switch (buffering) {
			case "full":
				return BUFF_FULL;
			case "line":
				return BUFF_LINE;
			case "none":
				return BUFF_NONE;
			default:
				return -1;
		}
	}

}
