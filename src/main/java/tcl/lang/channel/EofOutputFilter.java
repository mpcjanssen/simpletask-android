package tcl.lang.channel;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Emits the requested end-of-file character when the stream is closed, and
 * prevents the close() from propagating to the underlying stream.
 * 
 * @author Dan Bodoh
 * 
 */
 class EofOutputFilter extends FilterOutputStream {
	private byte eofChar;

	/**
	 * Create a new EofOutputFilter, and set the end of file character
	 * 
	 * @param out
	 *            the underlying OutputStream that will accept data from this
	 *            FilterOutputStream
	 * @param eofChar
	 *            the end-of-file character; if 0, nothing is emitted at close()
	 */
	EofOutputFilter(OutputStream out, byte eofChar) {
		super(out);
		setEofChar(eofChar);
	}

	/**
	 * Set the output end-of-file character
	 * 
	 * @param eofChar
	 *            new output end of file character; if 0, nothing is emitted at
	 *            close()
	 */
	void setEofChar(byte eofChar) {
		this.eofChar = eofChar;
	}

	/**
	 * Emit the end-of-file character, but don't propagate close() to underlying
	 * stream. The Channel implementation closes the underlying stream itself
	 * 
	 * @see java.io.FilterOutputStream#close()
	 */
	@Override
	public void close() throws IOException {
		if (eofChar != 0)
			out.write(eofChar);
	}

	/* (non-Javadoc)
	 * @see java.io.FilterOutputStream#write(byte[], int, int)
	 */
	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		out.write(b, off, len);
	}

}
