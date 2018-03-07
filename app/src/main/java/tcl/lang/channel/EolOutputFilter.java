package tcl.lang.channel;

import java.io.FilterWriter;
import java.io.IOException;
import java.io.Writer;

import tcl.lang.TclIO;

/**
 * This FilterWriter emits the Channel's configured EOL translation characters
 * when '\n' is seen on the input.
 * 
 * @author Dan Bodoh
 * 
 */
class EolOutputFilter extends FilterWriter {
	private int translation;

	/**
	 * Create a new EolOutputFilter
	 * 
	 * @param out
	 *            The underlying Writer that receives characters from this
	 *            stream
	 * @param translation
	 *            TclIO.TRANS_LF, TclIO.TRANS_BINARY, TclIO.TRANS_CR or
	 *            TclIO.TRANS_CRLF
	 */
	EolOutputFilter(Writer out, int translation) {
		super(out);
		setTranslation(translation);
	}

	/**
	 * @param translation
	 *            TclIO.TRANS_LF, TclIO.TRANS_BINARY, TclIO.TRANS_CR or
	 *            TclIO.TRANS_CRLF
	 */
	void setTranslation(int translation) {
		this.translation = translation;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.FilterWriter#write(char[], int, int)
	 */
	@Override
	public void write(char[] cbuf, int off, int len) throws IOException {

		/* No special handling required for LF or Binary mode */
		if (translation == TclIO.TRANS_LF || translation == TclIO.TRANS_BINARY) {
			out.write(cbuf, off, len);
			return;
		}

		if (translation == TclIO.TRANS_CR) {
			for (int i = off; i < off + len; i++) {
				if (cbuf[i] == '\n')
					cbuf[i] = '\r';
			}
			out.write(cbuf, off, len);
			return;
		}

		if (translation == TclIO.TRANS_CRLF) {
			int eolCnt = 0;
			for (int i = off; i < off + len; i++) {
				if (cbuf[i] == '\n')
					++eolCnt;
			}
			if (eolCnt == 0)
				out.write(cbuf, off, len);
			else {
				char[] tcbuf = new char[len + eolCnt];
				for (int i = off, j = 0; i < off + len; i++, j++) {
					if (cbuf[i] == '\n') {
						tcbuf[j++] = '\r';
					}
					tcbuf[j] = cbuf[i];
				}
				out.write(tcbuf, 0, len + eolCnt);
			}
		}
	}
}
