package tcl.lang;

import java.io.IOException;

import tcl.lang.channel.Channel;
import tcl.lang.channel.FileEvent;
import tcl.lang.channel.StdChannel;

/**
 * This class implements the Console Thread: it is started by
 * tcl.lang.Shell if the user gives no initial script to evaluate, or
 * when the -console option is specified. The console thread loops
 * forever, reading from the standard input, executing the user input
 * and writing the result to the standard output.
 */
public class ConsoleThread extends Thread {

	/**
	 *  Interpreter associated with this console thread.
	 */
	Interp interp;

	/**
	 * Collect the user input in this buffer until it forms a complete Tcl command
	 */
	StringBuffer sbuf;

	/**
	 *  Used to for interactive output
	 */
	private Channel out;
	
	/**
	 *  Used to for interactive error output
	 */
	private Channel err;

	/**
	 *  set to true to get extra debug output
	 */
	private static final boolean debug = false;

	/**
	 * Create a ConsoleThread.
	 */
	public ConsoleThread(Interp i) // Initial value for interp.
	{
		setName("ConsoleThread");
		interp = i;
		sbuf = new StringBuffer(100);

		out = TclIO.getStdChannel(StdChannel.STDOUT);
		err = TclIO.getStdChannel(StdChannel.STDERR);
	}

	/**
	 * Called by the JVM to start the execution of the console thread. It loops
	 * forever to handle user inputs.
	 * 
	 * Results: None.
	 * 
	 * Side effects: This method never returns. During its execution, some
	 * TclObjects may be locked inside the historyObjs vector. Remember to free
	 * them at "appropriate" times!
	 */
	public synchronized void run() {
		if (debug) {
			System.out.println("entered ConsoleThread run() method");
		}

		FileEvent.setStdinUsedForCommandInput(true);
		if (isInteractive()) put(out, "% ");

		while (true) {
			// Loop forever to collect user inputs in a StringBuffer.
			// When we have a complete command, then execute it and print
			// out the results.
			//
			// The loop is broken under two conditions: (1) when EOF is
			// received inside getLine(). (2) when the "exit" command is
			// executed in the script.

			getLine();

			final String command = sbuf.toString();

			if (debug) {
				System.out.println("got line from console");
				System.out.println("\"" + command + "\"");
			}

			// When interacting with the interpreter, one must
			// be careful to never call a Tcl method from
			// outside of the event loop thread. If we did
			// something like just call interp.eval() it
			// could crash the whole process because two
			// threads might write over each other.

			// The only safe way to interact with Tcl is
			// to create an event and add it to the thread
			// safe event queue.

			TclEvent event = new TclEvent() {
				public int processEvent(int flags) {

					// See if the command is a complete Tcl command

					if (Interp.commandComplete(command)) {
						if (debug) {
							System.out.println("line was a complete command");
						}

						boolean eval_exception = true;
						TclObject commandObj = TclString.newInstance(command);

						try {
							commandObj.preserve();
							interp.recordAndEval(commandObj, 0);
							eval_exception = false;
						} catch (TclException e) {
							if (debug) {
								System.out
										.println("eval returned exceptional condition");
							}
							
							// copy result into errorInfo by using addErrorInfo
							interp.addErrorInfo("");

							int code = e.getCompletionCode();
							switch (code) {
							case TCL.ERROR:
								// This really sucks. The getMessage() call on
								// a TclException will not always return a msg.
								// See TclException for super() problem.
								putLine(err, interp.getResult().toString());
								break;
							case TCL.BREAK:
								putLine(err,
										"invoked \"break\" outside of a loop");
								break;
							case TCL.CONTINUE:
								putLine(err,
										"invoked \"continue\" outside of a loop");
								break;
							default:
								putLine(err, "command returned bad code: "
										+ code);
							}
						} finally {
							commandObj.release();
						}

						if (!eval_exception) {
							if (debug) {
								System.out.println("eval returned normally");
							}

							String evalResult = interp.getResult().toString();

							if (debug) {
								System.out.println("eval result was \""
										+ evalResult + "\"");
							}

							if (evalResult.length() > 0  && isInteractive()) {
								putLine(out, evalResult);
							}
						}

						// Empty out the incoming command buffer
						sbuf.setLength(0);

						// See if the user set a custom shell prompt for the
						// next command

						TclObject prompt;

						try {
							prompt = interp.getVar("tcl_prompt1",
									TCL.GLOBAL_ONLY);
						} catch (TclException e) {
							prompt = null;
						}
						if (prompt != null) {
							try {
								interp.eval(prompt.toString(), TCL.EVAL_GLOBAL);
							} catch (TclException e) {
								if (isInteractive()) put(out, "% ");
							}
						} else {
							if (isInteractive()) put(out, "% ");
						}

						return 1;
					} else { // Interp.commandComplete() returned false

						if (debug) {
							System.out
									.println("line was not a complete command");
						}

						// We don't have a complete command yet. Print out a
						// level 2
						// prompt message and wait for further inputs.

						TclObject prompt;

						try {
							prompt = interp.getVar("tcl_prompt2",
									TCL.GLOBAL_ONLY);
						} catch (TclException e) {
							prompt = null;
						}
						if (prompt != null) {
							try {
								interp.eval(prompt.toString(), TCL.EVAL_GLOBAL);
							} catch (TclException e) {
								if (isInteractive()) put(out, "");
							}
						} else {
							if (isInteractive()) put(out, "");
						}

						return 1;
					}
				} // end processEvent method
			}; // end TclEvent innerclass

			// Add the event to the thread safe event queue
			interp.getNotifier().queueEvent(event, TCL.QUEUE_TAIL);

			// Tell this thread to wait until the event has been processed.
			event.sync();
		}
	}

	/**
	 * Gets a new line from System.in and put it in ConsoleThread.sbuf.
	 * 
	 * Result: The new line of user input, including the trailing carriage
	 * return character.
	 */
	private void getLine() {

		// Loop until user presses return or EOF is reached.
		char c2 = ' ';
		char c = ' ';

		if (debug) {
			System.out.println("now to read from System.in");
		}

		while (true) {
			try {
				/*
				 * Note that this System.in.read() will only really interact properly with the rest
				 * of TCL if System.in is an instance of ManagedSystemInStream.  Interp creates an instance
				 * of ManagedSystemInStream, which replaces the original System.in with itself.
				 */
				int i = System.in.read();
				
				if (i == -1) {
					if (sbuf.length() == 0) {
						System.exit(0);
					} else {
						return;
					}
				}

				c = (char) i;

				if (debug) {
					System.out.print("'" + c + "', ");
				}

				// Temporary hack until Channel drivers are complete. Convert
				// the Windows \r\n to \n.

				if (c == '\r') {
					if (debug) {
						System.out.println("checking windows hack");
					}

					i = System.in.read();
					if (i == -1) {
						if (sbuf.length() == 0) {
							System.exit(0);
						} else {
							return;
						}
					}
					c2 = (char) i;
					if (c2 == '\n') {
						c = c2;
					} else {
						sbuf.append(c);
						c = c2;
					}
				}
			} catch (IOException e) {
				// IOException shouldn't happen when reading from
				// System.in. The only exceptional state is the EOF event,
				// which is indicated by a return value of -1.

				e.printStackTrace();
				System.exit(0);
			}

			sbuf.append(c);

			// System.out.println("appending char '" + c + "' to sbuf");

			if (c == '\n') {
				return;
			}
		}
	}

	/**
	 * @return true if tcl_interactive is true, false if not
	 */
	private boolean isInteractive() {
		TclObject value = null;
		try {
			value = interp.getVar("tcl_interactive", TCL.GLOBAL_ONLY);
			return TclBoolean.get(interp, value);
		} catch (TclException e) {
			return true;
		}
	}
	/**
	 * Prints a string into the given channel with a trailing carriage return.
	 * 
	 * @param channel Channel to print to
	 * @param s String to print
	 */
	private void putLine(Channel channel,
			String s) 
	{
		try {
			channel.write(interp, s);
			channel.write(interp, "\n");
			channel.flush(interp);
		} catch (IOException ex) {
			System.err.println("IOException in Shell.putLine()");
			ex.printStackTrace(System.err);
		} catch (TclException ex) {
			System.err.println("TclException in Shell.putLine()");
			ex.printStackTrace(System.err);
		}
	}

	/**
	 * Prints a string into the given channel without a trailing carriage
	 * return.
	 * 
	 * @param channel Channel to print to
	 * @param s String to print
	 */
	private void put(Channel channel,
			String s) 
	{
		try {
			channel.write(interp, s);
			channel.flush(interp);
		} catch (IOException ex) {
			System.err.println("IOException in Shell.put()");
			ex.printStackTrace(System.err);
		} catch (TclException ex) {
			System.err.println("TclException in Shell.put()");
			ex.printStackTrace(System.err);
		}
	}
} // end of class ConsoleThread
