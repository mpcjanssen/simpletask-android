package tcl.lang.channel;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * This java.io.FilterInputStream subclass looks for a the Channel's specified
 * EOF character to signal end of file, or passes data through if there is no
 * specified EOF character. It will not propagate close() to the underlying
 * stream - that is left to the Channel subclass to handle.
 * 
 * @author Dan Bodoh
 * 
 */
class EofInputFilter extends FilterInputStream {

	/**
	 * Channel's input eof character, set to 0 if none
	 */
	private byte eofchar;
	/**
	 * True if end of file was seen on a previous read
	 */
	private boolean eofSeen = false;

	/**
	 * Set to true if we actually encounter the defined EOF char
	 */
	private boolean sawEofChar = false;

	/**
	 * The actual eof character that we saw, because it might have changed
	 */
	private int eofCharacterThatWasSeen = 0;

	/**
	 * Create a filter with no eof character
	 * 
	 * @param in
	 *            input stream to filter
	 */
	EofInputFilter(InputStream in) {
		super(in);
		eofchar = 0;
	}

	/**
	 * Create a EofInputFilter
	 * 
	 * @param in
	 *            input stream to filter
	 * @param eofchar
	 *            end of file character, or 0 for none
	 */
	EofInputFilter(InputStream in, byte eofchar) {
		super(in);
		this.eofchar = eofchar;
	}

	/**
	 * Reset any internal state after a seek was performed
	 */
	void seekReset() {
		cancelEof();
		sawEofChar = false;
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
	 * Set the input end-of-file character
	 * 
	 * @param eofChar
	 *            new input end of file character, or 0 for no eof character
	 */
	void setEofChar(byte eofChar) {
		if (eofChar != this.eofchar)
			cancelEof();
		this.eofchar = eofChar;
	}

	/**
	 * @return true if we actually read the EOF character, indicating that we
	 *         read read one too many bytes from the stream
	 */
	boolean sawEofChar() {
		return sawEofChar;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.FilterInputStream#available()
	 */
	@Override
	public int available() throws IOException {
		int avail = 0;
		try {
			avail = in.available();
		} catch (IOException e) {
			avail = 0;
		}
		return avail + (sawEofChar ? 1 : 0);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.FilterInputStream#read()
	 */
	@Override
	public int read() throws IOException {
		if (eofSeen)
			return -1;

		if (sawEofChar) {
			// eof was apparently cancelled
			// eof character was changed after it was encountered, so return it
			sawEofChar = false;
			return eofCharacterThatWasSeen;
		}

		int c = super.read();
		if (c == -1) {
			eofSeen = true;
			return -1;
		}
		if (eofchar != 0 && c == eofchar) {
			eofSeen = true;
			sawEofChar = true;
			eofCharacterThatWasSeen = c;
			return -1;
		}
		return c;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.FilterInputStream#read(byte[], int, int)
	 */
	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		if (eofSeen)
			return -1;

		if (sawEofChar && len > 0) {
			// eof was apparently cancelled
			// eof character was changed after it was encountered, so return it
			b[off] = (byte) (eofCharacterThatWasSeen & 0xff);
			sawEofChar = false;
			return 1;
		}

		if (eofchar == 0)
			return super.read(b, off, len);

		/*
		 * Otherwise, get one byte at a time so we don't pass end of 'file' from
		 * stream
		 */
		int cnt = 0;
		while (cnt < len) {
			int c = read();
			if (c == -1)
				break;
			b[off + cnt] = (byte) (c & 0xFF);
			++cnt;
		}
		return cnt;
	}

	/**
	 * Will not propagate close() to the lower level stream. This is left to the
	 * responsibility of the Channel subclass.
	 * 
	 * @see java.io.FilterInputStream#close()
	 */
	@Override
	public void close() throws IOException {

	}

}
