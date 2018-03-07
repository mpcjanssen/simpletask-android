package tcl.lang;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import tcl.lang.channel.Channel;
import tcl.lang.channel.TclByteArrayChannel;
import tcl.lang.process.Redirect;
import tcl.lang.process.TclProcess;

/**
 * This class encapsulates a pipeline of operating system commands
 */

public class Pipeline implements Runnable {

	/**
	 * The processes, in input to output order
	 */
	private ArrayList<TclProcess> processes = new ArrayList<>();

	/**
	 * set to true if 2>@1 seen
	 */
	private boolean redirectStderrToResult = false;

	/**
	 * If true, run Pipeline in the background
	 */
	private boolean execInBackground = false;

	/**
	 * Interpreter that this Pipeline is executing in
	 */
	Interp interp = null;

	/**
	 * tcl.lang.process.Redirect object for this Pipeline's stdin
	 */
	Redirect stdinRedirect = null;
	/**
	 * tcl.lang.process.Redirect object for this Pipeline's stdout
	 */
	Redirect stdoutRedirect = null;
	/**
	 * tcl.lang.process.Redirect object for this Pipelines stderr
	 */
	Redirect stderrRedirect = null;
	/**
	 * Exception caught in thread
	 */
	TclException savedException = null;

	/**
	 * A set of redirectors that the parser recognizes
	 */
	private static final Set<String> redirectors;
	static {
		Set<String> rd = new HashSet<>(13);
		rd.add("<");
		rd.add("<@");
		rd.add("<<");
		rd.add(">");
		rd.add("2>");
		rd.add(">&");
		rd.add(">>");
		rd.add("2>>");
		rd.add(">>&");
		rd.add(">@");
		rd.add("2>@");
		rd.add("2>@1");
		rd.add(">&@");
		redirectors = Collections.unmodifiableSet(rd);
	}

	/**
	 * Create a Pipeline initialized with commands.
	 * 
	 * @param interp
	 *            TCL interpreter for this Pipeline
	 * @param objv
	 *            Array of TclObjects that are in the for of TCL's 'exec' args
	 * @param startIndex
	 *            index of the objv object at which to start parsing
	 * 
	 * @throws TclException
	 *             if a syntax error occurs
	 */
	public Pipeline(Interp interp, TclObject[] objv, int startIndex) throws TclException {
		ArrayList<String> commandList = new ArrayList<>();
		File cwd = interp.getWorkingDir();
		this.interp = interp;

		/*
		 * Parse the objv array into a Pipeline
		 */
		int endIndex = objv.length - 1;
		if (objv[endIndex].toString().equals("&")) {
			setExecInBackground(true);
			--endIndex;
		}

		for (int i = startIndex; i <= endIndex; i++) {
			String arg = objv[i].toString();

			if (arg.equals("|")) {
				if (commandList.size() == 0 || i == endIndex) {
					// error message could be more specific, but this matches
					// exec.test
					throw new TclException(interp, "illegal use of | or |& in command");
				}
				addCommand(commandList, cwd);
				commandList = new ArrayList<>(); // reset command list
				continue;
			}

			if (arg.equals("|&")) {
				if (commandList.size() == 0 || i == endIndex) {
					// error message could be more specific, but this matches
					// exec.test
					throw new TclException(interp, "illegal use of | or |& in command");
				}
				int newCmdIndex = addCommand(commandList, cwd);
				processes.get(newCmdIndex).setStderrRedirect(Redirect.stderrToStdout());

				commandList = new ArrayList<>(); // reset command list
				continue;
			}

			/*
			 * Is the next object a redirector, or something prefixed with
			 * redirector (">", ">>", etc.)
			 */
			int argLen = arg.length();
			String redirector = null;
			for (int j = argLen > 4 ? 4 : argLen; j >= 1; --j) {
				redirector = arg.substring(0, j);
				if (redirectors.contains(redirector)) {
					break;
				}
				redirector = null;
			}
			if (redirector == null) {
				/*
				 * Not a redirector, so append it to the commandList and go on
				 * to next object
				 */
				commandList.add(arg);
				continue;
			} else if ("2>@1".equals(redirector)) {
				redirectStderrToResult = true;
				continue;
			} else {
				/* It was a redirector, so let's figure out what to do with it */

				/* Get the redirectee (the file or channel name) */
				String redirectee = null;
				if (arg.length() > redirector.length()) {
					redirectee = arg.substring(redirector.length());
				} else {
					/* Get it from the next object in the list */
					++i;
					if (i > endIndex) {
						throw new TclException(interp, "can't specify \"" + redirector + "\" as last word in command");
					}
					redirectee = objv[i].toString();
				}

				/*
				 * Convert redirectee to a Channel or File, and create a
				 * Redirect object
				 */
				Redirect redirect = null;

				if (redirector.contains("@")) {
					/* The redirectee is a channel name */
					Channel channel = TclIO.getChannel(interp, redirectee);
					if (channel == null) {
						throw new TclException(interp, "could not find channel named \"" + redirectee + "\"");
					}
					redirect = new Redirect(channel, false);
				} else if (redirector.equals("<<")) {
					Channel channel = new TclByteArrayChannel(interp, TclString.newInstance(redirectee));
					TclIO.registerChannel(interp, channel);
					redirect = new Redirect(channel, true);
				} else {
					File redirfile = FileUtil.getNewFileObj(interp, redirectee);
					redirect = new Redirect(redirfile, redirectee, redirector.contains(">>"));
				}

				/* Does this redirector apply to stderr, stdout or stdin? */
				if (redirector.startsWith("2")) {
					stderrRedirect = redirect;
				} else if (redirector.contains(">")) {
					stdoutRedirect = redirect;
					if (redirector.contains("&")) {
						stderrRedirect = redirect;
					}
				} else {
					// std input is redirected
					stdinRedirect = redirect;
				}
			}
		}
		if (commandList.size() > 0) {
			addCommand(commandList, cwd);
		}
	}

	/**
	 * Add an operating system command to the end of the pipeline
	 * 
	 * @param command
	 *            The command and its arguments. The List is not copied, so any
	 *            changes will be reflected in the Pipeline
	 * @param workingDir
	 *            New command starts in this directory
	 * @return index of the new command in the pipeline
	 */
	public int addCommand(List<String> command, File workingDir) throws TclException {
		String cmd = command.get(0);
		cmd = FileUtil.translateFileName(interp, cmd);

		TclProcess proc = TclProcess.newInstance(interp);
		proc.setCommand(command);
		if (workingDir != null)
			proc.setWorkingDir(workingDir);
		int lastProcIndex = processes.size() - 1;
		if (lastProcIndex >= 0) {
			/* Pipe from the previous command */
			TclProcess upstreamProcess = processes.get(lastProcIndex);
			proc.setStdinRedirect(new Redirect(upstreamProcess));
			upstreamProcess.setStdoutRedirect(new Redirect(proc));
		}
		processes.add(proc);
		return processes.size() - 1;
	}

	/**
	 * Executes the pipeline. If the execInBackground property is false, exec()
	 * waits for processes to complete before returning. If any input and output
	 * channels (pipeline*Channel) have not been set either in the constructor
	 * with the TCL redirection operator, or with setPipeline*Channel(), data
	 * will be read/written from System.in, System.out, System.err.
	 * 
	 * @throws TclException
	 */
	public void exec() throws TclException {
		if (processes.size() == 0)
			return;

		if (stdinRedirect != null) {
			/*
			 * Don't inherit stdin when in background, if the TclProcess
			 * subclass cannot support inheritance (for example, JavaProcess).
			 * Without inheritance, stdin bytes will disappear because they will
			 * be fed into the process's input, even though the process may not
			 * consume those bytes.
			 */
			if (stdinRedirect.getType() == Redirect.Type.INHERIT && execInBackground
					&& !processes.get(0).canInheritFileDescriptors()) {
				processes.get(0).setStdinRedirect(null);
			} else
				processes.get(0).setStdinRedirect(stdinRedirect);
		}
		if (stdoutRedirect != null) {
			processes.get(processes.size() - 1).setStdoutRedirect(stdoutRedirect);
		}
		/*
		 * Execute each command in the pipeline
		 */
		for (TclProcess process : processes) {
			if (process.getStderrRedirect() == null && stderrRedirect != null) {
				process.setStderrRedirect(stderrRedirect);
			}
			try {
				process.start();
			} catch (IOException e) {
				throw new TclPosixException(interp, e, true, "couldn't execute \"" + process.command().get(0) + "\"");
			}
		}

	}

	/**
	 * Wait for processes in pipeline to die, then close couplers and any open
	 * channels
	 * 
	 * @param force
	 *            Kill processes forcibly, if true
	 */
	public void waitForExitAndCleanup(boolean force) throws TclException {
		/*
		 * Wait for processes to finish
		 */
		for (TclProcess process : processes) {
			if (force)
				process.destroy();
			else {
				try {
					process.waitFor();
				} catch (InterruptedException e) {
					process.destroy();
				} catch (IOException e1) {
					throw new TclPosixException(interp, e1, true, "Error");
				}
			}
		}
	}

	/**
	 * @return the stdinRedirect
	 */
	public Redirect getStdinRedirect() {
		return stdinRedirect;
	}

	/**
	 * @param stdinRedirect
	 *            the stdinRedirect to set
	 */
	public void setStdinRedirect(Redirect stdinRedirect) {
		this.stdinRedirect = stdinRedirect;
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
	public void setStdoutRedirect(Redirect stdoutRedirect) {
		this.stdoutRedirect = stdoutRedirect;
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
	public void setStderrRedirect(Redirect stderrRedirect) {
		this.stderrRedirect = stderrRedirect;
	}

	/**
	 * @return the execInBackground property
	 */
	public boolean isExecInBackground() {
		return execInBackground;
	}

	/**
	 * @param execInBackground
	 *            Set the execInBackground property, which determines if the
	 *            Pipeline runs in the background
	 */
	public void setExecInBackground(boolean execInBackground) {
		this.execInBackground = execInBackground;
	}

	/**
	 * Return true if 2>@1 was seen
	 */
	public boolean isErrorRedirectedToResult() {
		return redirectStderrToResult;
	}

	/**
	 * @return Array of process identifiers, or array of -1 if we can't get PIDs
	 */
	public int[] getProcessIdentifiers() {
		int[] pid = new int[processes.size()];
		for (int i = 0; i < processes.size(); i++) {
			pid[i] = processes.get(i).getPid();
		}
		return pid;
	}

	/**
	 * @return Exit values of all processes in the pipeline; only valid if
	 *         processes have exited
	 */
	public int[] getExitValues() {
		int[] exitValues = new int[processes.size()];
		for (int i = 0; i < processes.size(); i++) {
			try {
				exitValues[i] = processes.get(i).exitValue();
			} catch (Exception e) {
				exitValues[i] = 0;
			}
		}
		return exitValues;
	}

	/**
	 * @throws IOException
	 *             if one has been caught during a background
	 *             waitForExitAndCleanup
	 */
	public void throwAnyExceptions() throws TclException {
		if (savedException != null)
			throw savedException;
	}

	public void run() {
		try {
			waitForExitAndCleanup(false);
		} catch (TclException e) {
			synchronized (this) {
				savedException = e;
			}
		}
	}

}
