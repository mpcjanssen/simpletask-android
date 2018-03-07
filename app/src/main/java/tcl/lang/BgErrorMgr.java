/*
 * BgErrorMgr --
 *
 *	This class manages the background errors for a Tcl interpreter.
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: BgErrorMgr.java,v 1.7 2006/01/26 19:49:18 mdejong Exp $
 *
 */

package tcl.lang;

import java.io.IOException;
import java.util.ArrayList;

import tcl.lang.channel.Channel;
import tcl.lang.channel.StdChannel;

/*
 * This class manages the background errors for a Tcl interpreter. It
 * stores the error information about the interpreter and use an idle
 * handler to report the error when the notifier is idle.
 */

class BgErrorMgr implements AssocData {

	/*
	 * We manage the background errors in this interp instance.
	 */

	Interp interp;

	/*
	 * A TclObject for invoking the "bgerror" command. We use a TclObject
	 * instead of a String so that we don't need to look up the command every
	 * time.
	 */

	TclObject bgerrorCmdObj;

	/*
	 * A list of the pending background error handlers.
	 */

	ArrayList errors;

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * BgErrorMgr --
	 * 
	 * Constructs a new BgErrorMgr instance.
	 * 
	 * Side effects: Member fields are initialized.
	 * 
	 * ----------------------------------------------------------------------
	 */

	BgErrorMgr(Interp i) // Manage the background errors in this interp.
	{
		interp = i;
		bgerrorCmdObj = TclString.newInstance("bgerror");
		bgerrorCmdObj.preserve();

		errors = new ArrayList();
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * addBgError --
	 * 
	 * Adds one background error to the list of pending background errors. The
	 * errors will be reported later as idle events.
	 * 
	 * The background errors will be reported in the order they were added by
	 * the addBgError() call. This order is guaranteed by the constructor
	 * IdleHandler.IdleHandler(Notifier);
	 * 
	 * Results: None.
	 * 
	 * Side effects: The command "bgerror" is invoked later as an idle handler
	 * to process the error, passing it the error message. If that fails, then
	 * an error message is output on stderr.
	 * 
	 * ----------------------------------------------------------------------
	 */

	void addBgError() {
		BgError bgErr = new BgError(interp.getNotifier());

		// The addErrorInfo() call below (with an empty string)
		// ensures that errorInfo gets properly set. It's needed in
		// cases where the error came from a utility procedure like
		// Interp.getVar() instead of Interp.eval(); in these cases
		// errorInfo still won't have been set when this procedure is
		// called.

		interp.addErrorInfo("");

		bgErr.errorMsg = interp.getResult();
		bgErr.errorInfo = null;
		try {
			bgErr.errorInfo = interp.getVar("errorInfo", null, TCL.GLOBAL_ONLY);
		} catch (TclException e) {
			// Do nothing if var does not exist.
		}

		bgErr.errorCode = null;
		try {
			bgErr.errorCode = interp.getVar("errorCode", null, TCL.GLOBAL_ONLY);
		} catch (TclException e) {
			// Do nothing if var does not exist.
		}

		bgErr.errorMsg.preserve();
		bgErr.errorInfo.preserve();
		bgErr.errorCode.preserve();

		errors.add(bgErr);
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * disposeAssocData --
	 * 
	 * This method is called when the interpreter is destroyed or when
	 * Interp.deleteAssocData is called on a registered AssocData instance.
	 * 
	 * Results: None.
	 * 
	 * Side effects: Removes any bgerror's that haven't been reported.
	 * 
	 * ----------------------------------------------------------------------
	 */

	public void disposeAssocData(Interp interp) // The interpreter in which this
												// AssocData
	// instance is registered in.
	{
		for (int i = errors.size() - 1; i >= 0; i--) {
			BgError bgErr = (BgError) errors.get(i);
			errors.remove(i);
			bgErr.cancel();

			bgErr.errorMsg.release();
			bgErr.errorMsg = null;
			bgErr.errorInfo.release();
			bgErr.errorInfo = null;
			bgErr.errorCode.release();
			bgErr.errorCode = null;
		}

		bgerrorCmdObj.release();
		bgerrorCmdObj = null;
	}

	/*
	 * This inner class implements an idle handler which reports one background
	 * error.
	 */

	class BgError extends IdleHandler {

		/*
		 * The interp's result, errorCode and errorInfo when the bgerror
		 * happened.
		 */

		TclObject errorMsg;
		TclObject errorCode;
		TclObject errorInfo;

		/*
		 * ----------------------------------------------------------------------
		 * 
		 * BgError --
		 * 
		 * Constructs a new BgError instance.
		 * 
		 * Side effects: The idle handler is initialized by the super class's
		 * constructor.
		 * 
		 * 
		 * ----------------------------------------------------------------------
		 */

		BgError(Notifier n) // The notifier to fire the event.
		{
			super(n);
		}

		/*
		 * ----------------------------------------------------------------------
		 * 
		 * processIdleEvent --
		 * 
		 * Process the idle event.
		 * 
		 * Results: None.
		 * 
		 * Side effects: The bgerror command is executed. It may have arbitrary
		 * side effects.
		 * 
		 * 
		 * ----------------------------------------------------------------------
		 */

		public void processIdleEvent() {

			// During the execution of this method, elements may be removed from
			// the errors list (because a TCL.BREAK was returned by the bgerror
			// command, or because the interp was deleted). We remove this
			// BgError instance from the list first so that this instance won't
			// be deleted twice.

			int index = errors.indexOf(this);
			errors.remove(index);

			// Restore important state variables to what they were at
			// the time the error occurred.

			try {
				interp.setVar("errorInfo", null, errorInfo, TCL.GLOBAL_ONLY);
			} catch (TclException e) {

				// Ignore any TclException's, possibly caused by variable traces
				// on
				// the errorInfo variable. This is compatible with the behavior
				// of
				// the Tcl C API.
			}

			try {
				interp.setVar("errorCode", null, errorCode, TCL.GLOBAL_ONLY);
			} catch (TclException e) {

				// Ignore any TclException's, possibly caused by variable traces
				// on
				// the errorCode variable. This is compatible with the behavior
				// of
				// the Tcl C API.
			}

			// Make sure, that the interpreter will surive the invocation
			// of the bgerror command.

			interp.preserve();

			try {

				// Invoke the bgerror command.

				TclObject argv[] = new TclObject[2];
				argv[0] = bgerrorCmdObj;
				argv[1] = errorMsg;

				Parser.evalObjv(interp, argv, 0, TCL.EVAL_GLOBAL);
			} catch (TclException e) {
				switch (e.getCompletionCode()) {
				case TCL.ERROR:
					try {
						Channel chan = TclIO.getStdChannel(StdChannel.STDERR);
						if (interp
								.getResult()
								.toString()
								.equals(
										"\"bgerror\" is an invalid command name or ambiguous abbreviation")) {
							chan.write(interp, errorInfo);
							chan.write(interp, "\n");
						} else {
							chan
									.write(interp,
											"bgerror failed to handle background error.\n");
							chan.write(interp, "    Original error: ");
							chan.write(interp, errorMsg);
							chan.write(interp, "\n");
							chan.write(interp, "    Error in bgerror: ");
							chan.write(interp, interp.getResult());
							chan.write(interp, "\n");
						}
						chan.flush(interp);
					} catch (TclException | IOException e1) {

						// Ignore.

					}
					break;

				case TCL.BREAK:

					// Break means cancel any remaining error reports for this
					// interpreter.

					for (int i = errors.size() - 1; i >= 0; i--) {
						BgError bgErr = (BgError) errors.get(i);
						errors.remove(i);
						bgErr.cancel();

						bgErr.errorMsg.release();
						bgErr.errorMsg = null;
						bgErr.errorInfo.release();
						bgErr.errorInfo = null;
						bgErr.errorCode.release();
						bgErr.errorCode = null;
					}
					break;
				}
			}

			interp.release();

			errorMsg.release();
			errorMsg = null;
			errorInfo.release();
			errorInfo = null;
			errorCode.release();
			errorCode = null;
		}

	} // end BgErrorMgr.BgError

} // end BgErrorMgr
