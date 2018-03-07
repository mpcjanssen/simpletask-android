/*
 * SocketConnectionEvent
 *
 * A subclass of TclEvent used to indicate that a connection
 * has been made to a server socket.
 */
package tcl.lang.channel;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import tcl.lang.Interp;
import tcl.lang.TCL;
import tcl.lang.TclEvent;
import tcl.lang.TclException;
import tcl.lang.TclIO;
import tcl.lang.TclInteger;
import tcl.lang.TclList;
import tcl.lang.TclObject;
import tcl.lang.TclString;

/**
 * A subclass of TclEvent used to indicate that a connection
 * has been made to a server socket.
 *
 */
public class SocketConnectionEvent extends TclEvent {

	Interp cbInterp;
	TclObject callbackCmd;
	Socket sock;
	ServerSocket serverSock;

	/**
	 * Create a new event to process a new socket connection
	 * 
	 * @param interp The interpreter in which the connection was made
	 * @param callbackObj Callback provided to 'socket -server'
	 * @param sock The new Java socket that was accepted
	 * @param serverSock The Java ServerSocket that created sock
	 */
	public SocketConnectionEvent(Interp interp, TclObject callbackObj, Socket sock, ServerSocket serverSock) {
		this.cbInterp = interp;
		this.callbackCmd = callbackObj;
		this.sock = sock;
		this.serverSock = serverSock;
	}

	public int processEvent(int flags) {
			/*
			 *  If the server socket was closed before we got around to this socket, just close this socket.
			 * This emulates Tcl's behavior of not making connections outside of event loop
			 */
			if (serverSock.isClosed()) {
				try {
					sock.close();
				} catch (IOException e) { }
				return 1;
			}
			
			/* Create a channel for the socket */
			SocketChannel chan = null;
			try {
				chan = new SocketChannel(cbInterp, sock);
			} catch (IOException e1) {
				new TclException(cbInterp, e1.getMessage());
				cbInterp.backgroundError();
				return 1;
			} catch (TclException e1) {
				cbInterp.backgroundError();
				return 1;
			}
			TclIO.registerChannel(cbInterp, chan);
			
			StringBuilder cblist = new StringBuilder();
			cblist.append(callbackCmd.toString());
			cblist.append(" ");
			cblist.append(chan.getChanName());
			cblist.append(" ");
			cblist.append("" + sock.getInetAddress().getHostAddress());
			cblist.append(" ");
			cblist.append("" + sock.getPort());

			// Process the event
			try {
				cbInterp.eval(cblist.toString(), TCL.EVAL_GLOBAL);
			} catch (TclException e2) {
				cbInterp.addErrorInfo("\n  during server socket callback \n");
				cbInterp.backgroundError();
			}
			
			return 1;
		} 
	}


