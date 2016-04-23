/*
 * SocketCmd.java --
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: SocketCmd.java,v 1.7 2002/01/21 01:59:35 mdejong Exp $
 *
 */

package tcl.lang.cmd;

import java.io.IOException;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TclException;
import tcl.lang.TclIO;
import tcl.lang.TclIndex;
import tcl.lang.TclObject;
import tcl.lang.TclString;
import tcl.lang.Util;
import tcl.lang.channel.ServerSocketChannel;
import tcl.lang.channel.SocketChannel;

/**
 * This class implements the built-in "socket" command in Tcl.
 */

public class SocketCmd implements Command {

	static final private String validCmds[] = { "-async", "-myaddr", "-myport",
			"-server", };

	static final int OPT_ASYNC = 0;
	static final int OPT_MYADDR = 1;
	static final int OPT_MYPORT = 2;
	static final int OPT_SERVER = 3;

	/**
	 * This procedure is invoked to process the "socket" Tcl command. See the
	 * user documentation for details on what it does.
	 * 
	 * @param interp
	 *            the current interpreter.
	 * @param argv
	 *            command arguments.
	 */

	public void cmdProc(Interp interp, TclObject argv[]) throws TclException {

		boolean server = false; // True if this is a server socket
		boolean async = false; // True if this is asynchronous
		String myaddr = ""; // DNS or IP address of the server
		String script = ""; // Script for server to run
		String host = ""; // The server fot the client
		int myport = 0; // The port to connect from
		int port = 0; // The port to connect to
		int index; // Index to the correct cmd
		int i; // Index to the current arg from argv

		for (i = 1; (i < argv.length); i++) {
			if ((argv[i].toString().length() > 0)
					&& (argv[i].toString().charAt(0) == '-')) {
				index = TclIndex.get(interp, argv[i], validCmds, "option", 0);
			} else {
				break;
			}

			switch (index) {
			case OPT_ASYNC: {
				if (server) {
					throw new TclException(interp,
							"cannot set -async option for server sockets");
				}
				async = true;
				break;
			}
			case OPT_MYADDR: {
				i++;
				if (i >= argv.length) {
					throw new TclException(interp,
							"no argument given for -myaddr option");
				}
				myaddr = argv[i].toString();
				break;
			}
			case OPT_MYPORT: {
				i++;
				if (i >= argv.length) {
					throw new TclException(interp,
							"no argument given for -myport option");
				}
				myport = getPort(interp, argv[i]);
				break;
			}
			case OPT_SERVER: {
				if (async) {
					throw new TclException(interp,
							"cannot set -async option for server sockets");
				}
				server = true;
				i++;
				if (i >= argv.length) {
					throw new TclException(interp,
							"no argument given for -server option");
				}
				script = argv[i].toString();
				break;
			}
			default: {
				throw new TclException(interp, "bad option \"" + argv[i]
						+ "\", must be -async, -myaddr, -myport,"
						+ " or -server");
			}
			}
		}

		if (server) {
			host = myaddr;
			if (myport != 0) {
				throw new TclException(interp,
						"Option -myport is not valid for servers");
			}
		} else if ((i + 1) < argv.length) {
			host = argv[i].toString();
			i++;
		} else {
			errorWrongNumArgs(interp, argv[0].toString());
		}

		if (i == argv.length - 1) {
			port = getPort(interp, argv[i]);
		} else {
			errorWrongNumArgs(interp, argv[0].toString());
		}

		if (server) {
			TclObject scr = TclString.newInstance(script);
			ServerSocketChannel sock = new ServerSocketChannel(interp, myaddr,
					port, scr);

			TclIO.registerChannel(interp, sock);
			interp.setResult(sock.getChanName());

			// FIXME: Comments from the C implementation below ...

			// Register with the interpreter to let us know when the
			// interpreter is deleted (by having the callback set the
			// acceptCallbackPtr->interp field to NULL). This is to
			// avoid trying to eval the script in a deleted interpreter.

			// Register a close callback. This callback will inform the
			// interpreter (if it still exists) that this channel does not
			// need to be informed when the interpreter is deleted.

		} else {
			try {
				SocketChannel sock = new SocketChannel(interp, TclIO.RDWR,
						myaddr, myport, async, host, port);

				TclIO.registerChannel(interp, sock);
				interp.setResult(sock.getChanName());
			} catch (IOException e) {
				throw new TclException(interp, "couldn't open socket: "
						+ e.getMessage().toLowerCase());
			}
		}
	}

	/**
	 * A unique error msg is printed for socket. Call this methods instead of
	 * the standard TclException.wrongNumArgs().
	 * 
	 * @param interp
	 *            the current interpreter.
	 * @param cmd
	 *            the name of the command (extracted form argv[0] of cmdProc)
	 */

	private static void errorWrongNumArgs(Interp interp, String cmdName)
			throws TclException {
		throw new TclException(interp, "wrong # args: should be either:\n"
				+ cmdName
				+ " ?-myaddr addr? ?-myport myport? ?-async? host port\n"
				+ cmdName + " -server command ?-myaddr addr? port");
	}

	/**
	 * TclSockGetPort -> getPort
	 * 
	 * Maps from a string, which could be a service name, to a port.
	 * Unfortunately, Java does not provide any way to access the getservbyname
	 * functionality so we are limited to port numbers.
	 * 
	 */

	private static int getPort(Interp interp, TclObject tobj)
			throws TclException {
		long num = Util.getInt(interp, tobj.toString());
		if (num > 0xFFFFL)
			throw new TclException(interp,
					"couldn't open socket: port number too high");
		return (int)num;
	}

}
