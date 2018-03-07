/*
 * WrappedCommand.java
 *
 *	Wrapper for commands located inside a Jacl interp.
 *
 * Copyright (c) 1999 Mo DeJong.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: WrappedCommand.java,v 1.6 2006/01/26 19:49:18 mdejong Exp $
 */

package tcl.lang;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * A Wrapped Command is like the Command struct defined in the C version in the
 * file generic/tclInt.h. It is "wrapped" around a TclJava Command interface
 * reference. We need to wrap Command references so that we can keep track of
 * sticky issues like what namespace the command is defined in without requiring
 * that every implementation of a Command interface provide method to do this.
 * This class also handles command and execution traces. This class is only used
 * in the internal implementation of Jacl.
 */

public class WrappedCommand {
	/**
	 * Reference to the table that this command is defined inside. The hashKey
	 * member can be used to lookup this WrappedCommand instance in the table of
	 * WrappedCommands. The table member combined with the hashKey member are
	 * are equivilent to the C version's Command->hPtr.
	 */
	public HashMap<String, WrappedCommand> table;
	/**
	 * A string that stores the name of the command. This name is NOT fully
	 * qualified.
	 */
	public String hashKey;

	/**
	 * The namespace where the command is located
	 */
	public Namespace ns;
	/**
	 * The actual command interface that is being wrapped
	 */
	public Command cmd;

	/**
	 * List of command traces on this command
	 */
	private ArrayList<CommandTrace> commandTraces = null;

	/**
	 * List of execution traces on this command
	 */
	private ArrayList<ExecutionTrace> executionTraces = null;

	/**
	 * Set to true while delete traces are being executed to prevent recursive
	 * traces
	 */
	private boolean deleteTraceInProgress = false;

	/**
	 * Set to true while rename traces are being executed to prevent recursive
	 * traces
	 */
	private boolean renameTraceInProgress = false;

	/**
	 * Set to true while an execution trace is in progress on this command
	 */
	private boolean executionTraceInProgress = false;
	
	/**
	 * set to true if a step trace exists on this command
	 */
	private boolean hasStepTrace = false;
	

	/**
	 * Means that the command is in the process of being deleted. Other attempts
	 * to delete the command should be ignored.
	 */
	public boolean deleted;

	/**
	 * List of each imported Command created in another namespace when this
	 * command is imported. These imported commands redirect invocations back to
	 * this command. The list is used to remove all those imported commands when
	 * deleting this "real" command.
	 */
	ImportRef importRef;

	/**
	 * incremented to invalidate any references. that point to this command when
	 * it is renamed, deleted, hidden, or exposed. This field always have a
	 * value in the range 1 to Integer.MAX_VALUE (inclusive). User code should
	 * NEVER modify this value.
	 */
	public int cmdEpoch;

	/**
	 * @return true if there are any command traces on this command
	 */
	public boolean hasCommandTraces() {
		return (commandTraces != null);
	}
	
	/**
	 * Eliminate all the command traces on thsi command
	 */
	public void removeAllCommandTraces() {
		commandTraces = null;
	}
	
	/**
	 * Remove a command trace on this command
	 * 
	 * @param type
	 *            CommandTrace.RENAME or CommandTrace.DELETE
	 * @param callbackCmd
	 *            callback command to remove
	 * @throws TclException
	 */
	public void untraceCommand(int type, String callbackCmd) throws TclException {
		if (commandTraces == null)
			return;
		for (int i = 0; i < commandTraces.size(); i++) {
			CommandTrace trace = commandTraces.get(i);
			if (type == trace.getType() && trace.getCallbackCmd().equals(callbackCmd)) {
				commandTraces.remove(i);
				break;
			}
		}
		if (commandTraces.size() == 0)
			commandTraces = null;
	}

	/**
	 * Set a command trace on this command
	 * 
	 * @param trace
	 *            trace to add to command
	 * @throws TclException
	 */
	public void traceCommand(CommandTrace trace) throws TclException {
		untraceCommand(trace.getType(), trace.getCallbackCmd()); // remove old
		// trace of
		// same
		// type/name
		if (commandTraces == null) {
			commandTraces = new ArrayList<>();
		}
		commandTraces.add(trace);
	}

	/**
	 * Get a list of all command traces on this command
	 * 
	 * @param interp
	 *            interpreter for error messages
	 * @return List, in the form of [trace command info]
	 * @throws TclException
	 */
	public TclObject traceCommandInfo(Interp interp) throws TclException {
		if (commandTraces == null || commandTraces.size() == 0)
			return TclString.newInstance("");
		TclObject rv = TclList.newInstance();
		boolean[] reported = new boolean[commandTraces.size()];
		for (int i = 0; i < reported.length; i++)
			reported[i] = false;

		for (int i = 0; i < commandTraces.size(); i++) {
			if (reported[i])
				continue;
			reported[i] = true;

			TclObject traceInfo = TclList.newInstance();
			TclList.append(interp, rv, traceInfo);

			CommandTrace trace = commandTraces.get(i);
			boolean isDelete = trace.getType() == CommandTrace.DELETE;
			boolean isRename = trace.getType() == CommandTrace.RENAME;

			/* look ahead to see if other type is traced */
			for (int j = i + 1; j < commandTraces.size(); j++) {
				if (reported[j])
					continue;
				CommandTrace trace2 = commandTraces.get(j);
				if (trace2.getCallbackCmd().equals(trace.getCallbackCmd())) {
					reported[j] = true;
					if (trace2.getType() == CommandTrace.DELETE)
						isDelete = true;
					else
						isRename = true;
					break;
				}
			}

			TclObject ops = TclList.newInstance();
			if (isRename)
				TclList.append(interp, ops, TclString.newInstance("rename"));
			if (isDelete)
				TclList.append(interp, ops, TclString.newInstance("delete"));
			TclList.append(interp, traceInfo, ops);
			TclList.append(interp, traceInfo, TclString.newInstance(trace.getCallbackCmd()));
		}
		return rv;
	}

	/**
	 * Call the command traces on this command
	 * 
	 * @param type
	 *            either CommandTrace.DELETE or CommandTrace.RENAME
	 * @param newName
	 *            qualified new name of command, if this is a RENAME
	 */
	void callCommandTraces(int type, String newName) {
		boolean inProgress = false;
		switch (type) {
		case CommandTrace.DELETE:
			inProgress = deleteTraceInProgress;
			deleteTraceInProgress = true;
			break;
		case CommandTrace.RENAME:
			inProgress = renameTraceInProgress;
			renameTraceInProgress = true;
			break;
		}

		/* Fire any command traces */
		if (commandTraces != null && !inProgress && !ns.interp.deleted) {
			String oldCommand = ns.fullName + (ns.fullName.endsWith("::") ? "" : "::") + hashKey;

			/*
			 * Copy the commandTrace array, because it can be modified by the
			 * callbacks
			 */
			Object[] copyOfTraces = commandTraces.toArray();
			for (Object commandTrace : copyOfTraces) {
				((CommandTrace) commandTrace).trace(ns.interp, type, oldCommand, (type == CommandTrace.DELETE ? ""
						: newName));
			}
			ns.interp.resetResult();
		}
		switch (type) {
		case CommandTrace.DELETE:
			deleteTraceInProgress = false;
			break;
		case CommandTrace.RENAME:
			renameTraceInProgress = false;
			break;
		}

	}

	/**
	 * Set an execution trace on this command
	 * 
	 * @param trace
	 *            trace to add to command
	 * @throws TclException
	 */
	public void traceExecution(ExecutionTrace trace) throws TclException {
		/* Remove old trace of same name/type */
		untraceExecution(trace.getType(), trace.getCallbackCmd());
		if (executionTraces == null) {
			executionTraces = new ArrayList<>();
		}
		executionTraces.add(trace);
		if (trace.getType()==ExecutionTrace.ENTERSTEP || trace.getType()==ExecutionTrace.LEAVESTEP)
			hasStepTrace = true;
	}

	/**
	 * Remove an execution trace on this command
	 * 
	 * @param type
	 *            ExecutionTrace.ENTER, ExecutionTrace.LEAVE,
	 *            ExecutionTrace.ENTERSTEP or ExecutionTrace.LEAVESTEP
	 * @param callbackCmd
	 *            callback command to remove
	 * @throws TclException
	 */
	public void untraceExecution(int type, String callbackCmd) throws TclException {
		if (executionTraces == null)
			return;
		for (int i = 0; i < executionTraces.size(); i++) {
			ExecutionTrace trace = executionTraces.get(i);
			if (type == trace.getType() && trace.getCallbackCmd().equals(callbackCmd)) {
				trace.setDeleted(true);
				executionTraces.remove(i);
				break;
			}
		}
		if (executionTraces.size() == 0) {
			hasStepTrace = false;
			executionTraces = null;
		}
		
		/* Reset the hasStepTrace flag */
		if (executionTraces!=null && (type==ExecutionTrace.ENTERSTEP || type==ExecutionTrace.LEAVESTEP)) {
			hasStepTrace = false;
			for (ExecutionTrace trace : executionTraces) {
				if (trace.getType()==ExecutionTrace.ENTERSTEP || trace.getType()==ExecutionTrace.LEAVESTEP) {
					hasStepTrace = true;
					break;
				}
			}
		}
	}

	/**
	 * Call all the execution traces of a particular type in this command
	 * 
	 * @param interp interpreter in which to execute the traces
	 * @param objv argument list to the command being traced
	 * @param type ExecutionTrace.ENTER, ExecutionTrace.ENTERSTEP, ExecutionTrace.LEAVE or ExecutionTrace.LEAVESTEP
	 * @param completionCode for LEAVE and LEAVESTEP, this is the command completion code
	 * @param result interpreter result, for LEAVE and LEAVESTEP calls
	 * @throws TclException
	 */
	void callExecutionTraces(Interp interp, TclObject[] objv, int type, int completionCode, TclObject result)
			throws TclException {
		if (executionTraces != null && executionTraces.size() > 0 && !executionTraceInProgress) {
			executionTraceInProgress = true;

			TclObject commandString = TclList.newInstance();
			TclList.append(interp, commandString, objv, 0, objv.length);

			/* Copy execution traces, because they might be deleted by a trace */
			ExecutionTrace[] copyOfTraces = new ExecutionTrace[0];
			copyOfTraces = executionTraces.toArray(copyOfTraces);

			try {
				/* ENTER and ENTERSTEP are in reverse order */
				if (type == ExecutionTrace.ENTER || type == ExecutionTrace.ENTERSTEP) {
					for (int i = copyOfTraces.length - 1; i >= 0; --i) {
						copyOfTraces[i].trace(interp, type, commandString.toString(), completionCode, result);
					}
				} else {
					for (ExecutionTrace copyOfTrace : copyOfTraces) {
						copyOfTrace.trace(interp, type, commandString.toString(), completionCode, result);
					}
				}
			} finally {
				executionTraceInProgress = false;
			}

		}
	}

	/**
	 * Get a list of all execution traces on this command
	 * 
	 * @param interp
	 *            interpreter for error messages
	 * @return List, in the form of [trace execution info]
	 * @throws TclException
	 */
	public TclObject traceExecutionInfo(Interp interp) throws TclException {
		if (executionTraces == null || executionTraces.size() == 0)
			return TclString.newInstance("");
		TclObject rv = TclList.newInstance();
		boolean[] reported = new boolean[executionTraces.size()];
		for (int i = 0; i < reported.length; i++)
			reported[i] = false;

		for (int i = 0; i < executionTraces.size(); i++) {
			if (reported[i])
				continue;
			reported[i] = true;

			TclObject traceInfo = TclList.newInstance();
			TclList.append(interp, rv, traceInfo);

			ExecutionTrace trace = executionTraces.get(i);
			boolean[] traceTypes = new boolean[4];
			for (int k = 0; k < 3; k++)
				traceTypes[k] = false;
			traceTypes[trace.getType()] = true;

			/* look ahead to see if other types are traced */
			for (int j = i + 1; j < executionTraces.size(); j++) {
				if (reported[j])
					continue;
				ExecutionTrace trace2 = executionTraces.get(j);
				if (trace2.getCallbackCmd().equals(trace.getCallbackCmd())) {
					reported[j] = true;
					traceTypes[trace2.getType()] = true;
				}
			}

			TclObject ops = TclList.newInstance();
			if (traceTypes[ExecutionTrace.ENTER])
				TclList.append(interp, ops, TclString.newInstance("enter"));
			if (traceTypes[ExecutionTrace.ENTERSTEP])
				TclList.append(interp, ops, TclString.newInstance("enterstep"));
			if (traceTypes[ExecutionTrace.LEAVE])
				TclList.append(interp, ops, TclString.newInstance("leave"));
			if (traceTypes[ExecutionTrace.LEAVESTEP])
				TclList.append(interp, ops, TclString.newInstance("leavestep"));

			TclList.append(interp, traceInfo, ops);
			TclList.append(interp, traceInfo, TclString.newInstance(trace.getCallbackCmd()));
		}
		return rv;
	}

	/**
	 * @return true if the caller must call WrappedCommand.invoke(Interp,
	 *         TclObject[]) because there are execution traces in effect. Note
	 *         that it is always OK to call invoke() instead of cmd.cmdProc(),
	 *         but doing so adds an extra Java stack frame that may limit how
	 *         deep Tcl code stack frames can go.
	 */
	public final boolean mustCallInvoke(Interp interp) {
		return ((executionTraces != null && !executionTraceInProgress) || interp.hasActiveExecutionStepTraces());
	}

	/**
	 * Call cmd.cmdProc after calling execution traces on this command, and call
	 * execution traces after this command exits. Application code should use
	 * this call, rather than directly calling Command.cmdProc(Interp,
	 * TclObject[]) which does not fire execution traces.
	 * 
	 * @param interp
	 *            The interpreter containing the context for thie command, and
	 *            where results are returned
	 * @param objv
	 *            argument list for command; objv[0] is the command name itself
	 * @throws TclException
	 *             on any errors
	 */
	public void invoke(Interp interp, TclObject[] objv) throws TclException {
		if (!mustCallInvoke(interp)) {
			cmd.cmdProc(interp, objv); // bypass all trace stuff
			return;
		} else {
			TclException savedException = null;
			
			/* Call ENTERSTEP traces */
			if (interp.hasActiveExecutionStepTraces()) {
				/* Call any step traces set up higher in the stack frame */
				WrappedCommand [] cmds = interp.getCopyOfActiveExecutionStepTraces();
				for (WrappedCommand cmd : cmds) {
					cmd.callExecutionTraces(interp, objv, ExecutionTrace.ENTERSTEP, TCL.OK, null);
				}
			}
			
			/* Call any ENTER traces for this command.  Turn off step tracing inside of enter traces (trace.test trace-34.6) */
			interp.enableExecutionStepTracing(false);
			try {
				callExecutionTraces(interp, objv, ExecutionTrace.ENTER, TCL.OK, null);
			} finally {
				interp.enableExecutionStepTracing(true);
			}

			/* A trace may have deleted the command */
			if (deleted) {
				/* See if command was destroyed and re-created */
				WrappedCommand newCmd = Namespace.findCommand(interp, objv[0].toString(), this.ns, 0);
				if (newCmd==null)
					throw new TclException(interp, "invalid command name \"" + objv[0] + "\"");
				else {
					newCmd.invoke(interp, objv);
					return;
				}
			}
				
			boolean hadStepTrace = hasStepTrace;
			
			if (hasStepTrace) 
				hadStepTrace = interp.activateExecutionStepTrace(this);
			
			try {
				cmd.cmdProc(interp, objv);
			} catch (TclException e) {
				savedException = e;
			}
			
			if (hadStepTrace) interp.deactivateExecutionStepTrace(this);
			
			TclObject savedResult = interp.getResult();
			savedResult.preserve();
			int savedReturnCode = interp.returnCode;
			// preserve errAlreadyLogged so ::errorInfo is preserved
			boolean errAlreadyLogged = interp.errAlreadyLogged;

			try {
				/* Call LEAVE traces */
				callExecutionTraces(interp, objv, ExecutionTrace.LEAVE, savedException == null ? TCL.OK : savedException
						.getCompletionCode(), savedResult);
	
				/* Call any LEAVESTEP traces */
				if (interp.hasActiveExecutionStepTraces()) {
					/* Call any step traces set up higher in the stack frame */
					WrappedCommand [] cmds = interp.getCopyOfActiveExecutionStepTraces();
					for (WrappedCommand cmd : cmds) {
						cmd.callExecutionTraces(interp, objv, ExecutionTrace.LEAVESTEP, savedException == null ? TCL.OK : savedException
								.getCompletionCode(), savedResult);
					}
				}
			} catch (TclException e) {
				// ignore error
			}
			
			interp.errAlreadyLogged = errAlreadyLogged;
			interp.returnCode = savedReturnCode;
			interp.setResult(savedResult);
			savedResult.release();
			if (savedException != null)
				throw savedException;
		}
	}

	/**
	 * Increment the cmdProch field. This method is used by the interpreter to
	 * indicate that a command was hidden, renamed, or deleted.
	 */

	void incrEpoch() {
		cmdEpoch++;
		if (cmdEpoch == Integer.MIN_VALUE) {
			// Integer overflow, really unlikely but possible.
			cmdEpoch = 1;
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append("Wrapper for ");
		if (ns != null) {
			sb.append(ns.fullName);
			if (!ns.fullName.equals("::")) {
				sb.append("::");
			}
		}
		if (table != null) {
			sb.append(hashKey);
		}

		sb.append(" -> ");
		sb.append(cmd.getClass().getName());

		sb.append(" cmdEpoch is ");
		sb.append(cmdEpoch);

		return sb.toString();
	}
}
