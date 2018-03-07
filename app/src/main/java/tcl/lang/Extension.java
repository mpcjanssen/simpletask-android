/*
 * Extension.java --
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: Extension.java,v 1.5 2006/04/13 07:36:50 mdejong Exp $
 *
 */

package tcl.lang;

/**
 * Base class for all Tcl Extensions. A Tcl Extension defines a set of commands
 * that can be loaded into an Interp as a single unit.
 * 
 * When a Tcl Extension is loaded into an Interp, either statically (using the
 * "new" operator inside Java code) or dynamically (using the java::load command
 * in Tcl scripts), it usually creates a set of commands inside the interpreter.
 * Occasionally, loading an Extension may lead to additional side effects. For
 * example, a communications Extension may open network connections when it's
 * loaded. Please refer to the documentation of the specific Extension for
 * details.
 */

abstract public class Extension {

	/**
	 * Default constructor. Does nothing. The purpose of this constructor is to
	 * make sure instances of this Extension can be loaded dynamically using the
	 * "java::load" command, which calls Class.newInstance().
	 */

	public Extension() {
	}

	/**
	 * Initialize the Extension to run in a normal (unsafe) interpreter. This
	 * usually means creating all the commands provided by this class. A
	 * particular implementation can arrange the commands to be loaded on-demand
	 * using the loadOnDemand() function.
	 * 
	 * @param interp
	 *            current interpreter.
	 */

	abstract public void init(Interp interp) throws TclException;

	/**
	 * Initialize the Extension to run in a safe interpreter. This method should
	 * be written carefully, so that it initializes the safe interpreter only
	 * with partial functionality provided by the Extension that is safe for use
	 * by untrusted code.
	 * 
	 * The default implementation always throws a TclException, so that a
	 * subclass of Extension cannot be loaded into a safe interpreter unless it
	 * has overridden the safeInit() method.
	 * 
	 * @param safeInterp
	 *            the safe interpreter in which the Extension should be
	 *            initialized.
	 */

	public void safeInit(Interp safeInterp) throws TclException {
		throw new TclException(safeInterp, "Extension \""
				+ getClass().toString()
				+ "\" cannot be loaded into a safe interpreter");
	}

	/**
	 * Create a stub command which autoloads the real command the first time the
	 * stub command is invoked. Register the stub command in the interpreter.
	 * 
	 * @param interp
	 *            current interp.
	 * @param cmdName
	 *            name of the command, e.g., "after".
	 * @param clsName
	 *            name of the Java class that implements this command, e.g.
	 *            "tcl.lang.AfterCmd"
	 */

	public static final void loadOnDemand(Interp interp, String cmdName,
			String clsName) {
		interp.createCommand(cmdName, new AutoloadStub(clsName));
	}
}

/**
 * The purpose of AutoloadStub is to load-on-demand the classes that implement
 * Tcl commands. This reduces Jacl start up time and, when running Jacl off a
 * web page, reduces download time significantly.
 */

class AutoloadStub implements Command {
	String className;

	/**
	 * Create a stub command which autoloads the real command the first time the
	 * stub command is invoked.
	 * 
	 * @param clsName
	 *            name of the Java class that implements this command, e.g.
	 *            "tcl.lang.AfterCmd"
	 */
	AutoloadStub(String clsName) {
		className = clsName;
	}

	/**
	 * Load the class that implements the given command and execute it.
	 * 
	 * @param interp
	 *            the current interpreter.
	 * @param argv
	 *            command arguments.
	 * @exception TclException
	 *                if error happens inside the real command proc.
	 */
	public void cmdProc(Interp interp, TclObject[] objv) throws TclException {
		Command cmd = load(interp, objv[0].toString());
		// don't call via WrappedCommand.invoke() because this cmdProc was already
		// called with invoke
		cmd.cmdProc(interp, objv);
	}

	/**
	 * Load the class that implements the given command, create the command in
	 * the interpreter, and return. This helper method is provided so to handle
	 * the case where a command wants to create a stub command without executing
	 * it. The qname argument should be the fully qualified name of the command.
	 */

	Command load(Interp interp, String qname) throws TclException {
		Class cmdClass = null;
		Command cmd;

		try {
			TclClassLoader classLoader = (TclClassLoader) interp
					.getClassLoader();
			cmdClass = classLoader.loadClass(className);
		} catch (ClassNotFoundException e) {
			throw new TclException(interp,
					"ClassNotFoundException for class \"" + className + "\"");
		} catch (PackageNameException e) {
			throw new TclException(interp, "PackageNameException for class \""
					+ className + "\"");
		}

		try {
			cmd = (Command) cmdClass.newInstance();
		} catch (IllegalAccessException e1) {
			throw new TclException(interp,
					"IllegalAccessException for class \"" + cmdClass.getName()
							+ "\"");
		} catch (InstantiationException e2) {
			throw new TclException(interp,
					"InstantiationException for class \"" + cmdClass.getName()
							+ "\"");
		} catch (ClassCastException e3) {
			throw new TclException(interp, "ClassCastException for class \""
					+ cmdClass.getName() + "\"");
		}
		interp.createCommand(qname, cmd);
		return cmd;
	}
}
