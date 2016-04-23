package tcl.lang.channel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import tcl.lang.Interp;
import tcl.lang.TclByteArray;
import tcl.lang.TclIO;
import tcl.lang.TclObject;
import tcl.lang.TclString;
import tcl.lang.cmd.EncodingCmd;

/**
 * Read and write channel for byte arrays, in the form of TclObjects
 * 
 */
public class TclByteArrayChannel extends Channel {
	Interp interp;
	ByteArrayOutputStream ostream = null;
	ByteArrayInputStream istream = null;

	/**
	 * Construct a TclByteArrayChannel that reads from a TclObject.
	 * 
	 * @param interp
	 *            This interpreter
	 * @param object
	 *            TclObject to read from.
	 */
	public TclByteArrayChannel(Interp interp, TclObject object) {
		this.mode = TclIO.RDONLY;
		setEncoding(null);
		if (object.getInternalRep() instanceof TclByteArray)
			istream = new ByteArrayInputStream(TclByteArray.getBytes(interp, object));
		else {
			String str = object.toString();
			ByteBuffer bb = Charset.forName(EncodingCmd.systemJavaEncoding).encode(str);
			byte[] barray = new byte[bb.remaining()];
			bb.get(barray);
			istream = new ByteArrayInputStream(barray);

		}
		setChanName(TclIO.getNextDescriptor(interp, "bytearray"));
	}

	/**
	 * Construct a TclByteArrayChannel that writes to a TclObject.
	 * 
	 * @param interp
	 */
	public TclByteArrayChannel(Interp interp) {
		setEncoding(null);
		this.mode = TclIO.WRONLY;
		ostream = new ByteArrayOutputStream();
		setChanName(TclIO.getNextDescriptor(interp, "bytearray"));
	}

	/**
	 * 
	 * @return a TclObject containing byte array written to this channel. The
	 *         byte array is not decoded.
	 */
	public TclObject getTclByteArray() {
		if (ostream == null)
			return TclString.newInstance("");
		TclObject rv = TclByteArray.newInstance(ostream.toByteArray());
		return rv;
	}

	/**
	 * @return a TclObject containing the data written to this channel, decoded
	 *         using the current system encoding
	 */
	public TclObject getTclString() {
		if (ostream == null)
			return TclString.newInstance("");
		try {
			return TclString.newInstance(ostream.toString(EncodingCmd.systemJavaEncoding));
		} catch (UnsupportedEncodingException e) {
			return getTclByteArray();
		}
	}

	@Override
	String getChanType() {
		return "bytearray";
	}

	/* (non-Javadoc)
	 * @see tcl.lang.channel.Channel#implClose()
	 */
	@Override
	void implClose() throws IOException {
		
	}

	@Override
	protected InputStream getInputStream() throws IOException {
		if (istream == null)
			throw new RuntimeException("should not be called");
		else
			return istream;
	}

	@Override
	protected OutputStream getOutputStream() throws IOException {
		if (ostream == null)
			throw new RuntimeException("should not be called");
		else
			return ostream;
	}

}
