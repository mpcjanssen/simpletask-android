package tcl.lang.channel;

import java.io.FilterOutputStream;
import java.io.IOException;
import tcl.lang.TclIO;

/**
 * Provides a resizeable output buffer for Tcl Channels. Unlike InputBuffer, it
 * is not responsible for non-blocking writes.  It also does a flush() after every
 * write to the underlying stream, to flush out any underlying buffers.
 * 
 * @author Dan Bodoh
 * 
 */
class OutputBuffer extends FilterOutputStream {
	/**
	 * Requested buffer size, which may not be honored immediately
	 */
	private int requestedSize = 0;
	/**
	 * One of TclIO.BUFF_NONE, TclIO.BUFF_LINE or TclIO.BUFF_FULL
	 */
	private int bufferingMode;
	/**
	 * The buffer that stores bytes
	 */
	private byte[] buffer = null;
	/**
	 * Position of next write to buffer
	 */
	private int position = 0;
	/**
	 * output stream, as a NonBlockingOutputStream
	 */
	private NonBlockingOutputStream nonBlockingOutputStream = null;
	/**
	 * number of bytes received by this OutputBuffer
	 */
	private long receivedByteCount = 0;
	
	/**
	 * Create a new OutputBuffer
	 * 
	 * @param out
	 *            the underlying stream to write to
	 * @param size
	 *            the initial size of the buffer
	 * @param bufferingMode
	 *            one of TclIO.BUFF_FULL, TclIO.BUFF_LINE, TclIO.BUFF_NONE
	 */
	OutputBuffer(NonBlockingOutputStream out, int size, int bufferingMode) {
		super(out);
		nonBlockingOutputStream = out;
		setBufferSize(size);
		setBuffering(bufferingMode);
	}

	/**
	 * @param bufferingMode
	 *            one of TclIO.BUFF_FULL, TclIO.BUFF_LINE, TclIO.BUFF_NONE
	 */
	void setBuffering(int bufferingMode) {
		this.bufferingMode = bufferingMode;
		resizeBuffer();
	}

	/**
	 * @param bufferSize
	 *            new buffer size, which may not be honored immediately
	 */
	void setBufferSize(int bufferSize) {
		this.requestedSize = bufferSize;
		resizeBuffer();
	}

	/**
	 * Set buffer to requestedSize, if it is empty.
	 * Always re-allocates buffer, even if size stays the same,
	 * because buffer gets passed down to nonBlockingOutputStream which
	 * may not use it immediately.
	 */
	private void resizeBuffer() {
		if (position != 0)
			return;
		int size = bufferingMode == TclIO.BUFF_NONE ? 0 : requestedSize;
		buffer = new byte[size];
	}

	/**
	 * @return number of bytes currently stored in the buffer
	 */
	int getBufferedByteCount() {
		return position;
	}

	/**
	 * @return number of bytes received through the write() methods
	 */
	long getReceivedByteCount() {
		return receivedByteCount;
		
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.FilterOutputStream#flush()
	 */
	@Override
	public void flush() throws IOException {
		if (position > 0) {
			nonBlockingOutputStream.writeAssumingExclusiveBufferUse(buffer, 0, position);
		}
		nonBlockingOutputStream.flush();
		position = 0;
		/* Reallocate the buffer and don't re-use it, because nonBlockingOutputStream might
		 * queue it up for a non-blocking write
		 */
		resizeBuffer();
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
		write(buf, 0, 1);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.FilterOutputStream#write(byte[], int, int)
	 */
	@Override
	public void write(byte[] b, int off, int len) throws IOException {

		receivedByteCount += len;
		
		// if there was a switch to BUFF_NONE,   flush out the buffer
		if (bufferingMode == TclIO.BUFF_NONE && buffer.length > 0) {
			flush();
		}

		if (buffer.length == 0) {
			/* Since we haven't copied this into the buffer, nonBlockingOutputStream may have to copy the data
			 * if this is not a blocking channel.  So use write(byte[], int, int).
			 */
			nonBlockingOutputStream.write(b, off, len);
			nonBlockingOutputStream.flush();
			return;
		}

		/* First, try to fill the buffer */
		int cnt = Math.min(len, buffer.length - position);
		System.arraycopy(b, off, buffer, position, cnt);
		position += cnt;
		len -= cnt;
		off += cnt;

		if (position == buffer.length)
			flush();

		/*
		 * How many buffer-sized chunks can we write directly, without having to
		 * write to the buffer?
		 */
		if (buffer.length > 0) {
			int nChunks = len / buffer.length;

			if (nChunks > 0) {
				cnt = nChunks * buffer.length;
				/* Since we are using caller's buffer, we can't assume that the caller won't re-use it,
				 * so use the write(byte[], int, int) which will possibly copy the buffer
				 */
				nonBlockingOutputStream.write(b, off, cnt);
				nonBlockingOutputStream.flush();
				len -= cnt;
				off += cnt;
			}
		}

		/* And transfer remainder to the buffer */
		System.arraycopy(b, off, buffer, position, len);
		position += len;
		if (position == buffer.length)
			flush();

		/*
		 * If in line buffering mode, flush buffer if any \n or \r seen. This
		 * replicates Tcl's strange behavior of flushing at end of buffer rather
		 * than at each EOL
		 */
		if (bufferingMode == TclIO.BUFF_LINE) {
			for (int i = position - 1; i >= 0; --i) {
				if (buffer[i] == '\n' || buffer[i] == '\r') {
					flush();
					break;
				}
			}
		}
	}
}
