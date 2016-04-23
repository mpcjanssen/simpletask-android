package tcl.lang;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * A class for managing the System.in. Avoids blocking the current thread on the
 * non-interruptible System.in.read(), by isolating all System.in.read()'s in a
 * separate thread. Allows other classes to use System.in in a coordinated way,
 * so that one class doesn't block access to System.in.
 * 
 * After the initial instance is created, Java objects can read from System.in
 * directly to get the benefits of this class, or can create another instance of
 * this class that can be close()'d to interrupt a blocked read.
 * 
 * This class forces standard input to remain unbuffered, so the JVM doesn't
 * steal bytes from stdin that might be useful to subsequent processes that run
 * after this JVM exits.
 */
public class ManagedSystemInStream extends InputStream implements Runnable {

	/**
	 * Encapsulates a read(byte [], int, int) call and return values
	 * 
	 */
	private static class ReadRequest {
		byte[] buf;
		int offset;
		int len;
		int actualLength;
		IOException exception = null;
		boolean validData = false;
		boolean requestData = false;
		
		/**
		 * Reset back to starting values.
		 */
		private void clear() {
			buf = null;
			offset = len = actualLength = 0;
			exception = null;
			validData = requestData = false;
		}
	}

	/**
	 * The object that communicates requests between this thread and the reader
	 * thread
	 */
	private static ReadRequest readRequest = new ReadRequest();
	
	/**
	 * The thread used for reading stdin
	 */
	private volatile static Thread readThread;
	
	/**
	 * Unbuffered FileInputStream that is attached to real stdin
	 */
	private volatile static FileInputStream stdin = null;
	
	/**
	 * Original System.in
	 */
	private static InputStream originalIn = System.in;
	
	/**
	 * Set to true with this ManagedSystemInStream is closed.
	 */
	private volatile boolean streamClosed;
	
	/**
	 * Set to true when an end-of-file is seen on System.in in this instance
	 */
	private boolean eofSeen;
	
	/**
	 * Latch to coordinate dispose action
	 */
	private CountDownLatch disposeLatch;

	/**
	 * Create a new ManagedSystemInStream. If this is the first
	 * ManagedSystemInStream instance, System.in is set to this instance, so all
	 * direct access to System.in goes through this instance. Other objects can
	 * either read System.in directly, or create an instance of this class to
	 * read from.
	 */
	public ManagedSystemInStream() {
		super();
		synchronized (readRequest) {
			// install this stream as System.in if a ManagedSystemInStream has
			// not yet been installed on System.in, and start the readThread
			if (stdin == null) {
				readRequest.clear();
				streamClosed = false;
				eofSeen = false;
				disposeLatch = new CountDownLatch(1);
				stdin = new FileInputStream(FileDescriptor.in);
				try {
					System.setIn(this);
				} catch (SecurityException e) {
					// do nothing, but if this doesn't work exec and redirection of shell's input probably won't work well either
				}
				readThread = new Thread(null, this, "ManagedSystemInStream reader thread");
				readThread.setDaemon(true);
				readThread.start();
			}
		}
	}

	/**
	 * Signal readThread to stop, restore original stdin
	 */
	public void dispose() {
		streamClosed = true;
		if (readThread != null) {
			try {
				// interrupt the readThread, and wait for the disposeLatch to complete
				readThread.interrupt();
				disposeLatch.await(10, TimeUnit.SECONDS);
			} catch (Exception e) {
				// ignore, not much we can do here
			}
		}
		// set System.in back to originalIn, in case it is needed for any other usage.
		try {
			System.setIn(originalIn);
		} catch (SecurityException e) {
			// do nothing
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.InputStream#available()
	 */
	@Override
	public int available() throws IOException {
		return originalIn.available();
	}

	/**
	 * Closes this ManagedSystemInStream instance. Stops any blocked read() on
	 * this instance.
	 */
	@Override
	public void close() throws IOException {
		if (streamClosed)
			return;
		synchronized (readRequest) {
			streamClosed = true;
			readRequest.notifyAll(); // interrupt any pending
			// ManagedSystemInStream.read()
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.InputStream#read()
	 */
	@Override
	public int read() throws IOException {
		byte[] b = new byte[1];
		int cnt = read(b, 0, 1);
		if (cnt == -1)
			return -1;
		else
			return b[0];
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.InputStream#read(byte[])
	 */
	@Override
	public int read(byte[] b) throws IOException {
		return read(b, 0, b.length);
	}

	@Override
	public int read(byte[] b, int offset, int length) throws IOException {
		boolean requestMade = false;

		/* create a new request */
		while (true) {
			if (eofSeen || streamClosed)
				return -1;

			synchronized (readRequest) {
				if (eofSeen || streamClosed)
					return -1;

				/*
				 * The previous read() may have been interrupted; if so, use
				 * data from that read()
				 */
				if (!requestMade && readRequest.validData) {
					if (readRequest.exception != null) {
						readRequest.validData = false;
						readRequest.requestData = false;
						throw readRequest.exception;
					}
					if (readRequest.actualLength == -1) {
						eofSeen = true;
						return -1;
					}
					int copyLength = length < readRequest.actualLength ? length : readRequest.actualLength;
					System.arraycopy(readRequest.buf, readRequest.offset, b, offset, copyLength);
					readRequest.actualLength -= copyLength;
					if (readRequest.actualLength <= 0) {
						readRequest.validData = false;
						readRequest.requestData = false;
					}
					return copyLength;
				}

				if (!requestMade && !readRequest.validData && !readRequest.requestData) {
					/* No pending request or data, so request bytes */
					requestMade = true;
					readRequest.exception = null;
					readRequest.buf = b;
					readRequest.len = length;
					readRequest.offset = offset;
					readRequest.requestData = true;
					readRequest.validData = false;
					readRequest.notifyAll();
				}

				if (requestMade && readRequest.validData) {
					/* Request fulfilled, so return data */
					readRequest.validData = false;
					if (readRequest.exception != null)
						throw readRequest.exception;
					if (readRequest.actualLength == -1)
						eofSeen = true;
					return readRequest.actualLength;
				}

				try {
					readRequest.wait();
				} catch (InterruptedException e) {
					// do nothing
				}
			}
		}
	}

	/**
	 * Reads from System.in directly in a separate thread.
	 */
	public void run() {
		boolean doRead;

		while (true) {
			if (Thread.interrupted()) {
				break;
			}
			synchronized (readRequest) {
				doRead = readRequest.requestData && !readRequest.validData;
				if (!doRead) {
					try {
						readRequest.wait();
					} catch (InterruptedException e) {
						break;
					}
				}
			}
			if (doRead) {
				// drop out of synchronized block, because we don't want to
				// block main thread's
				// close() while we are blocked on stdin.read().
				// readRequest.validData and
				// readRequest.requestData keep us out of trouble here
				try {
					readRequest.actualLength = stdin.read(readRequest.buf, readRequest.offset, readRequest.len);
				} catch (IOException e1) {
					readRequest.exception = e1;
				}
				synchronized (readRequest) {
					readRequest.validData = true;
					readRequest.requestData = false;
					readRequest.notifyAll();
				}
			}
		}
		
		// reset starting values, in case another Interp is created.
		stdin = null;
		readThread = null;
		disposeLatch.countDown();
	}
}
