package tcl.lang.channel;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import tcl.lang.Interp;
import tcl.lang.Pipeline;
import tcl.lang.TclException;
import tcl.lang.TclIO;
import tcl.lang.TclList;
import tcl.lang.TclObject;
import tcl.lang.TclString;
import tcl.lang.process.Redirect;

/**
 * This class provides a Channel view of a Pipeline object
 * 
 */
public class PipelineChannel extends Channel {
	private Pipeline pipeline;
	private TclByteArrayChannel stderr;
	Interp interp = null;

	/**
	 * Open a new pipeline channel
	 * 
	 * @param interp
	 * @param execString
	 *            String in the form of "exec" or "open"; first '|' is optional
	 * @param modeFlags
	 *            TclIO.RDONLY or TclIO.WRONLY or TclIO.RDWR
	 * @return channel name
	 * @throws IOException
	 * @throws TclException
	 */
	public String open(Interp interp, String execString, int modeFlags) throws IOException, TclException {

		this.interp = interp;

		if (execString.startsWith("|")) {
			execString = execString.substring(1);
		}
		TclObject[] objv = TclList.getElements(interp, TclString.newInstance(execString));

		if (objv.length == 0) {
			throw new TclException(interp, "illegal use of | or |& in command");
		}
		pipeline = new Pipeline(interp, objv, 0);

		stderr = new TclByteArrayChannel(interp);

		if (modeFlags == TclIO.RDONLY) {
			this.mode = TclIO.RDONLY;
			/* Read from pipelines output */
			if (pipeline.getStdoutRedirect() == null) {
				pipeline.setStdoutRedirect(Redirect.stream());
			} else {
				throw new TclException(interp, "can't read output from command: standard output was redirected");
			}
			if (pipeline.getStdinRedirect() == null) {
				pipeline.setStdinRedirect(Redirect.inherit());
			}
		} else if (modeFlags == TclIO.WRONLY) {
			this.mode = TclIO.WRONLY;
			/* Write to pipeline's input */
			if (pipeline.getStdinRedirect() == null) {
				pipeline.setStdinRedirect(Redirect.stream());
				this.setBuffering(TclIO.BUFF_NONE);
			} else {
				throw new TclException(interp, "can't write input to command: standard input was redirected");
			}
			if (pipeline.getStdoutRedirect() == null) {
				pipeline.setStdoutRedirect(Redirect.inherit());
			}
		} else if (modeFlags == TclIO.RDWR) {
			this.mode = TclIO.RDWR;
			boolean setRedir = false;
			if (pipeline.getStdoutRedirect() == null) {
				setRedir = true;
				pipeline.setStdoutRedirect(Redirect.stream());
			} else {
				this.mode = TclIO.WRONLY;
			}
			if (pipeline.getStdinRedirect() == null) {
				setRedir = true;
				pipeline.setStdinRedirect(Redirect.stream());
			} else {
				this.mode = TclIO.RDONLY;
			}
			if (!setRedir)
				throw new TclException(interp,
						"can't read/write output/input to command: standard output/input was redirected");

		}
		if (pipeline.getStderrRedirect() == null) {
			pipeline.setStderrRedirect(new Redirect(stderr, true));
		}
		pipeline.setExecInBackground(true);
		pipeline.exec();

		setChanName(TclIO.getNextDescriptor(interp, getChanType()));
		return getChanName();
	}

	/**
	 * @return The standard error output from the command, if any
	 */
	public String getStderrOutput() {
		if (stderr != null) {
			return stderr.getTclByteArray().toString();
		}
		return "";
	}

	@Override
	String getChanType() {
		return "pipeline";
	}

	/**
	 * 
	 * @return the Pipeline object for this Channel
	 */
	public Pipeline getPipeline() {
		return pipeline;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tcl.lang.channel.Channel#getInputStream()
	 */
	@Override
	protected InputStream getInputStream() throws IOException {
		/*
		 * TclProcess subclass should have called STREAM redirectors to set
		 * streams
		 */
		if (pipeline.getStdoutRedirect().getType() == Redirect.Type.STREAM) {
			return pipeline.getStdoutRedirect().getInputStream();
		} else {
			throw new IOException("should not call getInputStream()");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tcl.lang.channel.Channel#getOutputStream()
	 */
	@Override
	protected OutputStream getOutputStream() throws IOException {
		/*
		 * TclProcess subclass should have called STREAM redirectors to set
		 * streams
		 */
		if (pipeline.getStdinRedirect().getType() == Redirect.Type.STREAM) {
			return pipeline.getStdinRedirect().getOutputStream();
		} else {
			throw new IOException("should not call getOutputStream()");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tcl.lang.channel.Channel#implClose()
	 */
	@Override
	void implClose() throws IOException {
		IOException ex = null;

		if (pipeline.getStdinRedirect().getType() == Redirect.Type.STREAM) {
			try {
				pipeline.getStdinRedirect().getOutputStream().close();
			} catch (IOException e) {
				ex = e;
			}
		}
		if (pipeline.getStdoutRedirect().getType() == Redirect.Type.STREAM) {
			try {
				pipeline.getStdoutRedirect().getInputStream().close();
			} catch (IOException e) {
				ex = e;
			}
		}
		
		try {
			pipeline.waitForExitAndCleanup(false);
		} catch (TclException e) {
			throw new IOException(e.getMessage());
		}

		if (ex != null)
			throw ex;

	}

}
