package tcl.lang.process;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.Map;

import tcl.lang.ManagedSystemInStream;
import tcl.lang.TclByteArray;
import tcl.lang.TclException;
import tcl.lang.TclIO;
import tcl.lang.TclObject;
import tcl.lang.channel.Channel;

/**
 * Implements a pure Java process. Combines java.lang.ProcessBuilder and
 * java.lang.Process. Has the disadvantage of possibly sucking up too much
 * stdin, and cannot outlive the JVM if it has any PIPE or CHANNEL or INHERIT
 * redirects. When Java 1.7 is available, this class should be re-written to do
 * a proper INHERIT redirect, instead of using the pipe model of Java 1.5, to
 * avoid sucking up too much stdin.
 * 
 * @author danb
 * 
 */
public class JavaProcess extends TclProcess {
	/**
	 * The Java Process object
	 */
	protected Process process = null;
	/**
	 * The Java ProcessBuilder object
	 */
	protected ProcessBuilder processBuilder = new ProcessBuilder();
	/**
	 * The source of bytes for this process's stdin
	 */
	protected InputStream stdinStream = null;
	/**
	 * The sink for this process's stdout
	 */
	protected OutputStream stdoutStream = null;
	/**
	 * The sink for this process's stderr
	 */
	protected OutputStream stderrStream = null;

	/**
	 * Coupler that reads this process's stdout
	 */
	protected Coupler stdoutCoupler = null;
	/**
	 * Coupler that reads this process's stderr
	 */
	protected Coupler stderrCoupler = null;

	/**
	 * @return this process's stream to write stdin data to
	 */
	public OutputStream getOutputStream() {
		return process.getOutputStream();
	}

	/**
	 * @return this process's stream to read stdout from
	 */
	public InputStream getInputStream() {
		return process.getInputStream();
	}

	/**
	 * @return this process's stream to read stderr from
	 */
	public InputStream getErrorStream() {
		return process.getErrorStream();
	}

	@Override
	public int exitValue() throws IllegalThreadStateException {
		if (process == null)
			throw new IllegalThreadStateException("Process not yet started");
		return process.exitValue();
	}

	/**
	 * Use env array to initialize the environment for the process, since
	 * JTcl does not update the process environment on changes to env()
	 */
	private void initializeEnv(ProcessBuilder processBuilder) {
		Map<String, String> pbenv = processBuilder.environment();
		pbenv.clear();
		pbenv.putAll(getenv());
	}
	
	@Override
	public void start() throws IOException {
		processBuilder.command(command);
		initializeEnv(processBuilder);
		
		if (stderrRedirect != null && stderrRedirect.type == Redirect.Type.MERGE_ERROR) {
			processBuilder.redirectErrorStream(true);
		}
		process = processBuilder.start();
		
		/*
		 * Connect the process's stdin
		 */
		if (stdinRedirect != null) {
			switch (stdinRedirect.type) {
			case FILE:
				stdinStream = new BufferedInputStream(new FileInputStream(stdinRedirect.file));
				break;
			case PIPE:
				JavaProcess upstream = (JavaProcess) stdinRedirect.pipePartner;
				stdinStream = upstream.getInputStream();
				break;
			case INHERIT:
				stdinStream = new ManagedSystemInStream();
				break;
			case STREAM:
				stdinStream = null;
				break;
			case TCL_CHANNEL:
				// wrap channel in an inputStream
				stdinStream = new InputStream() {
					TclObject tclbuf = TclByteArray.newInstance();

					@Override
					public int read() throws IOException {
						try {
							if (stdinRedirect.channel.eof())
								return -1;
							int cnt = stdinRedirect.channel.read(interp, tclbuf, TclIO.READ_N_BYTES, 1);
							if (cnt > 0)
								return TclByteArray.getBytes(interp, tclbuf)[0];
							else
								return -1;
						} catch (TclException e) {
							throw new IOException(e.getMessage());
						}

					}

				};
				break;
			}
		}
		if (stdinStream != null) {
			Thread coupler = new Coupler(stdinStream, process.getOutputStream(), true, true);
			coupler.setDaemon(true);
			coupler.setName("JavaProcess Coupler stdin");
			coupler.start();
		} else if (stdinRedirect!=null && stdinRedirect.getType() == Redirect.Type.STREAM) {
			stdinRedirect.setOutputStream(process.getOutputStream());
		} else {
			// just close the output stream of the process, since it won't get any output anyway
			process.getOutputStream().close();
		}

		/*
		 * Connect process's stdout
		 */
		boolean closeOutput = true;
		if (stdoutRedirect != null) {
			switch (stdoutRedirect.type) {
			case FILE:
				stdoutStream = new BufferedOutputStream(new FileOutputStream(stdoutRedirect.file,
						stdoutRedirect.appendToFile));
				break;
			case INHERIT:
				stdoutStream = new FileOutputStream(FileDescriptor.out);
				closeOutput = false;  // don't want to close FileDescriptor.out
				break;
			case STREAM:
				stdoutStream = null;
				break;
			case TCL_CHANNEL:
				stdoutStream = new OutputStream() {
					byte[] buf = new byte[1];
					TclObject tclbuf;

					@Override
					public void write(int b) throws IOException {
						buf[0] = (byte) (b & 0xFF);
						tclbuf = TclByteArray.newInstance(buf);
						try {
							stdoutRedirect.channel.write(interp, tclbuf);
						} catch (TclException e) {
							throw new IOException(e.getMessage());
						}
					}

					/*
					 * (non-Javadoc)
					 * 
					 * @see java.io.OutputStream#flush()
					 */
					@Override
					public void flush() throws IOException {
						super.flush();
						try {
							stdoutRedirect.channel.flush(interp);
						} catch (TclException e) {
							throw new IOException(e.getMessage());
						}
					}

				};
				break;
			}

		}
		if (stdoutStream != null) {
			// don't close inherited STDOUT; that will close the descriptor and
			// TCL won't be
			// able to write
			stdoutCoupler = new Coupler(process.getInputStream(), stdoutStream,
					closeOutput, stdoutRedirect.type == Redirect.Type.INHERIT);
			stdoutCoupler.setDaemon(true);
			stdoutCoupler.setName("JavaProcess Coupler stdout");
			stdoutCoupler.start();
		} else if (stdoutRedirect!=null && stdoutRedirect.getType() == Redirect.Type.STREAM) {
			stdoutRedirect.setInputStream(process.getInputStream());
		}
		/*
		 * Connect process's stderr
		 */
		closeOutput = true;
		if (stderrRedirect != null && stderrRedirect.type != Redirect.Type.MERGE_ERROR) {
			switch (stderrRedirect.type) {
			case FILE:
				stderrStream = new BufferedOutputStream(new FileOutputStream(stderrRedirect.file,
						stderrRedirect.appendToFile));
				break;
			case INHERIT:
				stderrStream = new FileOutputStream(FileDescriptor.err);
				closeOutput = false;  // don't close FileDescriptor.err
				break;
			case STREAM:
				stderrStream = null;
				break;
			case TCL_CHANNEL:
				closeOutput = false;
				stderrStream = new OutputStream() {
					byte[] buf = new byte[1];
					TclObject tclbuf;

					@Override
					public void write(int b) throws IOException {
						buf[0] = (byte) (b & 0xFF);
						tclbuf = TclByteArray.newInstance(buf);
						try {
							try {
								stderrRedirect.channel.waitForOwnership(Channel.WRITE_OWNERSHIP);
							} catch (InterruptedException e) {
							}
							stderrRedirect.channel.write(interp, tclbuf);
						} catch (TclException e) {
							throw new IOException(e.getMessage());
						} finally {
							stderrRedirect.channel.setOwnership(false, Channel.WRITE_OWNERSHIP);
						}
					}
					
					

					/* (non-Javadoc)
					 * @see java.io.OutputStream#write(byte[], int, int)
					 */
					@Override
					public void write(byte[] b, int off, int len) throws IOException {
						tclbuf = TclByteArray.newInstance(b, off, len);
						try {
							try {
								stderrRedirect.channel.waitForOwnership(Channel.WRITE_OWNERSHIP);
							} catch (InterruptedException e) {
							}
							stderrRedirect.channel.write(interp, tclbuf);
						} catch (TclException e) {
							throw new IOException(e.getMessage());
						} finally {
							stderrRedirect.channel.setOwnership(false, Channel.WRITE_OWNERSHIP);
						}
					}

					/*
					 * (non-Javadoc)
					 * 
					 * @see java.io.OutputStream#flush()
					 */
					@Override
					public void flush() throws IOException {
						super.flush();
						try {
							try {
								stderrRedirect.channel.waitForOwnership(Channel.WRITE_OWNERSHIP);
							} catch (InterruptedException e) {
							}
							stderrRedirect.channel.flush(interp);
						} catch (TclException e) {
							throw new IOException(e.getMessage());
						} finally {
							stderrRedirect.channel.setOwnership(false, Channel.WRITE_OWNERSHIP);
						}
					}

				};
				break;
			}
		}
		if (stderrStream != null) {
			stderrCoupler = new Coupler(process.getErrorStream(), stderrStream,
					closeOutput, true);
			stderrCoupler.setDaemon(true);
			stderrCoupler.setName("JavaProcess Coupler stderr");
			stderrCoupler.start();
		} else if (stderrRedirect!=null && stderrRedirect.getType() == Redirect.Type.STREAM) {
			stderrRedirect.setInputStream(process.getErrorStream());
		}

	}

	@Override
	protected int implWaitFor() throws InterruptedException, IOException {
		if (process == null)
			throw new IllegalThreadStateException("Process not yet started");
		int rv = process.waitFor();
		if (stdinRedirect != null && stdinRedirect.type == Redirect.Type.INHERIT && stdinStream != null)
			stdinStream.close(); // so we don't keep sucking up stdin
		
		// wait for couplers to finish
		if (stdoutCoupler != null) {
			stdoutCoupler.join();
		}

		if (stderrCoupler != null) {
			stderrCoupler.join();
		}
		
		return rv;
	}

	@Override
	public int getPid() throws IllegalThreadStateException {
		/*
		 * Get the PID through reflection to get private fields in at least
		 * Sun's implementation of the java.lang.Process subclass
		 */
		if (process == null)
			throw new IllegalThreadStateException("Process not yet started");
		final String[] pidFields = { "pid", "handle" };

		for (String pidField : pidFields) {
			try {
				Field f = process.getClass().getDeclaredField(pidField);
				f.setAccessible(true);
				return f.getInt(process);
			} catch (Exception ee) {
				// do nothing
			}
		}
		return -1; // couldn't get pid through reflection
	}

	@Override
	public boolean isStarted() {
		return (process != null);
	}

	@Override
	public void setWorkingDir(File workingDir) {
		processBuilder.directory(workingDir);
	}

	@Override
	public void destroy() {
		if (stdinRedirect!= null && stdinRedirect.type == Redirect.Type.INHERIT) {
			try {
				stdinStream.close();
			} catch (IOException e) {
				// do nothing
			}
		}
		process.destroy();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tcl.lang.process.TclProcess#canInheritFileDescriptors()
	 */
	@Override
	public boolean canInheritFileDescriptors() {
		return false;
	}

	/**
	 * Stuff data into process's stdin, or get data out of stdout/stderr
	 * 
	 * @author danb
	 * 
	 */
	private class Coupler extends Thread {
		InputStream in = null;
		OutputStream out = null;
		boolean closeOut;
		boolean flushOut;

		public Coupler(InputStream in, OutputStream out, boolean closeOut, boolean flushOut) {
			this.in = in;
			this.out = out;
			this.closeOut = closeOut;
			this.flushOut = flushOut;
		}

		@Override
		public void run() {
			byte [] buf = new byte [256];
			while (true) {
				int b = -1;
				try {
					int avail = in.available();
					avail = avail > buf.length ? buf.length : avail;
					avail = avail == 0 ? 1 : avail;

					b = in.read(buf, 0, avail);
					if (b == -1) {
						if (closeOut)
							out.close();
						break;
					}
					out.write(buf, 0, b);
					if (flushOut)
						out.flush();

				} catch (IOException e) {
					// don't throw a Pipe Closed exception, pipes get closed in another thread
					if (! e.getMessage().toLowerCase().contains("pipe closed"))
						saveIOException(e);
					try {
						if (closeOut)
							out.close();
					} catch (IOException e1) {
						// do nothing
					}
					try {
						in.close();
					} catch (IOException e1) {
						// do nothing
					}
					break;
				}
			}
		}

	}



}
