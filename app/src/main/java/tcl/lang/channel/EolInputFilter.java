package tcl.lang.channel;

import java.io.FilterReader;
import java.io.IOException;
import tcl.lang.TclIO;

/**
 * This java.io.FilterReader subclass translates the end-of-line characters
 * specified by the Channel's translation property into '\n'
 * 
 * @author Dan Bodoh
 * 
 */
class EolInputFilter extends FilterReader {
	/**
	 * readline() return value on end-of-file
	 */
	final static int EOF = -1;
	/**
	 * readline() return value if blocking is true and 
	 * a complete line was not available
	 */
	final static int INCOMPLETE_LINE = -2;
	/**
	 * readLine() return value if the stringbuffer contains
	 * a valid line.
	 */
	final static int COMPLETE_LINE = 0;
	
	/**
	 * The filter's translation property - one of TclIO.TRANS_*
	 */
	private int translation;

	/**
	 * Set to true when EOF is seen
	 */
	private boolean eofSeen = false;

	/**
	 * Underlying InputStream as a UnicodeDecoder
	 */
	private UnicodeDecoder unicodeDecoder = null;


	/**
	 * Create an EolInputFilter
	 * 
	 * @param in
	 * @param translation
	 *            TclIO.TRANS_* translation value
	 */
	EolInputFilter(UnicodeDecoder in, int translation) {
		super(in);
		setTranslation(translation);
		unicodeDecoder = in;
	}

	/**
	 * Set the filter's EOL translation
	 * 
	 * @param translation
	 *            one of the TclIO.TRANS_* values
	 */
	void setTranslation(int translation) {
		this.translation = translation;
	}

	/**
	 * Reset any internal state after a seek was performed
	 */
	void seekReset() {
		eofSeen = false;
	}

	/**
	 * @return true if EOF was seen
	 */
	boolean eofSeen() {
		return eofSeen;
	}

	/**
	 * Read one line of input. The end of line characters are not returned.
	 * 
	 * @param sb
	 * 			  An empty stringbuffer that will contain the the line of
	 * 			 data, without EOL characters.
	 * @param block
	 *            if true, block for an entire line of input, possibly blocking
	 *            for input. If false, return an empty string if input would
	 *            block, and don't consume any input.
	 * 
	 * @return COMPLETE_LINE, INCOMPLETE_LINE or EOF
	 * @throws IOException
	 */
	int readLine(StringBuffer sb, boolean block) throws IOException {
		char[] cbuf = new char[1];
		boolean sawEol = false;
		/*
		 * Mark the stream in case we can't get a whole line due to blocking.
		 * The readlimit of 1024 is arbitrary, and is ignored by
		 * MarkableInputStream anyway, since its buffer grows as needed
		 */
		if (!block)
			unicodeDecoder.mark(1024);

		while (true) {
			int cnt = super.read(cbuf, 0, 1);
			if (cnt == -1) {
				eofSeen = true;
				break;
			}
			if (cnt == 0) {
				if (block)
					continue; // channel could have switched to non-blocking
				// during background refill
				else
					break;
			}
			int c1 = cbuf[0];

			if (isEolChar(c1)) {
				sawEol = true;
				break;
			}

			if (mightBeEol2CharSequence(c1)) {
				int c2;
				if (block) {
					c2 = unicodeDecoder.peek(false);
				} else {
					if (unicodeDecoder.available() > 0) {
						c2 = unicodeDecoder.peek(false);
					} else
						break; // can't get a character, so we don't know
				}

				if (isEol1CharSequence(c1, c2)) {
					// don't consume c2
					sawEol = true;
					break;
				}
				if (isEol2CharSequence(c1, c2)) {
					sawEol = true;
					// consume c2
					unicodeDecoder.peek(true);
					break;
				}
			}
			sb.append((char) c1);
		}

		if (eofSeen && sb.length() == 0)
			return EOF;

		if (block) {
			return COMPLETE_LINE;
		} else {
			/* non-blocking, don't return partial line */
			if (sawEol || eofSeen) {
				unicodeDecoder.mark(0); // cancel mark
				return COMPLETE_LINE;
			} else {
				unicodeDecoder.reset(); // read this substring again when more
										// data is available
				return INCOMPLETE_LINE;
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.FilterReader#read()
	 */
	@Override
	public int read() throws IOException {
		int c;

		c = super.read();
		if (c == -1) {
			eofSeen = true;
			return -1;
		}

		if (isEolChar(c)) {
			return '\n';
		}

		if (mightBeEol2CharSequence(c)) {
			int c2 = unicodeDecoder.peek(false);
			eofSeen = (c2 == -1);
			if (isEol1CharSequence(c, c2)) {
				return '\n';
			}
			if (isEol2CharSequence(c, c2)) {
				/* consume 2nd byte */
				unicodeDecoder.peek(true);
				return '\n';
			}
			/* wasn't eol return c */
		}

		return c;
	}

	/**
	 * @see java.io.FilterReader#read(char[], int, int)
	 */
	@Override
	public int read(char[] cbuf, int off, int len) throws IOException {

		int bytesRead = super.read(cbuf, off, len);

		if (bytesRead < 1 || translation == TclIO.TRANS_BINARY || translation == TclIO.TRANS_LF) {
			eofSeen = (bytesRead == -1);
			/* shortcut, nothing to do */
			return bytesRead;
		}

		/* Another shortcut */
		if (translation == TclIO.TRANS_CR) {
			for (int i = off; i < off + bytesRead; i++) {
				if (cbuf[i] == '\r')
					cbuf[i] = '\n';
			}
			return bytesRead;
		}

		/* Do cr-lf translation, or possibly just cr translation if TRANS_AUTO */
		int i = 0;
		int newCount = 0;
		boolean collapsedTwoCharEol = false;

		while (i < bytesRead - 1) {
			if (isEol1CharSequence(cbuf[i + off], cbuf[i + off + 1])) {
				cbuf[newCount + off] = '\n';
				++newCount;
				++i;
				continue;
			}
			if (isEol2CharSequence(cbuf[i + off], cbuf[i + off + 1])) {
				cbuf[newCount + off] = '\n';
				++newCount;
				i += 2;
				collapsedTwoCharEol = true;
				continue;
			}
			if (collapsedTwoCharEol) {
				/*
				 * If we collapsed \r\n to \n, even non-eol chars must be moved
				 * backwards in buffer
				 */
				cbuf[newCount + off] = cbuf[i + off];
			}
			++newCount;
			++i;
		}

		if (i >= bytesRead) {
			// last character was consumed
			return newCount;
		}

		// deal with possible look-ahead for last character in buffer
		int lastChar = cbuf[off + bytesRead - 1];
		if (isEolChar(lastChar)) {
			cbuf[newCount + off] = '\n';
			return ++newCount;
		}

		if (mightBeEol2CharSequence(lastChar)) {
			/* Get one more char just to see if this is a eol */
			int c2 = unicodeDecoder.peek(false);

			eofSeen = (c2 == -1);
			if (isEol1CharSequence(lastChar, c2)) {
				cbuf[newCount + off] = '\n';
				return ++newCount;
			}
			if (isEol2CharSequence(lastChar, c2)) {
				/* consume c2 */
				unicodeDecoder.peek(true);
				cbuf[newCount + off] = '\n';
				return ++newCount;
			}
		}
		cbuf[newCount + off] = (char) lastChar;
		return ++newCount;
	}

	/**
	 * @param c
	 *            character to test
	 * @return true if c is an EOL character on its own
	 */
	private final boolean isEolChar(int c) {
		switch (translation) {
		case TclIO.TRANS_BINARY:
		case TclIO.TRANS_LF:
		case TclIO.TRANS_AUTO:
			return (c == '\n');
		case TclIO.TRANS_CR:
			return (c == '\r');
		case TclIO.TRANS_CRLF:
			return false;
		default:
			return false;
		}
	}

	/**
	 * @param c
	 * @return true if c could be the start of a 2-character EOL sequence
	 */
	private final boolean mightBeEol2CharSequence(int c) {
		return (c == '\r' && (translation == TclIO.TRANS_AUTO || translation == TclIO.TRANS_CRLF));
	}

	/**
	 * @param c1
	 *            first character in sequence
	 * @param c2
	 *            second character in sequence
	 * @return true if c1 by itself constitutes a EOL character, and c2 does not
	 *         participate
	 */
	private final boolean isEol1CharSequence(int c1, int c2) {
		if (isEolChar(c1))
			return true;
		return (translation == TclIO.TRANS_AUTO && c1 == '\r' && c2 != '\n' && c2 != -1);
	}

	/**
	 * @param c1
	 *            first character in sequence
	 * @param c2
	 *            second character in sequence
	 * @return true if c1 and c2 together make up an eol sequence. Return false
	 *         if only c1, or the c1-c2 sequence does not constitute eol.
	 */
	private final boolean isEol2CharSequence(int c1, int c2) {
		switch (translation) {
		case TclIO.TRANS_BINARY:
		case TclIO.TRANS_LF:
		case TclIO.TRANS_CR:
			/* only need one character, not two */
			return false;
		case TclIO.TRANS_CRLF:
			return (c1 == '\r' && c2 == '\n');
		case TclIO.TRANS_AUTO:
			return (c1 == '\r' && (c2 == '\n' || c2 == -1));
		default:
			return false;
		}

	}
}
