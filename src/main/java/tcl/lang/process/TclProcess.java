package tcl.lang.process;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import tcl.lang.Interp;
import tcl.lang.TCL;
import tcl.lang.TclException;
import tcl.lang.TclIO;
import tcl.lang.TclPosixException;
import tcl.lang.TclRuntimeError;
import tcl.lang.Var;

/**
 * This abstract class represents a process that may be executed from Tcl.
 * Individual platforms should create a subclass of TclProcess, which is
 * instantiated with with TclProcess.newInstance(). This class is somewhat of a
 * combination of java.lang.ProcessBuilder and java.lang.Process, with
 * inspiration from the Java 1.7 API. It is provided to allow for better native
 * process control than Java's APIs.
 * 
 * @author Dan Bodoh
 * 
 */
public abstract class TclProcess {
	/**
	 * A tcl.lang.process.Redirect object specifying where stdin is redirected
	 * from
	 */
	protected Redirect stdinRedirect = null;
	/**
	 * A tcl.lang.process.Redirect object specifying where stdout is redirected
	 * to
	 */
	protected Redirect stdoutRedirect = null;
	/**
	 * A tcl.lang.process.Redirect object specifying where stderr is redirected
	 * to
	 */
	protected Redirect stderrRedirect = null;
	/**
	 * The command words that will be executed
	 */
	protected List<String> command = null;
	/**
	 * The current interpreter
	 */
	protected Interp interp;
	/**
	 * An exception to be thrown in the main thread, but caught in any child
	 * thread
	 */
	protected IOException savedException = null;

	/**
	 * Create a new platform-specific instance of a TclProcess subclass. This
	 * method detects the current platform and returns an instance of the
	 * appropriate TclProcess subclass.
	 * 
	 * @param interp
	 *            The current interpreter
	 * @return A platform-specific TclProcess subclass instance
	 */
	public static TclProcess newInstance(Interp interp) {
		TclProcess p = new JavaProcess(); // only JavaProcess is currently
											// implemented
		p.interp = interp;
		return p;
	}

	/**
	 * Set the command words to be executed
	 * 
	 * @param command
	 *            Command words to be executed
	 */
	public void setCommand(List<String> command) {
		this.command = command;
	}

	/**
	 * @return The command words to be executed
	 */
	public List<String> command() {
		return this.command;
	}

	/**
	 * @return a unmodifiable Map containing the environment that the subprocess should
	 * inherit.  System.getenv() is not sufficient because JTcl cannot update
	 * it on env() array changes.
	 */
	protected Map<String, String> getenv() {
		return interp.getenv();
	}
	/**
	 * Start the process executing, and register streams with any STREAM redirects
	 * 
	 * @throws IOException
	 */
	public abstract void start() throws IOException;

	/**
	 * @return The process's exit value
	 * @throws IllegalThreadStateException
	 *             if the process has not yet completed
	 */
	public abstract int exitValue() throws IllegalThreadStateException;

	/**
	 * Wait for the process to complete
	 * 
	 * @return this process's exit value
	 * @throws IllegalThreadStateException
	 *             if the process has not yet been started
	 * @throws InterruptedException
	 *             if the process is interrupted
	 * @throws IOException
	 */
	public int waitFor() throws IllegalThreadStateException, InterruptedException, IOException {
		if (!isStarted())
			throw new IllegalThreadStateException("Process not yet started");
		int rv = implWaitFor();
		if (stdoutRedirect != null && stdoutRedirect.type == Redirect.Type.TCL_CHANNEL && stdoutRedirect.closeChannel) {
			TclIO.unregisterChannel(interp, stdoutRedirect.channel);
		}
		if (stderrRedirect != null && stderrRedirect.type == Redirect.Type.TCL_CHANNEL && stderrRedirect.closeChannel) {
			TclIO.unregisterChannel(interp, stderrRedirect.channel);
		}
		throwAnyExceptions();
		return rv;
	}

	/**
	 * Platform-specific wait for the process to complete
	 * 
	 * @return this process's exit value
	 * @throws InterruptedException
	 *             if the process is interrupted
	 */
	protected abstract int implWaitFor() throws InterruptedException, IOException;

	/**
	 * @return The process identifier of this process, or -1 if it cannot be
	 *         determined
	 * @throws IllegalThreadStateException
	 *             if the process has not yet started
	 */
	public abstract int getPid() throws IllegalThreadStateException;

	/**
	 * @return true if the process has started; false otherwise
	 */
	public abstract boolean isStarted();

	/**
	 * @param workingDir
	 *            Directory the process starts in
	 */
	public abstract void setWorkingDir(File workingDir);

	/**
	 * Kill the running process
	 */
	public abstract void destroy();

	/**
	 * @return true if the this TclProcess subclass can actually inherit the open
	 * file descriptors, or false if it emulates inheritance
	 */
	public abstract boolean canInheritFileDescriptors();

	/**
	 * @return the stdinRedirect
	 */
	public Redirect getStdinRedirect() {
		return stdinRedirect;
	}

	/**
	 * 
	 * @param e
	 *            Exception to throw later in the main thread
	 */
	protected void saveIOException(IOException e) {
		synchronized (this) {
			if (savedException == null)
				savedException = e;
		}
	}

	/**
	 * @throws Exception
	 *             if any exception was passed to saveException()
	 */
	protected void throwAnyExceptions() throws IOException {
		synchronized (this) {
			if (savedException != null)
				throw savedException;
		}
	}

	/**
	 * @param stdinRedirect
	 *            the stdinRedirect to set
	 */
	public void setStdinRedirect(Redirect stdinRedirect) throws TclException {
		if (stdinRedirect == null) {
			this.stdinRedirect = null;
			return;
		}
		if (stdinRedirect.type == Redirect.Type.MERGE_ERROR) {
			throw new TclRuntimeError("Output redirects cannot be attached to process stdin");
		}
		if (stdinRedirect.type == Redirect.Type.TCL_CHANNEL) {
			if (stdinRedirect.channel.isWriteOnly())
				throw new TclException(interp, "channel \"" + stdinRedirect.channel.getChanName()
						+ "\" wasn't opened for reading");
		}
		if (stdinRedirect.type == Redirect.Type.FILE) {
			if (!stdinRedirect.file.exists()) {
				throw new TclPosixException(interp, TclPosixException.ENOENT, true, "couldn't read file \""
						+ stdinRedirect.specifiedFilePath + "\"");
			}
		}

		/* Convert stdin channel to inherit */
		if (stdinRedirect.type == Redirect.Type.TCL_CHANNEL && "stdin".equals(stdinRedirect.channel.getChanName())) {
			this.stdinRedirect = Redirect.inherit();
		} else {
			this.stdinRedirect = stdinRedirect;
		}
	}

	/**
	 * @return the stdoutRedirect
	 */
	public Redirect getStdoutRedirect() {
		return stdoutRedirect;
	}

	/**
	 * @param stdoutRedirect
	 *            the stdoutRedirect to set
	 */
	public void setStdoutRedirect(Redirect stdoutRedirect) throws TclException {
		if (stdoutRedirect.type == Redirect.Type.MERGE_ERROR) {
			throw new TclRuntimeError("Stdout  cannot be attached to MERGE_ERROR");
		}
		if (stdoutRedirect.type == Redirect.Type.TCL_CHANNEL) {
			if (stdoutRedirect.channel.isReadOnly())
				throw new TclException(interp, "channel \"" + stdoutRedirect.channel.getChanName()
						+ "\" wasn't opened for writing");
			try {
				stdoutRedirect.channel.flush(interp);
			} catch (IOException e) {
				throw new TclException(interp, e.getMessage());
			}
		}
		if (stdoutRedirect.type == Redirect.Type.FILE) {
			// test to verify that directory already exists for the new output file.
			File testFile = stdoutRedirect.file;
			if (!stdoutRedirect.appendToFile) {
				testFile = testFile.getAbsoluteFile().getParentFile();
			}
			if (!testFile.exists()) {
				throw new TclPosixException(interp, TclPosixException.ENOENT, true, "couldn't write file \""
						+ stdoutRedirect.specifiedFilePath + "\"");
			}
		}
		if (stdoutRedirect.type == Redirect.Type.TCL_CHANNEL && "stdout".equals(stdoutRedirect.channel.getChanName())) {
			this.stdoutRedirect = Redirect.inherit();
		} else {
			this.stdoutRedirect = stdoutRedirect;
		}
	}

	/**
	 * @return the stderrRedirect
	 */
	public Redirect getStderrRedirect() {
		return stderrRedirect;
	}

	/**
	 * @param stderrRedirect
	 *            the stderrRedirect to set
	 */
	public void setStderrRedirect(Redirect stderrRedirect) throws TclException {
		if (stderrRedirect.type == Redirect.Type.PIPE) {
			throw new TclRuntimeError("Stderr cannot be attached to Pipe, use MERGE_ERROR");
		}
		if (stderrRedirect.type == Redirect.Type.TCL_CHANNEL) {
			if (stderrRedirect.channel.isReadOnly())
				throw new TclException(interp, "channel \"" + stderrRedirect.channel.getChanName()
						+ "\" wasn't opened for writing");
			try {
				stderrRedirect.channel.flush(interp);
			} catch (IOException e) {
				throw new TclException(interp, e.getMessage());
			}
		}
		if (stderrRedirect.type == Redirect.Type.FILE) {
			// test to verify that directory already exists for the new output file.
			File testFile = stderrRedirect.file;
			if (!stderrRedirect.appendToFile) {
				testFile = testFile.getAbsoluteFile().getParentFile();
			}
			if (!testFile.exists()) {
				throw new TclPosixException(interp, TclPosixException.ENOENT, true, "couldn't write file \""
						+ stderrRedirect.specifiedFilePath + "\"");
			}
		}

		if (stderrRedirect.type == Redirect.Type.TCL_CHANNEL && "stderr".equals(stderrRedirect.channel.getChanName())) {
			this.stderrRedirect = Redirect.inherit();
		} else {
			this.stderrRedirect = stderrRedirect;
		}
	}
}
