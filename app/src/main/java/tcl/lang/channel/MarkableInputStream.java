package tcl.lang.channel;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * An InputStream that supports mark(), with an unlimited readlimit. Also
 * supports unread() to push bytes back into the sream.
 * 
 */
class MarkableInputStream extends FilterInputStream {
	/**
	 * Set to true if a mark is set in the stream
	 */
	protected boolean marked = false;
	/**
	 * Buffer to store data after mark() or unread()
	 */
	private byte[] buf = new byte[0];
	/**
	 * index of next character to read from buf[]
	 */
	int readPos = 0;
	/**
	 * index of next character to write to buf
	 */
	int writePos = 0;
	/**
	 * Index of mark in the buffer, if marked==true
	 */
	int markPos = 0;
	/**
	 * Buffer grows in increments of this
	 */
	final static int GrowSize = 512;

	/**
	 * Create a new MarkableStream
	 * 
	 * @param in
	 *            InputStream that provides bytes for MarkableStream
	 */
	public MarkableInputStream(InputStream in) {
		super(in);
	}

	/**
	 * Save bytes into the buffer, allocating space as necessary. Sets readPos
	 * and writePos to the end of the new data.
	 * 
	 * @param b
	 *            byte array containing bytes to save
	 * @param off
	 *            offset of data in b
	 * @param len
	 *            length of data in b
	 */
	private void saveBytes(byte[] b, int off, int len) {
		int additionalSpaceRequired = len - (buf.length - writePos);
		if (additionalSpaceRequired > 0) {
			/* Round up size by GrowSize */
			int newSize = (((additionalSpaceRequired / GrowSize) + 1) * GrowSize) + buf.length;
			byte[] newBuf = new byte[newSize];
			System.arraycopy(buf, 0, newBuf, 0, writePos);
			buf = newBuf;
		}
		System.arraycopy(b, off, buf, writePos, len);
		writePos += len;
		readPos += len;
	}

	/**
	 * Resize the buffer to minimum size, if it is not being used
	 */
	private void trimBuf() {
		if (!marked && readPos == writePos && buf.length > 0) {
			buf = new byte[0];
			readPos = 0;
			writePos = 0;
		}
	}

	/**
	 * Sets the mark at the current stream position, or removes this stream's
	 * mark.
	 * 
	 * @param readlimit
	 *            if <= 0, removes the current mark. Otherwise, set the mark and
	 *            ignore readlimit. The internal buffer grows as needed.
	 * 
	 * @see java.io.FilterInputStream#mark(int)
	 */
	@Override
	public synchronized void mark(int readlimit) {
		marked = (readlimit > 0);
		trimBuf();
		markPos = readPos;
	}

	/**
	 * Reset internal state of this stream
	 */
	public void seekReset() {
		readPos = 0;
		writePos = 0;
		mark(0);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.FilterInputStream#markSupported()
	 */
	@Override
	public boolean markSupported() {
		return true;
	}

	/**
	 * Put a byte back into the stream
	 * 
	 * @param c
	 *            byte to put back into the stream
	 */
	public void unread(int c) {
		if (c != -1) {
			byte[] b = new byte[1];
			b[0] = (byte) (c & 0xff);
			if (!marked)
				saveBytes(b, 0, 1);
			--readPos;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.FilterInputStream#read()
	 */
	@Override
	public int read() throws IOException {
		if (readPos == writePos) {
			int c = super.read();
			if (marked && c != -1) {
				byte[] b = new byte[1];
				b[0] = (byte) (c & 0xff);
				saveBytes(b, 0, 1);
			}
			return c;
		} else {
			byte c = buf[readPos++];
			trimBuf();
			return c;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.FilterInputStream#read(byte[], int, int)
	 */
	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int nBytes = 0;
		int cnt;
		int remaining = writePos - readPos;
		if (remaining > 0) {
			cnt = Math.min(remaining, len);
			System.arraycopy(buf, readPos, b, off, cnt);
			readPos += cnt;
			nBytes = cnt;
			off += cnt;
			len -= cnt;
		}
		trimBuf();
		if (len > 0) {
			cnt = super.read(b, off, len);
			if (cnt == -1 && nBytes == 0)
				nBytes = -1;
			else {
				if (writePos == readPos && marked)
					saveBytes(b, off, cnt);
				nBytes += cnt;
			}
		}
		return nBytes;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.FilterInputStream#reset()
	 */
	@Override
	public synchronized void reset() throws IOException {
		if (marked) {
			readPos = markPos;
		} else {
			throw new IOException("Stream was not marked prior to reset()");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.FilterInputStream#available()
	 */
	@Override
	public int available() throws IOException {
		return (writePos - readPos) + super.available();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.FilterInputStream#close()
	 */
	@Override
	public void close() throws IOException {
		seekReset();
		super.close();
	}

}
