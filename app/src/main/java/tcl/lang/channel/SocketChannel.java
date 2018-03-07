/*
 * SocketChannel.java
 *
 * Implements a socket channel.
 */
package tcl.lang.channel;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import tcl.lang.Interp;
import tcl.lang.TclException;
import tcl.lang.TclIO;
import tcl.lang.TclObject;
import tcl.lang.TclString;

/**
 * The SocketChannel class implements a channel object for Socket connections,
 * created using the socket command.
 **/

public class SocketChannel extends AbstractSocketChannel {

	/**
	 * The java Socket object associated with this Channel
	 **/
	private Socket sock = null;

	/**
	 * Indicates an error during the connection
	 */
	private IOException connectException = null;

	/**
	 * Thread responsible for connecting socket asynchronously
	 */
	private Thread asyncConnectThread = null;

	/**
	 * Local address, or null if any address can be used
	 */
	InetAddress localAddress = null;

	/**
	 * address to connect to
	 */
	InetAddress addr = null;

	/**
	 * The input stream returned to the Channel
	 */
	InputStream istream = null;

	/**
	 * The output stream returned to the Channel
	 */
	OutputStream ostream = null;

	/**
	 * Notifies that async connection has been made
	 */
	Object asyncNotifier = new Object();

	/**
	 * Constructor - creates a new SocketChannel object with the given options.
	 * 
	 *@param interp
	 *            the current interpreter
	 *@param localAddr
	 *            localAddress to use, or empty string if default is to be used
	 *@param localPort
	 *            local port to use, or 0 if any port is to be used
	 *@param async
	 *            true if socket should be connected asynchronously
	 *@param address
	 *            IP address or hostname to connect to
	 *@param port
	 *            port to connect to
	 *@throws IOException
	 *@throws TclException
	 **/
	public SocketChannel(Interp interp, int mode, String localAddr, final int localPort, boolean async, String address,
			final int port) throws IOException, TclException {

		// Resolve addresses
		if (!localAddr.equals("")) {
			try {
				localAddress = InetAddress.getByName(localAddr);
			} catch (UnknownHostException e) {
				throw new TclException(interp, "host unknown: " + localAddr);
			}
		}

		try {
			addr = InetAddress.getByName(address);
		} catch (UnknownHostException e) {
			throw new TclException(interp, "host unknown: " + address);
		}

		// Set the mode of this socket.
		this.mode = mode;

		// Create the Socket object
		if (async) {
			asyncConnectThread = new Thread(new Runnable() {
				public void run() {
					connectSocket(addr, port, localAddress, localPort);
				}
			});
			asyncConnectThread.setDaemon(true);
			asyncConnectThread.setName("SocketChannel (" + interp.toString() + "): " + address + ":" + port);
			asyncConnectThread.start();
		} else {
			connectSocket(addr, port, localAddress, localPort);
			if (connectException != null) {
				throw connectException;
			}
		}

		// If we got this far, then the socket has been created.
		// Create the channel name
		setChanName(TclIO.getNextDescriptor(interp, "sock"));
	}

	/**
	 * Constructor for making SocketChannel objects from connections made to a
	 * ServerSocket.
	 * 
	 * @param interp
	 *            the current interpreter
	 * @param s
	 *            A connected socket from which to create a channel
	 **/

	public SocketChannel(Interp interp, Socket s) throws IOException, TclException {
		this.mode = TclIO.RDWR;
		this.sock = s;

		setChanName(TclIO.getNextDescriptor(interp, "sock"));
	}

	/**
	 * Create the 'sock' field and connect the Socket
	 * 
	 * @param addr
	 *            Address to connect to
	 * @param port
	 *            Port to connect to
	 * @param localAddress
	 *            Local address to bind, or null for default
	 * @param localPort
	 *            Local port to bind to, or 0 for any port
	 * @throws IOException
	 */
	protected void connectSocket(InetAddress addr, int port, InetAddress localAddress, int localPort) {
		Socket s;
		try {
			s = new Socket(addr, port, localAddress, localPort);
			synchronized (asyncNotifier) {
				sock = s;
				asyncNotifier.notifyAll();
			}
		} catch (IOException e) {
			synchronized (this) {
				connectException = e;
				this.notifyAll();
			}
		}
	}

	/**
	 * Wait for the asynchronous connection to be established. Return
	 * immediately if connection is already made.
	 */
	private void waitForConnection() {
		if (asyncConnectThread == null)
			return; // synchronous connection
		synchronized (asyncNotifier) {
			while (sock == null && connectException == null) {
				try {
					asyncNotifier.wait();
				} catch (InterruptedException e) {
					return;
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tcl.lang.channel.Channel#implClose()
	 */
	@Override
	void implClose() throws IOException {
		synchronized (asyncNotifier) {
			if (asyncConnectThread != null)
				asyncConnectThread.interrupt();
			if (sock != null)
				sock.close();
		}
	}

	@Override
	protected InputStream getInputStream() throws IOException {
		/*
		 * Wrap sock.getInputStream() in a stream that waits for async
		 * connection to be made
		 */
		if (istream == null) {
			istream = new InputStream() {

				@Override
				public int read() throws IOException {
					waitForConnection();
					if (sock != null)
						return sock.getInputStream().read();
					else if (connectException != null)
						throw connectException;
					else
						throw new IOException("socket is null");
				}

				/*
				 * (non-Javadoc)
				 * 
				 * @see java.io.InputStream#read(byte[], int, int)
				 */
				@Override
				public int read(byte[] b, int off, int len) throws IOException {
					waitForConnection();
					if (sock != null)
						return sock.getInputStream().read(b, off, len);
					else if (connectException != null)
						throw connectException;
					else
						throw new IOException("socket is null");
				}

				/*
				 * (non-Javadoc)
				 * 
				 * @see java.io.InputStream#available()
				 */
				@Override
				public int available() throws IOException {
					synchronized (asyncNotifier) {
						if (sock == null)
							return 0;
						else
							return sock.getInputStream().available();
					}
				}

			};
		}
		return istream;
	}

	@Override
	protected OutputStream getOutputStream() throws IOException {
		/*
		 * Wrap sock.getOutputStream() in a stream that waits for async
		 * connection to be made
		 */
		if (ostream == null) {
			ostream = new OutputStream() {

				@Override
				public void write(int b) throws IOException {
					waitForConnection();
					if (sock != null)
						sock.getOutputStream().write(b);
					else if (connectException != null)
						throw connectException;
					else
						throw new IOException("socket is null");
				}

				/*
				 * (non-Javadoc)
				 * 
				 * @see java.io.OutputStream#flush()
				 */
				@Override
				public void flush() throws IOException {
					waitForConnection();
					if (sock != null)
						sock.getOutputStream().flush();
					else if (connectException != null)
						throw connectException;
					else
						throw new IOException("socket is null");
				}

				/*
				 * (non-Javadoc)
				 * 
				 * @see java.io.OutputStream#write(byte[], int, int)
				 */
				@Override
				public void write(byte[] b, int off, int len) throws IOException {
					waitForConnection();
					if (sock != null)
						sock.getOutputStream().write(b, off, len);
					else if (connectException != null)
						throw connectException;
					else
						throw new IOException("socket is null");
				}

			};
		}
		return ostream;
	}

	@Override
	public TclObject getError(Interp interp) throws TclException {
		synchronized (this) {
			return TclString.newInstance(connectException == null ? "" : connectException.getMessage());
		}
	}

	@Override
	InetAddress getLocalAddress() {
		waitForConnection();
		return (sock == null ? null : sock.getLocalAddress());
	}

	@Override
	int getLocalPort() {
		waitForConnection();
		return (sock == null ? null : sock.getLocalPort());
	}

	@Override
	InetAddress getPeerAddress() {
		waitForConnection();
		return sock.getInetAddress();
	}

	@Override
	int getPeerPort() {
		waitForConnection();
		return sock.getPort();
	}

}
