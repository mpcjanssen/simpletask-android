/*
 * ReadInputStreamChannel.java
 *
 */
package tcl.lang.channel;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import tcl.lang.Interp;
import tcl.lang.TclIO;

/**
 * The ReadInputStreamChannel class is a bridge between existing Java
 * InputStream objects and Tcl channels.
 **/

public class ReadInputStreamChannel extends Channel {

	InputStream inStream;

	/**
	 * Constructor - creates a new ReadInputStreamChannel object that will read
	 * from the passed in InputStream.
	 **/

	public ReadInputStreamChannel(Interp interp, InputStream in_stream) {
		this.mode = TclIO.RDONLY;
		this.inStream = in_stream;
	}

	String getChanType() {
		return "ReadInputStream";
	}

	protected InputStream getInputStream() throws IOException {
		return inStream;
	}

	protected OutputStream getOutputStream() throws IOException {
		throw new RuntimeException("should never be called");
	}

	@Override
	void implClose() throws IOException {
		// caller must close the input stream
	}
}
