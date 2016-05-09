/*
 * AfterCmd.java --
 *
 *	Implements the built-in "after" Tcl command.
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: AfterCmd.java,v 1.9 2006/06/13 06:52:47 mdejong Exp $
 *
 */

package tcl.lang.cmd;

import java.util.ArrayList;

import tcl.lang.AssocData;
import tcl.lang.Command;
import tcl.lang.IdleHandler;
import tcl.lang.Interp;
import tcl.lang.Notifier;
import tcl.lang.StrtoulResult;
import tcl.lang.TCL;
import tcl.lang.TclException;
import tcl.lang.TclIndex;
import tcl.lang.TclInteger;
import tcl.lang.TclList;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;
import tcl.lang.TclRuntimeError;
import tcl.lang.TclString;
import tcl.lang.TimerHandler;
import tcl.lang.Util;

/*
 * This class implements the built-in "after" command in Tcl.
 */

public class AfterCmd implements Command {

	/*
	 * The list of handler are stored as AssocData in the interp.
	 */

	AfterAssocData assocData = null;

	/*
	 * Valid command options.
	 */

	static final private String validOpts[] = { "cancel", "idle", "info" };

	static final int OPT_CANCEL = 0;
	static final int OPT_IDLE = 1;
	static final int OPT_INFO = 2;

	/**
	 * This procedure is invoked as part of the Command interface to process the
	 * "after" Tcl command. See the user documentation for details on what it
	 * does.
	 * 
	 * Results: None.
	 * 
	 * Side effects: See the user documentation.
	 */

	public void cmdProc(Interp interp, // Current interpreter.
			TclObject argv[]) // Argument list.
			throws TclException // A standard Tcl exception.
	{
		int i;
		Notifier notifier = (Notifier) interp.getNotifier();
		Object info;

		if (assocData == null) {
			/*
			 * Create the "after" information associated for this interpreter,
			 * if it doesn't already exist.
			 */

			assocData = (AfterAssocData) interp.getAssocData("tclAfter");
			if (assocData == null) {
				assocData = new AfterAssocData();
				interp.setAssocData("tclAfter", assocData);
			}
		}

		if (argv.length < 2) {
			throw new TclNumArgsException(interp, 1, argv,
					"option ?arg arg ...?");
		}

		/*
		 * First lets see if the command was passed a number as the first
		 * argument.
		 */

		boolean isNumber = false;
		long ms = 0;

		/* Don't convert argv[1] to integer, if we can avoid it */
		if (argv[1].isIntType()) {
			ms = TclInteger.getLong(interp, argv[1]);
			isNumber = true;
		} else {
			String s = argv[1].toString();
			if ((s.length() > 0) && (Character.isDigit(s.charAt(0)) || s.charAt(0)=='-')) {
				ms = TclInteger.getLong(interp, argv[1]);
				isNumber = true;
			}
		}

		if (isNumber) {
			if (ms < 0) {
				ms = 0;
			}
			if (argv.length == 2) {
				/*
				 * Sleep for at least the given milliseconds and return.
				 */

				long endTime = System.currentTimeMillis() + ms;
				while (true) {
					try {
						Thread.sleep(ms);
						return;
					} catch (InterruptedException e) {
						/*
						 * We got interrupted. Sleep again if we havn't slept
						 * long enough yet.
						 */

						long sysTime = System.currentTimeMillis();
						if (sysTime >= endTime) {
							return;
						}
						ms =  endTime - sysTime;
						continue;
					}
				}
			}

			TclObject cmd = getCmdObject(argv);
			cmd.preserve();

			assocData.lastAfterId++;
			TimerInfo timerInfo = new TimerInfo(notifier, ms);
			timerInfo.interp = interp;
			timerInfo.command = cmd;
			timerInfo.id = assocData.lastAfterId;

			assocData.handlers.add(timerInfo);

			interp.setResult("after#" + timerInfo.id);

			return;
		}

		/*
		 * If it's not a number it must be a subcommand.
		 */

		int index;

		try {
			index = TclIndex.get(interp, argv[1], validOpts, "option", 0);
		} catch (TclException e) {
			throw new TclException(interp, "bad argument \"" + argv[1]
					+ "\": must be cancel, idle, info, or a number");
		}

		switch (index) {
		case OPT_CANCEL:
			if (argv.length < 3) {
				throw new TclNumArgsException(interp, 2, argv, "id|command");
			}

			TclObject arg = getCmdObject(argv);
			arg.preserve();

			/*
			 * Search the timer/idle handler by id or by command.
			 */

			info = null;
			for (i = 0; i < assocData.handlers.size(); i++) {
				Object obj = assocData.handlers.get(i);
				if (obj instanceof TimerInfo) {
					TclObject cmd = ((TimerInfo) obj).command;

					if ((cmd == arg) || cmd.toString().equals(arg.toString())) {
						info = obj;
						break;
					}
				} else {
					TclObject cmd = ((IdleInfo) obj).command;

					if ((cmd == arg) || cmd.toString().equals(arg.toString())) {
						info = obj;
						break;
					}
				}
			}
			if (info == null) {
				info = getAfterEvent(interp, arg.toString());
			}
			arg.release();

			/*
			 * Cancel the handler.
			 */

			if (info != null) {
				if (info instanceof TimerInfo) {
					TimerInfo ti = (TimerInfo) info;
					ti.cancel();
					ti.command.release();
				} else {
					IdleInfo ii = (IdleInfo) info;
					ii.cancel();
					ii.command.release();
				}

				int hindex = assocData.handlers.indexOf(info);
				if (hindex == -1) {
					throw new TclRuntimeError("info " + info
							+ " has no handler");
				}
				if (assocData.handlers.remove(hindex) == null) {
					throw new TclRuntimeError("cound not remove handler "
							+ hindex);
				}
			}
			break;

		case OPT_IDLE:
			if (argv.length < 3) {
				throw new TclNumArgsException(interp, 2, argv,
						"script script ...");
			}

			TclObject cmd = getCmdObject(argv);
			cmd.preserve();
			assocData.lastAfterId++;

			IdleInfo idleInfo = new IdleInfo(notifier);
			idleInfo.interp = interp;
			idleInfo.command = cmd;
			idleInfo.id = assocData.lastAfterId;

			assocData.handlers.add(idleInfo);

			interp.setResult("after#" + idleInfo.id);
			break;

		case OPT_INFO:
			if (argv.length == 2) {
				/*
				 * No id is given. Return a list of current after id's.
				 */

				TclObject list = TclList.newInstance();
				for (i = 0; i < assocData.handlers.size(); i++) {
					int id;
					Object obj = assocData.handlers.get(i);
					if (obj instanceof TimerInfo) {
						id = ((TimerInfo) obj).id;
					} else {
						id = ((IdleInfo) obj).id;
					}
					TclList.append(interp, list, TclString.newInstance("after#"
							+ id));
				}
				interp.setResult(list);
				return;
			}
			if (argv.length != 3) {
				throw new TclNumArgsException(interp, 2, argv, "?id?");
			}

			/*
			 * Return command and type of the given after id.
			 */

			info = getAfterEvent(interp, argv[2].toString());
			if (info == null) {
				throw new TclException(interp, "event \"" + argv[2]
						+ "\" doesn't exist");
			}
			TclObject list = TclList.newInstance();
			TclList.append(interp, list,
					((info instanceof TimerInfo) ? ((TimerInfo) info).command
							: ((IdleInfo) info).command));
			TclList.append(interp, list,
					TclString.newInstance((info instanceof TimerInfo) ? "timer"
							: "idle"));

			interp.setResult(list);
			break;
		}
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * getCmdObject --
	 * 
	 * Returns a TclObject that contains the command script passed to the
	 * "after" command.
	 * 
	 * Results: A concatenation of the objects from argv[2] through argv[n-1].
	 * 
	 * Side effects: None.
	 * 
	 * ----------------------------------------------------------------------
	 */

	private TclObject getCmdObject(TclObject argv[]) // Argument list passed to
			// the "after" command.
			throws TclException {
		if (argv.length == 3) {
			return argv[2];
		} else {
			TclObject cmd = Util.concat(2, argv.length - 1, argv);
			return cmd;
		}
	}

	/**
	 * This procedure parses an "after" id such as "after#4" and returns an
	 * AfterInfo object.
	 * 
	 * @param interp
	 *            current interpreter
	 * @param string
	 *            text identifier for event, such as "after#6"
	 * 
	 * @return an AfterInfo object. if one is found that corresponds to
	 *         "string", or null if no corresponding after event can be found.
	 * 
	 */
	private Object getAfterEvent(Interp interp, String string) // Textual
	// identifier
	// for after
	// event, such
	// as "after#6".
	{
	
		if (!string.startsWith("after#")) {
			return null;
		}
		int id = 0;
		try {
			/* get the integer after "after#", throw exception
			 * if the remainder of the string is not an integer
			 */
			id = Integer.parseInt(string.substring(6));
		} catch (Exception e) {
			return null;
		}
		for (int i = 0; i < assocData.handlers.size(); i++) {
			Object obj = assocData.handlers.get(i);
			if (obj instanceof TimerInfo) {
				if (((TimerInfo) obj).id == id) {
					return obj;
				}
			} else {
				if (((IdleInfo) obj).id == id) {
					return obj;
				}
			}
		}

		return null;
	}

	/**
	 * This inner class manages the list of handlers created by the "after"
	 * command. We keep the handler has an AssocData so that they will continue
	 * to exist even if the "after" command is deleted.
	 */

	class AfterAssocData implements AssocData {

		/**
		 * The set of handlers created but not yet fired.
		 */
		ArrayList handlers = new ArrayList();

		/**
		 * Timer identifier of most recently created timer.
		 */
		int lastAfterId = 0;

		/**
		 * This method is called when the interpreter is destroyed or when
		 * Interp.deleteAssocData is called on a registered AssocData instance.
		 * 
		 * Results: None.
		 * 
		 * Side effects: All unfired handler are cancelled; their command
		 * objects are released.
		 * 
		 * @param interp the interpreter in which this AssocData instance is registered in
		 */
		public void disposeAssocData(Interp interp) 
		{
			for (int i = assocData.handlers.size() - 1; i >= 0; i--) {
				Object info = assocData.handlers.get(i);
				if (assocData.handlers.remove(i) == null) {
					throw new TclRuntimeError("cound not remove handler " + i);
				}
				if (info instanceof TimerInfo) {
					TimerInfo ti = (TimerInfo) info;
					ti.cancel();
					ti.command.release();
				} else {
					IdleInfo ii = (IdleInfo) info;
					ii.cancel();
					ii.command.release();
				}
			}
			assocData = null;
		}

	} // end AfterCmd.AfterAssocData

	/**
	 * This inner class implement timer handlers for the "after" command. It
	 * stores a script to be executed when the timer event is fired.
	 */
	class TimerInfo extends TimerHandler {

		/**
		 * Interpreter in which the script should be executed.
		 */
		Interp interp;

		/**
		 * Command to execute when the timer fires.
		 */
		TclObject command;

		/**
		 * Integer identifier for command; used to cancel it.
		 */
		int id;


		/**
		 * Constructs a new TimerInfo instance.
		 * 
		 * @paran n The notifier to fire the event
		 * @paran milliseconds number of milliseconds to wait before invoking
		 *        processTimerEvent()
		 */
		TimerInfo(Notifier n, long milliseconds)
		{
			super(n, milliseconds);
		}

		/**
		 *  Execute the command for this timer event
		 */

		public void processTimerEvent() {
			try {
				int index = assocData.handlers.indexOf(this);
				if (index == -1) {
					throw new TclRuntimeError("this " + this
							+ " has no handler");
				}
				if (assocData.handlers.remove(index) == null) {
					throw new TclRuntimeError("cound not remove handler "
							+ index);
				}
				interp.eval(command, TCL.EVAL_GLOBAL);
			} catch (TclException e) {
				interp.addErrorInfo("\n    (\"after\" script)");
				interp.backgroundError();
			} finally {
				command.release();
				command = null;
			}
		}

		public String toString() {
			StringBuilder sb = new StringBuilder(64);
			sb.append(super.toString());
			sb.append("AfterCmd.TimerInfo : " + command + "\n");
			return sb.toString();
		}

	} // end AfterCmd.AfterInfo

	/**
	 * This inner class implement idle handlers for the "after" command. It
	 * stores a script to be executed when the idle event is fired.
	 */
	class IdleInfo extends IdleHandler {

		/**
		 * Interpreter in which the script should be executed.
		 */
		Interp interp;

		/**
		 * Command to execute when the idle event fires.
		 */
		TclObject command;

		/**
		 * Integer identifier for command; used to cancel it.
		 */
		int id;

		/**
		 * Construct a new IdleInfo event
		 * 
		 * @param n notifier to fire the event
		 */
		IdleInfo(Notifier n) // The notifier to fire the event.
		{
			super(n);
		}

		/**
		 * Run the idle command
		 */
		public void processIdleEvent() {
			try {
				int index = assocData.handlers.indexOf(this);
				if (index == -1) {
					throw new TclRuntimeError("this " + this
							+ " has no handler");
				}
				if (assocData.handlers.remove(index) == null) {
					throw new TclRuntimeError("cound not remove handler "
							+ index);
				}
				interp.eval(command, TCL.EVAL_GLOBAL);
			} catch (TclException e) {
				interp.addErrorInfo("\n    (\"after\" script)");
				interp.backgroundError();
			} finally {
				command.release();
				command = null;
			}
		}

		public String toString() {
			StringBuilder sb = new StringBuilder(64);
			sb.append(super.toString());
			sb.append("AfterCmd.IdleInfo : " + command + "\n");
			return sb.toString();
		}

	} // end AfterCmd.AfterInfo

} // end AfterCmd
