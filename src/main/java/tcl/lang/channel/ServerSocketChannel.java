/*
 * ServerSocketChannel.java
 *
 * Implements a server side socket channel for the Jacl
 * interpreter.
 */
package tcl.lang.channel;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

import tcl.lang.Interp;
import tcl.lang.TCL;
import tcl.lang.TclEvent;
import tcl.lang.TclException;
import tcl.lang.TclIO;
import tcl.lang.TclObject;
import tcl.lang.TclPosixException;
import tcl.lang.TclString;

/**
 * The ServerSocketChannel class implements a channel object for ServerSocket
 * connections, created using the socket -server command.
 **/

public class ServerSocketChannel extends AbstractSocketChannel {

	/**
	 * The java ServerSocket object associated with this Channel.
	 **/

	private ServerSocket sock;

	/**
	 * The interpreter to evaluate the callback in, when a connection is made.
	 **/

	private Interp cbInterp;

	/**
	 * The script to evaluate in the interpreter.
	 **/

	private TclObject callback;

	/**
	 * The thread which listens for new connections.
	 **/

	private AcceptThread acceptThread;

	/**
	 * Creates a new ServerSocketChannel object with the given options. Creates
	 * an underlying ServerSocket object, and a thread to handle connections to
	 * the socket.
	 * 
	 * @param interp the current interpreter
	 * @param localAddr the IP address to bind to, or an empty string
	 * @param port the port to bind to, or 0 for any port
	 * @param callback the Tcl script specified by 'server -socket'
	 **/

	public ServerSocketChannel(Interp interp, String localAddr, int port,
			TclObject callback) throws TclException {
		InetAddress localAddress = null;

		// Resolve address (if given)
		if (!localAddr.equals("")) {
			try {
				localAddress = InetAddress.getByName(localAddr);
			} catch (UnknownHostException e) {
				throw new TclException(interp, "host unkown: " + localAddr);
			}
		}
		this.mode = TclIO.CREAT; // Allow no reading or writing on channel
		this.callback = callback;
		this.callback.preserve();
		this.cbInterp = interp;

		// Create the server socket.
		try {
			if (localAddress == null)
				sock = new ServerSocket(port);
			else
				sock = new ServerSocket(port, 0, localAddress);
		} catch (IOException ex) {
			throw new TclException(interp, "couldn't open socket: "+ex.getMessage().toLowerCase());
		}

		acceptThread = new AcceptThread(sock, this);

		setChanName(TclIO.getNextDescriptor(interp, "sock"));
		acceptThread.setDaemon(true);
		acceptThread.setName("ServerSocketChannel (" + interp.toString() + "): " + getChanName() + " " + localAddr + ":" + port);
		acceptThread.start();
	}

	/**
	 * Add an event to the TclEvent queue to process a new socket connection
	 * 
	 * @param s  the new socket returned from accept()
	 */
	synchronized void addConnection(Socket s) {
		SocketConnectionEvent evt = new SocketConnectionEvent(cbInterp, callback, s, this.sock);
		cbInterp.getNotifier().queueEvent(evt, TCL.QUEUE_TAIL);
	}


	/* (non-Javadoc)
	 * @see tcl.lang.channel.Channel#implClose()
	 */
	@Override
	void implClose() throws IOException {
		acceptThread.pleaseStop();
		sock.close();
		callback.release();
	}

	/**
	 * Override to provide specific errors for server socket.
	 **/

	@Override
	public void seek(Interp interp, long offset, int mode) throws IOException,
			TclException {
		throw new TclPosixException(interp, TclPosixException.EACCES, true,
				"error during seek on \"" + getChanName() + "\"");
	}

	@Override
	protected InputStream getInputStream() throws IOException {
		throw new RuntimeException("should never be called");
	}

	@Override
	protected OutputStream getOutputStream() throws IOException {
		throw new RuntimeException("should never be called");
	}

	@Override
	public TclObject getError(Interp interp) throws TclException {
		// FIXME: what kind of error do we return here?
		return TclString.newInstance("");
	}

	@Override
	InetAddress getLocalAddress() {
		return sock.getInetAddress();
	}

	@Override
	int getLocalPort() {
		return sock.getLocalPort();
	}

	@Override
	InetAddress getPeerAddress() {
		return null;  // not supported
	}

	@Override
	int getPeerPort() {
		return 0; // not supported
	}
}

/**
 * Thread that accepts connections on the ServerSocket
 *
 */
class AcceptThread extends Thread {

	/**
	 * The ServerSocket accepting connections
	 */
	private ServerSocket sock;
	/**
	 * The Tcl Channel associated with the socket
	 */
	private ServerSocketChannel sschan;
	/**
	 * Set to false when channel is shut down
	 */
	volatile boolean keepRunning;

	public AcceptThread(ServerSocket s1, ServerSocketChannel s2) {
		sock = s1;

		// Every 10 seconds, we check to see if this socket has been closed:
		try {
			sock.setSoTimeout(10000);
		} catch (SocketException e) {
		}

		sschan = s2;
		keepRunning = true;
	}

	@Override
	public void run() {
		try {
			while (keepRunning) {
				Socket s = null;
				try {
					s = sock.accept();
				} catch (InterruptedIOException ex) {
					// Timeout
					continue;
				} catch (IOException ex) {
					// Socket closed
					break;
				}
				// Get a connection
				sschan.addConnection(s);
			}
		} catch (Exception e) {
			// Something went wrong.
			e.printStackTrace();
		}
	}

	/**
	 * Request that this thread stop accepting connections
	 */
	public void pleaseStop() {
		keepRunning = false;
		interrupt();
	}
}
