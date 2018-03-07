package tcl.lang;

/**
 * This class is used to trace command execution
 * 
 */
public class ExecutionTrace {
	/**
	 * Indicates an ExecutionTrace that will execute on a command entry
	 */
	public static final int ENTER = 0;
	/**
	 * Indicates a ExecutionTrace that will execute on a command exit
	 */
	public static final int LEAVE = 1;
	/**
	 * Indicates a ExecutionTrace that will execute on before each command in a
	 * command
	 * 
	 */
	public static final int ENTERSTEP = 2;
	/**
	 * Indicates a ExecutionTrace that will execute after each command in a
	 * command
	 */
	public static final int LEAVESTEP = 3;

	/**
	 * op names to pass to callback command, indexed by type
	 */
	private static final String op[] = { "enter", "leave", "enterstep", "leavestep" };

	/**
	 * Command to call when trace is fired
	 */
	private String callbackCmd;

	/**
	 * The type of trace, either ENTER, LEAVE, ENTERSTEP, or LEAVESTEP
	 */
	protected int type;
	
	/**
	 * set to true if deleted, to prevent execution
	 */
	protected boolean deleted = false;

	/**
	 * Create a new ExecutionTrace
	 * 
	 * @param type
	 *            either ENTER, LEAVE, ENTERSTEP, or LEAVESTEP
	 * @param callbackCmd
	 *            command to execute when fired
	 * @throws TclException
	 */
	public ExecutionTrace(Interp interp, int type, TclObject callbackCmd) throws TclException {
		this.type = type;
		this.callbackCmd = callbackCmd.toString();
	}

	/**
	 * @return the type of this trace, either ENTER, LEAVE, ENTERSTEP, or
	 *         LEAVESTEP
	 */
	public int getType() {
		return type;
	}

	/**
	 * @return the callback command for this trace
	 */
	public String getCallbackCmd() {
		return callbackCmd;
	}

	/**
	 * @param deleted if true, this trace will no longer be executed
	 */
	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}
	/**
	 * Call the callback function for this ExecutionTrace if it matches type.
	 * 
	 * @param interp
	 * @param type
	 *            type of trace being executedeither ENTER, LEAVE, ENTERSTEP, or
	 *            LEAVESTEP
	 * @param commandString
	 * @param resultCode
	 * @param result
	 */
	public void trace(Interp interp, int type, String commandString, int resultCode, TclObject result)
			throws TclException {
		if (type == this.type && ! deleted) {
			StringBuilder sb = new StringBuilder(callbackCmd.length() + commandString.length()
					+ (result == null ? 0 : result.toString().length()) + 16);
			sb.append(callbackCmd).append(" {").append(commandString).append("} ");
			switch (type) {
			case ENTER:
			case ENTERSTEP:
				sb.append(op[type]);
				break;
			case LEAVE:
			case LEAVESTEP:
				String resultString = result.toString();
				if (resultString.length()==0) resultString = "{}";
				sb.append(resultCode).append(" ").append(resultString).append(" ").append(op[type]);
				break;
			}
			interp.eval(sb.toString(), 0);
		}
	}
}
