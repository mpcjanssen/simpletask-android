package tcl.lang.process;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

import tcl.lang.channel.Channel;

/**
 * Represents a redirection on one of the streams attached to a TclProcess.
 * Inspired by the Java 1.7 API.
 * 
 */
public class Redirect {
	/**
	 * The kind of redirection (see enum Type)
	 */
	protected Type type;
	/**
	 * The Channel object, for TCL_CHANNEL redirects
	 */
	protected Channel channel = null;
	/**
	 * The File object, for FILE redirects
	 */
	protected File file = null;
	
	/**
	 * A filename for file directs as specified to Tcl; used for error messages
	 */
	protected String specifiedFilePath = null;
	
	/**
	 * The TclProcess on the other side of the pipe, for PIPE redirects
	 */
	protected TclProcess pipePartner = null;
	/**
	 * If true, close channel when complete
	 */
	protected boolean closeChannel = false;
	/**
	 * If true, append to file
	 */
	protected boolean appendToFile = false;
	
	/**
	 * An inputStream, for STREAM redirects
	 */
	protected InputStream inputStream = null;
	/**
	 * And output stream, for STREAM redirects
	 */
	protected OutputStream outputStream = null;


	/**
	 * The types of redirection that can be defined
	 * 
	 */
	public enum Type {
		INHERIT, PIPE, FILE, TCL_CHANNEL, MERGE_ERROR, STREAM
	}

	private Redirect(Type type) {
		this.type = type;
	}

	/**
	 * Create a PIPE redirect
	 * 
	 * @param partner
	 *            TclProcess on the other side of the pipe. This Redirect object
	 *            is attached to the TclProcess on this side of the pipe.
	 */
	public Redirect(TclProcess partner) {
		this.type = Type.PIPE;
		this.pipePartner = partner;
	}

	/**
	 * Create a File redirect
	 * 
	 * @param f
	 *            File to redirect bytes from/to
	 * @param specifiedPath
	 * 			   File name as specified to Tcl; used for error messages          
	 */
	public Redirect(File f, String specifiedPath, boolean append) {
		this.type = Type.FILE;
		this.file = f;
		this.specifiedFilePath = specifiedPath;
		this.appendToFile = append;
	}

	/**
	 * Create a Tcl Channel redirect
	 * 
	 * @param channel
	 *            to redirect bytes from/to
	 * @param close
	 *  		   if true, close channel on process exit
	 */
	public Redirect(Channel channel, boolean close) {
		this.type = Type.TCL_CHANNEL;
		this.channel = channel;
		this.closeChannel = close;
	}

	/**
	 * Create an  STREAM redirect
	 */
	public static Redirect stream() {
		return new Redirect(Type.STREAM);
	}

	/**
	 * 
	 * @return a new INHERIT redirect
	 */
	public static Redirect inherit() {
		return new Redirect(Type.INHERIT);
	}

	/**
	 * @return a new redirect to merge stderr with stdout
	 */
	public static Redirect stderrToStdout() {
		return new Redirect(Type.MERGE_ERROR);
	}

	/**
	 * @return This Redirect's type
	 */
	public Type getType() {
		return this.type;
	}

	/**
	 * @return the inputStream for STREAM redirects
	 */
	public InputStream getInputStream() {
		return inputStream;
	}

	/**
	 * @param inputStream the inputStream to set  for STREAM redirects.  Must be set by TclProcess subclass.
	 */
	public void setInputStream(InputStream inputStream) {
		this.inputStream = inputStream;
	}

	/**
	 * @return the outputStream for STREAM redirects
	 */
	public OutputStream getOutputStream() {
		return outputStream;
	}

	/**
	 * @param outputStream the outputStream to set for STREAM redirects. Must be set by TclProcess subclass.
	 */
	public void setOutputStream(OutputStream outputStream) {
		this.outputStream = outputStream;
	}
	
}