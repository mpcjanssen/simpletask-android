package tcl.lang.channel;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ConcurrentLinkedQueue;

import tcl.lang.TclIO;

/**
 * This class implements non-blocking output for Tcl Channels
 * 
 * @author Dan Bodoh
 * 
 */
class NonBlockingOutputStream extends FilterOutputStream implements Runnable {
	/**
	 * True if channel is in blocking mode, false for non-blocking
	 */
	private boolean blocking;
	/**
	 * Queue of writes to occur in background
	 */
	private ConcurrentLinkedQueue<Transaction> queue;
	/**
	 * Thread that empties the queue
	 */
	private Thread bkgndWriter = null;
	/**
	 * Notifier between main thread and bkgndWriter thread
	 */
	private Object notifier = new Object();
	/**
	 * The channel that this stream serves
	 */
	private Channel channel = null;
	/**
	 * Exception from writer thread
	 */
	private volatile IOException ioException = null;
	/**
	 * Set to true when the channel is closed
	 */
	private boolean closed = false;

	/**
	 * Create a new non-blocking output stream
	 * 
	 * @param out
	 *            the underlying OutputStream to write to
	 * @param blocking
	 *            if true, perform blocking writes. If false, perform
	 *            non-blocking writes
	 * @param sync
	 *            Channel implementation specific Sync object, can be null
	 */
	NonBlockingOutputStream(OutputStream out, boolean blocking, Channel channel) {
		super(out);
		setBlocking(blocking);
		this.channel = channel;
		queue = new ConcurrentLinkedQueue<>();
		
		/* 
		 * The bkgndWriter thread reads the queue in both blocking and non-blocking mode.
		 * Using the thread all the time avoids race conditions when switching between blocked
		 * and non-blocked mode on the channel.
		 */
		bkgndWriter = new Thread(this);
		bkgndWriter.setDaemon(true);
		bkgndWriter.setName("NonBlockingOutputStream: " + channel.getChanName());
		bkgndWriter.start();
	}

	/**
	 * @param blocking
	 *            if true, perform blocking writes. If false, perform
	 *            non-blocking writes
	 */
	void setBlocking(boolean blocking) {
		this.blocking = blocking;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.FilterOutputStream#flush()
	 */
	@Override
	public void flush() throws IOException {
		synchronized (notifier) {
			checkClosed();
			queue.offer(new Transaction(Transaction.Flush));
			notifier.notifyAll();
		}
		if (blocking)
			waitForEmptyQueue();
		throwExceptionIfCaught();
	}

	/**
	 * This write() will assume the caller will overwrite the buffer, so it will
	 * make a copy if non-blocking writes are in effect. Use
	 * writeAssumingExclusiveBufferUse(byte[], int, int) to avoid the byte array
	 * copy.
	 * 
	 * @param b
	 *            buffer containing data to write
	 * @param off
	 *            offset of first byte in b to write
	 * @param len
	 *            number of bytes to write
	 * @throws IOException
	 * 
	 * @see java.io.FilterOutputStream#write(byte[], int, int)
	 */
	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		synchronized (notifier) {
			checkClosed();
			if (blocking) {
				queue.offer(new Transaction(b, off, len));
			} else {
				byte[] copy = new byte[len];
				System.arraycopy(b, off, copy, 0, len);
				queue.offer(new Transaction(copy, 0, len));
			}
			notifier.notifyAll();
		}
		if (blocking)
			waitForEmptyQueue();
		throwExceptionIfCaught();
	}

	/**
	 * Similar to write(byte [], int, int), although it assumes that the caller
	 * will not write to the b after this method returns. Use this method for
	 * optimized writes.
	 * 
	 * @param b
	 *            buffer containing data to write
	 * @param off
	 *            offset of first byte in b to write
	 * @param len
	 *            number of bytes to write
	 * @throws IOException
	 */
	void writeAssumingExclusiveBufferUse(byte[] b, int off, int len) throws IOException {
		synchronized (notifier) {
			checkClosed();
			queue.offer(new Transaction(b, off, len));
			notifier.notifyAll();
		}
		if (blocking)
			waitForEmptyQueue();
		throwExceptionIfCaught();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.FilterOutputStream#write(int)
	 */
	@Override
	public void write(int b) throws IOException {
		byte[] buf = new byte[1];
		buf[0] = (byte) (b & 0xff);
		writeAssumingExclusiveBufferUse(buf, 0, 1);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.FilterOutputStream#close()
	 */
	@Override
	public void close() throws IOException {
		synchronized (notifier) {
			checkClosed();
			queue.offer(new Transaction(Transaction.Close));
			notifier.notifyAll();
		}
		if (blocking) {
			waitForEmptyQueue();
		}
		throwExceptionIfCaught();
	}

	private void checkClosed() throws IOException {
		if (closed)
			throw new IOException("Stream is already closed");
	}

	/**
	 * If an exception was caught in the bkndWriter thread, throw it now
	 * 
	 * @throws IOException
	 */
	private void throwExceptionIfCaught() throws IOException {
		if (ioException != null) {
			IOException e = ioException;
			ioException = null;
			throw e;
		}
	}

	/**
	 * @return true if the queue is empty, false otherwise
	 */
	boolean isQueueEmpty() {
		return (queue.peek() == null);
	}

	/**
	 * Wait for the write queue to be empty
	 */
	void waitForEmptyQueue() {
		while (true) {
			synchronized (notifier) {
				if (isQueueEmpty()) return;
				try {
					notifier.wait();
				} catch (InterruptedException e) {
					return;
				}
			}
		}
	}

	/**
	 * Runs as the background writer thread; writes data placed in the queue
	 * in both blocking and non-blocking mode.
	 * 
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		while (true) {
			Transaction transaction;
			synchronized (notifier) {
				// don't remove it from the queue until we are done 
				// performing transaction
				transaction = queue.peek();

				if (transaction == null) { 
					try {
						notifier.wait();
					} catch (InterruptedException e) {
						closed = true;
						return;
					}
					continue;
				}
			}

			try {
				transaction.perform();
			} catch (IOException e) {
				ioException = e;
			}
			synchronized (notifier) {
				queue.poll();  // finally, remove it from the queue
				if (transaction.type == Transaction.Close) {
					/* shut everything down after a Close is complete */
					closed = true;
					channel = null;
					queue.clear();
					notifier.notifyAll();
					return;
				}
				if (isQueueEmpty())
					notifier.notifyAll();
			}
		}
	}

	/**
	 * Encapsulates a write, flush or close request to the underlying stream
	 */
	private class Transaction {
		/**
		 * Buffer to write
		 */
		byte[] b;
		/**
		 * Offset of first byte in buffer to write
		 */
		int off;
		/**
		 * Number of bytes to write
		 */
		int len;

		/**
		 * Transaction type (Write, Flush or Close)
		 */
		int type;
		/**
		 * Transaction type indicating a write
		 */
		final static int Write = 0;
		/**
		 * Transaction type indicating a flush
		 */
		final static int Flush = 1;
		/**
		 * Transaction type indication a close
		 */
		final static int Close = 2;

		/**
		 * Create a new transaction of the given type.
		 * 
		 * @param type
		 *            Either Write, Flush or Close. If Write, the data to write
		 *            is undefined.
		 */
		Transaction(int type) {
			this.type = type;
		}

		/**
		 * Create a Write Transaction
		 * 
		 * @param b
		 *            buffer to write
		 * @param off
		 *            index of first byte in buffer to write
		 * @param len
		 *            number of bytes to write
		 */
		Transaction(byte[] b, int off, int len) {
			this.b = b;
			this.off = off;
			this.len = len;
			this.type = Write;
		}

		/**
		 * Execute this transaction on the underlying stream, out.
		 * 
		 * @throws IOException
		 */
		void perform() throws IOException {
			if (channel instanceof SeekableChannel && (channel.mode & TclIO.APPEND)!=0) {
				((SeekableChannel)channel).prepareForAppendWrite();
			}
			switch (type) {
			case Write:
				out.write(b, off, len);
				break;
			case Flush:
				out.flush();
				channel.sync();
				break;
			case Close:
				out.close();
				channel.implClose();
				break;
			}
		}
	}

}
