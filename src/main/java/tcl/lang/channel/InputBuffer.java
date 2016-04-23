package tcl.lang.channel;

import java.io.FilterInputStream;
import java.io.IOException;

import tcl.lang.TclIO;

/**
 * Implements a resizeable buffer as an InputStream for Tcl Channels. It is also
 * responsible for non-blocking reads.
 * 
 * @author Dan Bodoh
 * 
 */
class InputBuffer extends FilterInputStream {
	/**
	 * Contains the buffered bytes
	 */
	private byte[] buffer = null;

	/**
	 * The last buffer size change requested
	 */
	private int requestedBufferSize;
	/**
	 * TclIO.BUFF_FULL, TclIO.BUFF_NONE or TclIO.BUFF_LINE
	 */
	private volatile int buffering;
	/**
	 * Next character position to read from buffer
	 */
	private int position = 0;
	/**
	 * Last character in buffer + 1
	 */
	private int limit = 0;

	/**
	 * Set to true when end of file is encountered
	 */
	boolean eofSeen = false;
	/**
	 * Character that indicates end of line, for TclIO.BUFF_LINE
	 */
	private static final int eolChar = TclIO.TRANS_PLATFORM == TclIO.TRANS_CR ? '\r' : '\n';
	/**
	 * Set to true for blocking mode, false for non-blocking
	 */
	private volatile boolean blockingMode;
	/**
	 * true if the last read was terminated early because it would have blocked
	 */
	private boolean lastReadWouldHaveBlocked = false;

	/**
	 * If true, a refill() operation is currently being executed
	 */
	private volatile boolean refillInProgress = false;
	/**
	 * Underlying stream, as an EofInputFilter
	 */
	private EofInputFilter eofInputFilter = null;
	/**
	 * Set to true when a refill is requested
	 */
	boolean requestRefill = false;
	/**
	 * This thread refills the buffer
	 */
	Refiller refiller = null;
	/**
	 * Set to true when refiller should stop
	 */
	boolean closed = false;

	/**
	 * Construct a new InputBuffer in blocking mode
	 * 
	 * @param in
	 *            underlying InputStream for the buffer
	 * @param size
	 *            initial size, in bytes. Can be 0.
	 * @param buffering
	 *            Buffering mode: TclIO.BUFF_NONE, TclIO.BUFF_LINE or
	 *            TclIO.BUFF_FULL
	 * @param blockingMode
	 *            Set to true for blocking input, false for non-blocking input
	 * @param channel
	 *            Channel which contains this InputBuffer
	 */
	InputBuffer(EofInputFilter in, int size, int buffering, boolean blockingMode, Channel channel) {
		super(in);
		eofInputFilter = in;
		setBuffering(buffering);
		setBlockingMode(true);
		setBufferSize(size);
		setBlockingMode(blockingMode);
		refiller = new Refiller();
		resizeBuffer();
		refiller.setDaemon(true);
		refiller.setName("InputBuffer Refiller: " + channel.getChanName());
		refiller.start();
	}

	/**
	 * @param blockingMode
	 *            Set to true for blocking operation, or false for non-blocking
	 */
	void setBlockingMode(boolean blockingMode) {
		this.blockingMode = blockingMode;
	}

	/**
	 * Change the buffer size if there is no refill in progress and if it is
	 * actually empty.
	 */
	private void resizeBuffer() {
		synchronized (getRefillerNotifier()) {
			if (refillInProgress)
				return;
			// must grab at least one byte, so we can detect EOF
			int size = buffering == TclIO.BUFF_NONE ? 1 : requestedBufferSize;
			if (remaining() > 0 || (buffer != null && buffer.length == size))
				return;
			buffer = new byte[size];
			limit = 0;
			position = 0;
		}
	}

	/**
	 * Request that the buffer be resized. The request may not be honored
	 * immediately.
	 * 
	 * @param size
	 *            new requested size. If size < 0 it is set to 0.
	 */
	void setBufferSize(int size) {
		if (refiller == null)
			requestedBufferSize = size;
		else
			synchronized (getRefillerNotifier()) {
				requestedBufferSize = size;
				resizeBuffer();
			}
	}

	/**
	 * Set the buffering mode
	 * 
	 * @param buffering
	 *            TclIO.BUFF_FULL, TclIO.BUFF_NONE or TclIO.BUFF_LINE
	 */
	void setBuffering(int buffering) {
		if (refiller == null) {
			this.buffering = buffering;
		} else {
			synchronized (getRefillerNotifier()) {
				this.buffering = buffering;
				resizeBuffer();
			}
		}
	}

	/**
	 * Throw away any buffered data and reset internal state
	 */
	void seekReset() {
		synchronized (getRefillerNotifier()) {
			position = 0;
			limit = 0;
			cancelEof();
		}
	}

	/**
	 * @return true if the channel is in non-blocking mode, and the last read
	 *         was terminated early because it would have blocked
	 */
	boolean lastReadWouldHaveBlocked() {
		return lastReadWouldHaveBlocked;
	}

	/**
	 * @return true if the EOF was seen
	 */
	boolean eof() {
		return eofSeen;
	}

	/**
	 * Close the inputBuffer and kill the refiller
	 */
	@Override
	public void close() throws IOException {
		closed = true;
		if (refiller != null) {
			refiller.interrupt();
		}
		super.close();
	}

	/**
	 * Sets the current eof state to false
	 * 
	 * @param eof
	 *            End of file indicator is set to this state
	 */
	void cancelEof() {
		this.eofSeen = false;
	}

	/**
	 * @return number of bytes currently in the buffer
	 */
	final int remaining() {
		synchronized (getRefillerNotifier()) {
			return limit - position;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.FilterInputStream#available()
	 */
	@Override
	public int available() throws IOException {
		if (isRefillInProgress()) return 0;
		return remaining() + eofInputFilter.available();
	}

	/**
	 * @return the object on which refills are requested and acknowledges
	 */
	final Object getRefillerNotifier() {
		return refiller;
	}

	/**
	 * @return true if a refill is currently in progress
	 * @throws IOException
	 *             if refill is not in progress, and an exception was detected
	 */
	final boolean isRefillInProgress() throws IOException {
		synchronized (getRefillerNotifier()) {
			if (requestRefill || refillInProgress) {
				return true;
			} else {
				refiller.throwIOExceptionIfCaught();
				return false;
			}
		}
	}

	/**
	 * Request that the InputBuffer be refiiled.
	 * 
	 * @param wait
	 *            if true, this method does not return until the refill
	 *            operation is complete
	 * @throws IOException
	 */
	void requestRefill(boolean wait) throws IOException {
		synchronized (getRefillerNotifier()) {
			requestRefill = true;
			refiller.notifyAll();
			if (wait) {
				while (isRefillInProgress()) {
					try {
						getRefillerNotifier().wait();
					} catch (InterruptedException e) {
					}
				}
			}
		}
	}

	/**
	 * Reads one byte from buffer or underlying stream. Does not honor
	 * blockingMode; will block of no bytes are in the buffer
	 * 
	 * @return next byte, or -1 if none available
	 * 
	 * @see java.io.FilterInputStream#read()
	 */
	@Override
	public int read() throws IOException {
		synchronized (getRefillerNotifier()) {
			while (isRefillInProgress()) {
				try {
					getRefillerNotifier().wait();
				} catch (InterruptedException e) {
				}
			}
			if (remaining() == 0)
				requestRefill(true);
			else
				refiller.throwIOExceptionIfCaught();
			if (eofSeen)
				return -1;
			if (buffer.length == 0) {
				int c = super.read();
				if (c == -1)
					eofSeen = true;
				return c;
			}

			return buffer[position++];
		}
	}

	/**
	 * Read bytes from buffer or underlying stream.
	 * 
	 * @param b
	 *            buffer to read into
	 * @param off
	 *            offset where bytes should go into buffer
	 * @param len
	 *            maximum number of bytes to read
	 * @return number of bytes read, or -1 at end-of-file. Will return 0 if in
	 *         non-blocking mode and no bytes are available in the buffer.
	 * 
	 * @see java.io.FilterInputStream#read(byte[], int, int)
	 */
	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		synchronized (getRefillerNotifier()) {
			lastReadWouldHaveBlocked = false;

			if (refillInProgress) {
				lastReadWouldHaveBlocked = true;
				return 0;
			}

			if (eofSeen)
				return -1;

			refiller.throwIOExceptionIfCaught();

			if (remaining() == 0) {

				/*
				 * Can we satisfy the request, at least partially, from anything
				 * available in the underlying stream? Don't try for line
				 * buffering, because we don't want to accidently read past EOL.
				 * And don't request any more than the Tcl buffer size, because
				 * that would confuse test cases
				 */
				if (buffering != TclIO.BUFF_LINE && super.available() > 0) {

					int directRequestSize = Math.min(len, super.available());
					if (requestedBufferSize > 0 && directRequestSize > requestedBufferSize)
						directRequestSize = requestedBufferSize;

					int cnt = super.read(b, off, directRequestSize);
					if (cnt == -1)
						eofSeen = true;
					lastReadWouldHaveBlocked = !blockingMode && (cnt < len) && !eofSeen;
					return cnt;

				} else {
					/*
					 * couldn't get any bytes directly from stream without
					 * blocking
					 */

					if (blockingMode) {
						requestRefill(true);
						/*
						 * fall through when refill completes to copy data out
						 * of the buffer
						 */
					} else {
						/* run refill() in the background for non-blocking mode */
						requestRefill(false);
						lastReadWouldHaveBlocked = true;
						return 0;
					}
				}

			}

			if (remaining() == 0 && eofSeen)
				return -1;
			/* Copy data out of the buffer */
			int cnt = Math.min(len, remaining());
			System.arraycopy(buffer, position, b, off, cnt);
			position += cnt;
			lastReadWouldHaveBlocked = (!blockingMode && cnt < len && !eofSeen);
			return cnt;
		}
	}

	/**
	 * Runs refill in the background
	 */
	private class Refiller extends Thread {
		IOException ioException = null;

		/**
		 * @throws IOException
		 *             if there was an exception caught during refill
		 */
		void throwIOExceptionIfCaught() throws IOException {
			synchronized (getRefillerNotifier()) {
				if (ioException != null) {
					IOException e = ioException;
					ioException = null;
					throw e;
				}
			}
		}

		/**
		 * Refill the buffer from the underlying input stream. Any data in the
		 * buffer is lost.
		 * 
		 * @throws IOException
		 */
		private void refill() throws IOException {
			if (eofSeen)
				return;
			if (remaining() > 0)
				return; // perhaps it was refilled in the background?

			if (buffering == TclIO.BUFF_FULL || buffering == TclIO.BUFF_NONE) {
				/*
				 * Get as many bytes as available in the underlying stream, up
				 * to buffer.length. But we must always get at least one
				 * character.
				 */
				int readSize = Math.min(buffer.length, eofInputFilter.available());
				if (readSize < 1)
					readSize = 1;

				// keep the blocking read out of the synchronized section. The
				// buffer is protected
				// by refillInProgress
				int cnt = eofInputFilter.read(buffer, 0, readSize);
				synchronized (getRefillerNotifier()) {
					if (cnt == -1) {
						eofSeen = true;
						position = 0;
						limit = 0;
						return;
					} else {
						position = 0;
						limit = cnt;
						return;
					}
				}
			} else {
				/* line buffering, look for first eolChar */
				synchronized (getRefillerNotifier()) {
					limit = 0;
					position = 0;
				}
				while (true) {
					/* don't put blocking read in synchronized section */
					int c = eofInputFilter.read();
					synchronized (getRefillerNotifier()) {
						if (c == -1) {
							if (limit == 0)
								eofSeen = true;
							return;
						}
						buffer[limit++] = (byte) (c & 0xFF);
						if (c == eolChar || limit >= buffer.length) {
							return;
						}
					}
				}
			}
		}

		@Override
		public void run() {
			ioException = null;	
			
			while (!closed) {
				synchronized (getRefillerNotifier()) {
					while (!closed && !requestRefill) {
						try {
							getRefillerNotifier().wait();
						} catch (InterruptedException e) {
							return;
						}
					}
					ioException = null;
					resizeBuffer();
					refillInProgress = true;
				}

				try {
					refill();
				} catch (IOException e) {
					synchronized (getRefillerNotifier()) {
						ioException = e;
					}
				}
				synchronized (getRefillerNotifier()) {
					refillInProgress = false;
					requestRefill = false;
					getRefillerNotifier().notifyAll();
				}
			}
		}
	}
}
