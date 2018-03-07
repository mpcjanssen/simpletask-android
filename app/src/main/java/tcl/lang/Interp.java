package tcl.lang;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownServiceException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;

import tcl.lang.channel.Channel;
import tcl.lang.channel.FileChannel;
import tcl.lang.channel.ReadInputStreamChannel;
import tcl.lang.cmd.EncodingCmd;
import tcl.lang.cmd.InterpAliasCmd;
import tcl.lang.cmd.InterpSlaveCmd;
import tcl.lang.cmd.PackageCmd;
import tcl.lang.cmd.RegexpCmd;

/**
 * Implements the core Tcl interpreter.
 * 
 * Copyright (c) 1997 Cornell University. Copyright (c) 1997-1998 Sun Microsystems, Inc.
 * 
 * See the file "license.terms" for information on usage and redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 */

public class Interp extends EventuallyFreed {

	// The following three variables are used to maintain a translation
	// table between ReflectObject's and their string names. These
	// variables are accessed by the ReflectObject class, they
	// are defined here be cause we need them to be per interp data.

	/**
	 * Translates Object to ReflectObject. This makes sure we have only one ReflectObject internalRep for the same
	 * Object -- this way Object identity can be done by string comparison.
	 */
	public HashMap reflectObjTable = new HashMap();

	/**
	 * Number of reflect objects created so far inside this Interp (including those that have be freed)
	 */
	public long reflectObjCount = 0;

	/**
	 * Table used to store reflect hash index conflicts, see ReflectObject implementation for more details
	 */
	public HashMap reflectConflictTable = new HashMap();

	/**
	 * The number of chars to copy from an offending command into error message.
	 */
	private static final int MAX_ERR_LENGTH = 200;

	/**
	 * We pretend this is Tcl 8.4
	 */
	static final String TCL_VERSION = "8.4";
	/**
	 * We pretend this is Tcl 8.4, patch level 0.
	 */
	static final String TCL_PATCH_LEVEL = "8.4.0";

	/**
	 * Total number of times a command procedure has been called for this interpreter.
	 */
	public int cmdCount;

	/**
	 * Table of channels currently registered in this interp.
	 */
	public HashMap<String, Channel> interpChanTable;

	/**
	 * Set to true if [encoding system] can set the encoding for stdout and stderr. This is an attempt to replicate TCL
	 * behavior seen in encoding.test encoding-24.3
	 */
	public boolean systemEncodingChangesStdoutStderr = true;
	/**
	 * The Notifier associated with this Interp.
	 */
	private Notifier notifier;

	/**
	 * Hash table for associating data with this interpreter. Cleaned up when this interpreter is deleted
	 */
	HashMap<String, AssocData> assocData;

	/**
	 * Current working directory.
	 */
	private File workingDir;

	/**
	 * Points to top-most in stack of all nested procedure invocations. null means there are no active procedures.
	 */
	public CallFrame frame;

	/**
	 * Points to the call frame whose variables are currently in use (same as frame unless an "uplevel" command is being
	 * executed). null means no procedure is active or "uplevel 0" is being exec'ed.
	 */
	public CallFrame varFrame;

	/**
	 * The interpreter's global namespace.
	 */
	Namespace globalNs;

	/**
	 * Hash table used to keep track of hidden commands on a per-interp basis.
	 */
	public HashMap hiddenCmdTable;

	/**
	 * Information used by InterpCmd.java to keep track of master/slave interps on a per-interp basis.
	 * 
	 * Keeps track of all interps for which this interp is the Master. First, slaveTable (a hashtable) maps from names
	 * of commands to slave interpreters. This hashtable is used to store information about slave interpreters of this
	 * interpreter, to map over all slaves, etc.
	 */

	public HashMap slaveTable;

	/**
	 * Hash table for Target Records. Contains all Target records which denote aliases from slaves or sibling
	 * interpreters that direct to commands in this interpreter. This table is used to remove dangling pointers from the
	 * slave (or sibling) interpreters when this interpreter is deleted.
	 */

	public HashMap targetTable;

	/**
	 * Information necessary for this interp to function as a slave.
	 */
	public InterpSlaveCmd slave;

	/**
	 * Table which maps from names of commands in slave interpreter to InterpAliasCmd objects.
	 */
	public HashMap aliasTable;

	// FIXME : does globalFrame need to be replaced by globalNs?
	// Points to the global variable frame.

	// CallFrame globalFrame;

	/**
	 * The script file currently under execution. Can be null if the interpreter is not evaluating any script file.
	 */
	public String scriptFile;

	/**
	 * Number of times the interp.eval() routine has been recursively invoked.
	 */
	public int nestLevel;

	/**
	 * Used to catch infinite loops in Parser.eval2.
	 */

	private int maxNestingDepth = 1000;

	/** Flags used when evaluating a command. */

	public int evalFlags;

	/**
	 * Flags used when evaluating a command.
	 */
	int flags;

	/** Is this interpreted marked as safe? */

	public boolean isSafe;

	/**
	 * Offset of character just after last one compiled or executed by Parser.eval2().
	 */
	public int termOffset;

	// List of name resolution schemes added to this interpreter.
	// Schemes are added/removed by calling addInterpResolver and
	// removeInterpResolver.

	ArrayList resolvers;

	/**
	 * The expression parser for this interp.
	 */
	public Expression expr;

	/**
	 * Used by the Expression class. If it is equal to zero, then the parser will evaluate commands and retrieve
	 * variable values from the interp.
	 */
	int noEval;

	/**
	 * Used in the Expression.java file for the SrandFunction.class and RandFunction.class. Set to true if a seed has
	 * been set.
	 */

	boolean randSeedInit;

	/**
	 * Used in the Expression.java file for the SrandFunction.class and RandFunction.class. Stores the value of the
	 * seed.
	 */

	long randSeed;

	/**
	 * If returnCode is TCL.ERROR, stores the errorInfo.
	 */
	public String errorInfo;

	/**
	 * If returnCode is TCL.ERROR, stores the errorCode.
	 */
	public String errorCode;

	/**
	 * Completion code to return if current procedure exits with a TCL_RETURN code.
	 */
	public int returnCode;

	/**
	 * True means the interpreter has been deleted: don't process any more commands for it, and destroy the structure as
	 * soon as all nested invocations of eval() are done.
	 */
	protected boolean deleted;

	/**
	 * True means an error unwind is already in progress. False means a command proc has been invoked since last error
	 * occured.
	 */
	public boolean errInProgress;

	/**
	 * True means information has already been logged in $errorInfo for the current eval() instance, so eval() needn't
	 * log it (used to implement the "error" command).
	 */
	public boolean errAlreadyLogged;

	/**
	 * True means that addErrorInfo has been called to record information for the current error. False means Interp.eval
	 * must clear the errorCode variable if an error is returned.
	 */

	protected boolean errCodeSet;

	/**
	 * When TCL_ERROR is returned, this gives the line number within the command where the error occurred (1 means first
	 * line).
	 */
	public int errorLine;

	/**
	 * Stores the current result in the interpreter.
	 */

	private TclObject m_result;

	/**
	 * Value m_result is set to when resetResult() is called.
	 */

	private final TclObject m_nullResult;

	// Shared common result values. For common values, it
	// is much better to use a shared TclObject. These
	// common values are used in interp.setResult()
	// methods for built-in Java types. The internal rep
	// of these shared values should not be changed.

	// The boolean true and false constants are tricky.
	// The true value is the integer 1, it is not
	// an instance of TclBoolean with a string rep
	// of "true". The false value is the integer 0.
	// This approach makes it possible for the expr
	// module to treat boolean results as integers.

	private final TclObject m_falseBooleanResult; // false (int 0)
	private final TclObject m_trueBooleanResult; // true (int 1)

	private final TclObject m_minusoneIntegerResult; // -1
	private final TclObject m_zeroIntegerResult; // 0
	private final TclObject m_oneIntegerResult; // 1
	private final TclObject m_twoIntegerResult; // 2

	private final TclObject m_zeroDoubleResult; // 0.0
	private final TclObject m_onehalfDoubleResult; // 0.5
	private final TclObject m_oneDoubleResult; // 1.0
	private final TclObject m_twoDoubleResult; // 2.0

	// Set to true to enable debug code that will double check
	// that each common value is a shared object. It is
	// possible that buggy code might decr the ref count
	// of a shared result so this code would raise an
	// error if that case were detected.

	private final static boolean VALIDATE_SHARED_RESULTS = false;

	// When a method like setResult(int) is invoked with
	// an int that is not a common value, the recycledI
	// TclObject is modified so that it contains the
	// new value. This is much faster than allocating
	// a new TclObject and setResult() and setVar()
	// are performance critical.

	private TclObject recycledI;
	private TclObject recycledD;

	// Common char values wrapped in a TclObject

	private final TclObject[] m_charCommon;
	private final int m_charCommonMax = 128;

	// Java thread this interp was created in. This is used
	// to check for user coding errors where the user tries
	// to create an interp in one thread and then invoke
	// methods from another thread.

	private Thread cThread;
	private String cThreadName;

	// Used ONLY by PackageCmd.

	public HashMap packageTable;
	public String packageUnknown;

	// Used ONLY by the Parser.

	TclObject[][][] parserObjv;
	int[] parserObjvUsed;

	TclToken[] parserTokens;
	int parserTokensUsed;

	/** Used ONLY by JavaImportCmd: classTable, packageTable, wildcardTable */
	public HashMap[] importTable = { new HashMap(), new HashMap(), new HashMap() };

	/**
	 * Used by callers of Util.strtoul(), also used in FormatCmd.strtoul(). There is typically only one instance of a
	 * StrtoulResult around at any one time. Callers should exercise care to use the results before any other code could
	 * call strtoul() again.
	 */
	public StrtoulResult strtoulResult = new StrtoulResult();

	/** Used by callers of Util.strtod(). Usage is same as {@link #strtoulResult} */

	public StrtodResult strtodResult = new StrtodResult();

	/** Used only with Namespace.getNamespaceForQualName() */

	public Namespace.GetNamespaceForQualNameResult getnfqnResult = new Namespace.GetNamespaceForQualNameResult();

	// Cached array object accessed only in Var.lookupVar().
	// This array is returned by Var.lookupVar(), so a ref
	// to it should not be held by the caller for longer than
	// is needed to query the return values.

	Var[] lookupVarResult = new Var[2];

	// List of unsafe commands:

	static final String[] unsafeCmds = { "encoding", "exit", "load", "cd", "fconfigure", "file", "glob", "open", "pwd",
			"socket", "beep", "echo", "ls", "resource", "source", "exec", "jaclloadjava", "jaclloadtjc",
			"auto_execok", "auto_import", "auto_load", "auto_load_index", "auto_qualify"};

	// Flags controlling the call of invoke.

	public static final int INVOKE_HIDDEN = 1;
	public static final int INVOKE_NO_UNKNOWN = 2;
	public static final int INVOKE_NO_TRACEBACK = 4;

	// The ClassLoader for this interp

	TclClassLoader classLoader = null;

	// Map of Tcl library scripts that is initialized
	// the first time a script is loaded. All interps
	// will use the cached value once it has been
	// initialized. Tcl library scripts are located
	// in jacl.jar and tcljava.jar, this logic assumes
	// that they will not change at runtime. This
	// feature can be disabled by changing the
	// USE_SCRIPT_CACHE flag in evalResource to false,
	// but this object must be statically initialized
	// in order to avoid a possible race condition
	// during the first call to evalResource.

	static HashMap tclLibraryScripts = new HashMap();

	// The interruptedEvent field is set after a call
	// to Interp.setInterrupted(). When non-null, this
	// field indicates that the user has requested
	// that the interp execution should be interrupted
	// at the next safe moment.

	private TclInterruptedExceptionEvent interruptedEvent = null;

	/**
	 * List of WrappedCommands that currently have active execution step traces
	 */
	ArrayList<WrappedCommand> activeExecutionStepTraces = null;

	/**
	 * Set to true to enable step tracing, if any exist
	 */
	boolean stepTracingEnabled = true;

	/**
	 * Using System.in directly creates non-interruptible block during System.in.read(). This instance prevents the
	 * read() block. The first instance created will replace System.in with itself, so code doesn't have to use this
	 * instance directly.
	 */
	static public ManagedSystemInStream systemIn = new ManagedSystemInStream();
	
	/**
	 * Temporary file name created for [info nameofexecutable].  Typically set by [info nameofexecutable]
	 */
	private String nameOfExecutable = null;

	/**
	 * Name of the shell class name, used to create [info nameofexecutable].
	 * The constructor sets this values as the name of the main class.
	 */
	private String shellClassName;

	/**
	 * Side effects: Various parts of the interpreter are initialized; built-in commands are created; global variables
	 * are initialized, etc.
	 * 
	 */

	public Interp() {

		// freeProc = null;
		errorLine = 0;

		// An empty result is used pretty often. We will use a shared
		// TclObject instance to represent the empty result so that we
		// don't need to create a new TclObject instance every time the
		// interpreter result is set to empty. Do the same for other
		// common values.

		m_nullResult = TclString.newInstance("");
		m_nullResult.preserve(); // Increment refCount to 1
		m_nullResult.preserve(); // Increment refCount to 2 (shared)
		m_result = m_nullResult; // correcponds to iPtr->objResultPtr

		m_minusoneIntegerResult = TclInteger.newInstance(-1);
		m_minusoneIntegerResult.preserve(); // Increment refCount to 1
		m_minusoneIntegerResult.preserve(); // Increment refCount to 2 (shared)

		m_zeroIntegerResult = TclInteger.newInstance(0);
		m_zeroIntegerResult.preserve(); // Increment refCount to 1
		m_zeroIntegerResult.preserve(); // Increment refCount to 2 (shared)

		m_oneIntegerResult = TclInteger.newInstance(1);
		m_oneIntegerResult.preserve(); // Increment refCount to 1
		m_oneIntegerResult.preserve(); // Increment refCount to 2 (shared)

		m_falseBooleanResult = m_zeroIntegerResult;
		m_trueBooleanResult = m_oneIntegerResult;

		m_twoIntegerResult = TclInteger.newInstance(2);
		m_twoIntegerResult.preserve(); // Increment refCount to 1
		m_twoIntegerResult.preserve(); // Increment refCount to 2 (shared)

		m_zeroDoubleResult = TclDouble.newInstance(0.0);
		m_zeroDoubleResult.preserve(); // Increment refCount to 1
		m_zeroDoubleResult.preserve(); // Increment refCount to 2 (shared)

		m_onehalfDoubleResult = TclDouble.newInstance(0.5);
		m_onehalfDoubleResult.preserve(); // Increment refCount to 1
		m_onehalfDoubleResult.preserve(); // Increment refCount to 2 (shared)

		m_oneDoubleResult = TclDouble.newInstance(1.0);
		m_oneDoubleResult.preserve(); // Increment refCount to 1
		m_oneDoubleResult.preserve(); // Increment refCount to 2 (shared)

		m_twoDoubleResult = TclDouble.newInstance(2.0);
		m_twoDoubleResult.preserve(); // Increment refCount to 1
		m_twoDoubleResult.preserve(); // Increment refCount to 2 (shared)

		// Create common char values wrapped in a TclObject

		m_charCommon = new TclObject[m_charCommonMax];
		for (int i = 0; i < m_charCommonMax; i++) {
			TclObject obj = null;
			if (((i < ((int) ' ')) && (i == ((int) '\t') || i == ((int) '\r') || i == ((int) '\n')))
					|| (i >= ((int) ' ')) && (i <= ((int) '~'))) {

				// Create cached value for '\t' '\r' '\n'
				// and all ASCII characters in the range
				// 32 -> ' ' to 126 -> '~'. Intern each
				// of the String objects so that an equals test
				// like tobj.toString().equals("\n") will
				// refrence compare to true.

				String s = "" + ((char) i);
				s = s.intern();
				obj = TclString.newInstance(s);

				// System.out.println("m_charCommon[" + i + "] is \"" + obj +
				// "\"");
			}

			m_charCommon[i] = obj;

			if (obj != null) {
				obj.preserve();
				obj.preserve();
			}
		}

		// Init recycled TclObject values.

		recycledI = TclInteger.newInstance(0);
		recycledI.preserve(); // refCount is 1 when unused

		recycledD = TclDouble.newInstance(0);
		recycledD.preserve(); // refCount is 1 when unused

		expr = new Expression();
		nestLevel = 0;

		frame = null;
		varFrame = null;

		returnCode = TCL.OK;
		errorInfo = null;
		errorCode = null;

		packageTable = new HashMap();
		packageUnknown = null;
		cmdCount = 0;
		termOffset = 0;
		resolvers = null;
		evalFlags = 0;
		scriptFile = null;
		flags = 0;
		isSafe = false;
		assocData = null;

		globalNs = null; // force creation of global ns below
		globalNs = Namespace.createNamespace(this, null, null);
		if (globalNs == null) {
			throw new TclRuntimeError("Interp(): can't create global namespace");
		}

		// Init things that are specific to the Jacl implementation

		workingDir = new File(Util.tryGetSystemProperty("user.dir", "."));
		noEval = 0;

		cThread = Thread.currentThread();
		cThreadName = cThread.getName();
		notifier = Notifier.getNotifierForThread(cThread);
		notifier.preserve();

		randSeedInit = false;

		deleted = false;
		errInProgress = false;
		errAlreadyLogged = false;
		errCodeSet = false;

		dbg = initDebugInfo();

		slaveTable = new HashMap();
		targetTable = new HashMap();
		aliasTable = new HashMap();
		
		// find the name of constructing class to use as the shell class name
		// can be overridden by setShellClassName()
		Throwable t = new Throwable();
		StackTraceElement[] es = t.getStackTrace();
		shellClassName = es[es.length-1].getClassName();

		// init parser variables
		Parser.init(this);
		TclParse.init(this);

		// Initialize the Global (static) channel table and the local
		// interp channel table.

		interpChanTable = TclIO.getInterpChanTable(this);

		// Sets up the variable trace for tcl_precision.

		Util.setupPrecisionTrace(this);

		// Create the built-in commands.

		createCommands();

		try {
			// Set up tcl_platform, tcl_version, tcl_library and other
			// global variables.

			setVar("tcl_platform", "platform", "java", TCL.GLOBAL_ONLY);
			setVar("tcl_platform", "byteOrder", "bigEndian", TCL.GLOBAL_ONLY);
			setVar("tcl_platform", "user", Util.tryGetSystemProperty("user.name", "unknown"), TCL.GLOBAL_ONLY);

			setVar("tcl_platform", "os", Util.tryGetSystemProperty("os.name", "?"), TCL.GLOBAL_ONLY);
			setVar("tcl_platform", "osVersion", Util.tryGetSystemProperty("os.version", "?"), TCL.GLOBAL_ONLY);
			setVar("tcl_platform", "machine", Util.tryGetSystemProperty("os.arch", "?"), TCL.GLOBAL_ONLY);

			setVar("tcl_version", TCL_VERSION, TCL.GLOBAL_ONLY);
			setVar("tcl_patchLevel", TCL_PATCH_LEVEL, TCL.GLOBAL_ONLY);
			setVar("tcl_library", "resource:/tcl/lang/library", TCL.GLOBAL_ONLY);
			if (Util.isWindows()) {
				setVar("tcl_platform", "host_platform", "windows", TCL.GLOBAL_ONLY);
			} else if (Util.isMac()) {
				setVar("tcl_platform", "host_platform", "macintosh", TCL.GLOBAL_ONLY);
			} else {
				setVar("tcl_platform", "host_platform", "unix", TCL.GLOBAL_ONLY);
			}

			// Create the env array an populated it with proper
			// values.

			Env.initialize(this);

			// Register Tcl's version number. Note: This MUST be
			// done before the call to evalResource, otherwise
			// calls to "package require tcl" will fail.

			pkgProvide("Tcl", TCL_VERSION);

			// Source the init.tcl script to initialize auto-loading.

			evalResource("/tcl/lang/library/init.tcl");

		} catch (TclException e) {
			System.out.println(getResult());
			e.printStackTrace();
			throw new TclRuntimeError("unexpected TclException: " + e);
		}

		// Debug print interp info, this is handy when tracking
		// down where an Interp that was not disposed of properly
		// was allocated.

		if (false) {
			try {
				throw new Exception();
			} catch (Exception e) {
				System.err.println("Interp() : " + this);
				e.printStackTrace(System.err);
			}
		}
	}

	/**
	 * Invoked to indicate that the interp should be disposed of. If there are no Tcl_Preserve calls in effect for this
	 * interpreter, it is deleted immediately, otherwise the interpreter is deleted when the last Tcl_Preserve is
	 * matched by a call to Tcl_Release.
	 * 
	 * Results: None.
	 * 
	 * Side effects: Cleans up the interpreter.
	 */
	public void dispose() {

		// Interp.dispose() must be invoked from thread that invoked Interp()

		if (Thread.currentThread() != cThread) {
			throw new TclRuntimeError("Interp.dispose() invoked in thread other than the one it was created in");
		}

		// Mark the interpreter as deleted. No further evals will be allowed.
		// Note that EventuallyFreed.dispose() is invoked below even if
		// this interpreter has already been marked as deleted since
		// this method can be invoked via EventuallyFreed.release().

		if (!deleted) {
			deleted = true;
		}

		super.dispose();
	}

	/**
	 * This method cleans up the state of the interpreter so that it can be garbage collected safely. This routine needs
	 * to break any circular references that might keep the interpreter alive indefinitely.
	 * 
	 * This proc should never be called directly. Instead it is called via the EventuallyFreed superclass. This method
	 * will only ever be invoked once.
	 * 
	 * Results: None.
	 * 
	 * Side effects: Cleans up the interpreter.
	 * 
	 */

	public void eventuallyDispose() {
		// The interpreter should already be marked deleted; otherwise how did
		// we
		// get here?

		if (!deleted) {
			throw new TclRuntimeError("eventuallyDispose called on interpreter not marked deleted");
		}

		if (nestLevel > 0) {
			throw new TclRuntimeError("dispose() called with active evals");
		}

		// Remove our association with the notifer (if we had one).

		if (notifier != null) {
			notifier.release();
			notifier = null;
		} else {
			throw new TclRuntimeError("eventuallyDispose() already invoked for " + this);
		}

		// Dismantle everything in the global namespace except for the
		// "errorInfo" and "errorCode" variables. These might be needed
		// later on if errors occur while deleting commands. We are careful
		// to destroy and recreate the "errorInfo" and "errorCode"
		// variables, in case they had any traces on them.
		//
		// Dismantle the namespace here, before we clear the assocData. If any
		// background errors occur here, they will be deleted below.

		Namespace.teardownNamespace(globalNs);

		// Delete all variables.

		TclObject errorInfoObj = null, errorCodeObj = null;

		try {
			errorInfoObj = getVar("errorInfo", null, TCL.GLOBAL_ONLY);
		} catch (TclException e) {
			// Do nothing when var does not exist.
		}

		if (errorInfoObj != null) {
			errorInfoObj.preserve();
		}

		try {
			errorCodeObj = getVar("errorCode", null, TCL.GLOBAL_ONLY);
		} catch (TclException e) {
			// Do nothing when var does not exist.
		}

		if (errorCodeObj != null) {
			errorCodeObj.preserve();
		}

		frame = null;
		varFrame = null;

		try {
			if (errorInfoObj != null) {
				setVar("errorInfo", null, errorInfoObj, TCL.GLOBAL_ONLY);
				errorInfoObj.release();
			}
			if (errorCodeObj != null) {
				setVar("errorCode", null, errorCodeObj, TCL.GLOBAL_ONLY);
				errorCodeObj.release();
			}
		} catch (TclException e) {
			// Ignore it -- same behavior as Tcl 8.4
		}

		// Tear down the math function table.

		expr = null;

		// Remove all the assoc data tied to this interp and invoke
		// deletion callbacks; note that a callback can create new
		// callbacks, so we iterate.

		while (assocData != null) {
			HashMap table = assocData;
			assocData = null;

			for (Iterator iter = table.entrySet().iterator(); iter.hasNext();) {
				Map.Entry entry = (Map.Entry) iter.next();
				AssocData data = (AssocData) entry.getValue();
				data.disposeAssocData(this);
				iter.remove();
			}
		}

		// Close any remaining channels

		for (Map.Entry<String, Channel> stringChannelEntry : interpChanTable.entrySet()) {
			Map.Entry entry = (Map.Entry) stringChannelEntry;
			Channel chan = (Channel) entry.getValue();
			try {
				chan.close();
			} catch (IOException ex) {
				// Ignore any IO errors
			}
		}
		interpChanTable.clear();
		interpChanTable = null;

		// Finish deleting the global namespace.

		Namespace.deleteNamespace(globalNs);
		globalNs = null;

		// Free up the result *after* deleting variables, since variable deletion could have transferred ownership of
		// the result string to Tcl.

		frame = null;
		varFrame = null;
		resolvers = null;

		// Free up classloader (makes sure Interp is released in container
		// environments.)
		if (classLoader != null) {
			classLoader.dispose();
			classLoader = null;
		}

		// stop the ManagedSystemInStream thread
		systemIn.dispose();
		
		resetResult();
	}

	/**
	 * Check if an interpreter is ready to eval commands or scripts, i.e., if it was not deleted and if the nesting
	 * level is not too high.
	 * 
	 * Results: Raises a TclExcetpion is the interp is not ready.
	 * 
	 * Side effects: The interpreters result is cleared.
	 */
	public void ready() throws TclException {
		// Reset the interpreter's result and clear out
		// any previous error information.

		resetResult();

		// If the interpreter was deleted, return an error.

		if (deleted) {
			setResult("attempt to call eval in deleted interpreter");
			setErrorCode(TclString.newInstance("CORE IDELETE {attempt to call eval in deleted interpreter}"));
			throw new TclException(TCL.ERROR);
		}

		// Check depth of nested calls to eval: if this gets too large,
		// it's probably because of an infinite loop somewhere.

		if (nestLevel >= maxNestingDepth) {
			Parser.infiniteLoopException(this);
		}
	}

	/**
	 * Create the build-in commands. These commands are loaded on demand -- the class file of a Command class are loaded
	 * into the JVM the first time the given command is executed.
	 */
	protected void createCommands() {
		Extension.loadOnDemand(this, "after", "tcl.lang.cmd.AfterCmd");
		Extension.loadOnDemand(this, "append", "tcl.lang.cmd.AppendCmd");
        Extension.loadOnDemand(this, "apply", "tcl.lang.cmd.ApplyCmd");
		Extension.loadOnDemand(this, "array", "tcl.lang.cmd.ArrayCmd");
		Extension.loadOnDemand(this, "binary", "tcl.lang.cmd.BinaryCmd");
		Extension.loadOnDemand(this, "break", "tcl.lang.cmd.BreakCmd");
		Extension.loadOnDemand(this, "case", "tcl.lang.cmd.CaseCmd");
		Extension.loadOnDemand(this, "catch", "tcl.lang.cmd.CatchCmd");
		Extension.loadOnDemand(this, "cd", "tcl.lang.cmd.CdCmd");
		Extension.loadOnDemand(this, "clock", "tcl.lang.cmd.ClockCmd");
		Extension.loadOnDemand(this, "close", "tcl.lang.cmd.CloseCmd");
		Extension.loadOnDemand(this, "continue", "tcl.lang.cmd.ContinueCmd");
		Extension.loadOnDemand(this, "concat", "tcl.lang.cmd.ConcatCmd");
		Extension.loadOnDemand(this, "dict", "tcl.lang.cmd.DictCmd");
		Extension.loadOnDemand(this, "encoding", "tcl.lang.cmd.EncodingCmd");
		Extension.loadOnDemand(this, "eof", "tcl.lang.cmd.EofCmd");
		Extension.loadOnDemand(this, "eval", "tcl.lang.cmd.EvalCmd");
		Extension.loadOnDemand(this, "error", "tcl.lang.cmd.ErrorCmd");
		if (!Util.isMac()) {
			Extension.loadOnDemand(this, "exec", "tcl.lang.cmd.ExecCmd");
		}
		Extension.loadOnDemand(this, "exit", "tcl.lang.cmd.ExitCmd");
		Extension.loadOnDemand(this, "expr", "tcl.lang.cmd.ExprCmd");
		Extension.loadOnDemand(this, "fblocked", "tcl.lang.cmd.FblockedCmd");
		Extension.loadOnDemand(this, "fconfigure", "tcl.lang.cmd.FconfigureCmd");
		Extension.loadOnDemand(this, "fcopy", "tcl.lang.cmd.FcopyCmd");
		Extension.loadOnDemand(this, "file", "tcl.lang.cmd.FileCmd");
		Extension.loadOnDemand(this, "fileevent", "tcl.lang.cmd.FileeventCmd");
		Extension.loadOnDemand(this, "flush", "tcl.lang.cmd.FlushCmd");
		Extension.loadOnDemand(this, "for", "tcl.lang.cmd.ForCmd");
		Extension.loadOnDemand(this, "foreach", "tcl.lang.cmd.ForeachCmd");
		Extension.loadOnDemand(this, "format", "tcl.lang.cmd.FormatCmd");
		Extension.loadOnDemand(this, "gets", "tcl.lang.cmd.GetsCmd");
		Extension.loadOnDemand(this, "global", "tcl.lang.cmd.GlobalCmd");
		Extension.loadOnDemand(this, "glob", "tcl.lang.cmd.GlobCmd");
		Extension.loadOnDemand(this, "if", "tcl.lang.cmd.IfCmd");
		Extension.loadOnDemand(this, "incr", "tcl.lang.cmd.IncrCmd");
		Extension.loadOnDemand(this, "info", "tcl.lang.cmd.InfoCmd");
		Extension.loadOnDemand(this, "interp", "tcl.lang.cmd.InterpCmd");
		Extension.loadOnDemand(this, "list", "tcl.lang.cmd.ListCmd");
		Extension.loadOnDemand(this, "join", "tcl.lang.cmd.JoinCmd");
		Extension.loadOnDemand(this, "lappend", "tcl.lang.cmd.LappendCmd");
		Extension.loadOnDemand(this, "lassign", "tcl.lang.cmd.LassignCmd");
		Extension.loadOnDemand(this, "lindex", "tcl.lang.cmd.LindexCmd");
		Extension.loadOnDemand(this, "linsert", "tcl.lang.cmd.LinsertCmd");
		Extension.loadOnDemand(this, "llength", "tcl.lang.cmd.LlengthCmd");
		Extension.loadOnDemand(this, "lrange", "tcl.lang.cmd.LrangeCmd");
		Extension.loadOnDemand(this, "lrepeat", "tcl.lang.cmd.LrepeatCmd");
		Extension.loadOnDemand(this, "lreplace", "tcl.lang.cmd.LreplaceCmd");
		Extension.loadOnDemand(this, "lreverse", "tcl.lang.cmd.LreverseCmd");
		Extension.loadOnDemand(this, "lsearch", "tcl.lang.cmd.LsearchCmd");
		Extension.loadOnDemand(this, "lset", "tcl.lang.cmd.LsetCmd");
		Extension.loadOnDemand(this, "lsort", "tcl.lang.cmd.LsortCmd");
		Extension.loadOnDemand(this, "namespace", "tcl.lang.cmd.NamespaceCmd");
		Extension.loadOnDemand(this, "open", "tcl.lang.cmd.OpenCmd");
		Extension.loadOnDemand(this, "package", "tcl.lang.cmd.PackageCmd");
		Extension.loadOnDemand(this, "pid", "tcl.lang.cmd.PidCmd");
		Extension.loadOnDemand(this, "proc", "tcl.lang.cmd.ProcCmd");
		Extension.loadOnDemand(this, "puts", "tcl.lang.cmd.PutsCmd");
		Extension.loadOnDemand(this, "pwd", "tcl.lang.cmd.PwdCmd");
		Extension.loadOnDemand(this, "read", "tcl.lang.cmd.ReadCmd");
		Extension.loadOnDemand(this, "regsub", "tcl.lang.cmd.RegsubCmd");
		Extension.loadOnDemand(this, "rename", "tcl.lang.cmd.RenameCmd");
		Extension.loadOnDemand(this, "return", "tcl.lang.cmd.ReturnCmd");
		Extension.loadOnDemand(this, "scan", "tcl.lang.cmd.ScanCmd");
		Extension.loadOnDemand(this, "seek", "tcl.lang.cmd.SeekCmd");
		Extension.loadOnDemand(this, "set", "tcl.lang.cmd.SetCmd");
		Extension.loadOnDemand(this, "socket", "tcl.lang.cmd.SocketCmd");
		Extension.loadOnDemand(this, "source", "tcl.lang.cmd.SourceCmd");
		Extension.loadOnDemand(this, "split", "tcl.lang.cmd.SplitCmd");
		Extension.loadOnDemand(this, "string", "tcl.lang.cmd.StringCmd");
		Extension.loadOnDemand(this, "subst", "tcl.lang.cmd.SubstCmd");
		Extension.loadOnDemand(this, "switch", "tcl.lang.cmd.SwitchCmd");
		Extension.loadOnDemand(this, "tell", "tcl.lang.cmd.TellCmd");
		Extension.loadOnDemand(this, "time", "tcl.lang.cmd.TimeCmd");
		Extension.loadOnDemand(this, "trace", "tcl.lang.cmd.TraceCmd");
		Extension.loadOnDemand(this, "unset", "tcl.lang.cmd.UnsetCmd");
		Extension.loadOnDemand(this, "update", "tcl.lang.cmd.UpdateCmd");
		Extension.loadOnDemand(this, "uplevel", "tcl.lang.cmd.UplevelCmd");
		Extension.loadOnDemand(this, "upvar", "tcl.lang.cmd.UpvarCmd");
		Extension.loadOnDemand(this, "variable", "tcl.lang.cmd.VariableCmd");
		Extension.loadOnDemand(this, "vwait", "tcl.lang.cmd.VwaitCmd");
		Extension.loadOnDemand(this, "while", "tcl.lang.cmd.WhileCmd");

		// Add "regexp" and related commands to this interp.
		RegexpCmd.init(this);

		// Load tcltest package as a result of "package require tcltest"

		try {
			eval("package ifneeded tcltest 1.0 {source " + "resource:/tcl/lang/library/tcltest/tcltest.tcl}");
		} catch (TclException e) {
			System.out.println(getResult());
			e.printStackTrace();
			throw new TclRuntimeError("unexpected TclException: " + e);
		}

		// The Java package is only loaded when the user does a
		// "package require java" in the interp. We need to create a small
		// command that will load when "package require java" is called.

		Extension.loadOnDemand(this, "jaclloadjava", "tcl.pkg.java.JaclLoadJavaCmd");

		try {
			eval("package ifneeded java 1.4.1 jaclloadjava");
		} catch (TclException e) {
			System.out.println(getResult());
			e.printStackTrace();
			throw new TclRuntimeError("unexpected TclException: " + e);
		}

		// Load the Itcl package as a result of the user running
		// "package require Itcl".

		Extension.loadOnDemand(this, "jaclloaditcl", "tcl.pkg.itcl.ItclExtension");

		try {
			eval("package ifneeded Itcl 3.3 {jaclloaditcl ; package provide Itcl 3.3}");
		} catch (TclException e) {
			System.out.println(getResult());
			e.printStackTrace();
			throw new TclRuntimeError("unexpected TclException: " + e);
		}

		// Load the parser package as a result of the user
		// running "package require parser".

		Extension.loadOnDemand(this, "jaclloadparser", "tcl.lang.TclParserExtension");

		try {
			eval("package ifneeded parser 1.4 {jaclloadparser}");
		} catch (TclException e) {
			System.out.println(getResult());
			e.printStackTrace();
			throw new TclRuntimeError("unexpected TclException: " + e);
		}

		// Load the TJC package as a result of the user running
		// "package require tjc"
		Extension.loadOnDemand(this, "jaclloadtjc", "tcl.pkg.tjc.JaclLoadTJCCmd");

		try {
			eval("package ifneeded TJC 1.0 {jaclloadtjc ; package provide TJC 1.0}");
		} catch (TclException e) {
			System.out.println(getResult());
			e.printStackTrace();
			throw new TclRuntimeError("unexpected TclException: " + e);
		}

	}

	/**
	 * Creates a named association between user-specified data and this interpreter. If the association already exists
	 * the data is overwritten with the new data. The data.deleteAssocData() method will be invoked when the interpreter
	 * is deleted.
	 * 
	 * NOTE: deleteAssocData() is not called when an old data is replaced by a new data. Caller of setAssocData() is
	 * responsible with deleting the old data.
	 * 
	 * Results: None.
	 * 
	 * Side effects: Sets the associated data, creates the association if needed.
	 * 
	 * @param name
	 *            name for the association
	 * @param data
	 *            Object associated with the name
	 */

	public void setAssocData(String name, AssocData data) {
		if (assocData == null) {
			assocData = new HashMap<>();
		}
		assocData.put(name, data);
	}

	/**
	 * Deletes a named association of user-specified data with the specified interpreter.
	 * 
	 * Results: None.
	 * 
	 * Side effects: Deletes the association.
	 * 
	 * @param name
	 *            name of the association
	 */
	public synchronized void deleteAssocData(String name) {
		if (assocData == null) {
			return;
		}

		AssocData d = assocData.remove(name);
		if (d != null)
			d.disposeAssocData(this);
	}

	/**
	 * Returns the AssocData instance associated with this name in the specified interpreter.
	 * 
	 * Results: The AssocData instance in the AssocData record denoted by the named association, or null.
	 * 
	 * Side effects: None.
	 * 
	 * @param name
	 *            name of the association
	 * @return AssocData instance indicated by name, or null if it does not exist
	 */

	public AssocData getAssocData(String name) // Name of association.
	{
		if (assocData == null) {
			return null;
		} else {
			return (AssocData) assocData.get(name);
		}
	}

	/**
	 * This procedure is invoked to handle errors that occur in Tcl commands that are invoked in "background" (e.g. from
	 * event or timer bindings).
	 * 
	 * Results: None.
	 * 
	 * Side effects: The command "bgerror" is invoked later as an idle handler to process the error, passing it the
	 * error message. If that fails, then an error message is output on stderr.
	 */
	public void backgroundError() {
		BgErrorMgr mgr = (BgErrorMgr) getAssocData("tclBgError");
		if (mgr == null) {
			mgr = new BgErrorMgr(this);
			setAssocData("tclBgError", mgr);
		}
		mgr.addBgError();
	}

	/*-----------------------------------------------------------------
	 *
	 *	                     VARIABLES
	 *
	 *-----------------------------------------------------------------
	 */

	/**
	 * Set the value of a variable
	 * 
	 * @param nameObj
	 *            name of variable, array or array element
	 * @param value
	 *            new value for variable
	 * @param flags
	 *            any of TCL.GLOBAL_ONLY, TCL.NAMESPACE_ONLY, TCL.APPEND_VALUE, or TCL.LIST_ELEMENT.
	 * @return new value for variable
	 * @throws TclException
	 */
	public final TclObject setVar(TclObject nameObj, TclObject value, int flags) throws TclException {
		return Var.setVar(this, nameObj.toString(), null, value, (flags | TCL.LEAVE_ERR_MSG));
	}

	/**
	 * Set the value of a variable
	 * 
	 * @param name
	 *            name of variable, array or array element
	 * @param value
	 *            new value for variable
	 * @param flags
	 *            any of TCL.GLOBAL_ONLY, TCL.NAMESPACE_ONLY, TCL.APPEND_VALUE, or TCL.LIST_ELEMENT.
	 * @return new value for variable
	 * @throws TclException
	 */
	public final TclObject setVar(String name, TclObject value, int flags) throws TclException {
		return Var.setVar(this, name, null, value, (flags | TCL.LEAVE_ERR_MSG));
	}

	/**
	 * Set the value of a variable
	 * 
	 * @param name1
	 *            name of variable if name2 is null, or an array
	 * @param name2
	 *            name of an array element, or null
	 * @param value
	 *            new value for variable
	 * @param flags
	 *            any of TCL.GLOBAL_ONLY, TCL.NAMESPACE_ONLY, TCL.APPEND_VALUE, or TCL.LIST_ELEMENT.
	 * @return new value for variable
	 * @throws TclException
	 */
	public final TclObject setVar(String name1, String name2, TclObject value, int flags) throws TclException {
		return Var.setVar(this, name1, name2, value, (flags | TCL.LEAVE_ERR_MSG));
	}

	/**
	 * Set the value of a variable
	 * 
	 * @param name
	 *            name of variable, array or array element
	 * @param strValue
	 *            new value for variable
	 * @param flags
	 *            any of TCL.GLOBAL_ONLY, TCL.NAMESPACE_ONLY, TCL.APPEND_VALUE, or TCL.LIST_ELEMENT.
	 * @return new value for variable
	 * @throws TclException
	 */
	public final TclObject setVar(String name, String strValue, int flags) throws TclException {
		return Var.setVar(this, name, null, checkCommonString(strValue), (flags | TCL.LEAVE_ERR_MSG));
	}

	/**
	 * Set a variable to the value of a String
	 * 
	 * @param name1
	 *            If name2 is null, this is the name of a scalar variable; otherwise it is the name of an array
	 * @param name2
	 *            name of element within an array, or null
	 * @param strValue
	 *            new value for variable
	 * @param flags
	 *            any of TCL.GLOBAL_ONLY, TCL.NAMESPACE_ONLY, TCL.APPEND_VALUE, or TCL.LIST_ELEMENT.
	 * @return the new value of the variable
	 * @throws TclException
	 */
	public final TclObject setVar(String name1, String name2, String strValue, int flags) throws TclException {
		return Var.setVar(this, name1, name2, checkCommonString(strValue), (flags | TCL.LEAVE_ERR_MSG));
	}

	/**
	 * Set a variable to the value in a long argument.
	 * 
	 * @param name1
	 *            name of scalar variable, or name of array if name2 is non-null
	 * @param name2
	 *            Name of element within an array, or null if setting a scalar
	 * @param intValue
	 *            new value for avariable
	 * @param flags
	 *            Various flags that tell how to set valu any of TCL.GLOBAL_ONLY, TCL.NAMESPACE_ONLY, TCL.APPEND_VALUE,
	 *            or TCL.LIST_ELEMENT.
	 * @return Variable with new value
	 * 
	 * @throws TclException
	 */
	public final TclObject setVar(String name1, String name2, long intValue, int flags) throws TclException {
		return Var.setVar(this, name1, name2, checkCommonInteger(intValue), (flags | TCL.LEAVE_ERR_MSG));
	}

	/**
	 * Set a variable to the value of a double
	 * 
	 * @param name1
	 *            If name2 is null, this is the name of a scalar variable; otherwise it is the name of an array
	 * @param name2
	 *            name of element within an array, or null
	 * @param dValue
	 *            new value for variable
	 * @param flags
	 *            any of TCL.GLOBAL_ONLY, TCL.NAMESPACE_ONLY, TCL.APPEND_VALUE, or TCL.LIST_ELEMENT.
	 * @return the new value of the variable
	 * @throws TclException
	 */
	public final TclObject setVar(String name1, String name2, double dValue, int flags) throws TclException {
		return Var.setVar(this, name1, name2, checkCommonDouble(dValue), (flags | TCL.LEAVE_ERR_MSG));
	}

	/**
	 * Set a variable to the value of a boolean
	 * 
	 * @param name1
	 *            If name2 is null, this is the name of a scalar variable; otherwise it is the name of an array
	 * @param name2
	 *            name of element within an array, or null
	 * @param bValue
	 *            new value for variable
	 * @param flags
	 *            any of TCL.GLOBAL_ONLY, TCL.NAMESPACE_ONLY, TCL.APPEND_VALUE, or TCL.LIST_ELEMENT.
	 * @return the new value of the variable
	 * @throws TclException
	 */
	public final TclObject setVar(String name1, String name2, boolean bValue, int flags) throws TclException {
		return Var.setVar(this, name1, name2, checkCommonBoolean(bValue), (flags | TCL.LEAVE_ERR_MSG));
	}

	/**
	 * Get the value of a variable. Side effects: May trigger traces.
	 * 
	 * @param nameObj
	 *            the name of a variable, array or array element
	 * @param flags
	 *            TCL.GLOBAL_ONLY or TCL.NAMESPACE_ONLY.
	 * @return the value of the variable.
	 * @throws TclException
	 *             If the variable is not
	 */
	public final TclObject getVar(TclObject nameObj, // The name of a variable,
														// array, or array
			// element.
			int flags) // Various flags that tell how to get value:
			// any of TCL.GLOBAL_ONLY or TCL.NAMESPACE_ONLY.
			throws TclException {
		return Var.getVar(this, nameObj.toString(), null, (flags | TCL.LEAVE_ERR_MSG));
	}

	/**
	 * Get the value of a variable. Side effects: May trigger traces.
	 * 
	 * @param name
	 *            the name of a variable, array or array element
	 * @param flags
	 *            TCL.GLOBAL_ONLY or TCL.NAMESPACE_ONLY.
	 * @return the value of the variable.
	 * @throws TclException
	 *             If the variable is not
	 */
	public final TclObject getVar(String name, int flags) throws TclException {
		return Var.getVar(this, name, null, (flags | TCL.LEAVE_ERR_MSG));
	}

	/**
	 * Get the value of a variable. Side effects: May trigger traces.
	 * 
	 * @param name1
	 *            if name2 is null, this is the name of a scalar. Otherwise, it is the name of an array
	 * @param name2
	 *            name of an element within an array, or null
	 * @param flags
	 *            TCL.GLOBAL_ONLY or TCL.NAMESPACE_ONLY.
	 * @return the value of the variable.
	 * @throws TclException
	 *             If the variable is not
	 */

	public final TclObject getVar(String name1, String name2, int flags) throws TclException {
		return Var.getVar(this, name1, name2, (flags | TCL.LEAVE_ERR_MSG));
	}
	
	/**
	 * Replacement for System.getenv().  Returns JTcl's view of the environment,
	 * as stored in the ::env() array.  
	 * 
	 * JTcl modifications to values in the env array are not visible to
	 * System.getenv() due to Java API restrictions.
	 * 
	 * @return an unmodifiable Map<String, String> view of JTcl's ::env array. 
	 */
	public Map<String, String> getenv() {
		return Env.getenv(this);
	}
	/**
	 * Unset a variable. May trigget traces.
	 * 
	 * @param nameObj
	 *            name of a variable, array or array element
	 * @param flags
	 *            any of TCL.GLOBAL_ONLY or TCL.NAMESPACE_ONLY.
	 * @throws TclException
	 */
	public final void unsetVar(TclObject nameObj, int flags) throws TclException {
		Var.unsetVar(this, nameObj.toString(), null, (flags | TCL.LEAVE_ERR_MSG));
	}

	/**
	 * Unset a variable. May trigget traces.
	 * 
	 * @param name
	 *            name of a variable, array or array element
	 * @param flags
	 *            any of TCL.GLOBAL_ONLY or TCL.NAMESPACE_ONLY.
	 * @throws TclException
	 */
	public final void unsetVar(String name, int flags) throws TclException {
		Var.unsetVar(this, name, null, (flags | TCL.LEAVE_ERR_MSG));
	}

	/**
	 * Unset a variable. May trigget traces.
	 * 
	 * @param name1
	 *            if name2 is null, this is the name of a scalar. Otherwise it is the name of an array
	 * @param name2
	 *            name of an element within an array, or null
	 * @param flags
	 *            any of TCL.GLOBAL_ONLY or TCL.NAMESPACE_ONLY.
	 * @throws TclException
	 */
	public final void unsetVar(String name1, String name2, int flags) throws TclException {
		Var.unsetVar(this, name1, name2, (flags | TCL.LEAVE_ERR_MSG));
	}

	/**
	 * Add a trace to a variable.
	 * 
	 * Results: None.
	 * 
	 * Side effects: None.
	 * 
	 * @param nameObj
	 *            name of variable, may contain a parenthesized index value
	 * @param trace
	 *            object to notify when specified ops are invoked upon the variable
	 * @param flags
	 *            OR-ed collection of bits, including any of TCL.TRACE_READS, TCL.TRACE_WRITES, TCL.TRACE_UNSETS,
	 *            TCL.GLOBAL_ONLY, TCL.NAMESPACE_ONLY.
	 */

	void traceVar(TclObject nameObj, VarTrace trace, int flags) throws TclException {
		Var.traceVar(this, nameObj.toString(), null, flags, trace);
	}

	/**
	 * Add a trace to a variable.
	 * 
	 * @param name
	 *            name of variable, may contain a parenthesized index value
	 * @param trace
	 *            object to notify when specified ops are invoked upon the variable
	 * @param flags
	 *            OR-ed collection of bits, including any of TCL.TRACE_READS, TCL.TRACE_WRITES, TCL.TRACE_UNSETS,
	 *            TCL.GLOBAL_ONLY, TCL.NAMESPACE_ONLY.
	 */

	public void traceVar(String name, // Name of variable; may end with
										// "(index)"
			// to signify an array reference.
			VarTrace trace, // Object to notify when specified ops are
			// invoked upon varName.
			int flags) throws TclException {
		Var.traceVar(this, name, null, flags, trace);
	}

	/**
	 * Add a trace to a variable.
	 * 
	 * @param part1
	 *            name of variable
	 * 
	 * @param part2
	 *            name of element in within an array, null means part1 is a scalar or entire array
	 * @param trace
	 *            object to notify when specified ops are invoked upon the variable
	 * @param flags
	 *            OR-ed collection of bits, including any of TCL.TRACE_READS, TCL.TRACE_WRITES, TCL.TRACE_UNSETS,
	 *            TCL.GLOBAL_ONLY, TCL.NAMESPACE_ONLY.
	 */

	public void traceVar(String part1, String part2, VarTrace trace, int flags) throws TclException {
		Var.traceVar(this, part1, part2, flags, trace);
	}

	/**
	 * Remove a trace from a variable.
	 * 
	 * Results: None.
	 * 
	 * Side effects: None.
	 * 
	 * @param nameObj
	 *            Name of variable; may end with "(index)" to signify an array reference.
	 * @param trace
	 *            Object associated with trace.
	 * @param flags
	 *            OR-ed collection of bits describing current trace, including any of TCL.TRACE_READS, TCL.TRACE_WRITES,
	 *            TCL.TRACE_UNSETS, TCL.GLOBAL_ONLY and TCL.NAMESPACE_ONLY.
	 */
	void untraceVar(TclObject nameObj, VarTrace trace, int flags) {
		Var.untraceVar(this, nameObj.toString(), null, flags, trace);
	}

	/**
	 * ----------------------------------------------------------------------
	 * 
	 * untraceVar --
	 * 
	 * Remove a trace from a variable.
	 * 
	 * Results: None.
	 * 
	 * Side effects: None.
	 * 
	 * 
	 * @param name
	 *            Name of variable; may end with "(index)" to signify an array reference.
	 * @param trace
	 *            Object associated with trace.
	 * @param flags
	 *            OR-ed collection of bits describing current trace, including any of TCL.TRACE_READS, TCL.TRACE_WRITES,
	 *            TCL.TRACE_UNSETS, TCL.GLOBAL_ONLY and TCL.NAMESPACE_ONLY.
	 */
	public void untraceVar(String name, VarTrace trace, int flags) {
		Var.untraceVar(this, name, null, flags, trace);
	}

	/**
	 * Remove a trace from a variable.
	 * 
	 * Results: None.
	 * 
	 * Side effects: None.
	 * 
	 * @param part1
	 *            Name of scalar variable or array.
	 * @param part2
	 *            Name of element within array; null means trace applies to scalar variable or array as-a-whole.
	 * @param trace
	 *            Object associated with trace.
	 * @param flags
	 *            OR-ed collection of bits describing current trace, including any of TCL.TRACE_READS, TCL.TRACE_WRITES,
	 *            TCL.TRACE_UNSETS, TCL.GLOBAL_ONLY and TCL.NAMESPACE_ONLY.
	 */
	public void untraceVar(String part1, String part2, VarTrace trace, int flags) {
		Var.untraceVar(this, part1, part2, flags, trace);
	}

	/**
	 * Define a new command in the interpreter. Side effects: If a command named cmdName already exists for interp, it
	 * is deleted. In the future, when cmdName is seen as the name of a command by eval(), cmd will be called. When the
	 * command is deleted from the table, cmd.disposeCmd() be called if cmd implements the CommandWithDispose interface.
	 * 
	 * @param cmdName
	 *            name of the command
	 * @param cmdImpl
	 *            Command instance that implements the command
	 */
	public void createCommand(String cmdName, Command cmdImpl) {
		ImportRef oldRef = null;
		Namespace ns;
		WrappedCommand cmd, refCmd;
		String tail;
		ImportedCmdData data;

		if (deleted) {
			// The interpreter is being deleted. Don't create any new
			// commands; it's not safe to muck with the interpreter anymore.

			return;
		}

		// Determine where the command should reside. If its name contains
		// namespace qualifiers, we put it in the specified namespace;
		// otherwise, we always put it in the global namespace.

		if (cmdName.contains("::")) {
			Namespace.GetNamespaceForQualNameResult gnfqnr = this.getnfqnResult;
			Namespace.getNamespaceForQualName(this, cmdName, null, Namespace.CREATE_NS_IF_UNKNOWN, gnfqnr);
			ns = gnfqnr.ns;
			tail = gnfqnr.simpleName;

			if ((ns == null) || (tail == null)) {
				return;
			}
		} else {
			ns = globalNs;
			tail = cmdName;
		}

		cmd = (WrappedCommand) ns.cmdTable.get(tail);
		if (cmd != null) {
			// Command already exists. Delete the old one.
			// Be careful to preserve any existing import links so we can
			// restore them down below. That way, you can redefine a
			// command and its import status will remain intact.

			oldRef = cmd.importRef;
			cmd.importRef = null;

			deleteCommandFromToken(cmd);

			// FIXME : create a test case for this condition!

			cmd = (WrappedCommand) ns.cmdTable.get(tail);
			if (cmd != null) {
				// If the deletion callback recreated the command, just throw
				// away the new command (if we try to delete it again, we
				// could get stuck in an infinite loop).

				cmd.table.remove(cmd.hashKey);
			}
		}

		cmd = new WrappedCommand();
		ns.cmdTable.put(tail, cmd);
		cmd.table = ns.cmdTable;
		cmd.hashKey = tail;
		cmd.ns = ns;
		cmd.cmd = cmdImpl;
		cmd.deleted = false;
		cmd.importRef = null;
		cmd.cmdEpoch = 1;

		// Plug in any existing import references found above. Be sure
		// to update all of these references to point to the new command.

		if (oldRef != null) {
			cmd.importRef = oldRef;
			while (oldRef != null) {
				refCmd = oldRef.importedCmd;
				data = (ImportedCmdData) refCmd.cmd;
				data.realCmd = cmd;
				oldRef = oldRef.next;
			}
		}

		// We just created a command, so in its namespace and all of its parent
		// namespaces, it may shadow global commands with the same name. If any
		// shadowed commands are found, invalidate all cached command references
		// in the affected namespaces.

		Namespace.resetShadowedCmdRefs(this, cmd);
		return;
	}

	/**
	 * Given a token returned by, e.g., Tcl_CreateCommand or Tcl_FindCommand, this procedure returns the command's full
	 * name, qualified by a sequence of parent namespace names. The command's fully-qualified name may have changed due
	 * to renaming.
	 * 
	 * @param cmd
	 *            command of which to get the full name
	 * @return full name, including namepaces, of the command
	 */
	public String getCommandFullName(WrappedCommand cmd) {
		Interp interp = this;
		StringBuilder name = new StringBuilder();

		// Add the full name of the containing namespace, followed by the "::"
		// separator, and the command name.

		if (cmd != null) {
			if (cmd.ns != null) {
				name.append(cmd.ns.fullName);
				if (cmd.ns != interp.globalNs) {
					name.append("::");
				}
			}
			if (cmd.table != null) {
				name.append(cmd.hashKey);
			}
		}

		return name.toString();
	}

	/**
	 * Given a token returned by, e.g., Tcl_CreateCommand or Tcl_FindCommand, this procedure returns the command's name.
	 * The command's fully-qualified name may have changed due to renaming.
	 * 
	 * @param cmd
	 *            command of which to get the name of
	 * @return name of the command
	 */

	public String getCommandName(WrappedCommand cmd) {
		if ((cmd == null) || (cmd.table == null)) {
			// This should only happen if command was "created" after the
			// interpreter began to be deleted, so there isn't really any
			// command. Just return an empty string.

			return "";
		}
		return cmd.hashKey;
	}

	/**
	 * Remove the given command from the this interpreter.
	 * 
	 * Side effects: CmdName will no longer be recognized as a valid command for the interpreter.
	 * 
	 * @param cmdName
	 *            name of command to remove in this interpreter
	 * @return 0 is returned if the command was deleted successfully. -1 is returned if there didn't exist a command by
	 *         that name.
	 */

	public int deleteCommand(String cmdName) {
		WrappedCommand cmd;

		// Find the desired command and delete it.

		try {
			cmd = Namespace.findCommand(this, cmdName, null, 0);
		} catch (TclException e) {
			// This should never happen
			throw new TclRuntimeError("unexpected TclException: " + e);
		}
		if (cmd == null) {
			return -1;
		}
		return deleteCommandFromToken(cmd);
	}

	/**
	 * Remove the given command from the given interpreter.
	 * 
	 * Side effects: cmdName will no longer be recognized as a valid command for the interpreter.
	 * 
	 * @param cmd
	 *            wrapper token for the command to delete; may be null
	 * @return 0 is returned if the command was deleted successfully. -1 is returned if there didn't exist a command by
	 *         that name (cmd is null)
	 */

	public int deleteCommandFromToken(WrappedCommand cmd) {
		/*
		 * dont' try to delete a command in a delete trace callback; it's going to be deleted anyway
		 */
		if (cmd == null) {
			return -1;
		}
		/* Call the command traces */
		cmd.callCommandTraces(CommandTrace.DELETE, "");

		ImportRef ref, nextRef;
		WrappedCommand importCmd;

		// The code here is tricky. We can't delete the hash table entry
		// before invoking the deletion callback because there are cases
		// where the deletion callback needs to invoke the command (e.g.
		// object systems such as OTcl). However, this means that the
		// callback could try to delete or rename the command. The deleted
		// flag allows us to detect these cases and skip nested deletes.

		if (cmd.deleted) {
			// Another deletion is already in progress. Remove the hash
			// table entry now, but don't invoke a callback or free the
			// command structure.

			if (cmd.hashKey != null && cmd.table != null) {
				cmd.table.remove(cmd.hashKey);
				cmd.table = null;
				cmd.hashKey = null;
			}
			return 0;
		}

		cmd.deleted = true;
		if (cmd.cmd instanceof CommandWithDispose) {
			((CommandWithDispose) cmd.cmd).disposeCmd();
		}

		// Bump the command epoch counter. This will invalidate all cached
		// references that point to this command.

		cmd.incrEpoch();

		// If this command was imported into other namespaces, then imported
		// commands were created that refer back to this command. Delete these
		// imported commands now.

		for (ref = cmd.importRef; ref != null; ref = nextRef) {
			nextRef = ref.next;
			importCmd = ref.importedCmd;
			deleteCommandFromToken(importCmd);
		}

		// FIXME : what does this mean? Is this a mistake in the C comment?

		// Don't use hPtr to delete the hash entry here, because it's
		// possible that the deletion callback renamed the command.
		// Instead, use cmdPtr->hptr, and make sure that no-one else
		// has already deleted the hash entry.

		if (cmd.table != null) {
			cmd.table.remove(cmd.hashKey);
			cmd.table = null;
			cmd.hashKey = null;
		}

		// Drop the reference to the Command instance inside the WrappedCommand

		cmd.cmd = null;

		// We do not need to cleanup the WrappedCommand because GC will get it.

		return 0;
	}

	/**
	 * Called to give an existing Tcl command a different name. Both the old command name and the new command name can
	 * have "::" namespace qualifiers. If the new command has a different namespace context, the command will be moved
	 * to that namespace and will execute in the context of that new namespace.
	 * 
	 * If the new command name is null or the empty string, the command is deleted.
	 * 
	 * @param oldName
	 *            existing command name
	 * @param newName
	 *            new name
	 * @throws TclException
	 */
	public void renameCommand(String oldName, String newName) throws TclException {
		Interp interp = this;
		String newTail;
		Namespace cmdNs, newNs;
		WrappedCommand cmd;
		HashMap table, oldTable;
		String hashKey, oldHashKey;

		// Find the existing command. An error is returned if cmdName can't
		// be found.

		cmd = Namespace.findCommand(interp, oldName, null, 0);
		if (cmd == null) {
			throw new TclException(interp, "can't "
					+ (((newName == null) || (newName.length() == 0)) ? "delete" : "rename") + " \"" + oldName
					+ "\": command doesn't exist");
		}
		cmdNs = cmd.ns;

		// If the new command name is NULL or empty, delete the command. Do this
		// with Tcl_DeleteCommandFromToken, since we already have the command.

		if ((newName == null) || (newName.length() == 0)) {
			deleteCommandFromToken(cmd);
			return;
		}

		// Make sure that the destination command does not already exist.
		// The rename operation is like creating a command, so we should
		// automatically create the containing namespaces just like
		// Tcl_CreateCommand would.

		Namespace.GetNamespaceForQualNameResult gnfqnr = interp.getnfqnResult;
		Namespace.getNamespaceForQualName(interp, newName, null, Namespace.CREATE_NS_IF_UNKNOWN, gnfqnr);
		newNs = gnfqnr.ns;
		newTail = gnfqnr.simpleName;

		if ((newNs == null) || (newTail == null)) {
			throw new TclException(interp, "can't rename to \"" + newName + "\": bad command name");
		}
		if (newNs.cmdTable.get(newTail) != null) {
			throw new TclException(interp, "can't rename to \"" + newName + "\": command already exists");
		}

		/* Call the command traces */
		String newCommand = newNs.fullName + (newNs.fullName.endsWith("::") ? "" : "::") + newTail;
		cmd.callCommandTraces(CommandTrace.RENAME, newCommand);
		/* It's possible that the command was deleted in one of the callbacks */
		cmd = Namespace.findCommand(interp, oldName, null, 0);
		if (cmd == null)
			return;

		// Warning: any changes done in the code here are likely
		// to be needed in Tcl_HideCommand() code too.
		// (until the common parts are extracted out) --dl

		// Put the command in the new namespace so we can check for an alias
		// loop. Since we are adding a new command to a namespace, we must
		// handle any shadowing of the global commands that this might create.

		oldTable = cmd.table;
		oldHashKey = cmd.hashKey;
		newNs.cmdTable.put(newTail, cmd);
		cmd.table = newNs.cmdTable;
		cmd.hashKey = newTail;
		cmd.ns = newNs;
		Namespace.resetShadowedCmdRefs(this, cmd);

		// Now check for an alias loop. If we detect one, put everything back
		// the way it was and report the error.

		try {
			interp.preventAliasLoop(interp, cmd);
		} catch (TclException e) {
			newNs.cmdTable.remove(newTail);
			cmd.table = oldTable;
			cmd.hashKey = oldHashKey;
			cmd.ns = cmdNs;
			throw e;
		}

		// The new command name is okay, so remove the command from its
		// current namespace. This is like deleting the command, so bump
		// the cmdEpoch to invalidate any cached references to the command.

		oldTable.remove(oldHashKey);
		cmd.incrEpoch();

		return;
	}

	/**
	 * TclPreventAliasLoop -> preventAliasLoop
	 * 
	 * When defining an alias or renaming a command, prevent an alias loop from being formed.
	 * 
	 * Results: A standard Tcl object result.
	 * 
	 * Side effects: If TCL_ERROR is returned, the function also stores an error message in the interpreter's result
	 * object.
	 * 
	 * NOTE: This function is public internal (instead of being static to this file) because it is also used from
	 * TclRenameCommand.
	 * 
	 * @param cmdInterp
	 *            Interp in which the command is being defined.
	 * @param cmd
	 *            Tcl command we are attempting to define.
	 * @throws TclException
	 */
	public void preventAliasLoop(Interp cmdInterp, WrappedCommand cmd) throws TclException {
		// If we are not creating or renaming an alias, then it is
		// always OK to create or rename the command.

		if (!(cmd.cmd instanceof InterpAliasCmd)) {
			return;
		}

		// OK, we are dealing with an alias, so traverse the chain of aliases.
		// If we encounter the alias we are defining (or renaming to) any in
		// the chain then we have a loop.

		InterpAliasCmd alias = (InterpAliasCmd) cmd.cmd;
		InterpAliasCmd nextAlias = alias;
		while (true) {

			// If the target of the next alias in the chain is the same as
			// the source alias, we have a loop.

			WrappedCommand aliasCmd = nextAlias.getTargetCmd(this);
			if (aliasCmd == null) {
				return;
			}
			if (aliasCmd.cmd == cmd.cmd) {
				throw new TclException(this, "cannot define or rename alias \"" + alias.name
						+ "\": would create a loop");
			}

			// Otherwise, follow the chain one step further. See if the target
			// command is an alias - if so, follow the loop to its target
			// command. Otherwise we do not have a loop.

			if (!(aliasCmd.cmd instanceof InterpAliasCmd)) {
				return;
			}
			nextAlias = (InterpAliasCmd) aliasCmd.cmd;
		}
	}

	/**
	 * Returns the command procedure of the given command.
	 * 
	 * @param cmdName
	 *            string name of the command
	 * 
	 * @return The command procedure of the given command, or null if the command doesn't exist. Do not directly call
	 *         getCommand().cmdProc(); see WrappedCommand.invoke() and WrappedCommand.mustCallInvoke()
	 */
	public Command getCommand(String cmdName) {
		// Find the desired command and return it.

		WrappedCommand cmd;

		try {
			cmd = Namespace.findCommand(this, cmdName, null, 0);
		} catch (TclException e) {
			// This should never happen
			throw new TclRuntimeError("unexpected TclException: " + e);
		}

		return ((cmd == null) ? null : cmd.cmd);
	}

	/**
	 * @param cmdName
	 *            name of command to get
	 * @return WrappedCommand object for given command
	 */
	public WrappedCommand getWrappedCommand(String cmdName) {
		try {
			return Namespace.findCommand(this, cmdName, null, 0);
		} catch (TclException e) {
			// This should never happen
			throw new TclRuntimeError("unexpected TclException: " + e);
		}
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * commandComplete --
	 * 
	 * Check if the string is a complete Tcl command string.
	 * 
	 * Result: A boolean value indicating whether the string is a complete Tcl command string.
	 * 
	 * Side effects: None.
	 * 
	 * @param string The string to check.
	 * 
	 * @return
	 */
	public static boolean commandComplete(String string) {
		return Parser.commandComplete(string, string.length());
	}

	/**
	 * Set a command trace on a command
	 * 
	 * @param command
	 *            fully qualified command name
	 * @param trace
	 *            trace to add to command
	 * @throws TclException
	 */
	public void traceCommand(String command, CommandTrace trace) throws TclException {
		WrappedCommand wrappedCommand = Namespace.findCommand(this, command, null, TCL.LEAVE_ERR_MSG);
		wrappedCommand.traceCommand(trace);
	}

	/**
	 * Remove a command trace on a command
	 * 
	 * @param command
	 *            fully qualified command name
	 * @param type
	 *            CommandTrace.RENAME or CommandTrace.DELETE
	 * @param callbackCmd
	 *            callback command to remove
	 * @throws TclException
	 */
	public void untraceCommand(String command, int type, TclObject callbackCmd) throws TclException {
		WrappedCommand cmd = Namespace.findCommand(this, command, null, TCL.LEAVE_ERR_MSG);
		cmd.untraceCommand(type, callbackCmd.toString());
	}

	/**
	 * Get a list of all command traces on a command
	 * 
	 * @param command
	 *            fully qualified name of command
	 * @return List, in the form of [trace command info]
	 * @throws TclException
	 */
	public TclObject traceCommandInfo(String command) throws TclException {
		WrappedCommand wrappedCommand = Namespace.findCommand(this, command, null, TCL.LEAVE_ERR_MSG);
		return wrappedCommand.traceCommandInfo(this);
	}

	/**
	 * Set a execution trace on a command
	 * 
	 * @param command
	 *            fully qualified command name
	 * @param trace
	 *            trace to add to command
	 * @throws TclException
	 */
	public void traceExecution(String command, ExecutionTrace trace) throws TclException {
		WrappedCommand wrappedCommand = Namespace.findCommand(this, command, null, TCL.LEAVE_ERR_MSG);
		wrappedCommand.traceExecution(trace);
	}

	/**
	 * Remove an execution trace on a command
	 * 
	 * @param command
	 *            fully qualified command name
	 * @param type
	 *            ExecutionTrace.ENTER, ExecutionTrace.LEAVE, ExecutionTrace.ENTERSTEP, ExecutionTrace.LEAVESTEP
	 * @param callbackCmd
	 *            callback command to remove
	 * @throws TclException
	 */
	public void untraceExecution(String command, int type, TclObject callbackCmd) throws TclException {
		WrappedCommand cmd = Namespace.findCommand(this, command, null, TCL.LEAVE_ERR_MSG);
		cmd.untraceExecution(type, callbackCmd.toString());
		deactivateExecutionStepTrace(cmd);
	}

	/**
	 * Get a list of all execution traces on a command
	 * 
	 * @param command
	 *            fully qualified name of command
	 * @return List, in the form of [trace command info]
	 * @throws TclException
	 */
	public TclObject traceExecutionInfo(String command) throws TclException {
		WrappedCommand wrappedCommand = Namespace.findCommand(this, command, null, TCL.LEAVE_ERR_MSG);
		return wrappedCommand.traceExecutionInfo(this);
	}

	/**
	 * Add a new execution step trace to the active traces
	 * 
	 * @param cmd
	 *            Command which will be stepped through
	 * @return true if it was added
	 */
	boolean activateExecutionStepTrace(WrappedCommand cmd) {
		if (activeExecutionStepTraces == null) {
			activeExecutionStepTraces = new ArrayList<>();
		}
		if (activeExecutionStepTraces.contains(cmd))
			return false;
		activeExecutionStepTraces.add(cmd);
		return true;
	}

	/**
	 * Remove an execution step trace from the active traces
	 * 
	 * @param cmd
	 *            Command which was being stepped through
	 */
	void deactivateExecutionStepTrace(WrappedCommand cmd) {
		if (activeExecutionStepTraces == null)
			return;
		activeExecutionStepTraces.remove(cmd);
		if (activeExecutionStepTraces.size() == 0) {
			activeExecutionStepTraces = null;
		}
	}

	/**
	 * @return true if there are active execution step traces in effect
	 */
	final boolean hasActiveExecutionStepTraces() {
		return (stepTracingEnabled && activeExecutionStepTraces != null);
	}

	/**
	 * @return a copy of the active execution step traces. A zero-length array is returned if there are no active traces
	 */
	WrappedCommand[] getCopyOfActiveExecutionStepTraces() {
		WrappedCommand[] rv = new WrappedCommand[0];
		if (activeExecutionStepTraces == null)
			return rv;
		rv = activeExecutionStepTraces.toArray(rv);
		return rv;
	}

	/**
	 * @param enable
	 *            if false, suppress execution step tracing. If true, re-enable step tracing
	 */
	void enableExecutionStepTracing(boolean enable) {
		stepTracingEnabled = enable;
	}

	/*-----------------------------------------------------------------
	 *
	 *	                     EVAL
	 *
	 *-----------------------------------------------------------------
	 */

	/**
	 * Queries the value of this interpreter's result value
	 * 
	 * @return The current result in the interpreter.
	 * 
	 */

	public final TclObject getResult() {
		return m_result;
	}

	/**
	 * Arrange for the given Tcl Object to be placed as the result object for the interpreter. Convenience functions are
	 * also available to create a Tcl Object out of the most common Java types. Note that the ref count for m_nullResult
	 * is not changed.
	 * 
	 * @param newResult
	 *            A Tcl Object to be set as the result of this interpreter
	 */

	public final void setResult(TclObject newResult) {
		if (newResult == m_result) {
			// Setting to current value (including m_nullResult) is a no-op.
			return;
		}

		if (newResult != m_nullResult) {
			newResult.preserve();
		}

		TclObject oldResult = m_result;
		m_result = newResult;

		if (oldResult != m_nullResult) {
			oldResult.release();
		}
	}

	/**
	 * Set this interpreter's result object to a String value.
	 * 
	 * @param r
	 *            value to set result to
	 */
	public final void setResult(String r) {
		setResult(checkCommonString(r));
	}

	/**
	 * Set this interpreter's result object to a long value.
	 * 
	 * @param r
	 *            value to set result to
	 */
	public final void setResult(final long r) {
		setResult(checkCommonInteger(r));
	}

	/**
	 * Set this interpreter's result object to a double value.
	 * 
	 * @param r
	 *            value to set result to
	 */
	public final void setResult(final double r) {
		setResult(checkCommonDouble(r));
	}

	/**
	 * Set this interpreter's result object to a boolean value.
	 * 
	 * @param r
	 *            value to set result to
	 */
	public final void setResult(final boolean r) {
		if (VALIDATE_SHARED_RESULTS) {
			setResult(checkCommonBoolean(r));
		} else {
			setResult(r ? m_trueBooleanResult : m_falseBooleanResult);
		}
	}

	/**
	 * This procedure resets this interpreter's result object.
	 * 
	 * Results: None.
	 * 
	 * Side effects: It resets the result object to an unshared empty object. It also clears any error information for
	 * the interpreter.
	 */
	public final void resetResult() {
		if (m_result != m_nullResult) {
			m_result.release();
			m_result = m_nullResult;
			if (VALIDATE_SHARED_RESULTS) {
				if (!m_nullResult.isShared()) {
					throw new TclRuntimeError("m_nullResult is not shared");
				}
			}
		}
		errAlreadyLogged = false;
		errInProgress = false;
		errCodeSet = false;
		returnCode = TCL.OK;
	}

	/**
	 * Convert a string to a valid Tcl list element and append it to the result (which is ostensibly a list).
	 * 
	 * Results: None.
	 * 
	 * Side effects: The result in the interpreter given by the first argument is extended with a list element converted
	 * from string. A separator space is added before the converted list element unless the current result is empty,
	 * contains the single character "{", or ends in " {".
	 * 
	 * If the string result is empty, the object result is moved to the string result, then the object result is reset.
	 * 
	 * @param string
	 *            String to append to list result
	 */

	public void appendElement(String string) throws TclException {
		TclObject result;

		result = getResult();
		if (result.isShared()) {
			result = result.duplicate();
		}
		TclList.append(this, result, TclString.newInstance(string));
		setResult(result);
	}

	/**
	 * Execute a Tcl command in a string.
	 * 
	 * A standard Tcl Exception may be generated. The interpreter's result object will contain the value of the
	 * evaluation but will persist only until the next call to one of the eval functions.
	 * 
	 * @param script
	 *            The script to be evaluated
	 * @throws TclException
	 *             on any TCL errors generated by the script
	 */

	public void eval(String script) throws TclException {
		eval(script, 0);
	}

	/**
	 * Evaluate a TCL script in a string
	 * 
	 * @param string
	 *            containing the TCL script
	 * @param flags
	 *            Either 0 or TCL.EVAL_GLOBAL
	 * @throws TclException
	 *             on any TCL error
	 */
	public void eval(String string, int flags) throws TclException {
		if (string == null) {
			throw new NullPointerException("passed null String to eval()");
		}

		int evalFlags = this.evalFlags;
		this.evalFlags &= ~Parser.TCL_ALLOW_EXCEPTIONS;

		CharPointer script = new CharPointer(string);
		try {
			Parser.eval2(this, script.array, script.index, script.length(), flags);
		} catch (TclException e) {

			if (nestLevel != 0) {
				throw e;
			}

			// Update the interpreter's evaluation level count. If we are again
			// at
			// the top level, process any unusual return code returned by the
			// evaluated code. Note that we don't propagate an exception that
			// has a TCL.RETURN error code when updateReturnInfo() returns
			// TCL.OK.

			int result = e.getCompletionCode();
			if (result == TCL.RETURN) {
				result = updateReturnInfo();
			}
			if (result != TCL.OK && result != TCL.ERROR && (evalFlags & Parser.TCL_ALLOW_EXCEPTIONS) == 0) {
				/*
				 * set the line number on which we got the unexpected result, which is really probably a break or
				 * continue outside of the loop. Also get the contents of that line for the error message
				 */
				errorLine = 1;
				int lineEnd = 0;
				int lineStart = 0;
				while (lineEnd < termOffset - 1) {
					if (script.charAt(lineEnd) == '\n') {
						++errorLine;
						lineStart = lineEnd + 1;
					}
					++lineEnd;
				}
				while (Character.isWhitespace(script.charAt(lineStart)) && lineStart < lineEnd)
					++lineStart;
				try {
					processUnexpectedResult(result);
				} catch (TclException e1) {
					/* Add the correct 'while executing' command to match C Tcl */
					addErrorInfo("\n    while executing\n\"" + script.toString().substring(lineStart, lineEnd) + "\"");
					throw e1;
				}
			}
			if (result != TCL.OK) {
				e.setCompletionCode(result);
				throw e;
			}
		} finally {
			checkInterrupted();
		}
	}

	/**
	 * Execute a Tcl script in a TclObject.
	 * 
	 * @param tobj
	 *            A Tcl object holding a script to evaluate
	 * @param flags
	 *            either 0 or TCL.EVAL_GLOBAL
	 * @throws TclException
	 *             on any TCL error
	 */
	public void eval(TclObject tobj, int flags) throws TclException {
		boolean isPureList = false;

		if (tobj.hasNoStringRep() && tobj.isListType()) {
			isPureList = true;
		}

		// Non-optimized eval(), used when tobj is not a pure list.

		if (!isPureList) {
			tobj.preserve();
			try {
				eval(tobj.toString(), flags);
			} finally {
				tobj.release();

				checkInterrupted();
			}

			return;
		}

		// In the pure list case, use an optimized implementation that
		// skips the costly reparse operation. In the pure list case
		// the TclObject arguments to the command can be used as is
		// by invoking Parse.evalObjv();

		int evalFlags = this.evalFlags;
		this.evalFlags &= ~Parser.TCL_ALLOW_EXCEPTIONS;
		TclObject[] objv = null;
		boolean invokedEval = false;

		tobj.preserve();
		try {
			// Grab a TclObject[] from the cache and populate
			// it with the TclObject refs from the TclList.
			// Increment the refs in case tobj loses the
			// TclList internal rep during the evaluation.

			final int llength = TclList.getLength(this, tobj);
			objv = Parser.grabObjv(this, llength);
			for (int i = 0; i < llength; i++) {
				objv[i] = TclList.index(this, tobj, i);
				objv[i].preserve();
			}

			invokedEval = true;
			Parser.evalObjv(this, objv, -1, flags);
		} catch (StackOverflowError e) {
			Parser.infiniteLoopException(this);
		} catch (TclException e) {
			int result = e.getCompletionCode();

			// Generate various pieces of error information, such
			// as the line number where the error occurred and
			// information to add to the errorInfo variable. Then
			// free resources that had been allocated
			// to the command.

			if (invokedEval && result == TCL.ERROR && !(this.errAlreadyLogged)) {
				StringBuffer cmd_strbuf = new StringBuffer(64);

				for (TclObject anObjv : objv) {
					Util.appendElement(this, cmd_strbuf, anObjv.toString());
				}

				String cmd_str = cmd_strbuf.toString();
				char[] script_array = cmd_str.toCharArray();
				int script_index = 0;
				int command_start = 0;
				int command_length = cmd_str.length();
				Parser.logCommandInfo(this, script_array, script_index, command_start, command_length, e);
			}

			// Process results when the next level is zero

			if (nestLevel != 0) {
				throw e;
			}

			// Update the interpreter's evaluation level count. If we are again
			// at
			// the top level, process any unusual return code returned by the
			// evaluated code. Note that we don't propagate an exception that
			// has a TCL.RETURN error code when updateReturnInfo() returns
			// TCL.OK.

			if (result == TCL.RETURN) {
				result = updateReturnInfo();
			}
			if (result != TCL.OK && result != TCL.ERROR && (evalFlags & Parser.TCL_ALLOW_EXCEPTIONS) == 0) {
				processUnexpectedResult(result);
			}
			if (result != TCL.OK) {
				e.setCompletionCode(result);
				throw e;
			}
		} finally {
			if (objv != null) {
				for (TclObject obj : objv) {
					if (obj != null) {
						obj.release();
					}
				}
				Parser.releaseObjv(this, objv, objv.length);
			}
			tobj.release();

			checkInterrupted();
		}
	}

	/**
	 * This procedure adds its command argument to the current list of recorded events and then executes the command by
	 * calling eval.
	 * 
	 * Results: The return value is void. However, a standard Tcl Exception may be generated. The interpreter's result
	 * object will contain the value of the evaluation but will persist only until the next call to one of the eval
	 * functions.
	 * 
	 * Side effects: The side effects will be determined by the exact Tcl code to be evaluated.
	 * 
	 * @param script
	 *            a script to evaluate
	 * @param flags
	 *            TCL.NO_EVAL records only; TCL.EVAL_GLOBAL evalutes the script in a global context
	 * @throws TclException
	 */

	public void recordAndEval(TclObject script, int flags) throws TclException {
		// Append the script to the event list by calling
		// "history add <script>".
		// We call the eval method with the command of type TclObject, so that
		// we don't have to deal with funny chars ("{}[]$\) in the script.

		TclObject cmd = null;
		try {
			cmd = TclList.newInstance();
			TclList.append(this, cmd, TclString.newInstance("history"));
			TclList.append(this, cmd, TclString.newInstance("add"));
			TclList.append(this, cmd, script);
			eval(cmd, TCL.EVAL_GLOBAL);
		} catch (Exception e) {
			// No-op
		}

		// Execute the command.

		if ((flags & TCL.NO_EVAL) == 0) {
			eval(script, flags & TCL.EVAL_GLOBAL);
		}
	}

	/**
	 * Loads a Tcl script from a file using the system encoding
	 *  and evaluates it in the current interpreter.
	 * 
	 * @param s
	 *            Name of the file to evaluate
	 * @throws TclException
	 *             on any TCL error or on a file read error
	 */             
	public void evalFile(String s) throws TclException {
		evalFile(s, EncodingCmd.systemJavaEncoding);
	}
	/**
	 * Loads a Tcl script from a file and evaluates it in the current interpreter.
	 * 
	 * @param s
	 *            Name of the file to evaluate
	 * @param encoding
	 *            Java charset name of file's encoding
	 * @throws TclException
	 *             on any TCL error or on a file read error
	 */
	public void evalFile(String s, String encoding) throws TclException {
		String fileContent; // Contains the content of the file.

		fileContent = readScriptFromFile(s,encoding);

		if (fileContent == null) {
			throw new TclException(this, "couldn't read file \"" + s + "\"");
		}

		String oldScript = scriptFile;
		scriptFile = s;

		try {
			pushDebugStack(s, 1);
			eval(fileContent, 0);
		} catch (TclException e) {
			if (e.getCompletionCode() == TCL.ERROR) {
				addErrorInfo("\n    (file \"" + s + "\" line " + errorLine + ")");
			}
			throw e;
		} finally {
			scriptFile = oldScript;
			popDebugStack();
		}
	}

	/**
	 * Loads a Tcl script (encoded in system encoding) from a URL and evaluate it in the current interpreter.
	 * 
	 * @param context
	 *            URL context under which s is to be interpreted
	 * @param s
	 *            the URL
	 * @throws TclException
	 *             on any TCL or read error
	 */
	public void evalURL(URL context, String s) throws TclException {
		evalURL(context,s, EncodingCmd.systemJavaEncoding);
	}
	
	/**
	 * Loads a Tcl script from a URL and evaluate it in the current interpreter.
	 * 
	 * @param context
	 *            URL context under which s is to be interpreted
	 * @param s
	 *            the URL
	 * @param javaEncoding
	 *            Java charset name in which script is encoded
	 * @throws TclException
	 *             on any TCL or read error
	 */
	public void evalURL(URL context, String s, String javaEncoding) throws TclException {
		String fileContent; // Contains the content of the file.

		fileContent = readScriptFromURL(context, s, javaEncoding);
		if (fileContent == null) {
			throw new TclException(this, "cannot read URL \"" + s + "\"");
		}

		String oldScript = scriptFile;
		scriptFile = s;

		try {
			eval(fileContent, 0);
		} finally {
			scriptFile = oldScript;
		}
	}

	/**
	 * Read the script file into a string.
	 * 
	 * @param s
	 *            name of file
	 *  @param encoding
	 *            Java charset encoder name
	 * @returns script as a String, or null if it can't be read
	 * 
	 * @throws TclException
	 *             on file errors
	 */

	private String readScriptFromFile(String s, String encoding) throws TclException {
		File sourceFile;
		FileChannel fchan = new FileChannel();
		fchan.setEncoding(encoding);
		boolean wasOpened = false;
		TclObject result = TclString.newInstance(new StringBuffer(64));

		try {
			sourceFile = FileUtil.getNewFileObj(this, s);
			fchan.open(this, sourceFile.getPath(), TclIO.RDONLY);
			wasOpened = true;
			fchan.read(this, result, TclIO.READ_ALL, 0);
			// return up to ^Z for tcl8.4 compatibility
			return trimToCtrlZ(result.toString());
		} catch (TclPosixException e) {
			throw new TclPosixException(this, e.getErrorNo(), true, "couldn't read file \"" + s + "\"");
		} catch (IOException e) {
			throw new TclPosixException(this, e, true, "couldn't read file \"" + s + "\"");
		} catch (SecurityException e) {
			throw new TclException(this, e.getMessage());
		} finally {
			if (wasOpened) {
				closeChannel(fchan);
			}
		}
	}

	/**
	 * Retrun a string, up to the first ^Z, if any.
	 * 
	 * @param source
	 * @return
	 */
	private String trimToCtrlZ(String source) {
		char ctrlZ = 26;
		int end = source.indexOf(ctrlZ);
		if (end == -1) {
			return source;
		} else {
			return source.substring(0, end);
		}
	}

	/**
	 * Read the script file into a string, treating the file as an URL.
	 * 
	 * @param context
	 *            the context for the URL. @see java.net.URL.URL.
	 * @param s
	 *            URL to be read (based on the context)
	 * @return The content of the script file.
	 */
	private String readScriptFromURL(URL context, String s, String javaEncoding) {
		Object content = null;
		URL url;

		try {
			url = new URL(context, s);
		} catch (MalformedURLException e) {
			return null;
		}

		try {
			content = url.getContent();
		} catch (UnknownServiceException e) {
			Class jar_class;

			try {
				// Load JarURLConnection via the system class loader
				jar_class = Class.forName("java.net.JarURLConnection");
			} catch (Exception e2) {
				return null;
			}

			Object jar;
			try {
				jar = url.openConnection();
			} catch (IOException e2) {
				return null;
			}

			if (jar == null) {
				return null;
			}

			// We must call JarURLConnection.getInputStream() dynamically
			// Because the class JarURLConnection does not exist in JDK1.1

			try {
				Method m = jar_class.getMethod("openConnection", null);
				content = m.invoke(jar, null);
			} catch (Exception e2) {
				return null;
			}
		} catch (IOException e) {
			return null;
		} catch (SecurityException e) {
			return null;
		}

		if (content instanceof String) {
			// return up to ^Z for tcl8.4 compatibility
			return trimToCtrlZ(convertStringCRLF((String) content));
		} else if (content instanceof InputStream) {
			// return up to ^Z for tcl8.4 compatibility
			return trimToCtrlZ(readScriptFromInputStream((InputStream) content, javaEncoding));
		} else {
			return null;
		}
	}

	/**
	 * Convert CRLF sequences into LF sequences in a String.
	 * 
	 * @param inString
	 *            string that may contain CRLF
	 * @return A new string with LF instead of CRLF.
	 */
	String convertStringCRLF(String inStr) {
		String str = inStr;
		StringBuilder sb = new StringBuilder(str.length());
		char c, prev = '\n';
		boolean foundCRLF = false;
		final int length = str.length();

		for (int i = 0; i < length; i++) {
			c = str.charAt(i);
			if (c == '\n' && prev == '\r') {
				sb.setCharAt(sb.length() - 1, '\n');
				prev = '\n';
				foundCRLF = true;
			} else {
				sb.append(c);
				prev = c;
			}
		}

		if (foundCRLF) {
			return sb.toString();
		} else {
			return str;
		}
	}

	/**
	 * readScriptFromInputStream --
	 * 
	 * Read a script from a Java InputStream into a string.
	 * 
	 * @param s
	 *            Java InputStream containing script
	 * @param javaEncoding
	 *            Java charset name of stream
	 * @return the content of the script.
	 */
	private String readScriptFromInputStream(InputStream s, String javaEncoding) {
		TclObject result = TclString.newInstance(new StringBuffer(64));
		ReadInputStreamChannel rc = new ReadInputStreamChannel(this, s);
		rc.setEncoding(javaEncoding);
		
		try {
			rc.read(this, result, TclIO.READ_ALL, 0);
			return result.toString();
		} catch (TclException e) {
			resetResult();
			return null;
		} catch (FileNotFoundException e) {
			return null;
		} catch (IOException e) {
			return null;
		} catch (SecurityException e) {
			return null;
		} finally {
			closeChannel(rc);
			// FIXME: Closing the channel should close the stream!
			closeInputStream(s);
		}
	}

	/**
	 * Close the InputStream; catch any IOExceptions and ignore them.
	 * 
	 * @param fs
	 *            input stream to close
	 */
	private void closeInputStream(InputStream fs) {
		try {
			fs.close();
		} catch (IOException e) {
			;
		}
	}

	/**
	 * Close the Channel; catch any IOExceptions and ignore them.
	 * 
	 * @param chan
	 *            Channel to close
	 */
	private void closeChannel(Channel chan) {
		try {
			chan.close();
		} catch (IOException e) {
		}
	}

	/**
	 * Execute a Tcl script stored in the given Java resource location, encoded in the system encoding
	 * 
	 * Results: The return value is void. However, a standard Tcl Exception may be generated. The interpreter's result
	 * object will contain the value of the evaluation but will persist only until the next call to one of the eval
	 * functions.
	 * 
	 * Side effects: The side effects will be determined by the exact Tcl code to be evaluated.
	 * 
	 * @param resName
	 *            the location of the Java resource. See the Java documentation of Class.getResourceAsStream() for
	 *            details on resource naming.
	 */

	public void evalResource(String resName) throws TclException {
		evalResource(resName, EncodingCmd.systemJavaEncoding);
	}
	/**
	 * Execute a Tcl script stored in the given Java resource location.
	 * 
	 * Results: The return value is void. However, a standard Tcl Exception may be generated. The interpreter's result
	 * object will contain the value of the evaluation but will persist only until the next call to one of the eval
	 * functions.
	 * 
	 * Side effects: The side effects will be determined by the exact Tcl code to be evaluated.
	 * 
	 * @param resName
	 *            the location of the Java resource. See the Java documentation of Class.getResourceAsStream() for
	 *            details on resource naming.
	 *  @param javaEncoding
	 *            Java charset name for Unicode encoding of resource
	 */

	public void evalResource(String resName, String javaEncoding) throws TclException {
		final boolean USE_SCRIPT_CACHE = true;

		boolean couldBeCached = false;
		boolean isCached = false;
		InputStream stream;
		String script = null;

		// Tcl library scripts can be cached since they will not change
		// after the JVM has started. A String value for the script is
		// cached after it has been read from the filesystem and been
		// processed for CRLF by the Tcl IO subsystem.

		if (USE_SCRIPT_CACHE && resName.startsWith("/tcl/lang/library/")
				&& (resName.equals("/tcl/lang/library/tclIndex") || resName.endsWith(".tcl"))) {
			couldBeCached = true;
		}

		// Note, we only want to synchronize to the tclLibraryScripts
		// table when dealing with a Tcl library resource. Other
		// resource loads should not need to grab a static monitor.

		if (USE_SCRIPT_CACHE && couldBeCached) {
			synchronized (tclLibraryScripts) {
				if ((script = (String) tclLibraryScripts.get(resName)) == null) {
					isCached = false;
				} else {
					isCached = true;
				}

				if (!isCached) {

					// When not cached, attempt to load via
					// getResourceAsStream and then add to the cache.

					stream = getResourceAsStream(resName);
					if (stream == null) {
						throw new TclException(this, "cannot read resource \"" + resName + "\"");
					}
					script = readScriptFromInputStream(stream, javaEncoding);
					if (script == null) {
						throw new TclException(this, "cannot read resource \"" + resName + "\"");
					}

					tclLibraryScripts.put(resName, script);
				} else {

					// No-op, just use script that was set above
				}
			}
		} else {

			stream = getResourceAsStream(resName);
			if (stream == null) {
				throw new TclException(this, "cannot read resource \"" + resName + "\"");
			}
			script = readScriptFromInputStream(stream, javaEncoding);
			if (script == null) {
				throw new TclException(this, "cannot read resource \"" + resName + "\"");
			}
		}

		// Define Interp.scriptFile as a resource so that [info script]
		// can be used to construct names of other resources in the
		// same resource directory.

		String oldScript = scriptFile;
		scriptFile = "resource:" + resName;

		try {
			eval(script, 0);
		} finally {
			scriptFile = oldScript;
		}
	}

	/**
	 * Figure out how to handle a backslash sequence. The index of the ChapPointer must be pointing to the first /.
	 * 
	 * Results: The return value is an instance of BackSlashResult that contains the character that should be
	 * substituted in place of the backslash sequence that starts at src.index, and an index to the next character after
	 * the backslash sequence.
	 * 
	 * Side effects: None.
	 * 
	 * @param s
	 * @param i
	 * @param len
	 * @return an instance of BackSlashResult that contains the character that should be substituted in place of 
	 * 		the backslash sequence that starts at src.index, and an index to the next character after the backslash sequence.
	 */
	
	public static BackSlashResult backslash(String s, int i, int len) {
		CharPointer script = new CharPointer(s.substring(0, len));
		script.index = i;
		return Parser.backslash(script.array, script.index);
	}

	/**
	 * This procedure is called to record machine-readable information about an error that is about to be returned. The
	 * caller should build a list object up and pass it to this routine.
	 * 
	 * Results: None.
	 * 
	 * Side effects: The errorCode global variable is modified to be the new value. A flag is set internally to remember
	 * that errorCode has been set, so the variable doesn't get set automatically when the error is returned.
	 * 
	 * If the errorCode variable have write traces, any arbitrary side effects may happen in those traces.
	 * TclException's caused by the traces, however, are ignored and not passed back to the caller of this function.
	 * 
	 * @param code
	 *            the errorCode object
	 */
	public void setErrorCode(TclObject code) {
		try {
			setVar("errorCode", null, code, TCL.GLOBAL_ONLY);
			errCodeSet = true;
		} catch (TclException excp) {
			// Ignore any TclException's, possibly caused by variable traces on
			// the errorCode variable. This is compatible with the behavior of
			// the Tcl C API.
		}
	}

	/**
	 * Add information to the "errorInfo" variable that describes the current error. Side effects: The contents of
	 * message are added to the "errorInfo" variable. If eval() has been called since the current value of errorInfo was
	 * set, errorInfo is cleared before adding the new message. If we are just starting to log an error, errorInfo is
	 * initialized from the error message in the interpreter's result. If the errorInfo variable have write traces, any
	 * arbitrary side effects may happen in those traces. TclException's caused by the traces, however, are ignored and
	 * not passed back to the caller of this function.
	 * 
	 * @param message
	 *            message to record in errorInfo
	 */
	public void addErrorInfo(String message) {
		if (!errInProgress) {
			errInProgress = true;

			try {
				setVar("::errorInfo", null, getResult().toString(), TCL.GLOBAL_ONLY);
			} catch (TclException e1) {
				// Ignore (see try-block above).
			}

			// If the errorCode variable wasn't set by the code
			// that generated the error, set it to "NONE".

			if (!errCodeSet) {
				try {
					setVar("errorCode", null, "NONE", TCL.GLOBAL_ONLY);
				} catch (TclException e1) {
					// Ignore (see try-block above).
				}
			}
		}

		try {
			setVar("::errorInfo", null, message, TCL.APPEND_VALUE | TCL.GLOBAL_ONLY);
		} catch (TclException e1) {
			// Ignore (see try-block above).
		}
	}

	/**
	 * Procedure called by Tcl_EvalObj to set the interpreter's result value to an appropriate error message when the
	 * code it evaluates returns an unexpected result code (not TCL_OK and not TCL_ERROR) to the topmost evaluation
	 * level.
	 * 
	 * Results: None.
	 * 
	 * Side effects: The interpreter result is set to an error message appropriate to the result code.
	 * 
	 * @param returnCode
	 *            the unexpected result code
	 */

	public void processUnexpectedResult(int returnCode) throws TclException {
		resetResult();
		if (returnCode == TCL.BREAK) {
			throw new TclException(this, "invoked \"break\" outside of a loop");
		} else if (returnCode == TCL.CONTINUE) {
			throw new TclException(this, "invoked \"continue\" outside of a loop");
		} else {
			throw new TclException(this, "command returned bad code: " + returnCode);
		}
	}

	/**
	 * This method is used by various parts of the Jacl and external packages. interpreter when a TclException of
	 * TCL.RETURN is received. The most common case is when the "return" command is executed inside a Tcl procedure.
	 * This method examines fields such as interp.returnCode and interp.errorCode and determines the real return status
	 * of the Tcl procedure accordingly. Side effects: The errorInfo and errorCode variables may get modified.
	 * 
	 * @return The return value is the true completion code to use for the Tcl procedure, instead of TCL.RETURN. It's
	 *         the same value that was given to the "return -code" option.
	 * 
	 *         If TCL.OK is returned, it means than the caller of this method should ignore any TclException that it has
	 *         received.
	 */
	public int updateReturnInfo() {
		int code;

		code = returnCode;
		returnCode = TCL.OK;

		if (code == TCL.ERROR) {
			try {
				setVar("errorCode", null, (errorCode != null) ? errorCode : "NONE", TCL.GLOBAL_ONLY);
			} catch (TclException e) {
				// An error may happen during a trace to errorCode. We ignore
				// it.
				// This may leave error messages inside Interp.result (which
				// is compatible with Tcl 8.4 behavior.
			}
			errCodeSet = true;

			if (errorInfo != null) {
				try {
					setVar("::errorInfo", null, errorInfo, TCL.GLOBAL_ONLY);
				} catch (TclException e) {
					// An error may happen during a trace to errorInfo. We
					// ignore it. This may leave error messages inside
					// Interp.result (which is compatible with Tcl 8.4
					// behavior.
				}
				errInProgress = true;
			}
		}

		return code;
	}

	/**
	 * Creates a new callframe. This method can be overrided to provide debugging support.
	 * 
	 * @param proc
	 *            the procedure which will later execute in the new CallFrame
	 * @param objv
	 *            the arguments that will be passed to the procedure
	 * @return A new CallFrame.
	 * @throws TclException
	 *             incorrect arguments passed.
	 */
	protected CallFrame newCallFrame(Procedure proc, TclObject[] objv) throws TclException {
		return new CallFrame(this, proc, objv);
	}

	/**
	 * Creates a new callframe. This method can be overrided to provide debugging support.
	 * 
	 * @return A new CallFrame.
	 */

	public CallFrame newCallFrame() {
		return new CallFrame(this);
	}

	/**
	 * Retrieve the current working directory for this interpreter. Side effects: If the working dir is null, set it to
	 * env(HOME)
	 * 
	 * @return the File for the current directory
	 */
	public File getWorkingDir() {
		if (workingDir == null) {
			try {
				String dirName = getVar("env", "HOME", 0).toString();
				workingDir = FileUtil.getNewFileObj(this, dirName);
			} catch (TclException e) {
				resetResult();
			}
			workingDir = new File(Util.tryGetSystemProperty("user.home", "."));
		}
		return workingDir;
	}

	/**
	 * Set the current working directory for this interpreter.
	 * 
	 * @param dirName
	 *            name of directory that will become working directory
	 */
	public void setWorkingDir(String dirName) throws TclException {
		File dirObj = FileUtil.getNewFileObj(this, dirName);

		// Use the canonical name of the path, if possible.

		try {
			dirObj = new File(dirObj.getCanonicalPath());
		} catch (IOException e) {
		}

		if (dirObj.isDirectory() && dirName.length() > 0) {
			workingDir = dirObj;
		} else {
			String dname = FileUtil.translateFileName(this, dirName);
			if (FileUtil.getPathType(dname) == FileUtil.PATH_RELATIVE) {
				dname = dirName;
			} else {
				dname = dirObj.getPath();
			}
			throw new TclException(this, "couldn't change working directory to \"" + dname
					+ "\": no such file or directory");
		}
	}

	/**
	 * Retrieve the Notifier associated with this Interp. This method can safely be invoked from a thread other than the
	 * thread the Interp was created in. If this method is invoked after the Interp object has been disposed of then
	 * null will be returned.
	 * 
	 * @return the Notifier for the thread the interp was created in.
	 */
	public Notifier getNotifier() {
		return notifier;
	}

	/**
	 * This procedure is invoked to declare that a particular version of a particular package is now present in an
	 * interpreter. There must not be any other version of this package already provided in the interpreter.
	 * 
	 * Results: Normally does nothing; if there is already another version of the package loaded then an error is
	 * raised.
	 * 
	 * Side effects: The interpreter remembers that this package is available, so that no other version of the package
	 * may be provided for the interpreter.
	 * 
	 * @param name
	 *            name of package
	 * @param version
	 *            version of package
	 * @throws TclException
	 */
	public final void pkgProvide(String name, String version) throws TclException {
		PackageCmd.pkgProvide(this, name, version);
	}

	/**
	 * This procedure is called by code that depends on a particular version of a particular package. If the package is
	 * not already provided in the interpreter, this procedure invokes a Tcl script to provide it. If the package is
	 * already provided, this procedure makes sure that the caller's needs don't conflict with the version that is
	 * present. Side effects: The script from some previous "package ifneeded" command may be invoked to provide the
	 * package.
	 * 
	 * @param pkgname
	 *            name of the package to require
	 * @param version
	 *            version to require
	 * @param exact
	 *            if true, require the exact versions specified
	 * 
	 * @return If successful, returns the version string for the currently provided version of the package, which may be
	 *         different from the "version" argument.
	 * 
	 * @throws TclException
	 *             If the caller's requirements cannot be met (e.g. the version requested conflicts with a currently
	 *             provided version, or the required version cannot be found, or the script to provide the required
	 *             version generates an error), a TclException is raised.
	 */
	public final String pkgRequire(String pkgname, String version, boolean exact) throws TclException {
		return PackageCmd.pkgRequire(this, pkgname, version, exact);
	}

	protected DebugInfo dbg;

	/**
	 * Initialize the debugging information. * Debugging API.
	 * 
	 * The following section defines two debugging API functions for logging information about the point of execution of
	 * Tcl scripts:
	 * 
	 * - pushDebugStack() is called when a procedure body is executed, or when a file is source'd. - popDebugStack() is
	 * called when the flow of control is about to return from a procedure body, or from a source'd file.
	 * 
	 * Two other API functions are used to determine the current point of execution:
	 * 
	 * - getScriptFile() returns the script file current being executed. - getArgLineNumber(i) returns the line number
	 * of the i-th argument of the current command.
	 * 
	 * Note: The point of execution is automatically maintained for control structures such as while, if, for and
	 * foreach, as long as they use Interp.eval(argv[?]) to evaluate control blocks.
	 * 
	 * The case and switch commands need to set dbg.cmdLine explicitly because they may evaluate control blocks that are
	 * not elements inside the argv[] array. ** This feature not yet implemented. **
	 * 
	 * The proc command needs to call getScriptFile() and getArgLineNumber(3) to find out the location of the proc body.
	 * 
	 * The debugging API functions in the Interp class are just dummy stub functions. These functions are usually
	 * implemented in a subclass of Interp (e.g. DbgInterp) that has real debugging support.
	 * 
	 * 
	 * @return a DebugInfo object used by Interp in non-debugging mode.
	 */
	protected DebugInfo initDebugInfo() {
		return new DebugInfo(null, 1);
	}

	/**
	 * Add more more level at the top of the debug stack.
	 * 
	 * @param fileName
	 *            the filename for the new stack level
	 * @param lineNumber
	 *            the line number at which the execution of the new stack level begins.
	 */
	void pushDebugStack(String fileName, int lineNumber) {
		// do nothing.
	}

	/**
	 * Remove the top-most level of the debug stack.
	 */
	void popDebugStack() throws TclRuntimeError {
		// do nothing
	}

	/**
	 * Returns the name of the script file currently under execution.
	 * 
	 * @return the name of the script file currently under execution.
	 */
	public String getScriptFile() {
		return dbg.fileName;
	}

	/**
	 * Returns the line number where the given command argument begins. E.g, if the following command is at line 10:
	 * 
	 * foo {a b } c
	 * 
	 * getArgLine(0) = 10 getArgLine(1) = 10 getArgLine(2) = 11
	 * 
	 * @param index
	 *            specifies an argument.
	 * @return the line number of the given argument.
	 */
	public int getArgLineNumber(int index) {
		return 0;
	}

	/**
	 * Copy the result (and error information) from one interp to another. Used when one interp has caused another
	 * interp to evaluate a script and then wants to transfer the results back to itself.
	 * 
	 * This routine copies the string reps of the result and error information. It does not simply increment the
	 * refcounts of the result and error information objects themselves. It is not legal to exchange objects between
	 * interps, because an object may be kept alive by one interp, but have an internal rep that is only valid while
	 * some other interp is alive.
	 * 
	 * Results: The target interp's result is set to a copy of the source interp's result. The source's error
	 * information "$errorInfo" may be appended to the target's error information and the source's error code
	 * "$errorCode" may be stored in the target's error code.
	 * 
	 * Side effects: None.
	 * 
	 * @param sourceInterp
	 *            interp whose result and error information should be moved to the target interp. AFter moving the
	 *            results, the interp's result is reset
	 * @param result
	 *            TCL.OK if just the result should be copied, TCL.ERROR if both teh result and the error information
	 *            should be copied
	 * @throws TclException
	 */

	public void transferResult(Interp sourceInterp, int result) throws TclException {
		if (sourceInterp == this) {
			if (result != TCL.OK) {
				throw new TclException(this, getResult().toString(), result);
			}
			return;
		}

		if (result == TCL.ERROR) {
			TclObject obj;

			// An error occurred, so transfer error information from the source
			// interpreter to the target interpreter. Setting the flags tells
			// the target interp that it has inherited a partial traceback
			// chain, not just a simple error message.

			if (!sourceInterp.errAlreadyLogged) {
				sourceInterp.addErrorInfo("");
			}
			sourceInterp.errAlreadyLogged = true;

			resetResult();

			obj = sourceInterp.getVar("errorInfo", TCL.GLOBAL_ONLY);
			setVar("errorInfo", obj, TCL.GLOBAL_ONLY);

			obj = sourceInterp.getVar("errorCode", TCL.GLOBAL_ONLY);
			setVar("errorCode", obj, TCL.GLOBAL_ONLY);

			errInProgress = true;
			errCodeSet = true;
		}

		returnCode = result;
		setResult(sourceInterp.getResult());
		sourceInterp.resetResult();

		if (result != TCL.OK) {
			throw new TclException(this, getResult().toString(), result);
		}
	}

	/**
	 * Makes a command hidden so that it cannot be invoked from within an interpreter, only from within an ancestor.
	 * 
	 * Results: A standard Tcl result; also leaves a message in the interp's result if an error occurs.
	 * 
	 * Side effects: Removes a command from the command table and create an entry into the hidden command table under
	 * the specified token name.
	 * 
	 * 
	 * @param cmdName
	 *            Name of command to hide.
	 * @param hiddenCmdToken
	 *            Token name of the to-be-hidden command.
	 * @throws TclException
	 */
	public void hideCommand(String cmdName, String hiddenCmdToken) throws TclException {
		WrappedCommand cmd;

		if (deleted) {
			// The interpreter is being deleted. Do not create any new
			// structures, because it is not safe to modify the interpreter.
			return;
		}

		// Disallow hiding of commands that are currently in a namespace or
		// renaming (as part of hiding) into a namespace.
		//
		// (because the current implementation with a single global table
		// and the needed uniqueness of names cause problems with namespaces)
		//
		// we don't need to check for "::" in cmdName because the real check is
		// on the nsPtr below.
		//
		// hiddenCmdToken is just a string which is not interpreted in any way.
		// It may contain :: but the string is not interpreted as a namespace
		// qualifier command name. Thus, hiding foo::bar to foo::bar and then
		// trying to expose or invoke ::foo::bar will NOT work; but if the
		// application always uses the same strings it will get consistent
		// behaviour.
		//
		// But as we currently limit ourselves to the global namespace only
		// for the source, in order to avoid potential confusion,
		// lets prevent "::" in the token too. --dl

		if (hiddenCmdToken.contains("::")) {
			throw new TclException(this, "cannot use namespace qualifiers in " + "hidden command token (rename)");
		}

		// Find the command to hide. An error is returned if cmdName can't
		// be found. Look up the command only from the global namespace.
		// Full path of the command must be given if using namespaces.

		cmd = Namespace.findCommand(this, cmdName, null,
		/* flags */TCL.LEAVE_ERR_MSG | TCL.GLOBAL_ONLY);

		// Check that the command is really in global namespace

		if (cmd.ns != globalNs) {
			throw new TclException(this, "can only hide global namespace commands" + " (use rename then hide)");
		}

		// Initialize the hidden command table if necessary.

		if (hiddenCmdTable == null) {
			hiddenCmdTable = new HashMap();
		}

		// It is an error to move an exposed command to a hidden command with
		// hiddenCmdToken if a hidden command with the name hiddenCmdToken
		// already
		// exists.

		if (hiddenCmdTable.containsKey(hiddenCmdToken)) {
			throw new TclException(this, "hidden command named \"" + hiddenCmdToken + "\" already exists");
		}

		// Nb : This code is currently 'like' a rename to a specialy set apart
		// name table. Changes here and in TclRenameCommand must
		// be kept in synch untill the common parts are actually
		// factorized out.

		// Remove the hash entry for the command from the interpreter command
		// table. This is like deleting the command, so bump its command epoch;
		// this invalidates any cached references that point to the command.

		if (cmd.table.containsKey(cmd.hashKey)) {
			cmd.table.remove(cmd.hashKey);
			cmd.incrEpoch();
		}

		// Now link the hash table entry with the command structure.
		// We ensured above that the nsPtr was right.

		cmd.table = hiddenCmdTable;
		cmd.hashKey = hiddenCmdToken;
		hiddenCmdTable.put(hiddenCmdToken, cmd);
	}

	/**
	 * Makes a previously hidden command callable from inside the interpreter instead of only by its ancestors.
	 * 
	 * Results: A standard Tcl result. If an error occurs, a message is left in the interp's result.
	 * 
	 * Side effects: Moves commands from one hash table to another.
	 * 
	 * @param hiddenCmdToken
	 *            Token name of the to-be-hidden command.
	 * @param cmdName
	 *            Name of command to hide.
	 * @throws TclException
	 */
	public void exposeCommand(String hiddenCmdToken, String cmdName) throws TclException {
		WrappedCommand cmd;

		if (deleted) {
			// The interpreter is being deleted. Do not create any new
			// structures, because it is not safe to modify the interpreter.
			return;
		}

		// Check that we have a regular name for the command
		// (that the user is not trying to do an expose and a rename
		// (to another namespace) at the same time)

		if (cmdName.contains("::")) {
			throw new TclException(this, "can not expose to a namespace " + "(use expose to toplevel, then rename)");
		}

		// Get the command from the hidden command table:

		if (hiddenCmdTable == null || !hiddenCmdTable.containsKey(hiddenCmdToken)) {
			throw new TclException(this, "unknown hidden command \"" + hiddenCmdToken + "\"");
		}
		cmd = (WrappedCommand) hiddenCmdTable.get(hiddenCmdToken);

		// Check that we have a true global namespace
		// command (enforced by Tcl_HideCommand() but let's double
		// check. (If it was not, we would not really know how to
		// handle it).

		if (cmd.ns != globalNs) {

			// This case is theoritically impossible,
			// we might rather panic() than 'nicely' erroring out ?

			throw new TclException(this, "trying to expose " + "a non global command name space command");
		}

		// This is the global table
		Namespace ns = cmd.ns;

		// It is an error to overwrite an existing exposed command as a result
		// of exposing a previously hidden command.

		if (ns.cmdTable.containsKey(cmdName)) {
			throw new TclException(this, "exposed command \"" + cmdName + "\" already exists");
		}

		// Remove the hash entry for the command from the interpreter hidden
		// command table.

		if (cmd.hashKey != null) {
			cmd.table.remove(cmd.hashKey);
			cmd.table = ns.cmdTable;
			cmd.hashKey = cmdName;
		}

		// Now link the hash table entry with the command structure.
		// This is like creating a new command, so deal with any shadowing
		// of commands in the global namespace.

		ns.cmdTable.put(cmdName, cmd);

		// Not needed as we are only in the global namespace
		// (but would be needed again if we supported namespace command hiding)

		// TclResetShadowedCmdRefs(interp, cmdPtr);
	}

	/**
	 * ----------------------------------------------------------------------
	 * 
	 * TclHideUnsafeCommands -> hideUnsafeCommands
	 * 
	 * Hides base commands that are not marked as safe from this interpreter.
	 * 
	 * Results: None
	 * 
	 * Side effects: Hides functionality in an interpreter.
	 * 
	 * ----------------------------------------------------------------------
	 */

	public void hideUnsafeCommands() throws TclException {
		for (String unsafeCmd : unsafeCmds) {
			try {
				hideCommand(unsafeCmd, unsafeCmd);
			} catch (TclException e) {
				if (!e.getMessage().startsWith("unknown command")) {
					throw e;
				}
			}
		}
	}

	/**
	 * Invokes a Tcl command, given an objv/objc, from either the exposed or hidden set of commands in the given
	 * interpreter. NOTE: The command is invoked in the global stack frame of the interpreter, thus it cannot see any
	 * current state on the stack of that interpreter.
	 * 
	 * Results: A standard Tcl result.
	 * 
	 * Side effects: Whatever the command does.
	 * 
	 * @param objv
	 *            Argument objects; objv[0] points to the name of the command to invoke.
	 * @param flags
	 *            Combination of flags controlling the call: INVOKE_HIDDEN,_INVOKE_NO_UNKNOWN, or INVOKE_NO_TRACEBACK.
	 * @return result
	 * @throws TclException
	 */
	public int invokeGlobal(TclObject[] objv, int flags) throws TclException {
		CallFrame savedVarFrame = varFrame;

		try {
			varFrame = null;
			return invoke(objv, flags);
		} finally {
			varFrame = savedVarFrame;
		}
	}

	/**
	 * Invokes a Tcl command, given an objv/objc, from either the exposed or the hidden sets of commands in the given
	 * interpreter.
	 * 
	 * @param objv
	 *            argument objects, objv[0] is the name of the command to invoke
	 * @param flags
	 *            Combination of flags controlling the call: INVOKE_HIDDEN,_INVOKE_NO_UNKNOWN, or INVOKE_NO_TRACEBACK.
	 * @return A standard Tcl object result.
	 */

	public int invoke(TclObject[] objv, int flags) throws TclException {
		if ((objv.length < 1) || (objv == null)) {
			throw new TclException(this, "illegal argument vector");
		}

		ready();

		String cmdName = objv[0].toString();
		WrappedCommand cmd;
		TclObject localObjv[] = null;

		if ((flags & INVOKE_HIDDEN) != 0) {

			// We never invoke "unknown" for hidden commands.

			if (hiddenCmdTable == null || !hiddenCmdTable.containsKey(cmdName)) {
				throw new TclException(this, "invalid hidden command name \"" + cmdName + "\"");
			}
			cmd = (WrappedCommand) hiddenCmdTable.get(cmdName);
		} else {
			cmd = Namespace.findCommand(this, cmdName, null, TCL.GLOBAL_ONLY);
			if (cmd == null) {
				if ((flags & INVOKE_NO_UNKNOWN) == 0) {
					cmd = Namespace.findCommand(this, "unknown", null, TCL.GLOBAL_ONLY);
					if (cmd != null) {
						localObjv = new TclObject[objv.length + 1];
						localObjv[0] = TclString.newInstance("unknown");
						localObjv[0].preserve();
						for (int i = 0; i < objv.length; i++) {
							localObjv[i + 1] = objv[i];
						}
						objv = localObjv;
					}
				}

				// Check again if we found the command. If not, "unknown" is
				// not present and we cannot help, or the caller said not to
				// call "unknown" (they specified TCL_INVOKE_NO_UNKNOWN).

				if (cmd == null) {
					throw new TclException(this, "invalid command name \"" + cmdName + "\"");
				}
			}
		}

		// Invoke the command procedure. First reset the interpreter's string
		// and object results to their default empty values since they could
		// have gotten changed by earlier invocations.

		resetResult();
		cmdCount++;

		int result = TCL.OK;
		try {
			if (cmd.mustCallInvoke(this))
				cmd.invoke(this, objv);
			else
				cmd.cmd.cmdProc(this, objv);
		} catch (TclException e) {
			result = e.getCompletionCode();
		}

		// If we invoke a procedure, which was implemented as AutoloadStub,
		// it was entered into the ordinary cmdTable. But here we know
		// for sure, that this command belongs into the hiddenCmdTable.
		// So if we can find an entry in cmdTable with the cmdName, just
		// move it into the hiddenCmdTable.

		if ((flags & INVOKE_HIDDEN) != 0) {
			cmd = Namespace.findCommand(this, cmdName, null, TCL.GLOBAL_ONLY);
			if (cmd != null) {
				// Basically just do the same as in hideCommand...
				cmd.table.remove(cmd.hashKey);
				cmd.table = hiddenCmdTable;
				cmd.hashKey = cmdName;
				hiddenCmdTable.put(cmdName, cmd);
			}
		}

		// If an error occurred, record information about what was being
		// executed when the error occurred.

		if ((result == TCL.ERROR) && ((flags & INVOKE_NO_TRACEBACK) == 0) && !errAlreadyLogged) {
			StringBuffer ds;

			if (errInProgress) {
				ds = new StringBuffer("\n    while invoking\n\"");
			} else {
				ds = new StringBuffer("\n    invoked from within\n\"");
			}
			for (int i = 0; i < objv.length; i++) {
				ds.append(objv[i].toString());
				if (i < (objv.length - 1)) {
					ds.append(" ");
				} else if (ds.length() > 100) {
					ds.append("...");
					break;
				}
			}
			ds.append("\"");
			addErrorInfo(ds.toString());
			errInProgress = true;
		}

		// Free any locally allocated storage used to call "unknown".

		if (localObjv != null) {
			localObjv[0].release();
		}

		return result;
	}

	/**
	 * Sets a flag in an interpreter so that exceptions can occur in the next call to Tcl_Eval without them being turned
	 * into errors.
	 * 
	 * Results: None.
	 * 
	 * Side effects: The TCL_ALLOW_EXCEPTIONS flag gets set in the interpreter's evalFlags structure. See the reference
	 * documentation for more details.
	 */
	public void allowExceptions() {
		evalFlags |= Parser.TCL_ALLOW_EXCEPTIONS;
	}

	class ResolverScheme {
		String name; // Name identifying this scheme.
		Resolver resolver;

		ResolverScheme(String name, Resolver resolver) {
			this.name = name;
			this.resolver = resolver;
		}
	}

	/**
	 * ----------------------------------------------------------------------
	 * 
	 * Tcl_AddInterpResolvers -> addInterpResolver
	 * 
	 * Adds a set of command/variable resolution procedures to an interpreter. These procedures are consulted when
	 * commands are resolved in Namespace.findCommand, and when variables are resolved in Namespace.findNamespaceVar and
	 * thus Var.lookupVar. Each namespace may also have its own resolution object which take precedence over those for
	 * the interpreter.
	 * 
	 * When a name is resolved, it is handled as follows. First, the name is passed to the resolution objects for the
	 * namespace. If not resolved, the name is passed to each of the resolution procedures added to the interpreter.
	 * Finally, if still not resolved, the name is handled using the default Tcl rules for name resolution.
	 * 
	 * Results: None.
	 * 
	 * Side effects: The list of resolvers of the given interpreter is modified.
	 * 
	 * @param name
	 *            Name of this resolution scheme
	 * @param resolver
	 *            Object to resolve commands/variables.
	 */
	public void addInterpResolver(String name, Resolver resolver) {
		ResolverScheme res;

		// Look for an existing scheme with the given name.
		// If found, then replace its rules.

		if (resolvers != null) {
			for (Object resolver1 : resolvers) {
				res = (ResolverScheme) resolver1;
				if (name.equals(res.name)) {
					res.resolver = resolver;
					return;
				}
			}
		}

		if (resolvers == null) {
			resolvers = new ArrayList();
		}

		// Otherwise, this is a new scheme. Add it to the FRONT
		// of the linked list, so that it overrides existing schemes.

		res = new ResolverScheme(name, resolver);

		resolvers.add(0, res);
	}

	/**
	 * Looks for a set of command/variable resolution procedures with the given name in an interpreter. These procedures
	 * are registered by calling addInterpResolver.
	 * 
	 * Results: If the name is recognized, this procedure returns the object implementing the name resolution
	 * procedures. If the name is not recognized, this procedure returns null.
	 * 
	 * Side effects: None.
	 * 
	 * @param name
	 *            Look for a scheme with scheme.
	 * @return the object implementing the name resolution procedures.
	 */
	public Resolver getInterpResolver(String name) {
		ResolverScheme res;
		Enumeration e;

		// Look for an existing scheme with the given name. If found,
		// then return pointers to its procedures.

		if (resolvers != null) {
			for (Object resolver : resolvers) {
				res = (ResolverScheme) resolver;
				if (name.equals(res.name)) {
					return res.resolver;
				}
			}
		}

		return null;
	}

	/**
	 * Removes a set of command/variable resolution procedures previously added by addInterpResolver. The next time a
	 * command/variable name is resolved, these procedures won't be consulted.
	 * 
	 * Results: Returns true if the name was recognized and the resolution scheme was deleted. Returns false otherwise.
	 * 
	 * Side effects: The list of resolvers of the given interpreter may be modified.
	 * 
	 * @param name
	 *            Name of the scheme to be removed.
	 * @return true if the name was recognized and the resolution scheme was deleted. Returns false otherwise.
	 */
	public boolean removeInterpResolver(String name) {
		ResolverScheme res;
		boolean found = false;

		// Look for an existing scheme with the given name.

		if (resolvers != null) {
			for (Object resolver : resolvers) {
				res = (ResolverScheme) resolver;
				if (name.equals(res.name)) {
					found = true;
					break;
				}
			}
		}

		// If we found the scheme, delete it.

		if (found) {
			int index = resolvers.indexOf(name);
			if (index == -1) {
				throw new TclRuntimeError("name " + name + " not found in resolvers");
			}
			resolvers.remove(index);
		}

		return found;
	}

	/**
	 * checkCommonInteger()
	 * 
	 * If a given integer value is in the common value pool then return a shared object for that integer. If the integer
	 * value is not in the common pool then use to use the recycled int value or a new TclObject.
	 * 
	 * @param value
	 *            integer to test
	 * @return TclObject containing 'value'
	 */

	public final TclObject checkCommonInteger(long value) {
		if (VALIDATE_SHARED_RESULTS) {
			TclObject[] objv = { m_minusoneIntegerResult, m_zeroIntegerResult, m_oneIntegerResult, m_twoIntegerResult };
			for (TclObject obj : objv) {
				if (!obj.isShared()) {
					throw new TclRuntimeError("ref count error: " + "integer constant for " + obj.toString()
							+ " should be shared but refCount was " + obj.getRefCount());
				}
			}
		}

		if (value == -1) {
			return m_minusoneIntegerResult;
		} else if (value == 0) {
			return m_zeroIntegerResult;
		} else if (value == 1) {
			return m_oneIntegerResult;
		} else if (value == 2) {
			return m_twoIntegerResult;
		} else {
			if ((recycledI.getRefCount() == 1) || ((recycledI.getRefCount() == 2) && (recycledI == m_result))) {
				// If (refCount == 1) then interp result
				// is not recycledI and nobody else holds a ref,
				// so we can modify recycledI.

				// If (refCount == 2) and this object is the
				// interp result then we can modify recycledI.

				recycledI.setRecycledIntValue(value);
			} else {
				// This logic is executed when some other
				// code holds a ref to recycledI. This
				// can happen when recycledI's refCount
				// is (refCount > 2) or (refCount == 2)
				// but the result is not recycledI.
				// If (refCount == 0) then release()
				// will raise an exception.

				recycledI.release();
				recycledI = TclInteger.newInstance(value);
				recycledI.preserve();
			}

			if (VALIDATE_SHARED_RESULTS) {
				if (!((recycledI.getRefCount() == 1) || (recycledI.getRefCount() == 2))) {
					throw new TclRuntimeError("ref count error: " + "recycledI refCount should be 1 or 2, it was "
							+ recycledI.getRefCount());
				}
			}

			return recycledI;
		}
	}

	/**
	 * If a given double value is in the common value pool the return a shared object for that double. If the double
	 * value is not in the common pool then a new TclDouble wrapped in a TclObject will be created.
	 * 
	 */

	final TclObject checkCommonDouble(double value) {
		if (VALIDATE_SHARED_RESULTS) {
			TclObject[] objv = { m_zeroDoubleResult, m_onehalfDoubleResult, m_oneDoubleResult, m_twoDoubleResult };
			for (TclObject obj : objv) {
				if (!obj.isShared()) {
					throw new TclRuntimeError("ref count error: " + "double constant for " + obj.toString()
							+ " should be shared but refCount was " + obj.getRefCount());
				}
			}
		}

		if (value == 0.0) {
			return m_zeroDoubleResult;
		} else if (value == 0.5) {
			return m_onehalfDoubleResult;
		} else if (value == 1.0) {
			return m_oneDoubleResult;
		} else if (value == 2.0) {
			return m_twoDoubleResult;
		} else {
			if ((recycledD.getRefCount() == 1) || ((recycledD.getRefCount() == 2) && (recycledD == m_result))) {
				// If (refCount == 1) then interp result
				// is not recycledD and nobody else holds a ref,
				// so we can modify recycledD.

				// If (refCount == 2) and this object is the
				// interp result then we can modify recycledD.

				recycledD.setRecycledDoubleValue(value);
			} else {
				// This logic is executed when some other
				// code holds a ref to recycledD. This
				// can happen when recycledD's refCount
				// is (refCount > 2) or (refCount == 2)
				// but the result is not recycledD.
				// If (refCount == 0) then release()
				// will raise an exception.

				recycledD.release();
				recycledD = TclDouble.newInstance(value);
				recycledD.preserve();
			}

			if (VALIDATE_SHARED_RESULTS) {
				if (!((recycledD.getRefCount() == 1) || (recycledD.getRefCount() == 2))) {
					throw new TclRuntimeError("ref count error: " + "recycledD refCount should be 1 or 2, it was "
							+ recycledD.getRefCount());
				}
			}

			return recycledD;
		}
	}

	/**
	 * Always return a shared boolean TclObject.
	 * 
	 */

	final TclObject checkCommonBoolean(boolean value) {
		if (VALIDATE_SHARED_RESULTS) {
			TclObject[] objv = { m_trueBooleanResult, m_falseBooleanResult };
			for (TclObject obj : objv) {
				if (!obj.isShared()) {
					throw new TclRuntimeError("ref count error: " + "boolean constant for " + obj.toString()
							+ " should be shared but refCount was " + obj.getRefCount());
				}
			}
		}

		return (value ? m_trueBooleanResult : m_falseBooleanResult);
	}

	/**
	 * If a given String value is in the common value pool the return a shared object for that String. If the String
	 * value is not in the common pool then a new TclString wrapped in a TclObject will be created.
	 * 
	 */

	public final TclObject checkCommonString(String value) {
		if (value == null || "".equals(value) || value.length() == 0) {
			if (VALIDATE_SHARED_RESULTS) {
				if (!m_nullResult.isShared()) {
					throw new TclRuntimeError("ref count error: "
							+ "empty string constant should be shared but refCount was " + m_nullResult.getRefCount());
				}
			}
			return m_nullResult;
		} else {
			return TclString.newInstance(value);
		}
	}

	/**
	 * It is very common to create a TclObject that contains a single character. It can be costly to allocate a
	 * TclObject, a TclString internal rep, and a String to represent a character. This method avoids that overhead for
	 * the most common characters. This method will return null if a character does not have a cached value.
	 * 
	 */

	public final TclObject checkCommonCharacter(int c) {
		if ((c <= 0) || (c >= m_charCommonMax)) {
			return null;
		}
		if (VALIDATE_SHARED_RESULTS) {
			if ((m_charCommon[c] != null) && !m_charCommon[c].isShared()) {
				throw new TclRuntimeError("ref count error: " + "common character for '" + c + "' is not shared");
			}
		}
		return m_charCommon[c];
	}

	/**
	 * Query the interp.errorLine member. This is like accessing the public Tcl_Interp.errorLine field in the C impl.
	 * this method should be used by classes outside the tcl.lang package.
	 * 
	 * Results: None.
	 * 
	 * Side effects: None.
	 * 
	 */

	public int getErrorLine() {
		return errorLine;
	}

	/**
	 * Get the TclClassLoader used for the interp. This class loader delagates to the context class loader which
	 * delagates to the system class loader. The TclClassLoader will read classes and resources from the
	 * env(TCL_CLASSPATH).
	 * 
	 * Results: This method will return the classloader in use, it will never return null.
	 * 
	 * Side effects: None.
	 * 
	 */

	public ClassLoader getClassLoader() {
		// Allocate a TclClassLoader that will delagate to the
		// context class loader and then search on the
		// env(TCL_CLASSPATH) for classes.

		if (classLoader == null) {
			classLoader = new TclClassLoader(this, null, Thread.currentThread().getContextClassLoader()
			// Interp.class.getClassLoader()
			);
		}
		return classLoader;
	}

	/**
	 * Resolve a resource name into an InputStream. This method will search for a resource using the TclClassLoader.
	 * This method will return null if a resource can't be found.
	 * 
	 * Results: None.
	 * 
	 * Side effects: None.
	 * 
	 * @param resName
	 *            resource to open as InputStream
	 * @return InputStream
	 */
	InputStream getResourceAsStream(String resName) {
		if (classLoader == null) {
			getClassLoader();
		}

		try {
			// Search for resource using TclClassLoader. This
			// will search on the CLASSPATH, then with the
			// context loader (if there is one), and then on
			// the env(TCL_CLASSPATH).

			return classLoader.getResourceAsStream(resName);
		} catch (PackageNameException e) {
			// Failed attempt to load resource with java or tcl prefix.

			return null;
		} catch (SecurityException e) {
			// Resource loading does not work in an applet, and Jacl
			// has never really worked as an applet anyway.

			return null;
		}
	}

	/**
	 * Invoke this method to indicate that an executing interp should be interrupted at the next safe moment.
	 * Interrupting a running interpreter will unwind the stack by throwing an exception. This method can safely be
	 * called from a thread other than the one processsing events. No explicit synchronization is needed. Once a thread
	 * has been interrupted or disposed of, setInterrupted() calls will do nothing.
	 * 
	 * Results: Stops execution of the Interp via an Exception.
	 * 
	 * Side effects: None.
	 * 
	 */

	public void setInterrupted() {
		if (deleted || (interruptedEvent != null)) {
			// This interpreter was interrupted already. Do nothing and
			// return right away. This logic handles the case of an
			// interpreter that was already disposed of because of a
			// previous interrupted event.
			//
			// The disposed check avoids a race condition between a
			// timeout thread that will interrupt an interp and the
			// main thread that could interrupt and then dispose
			// of the interp. The caller of this method has no way
			// to check if the interp has been disposed of, so this
			// method needs to no-op on an already deleted interp.

			return;
		}

		TclInterruptedExceptionEvent ie = new TclInterruptedExceptionEvent(this);

		// Set the interruptedEvent field in the Interp. It is possible
		// that a race condition between two threads could cause
		// multiple assignments of the interruptedEvent field to
		// overwrite each other. Give up if the assignment was overwritten
		// so that only one thread continues to execute.

		interruptedEvent = ie;

		if (interruptedEvent != ie) {
			return;
		}

		// Queue up an event that will generate a TclInterruptedException
		// the next time events from the Tcl event queue are processed.
		// If an eval returns and invokes checkInterrupted() before
		// the event loop is entered, then this event will be canceled.
		// The getNotifier() method should never return null since
		// the deleted flag was already checked above.

		getNotifier().queueEvent(interruptedEvent, TCL.QUEUE_TAIL);
	}

	/**
	 * This method is invoked after an eval operation to check if a running interp has been marked as interrupted. This
	 * method is not public since it should only be used by the Jacl internal implementation.
	 * 
	 * Results: This method will raise a TclInterruptedException if the Interp.setInterrupted() method was invoked for
	 * this interp. This method will only raise a TclInterruptedException once.
	 * 
	 * Side effects: None.
	 * 
	 */

	public final void checkInterrupted() {
		if ((interruptedEvent != null) && (!interruptedEvent.exceptionRaised)) {
			// Note that the interruptedEvent in not removed from the
			// event queue since all queued events should be removed
			// from the queue in the disposeInterrupted() method.

			interruptedEvent.exceptionRaised = true;

			throw new TclInterruptedException(this);
		}
	}

	/**
	 * This method is invoked to cleanup an Interp object that has been interrupted and had its stack unwound. This
	 * method will remove any pending events from the Tcl event queue and then invoke the dispose() method for this
	 * interp. The interp object should not be used after this method has finished. This method must only ever be
	 * invoked after catching a TclInterrupted exception at the outermost level of the Tcl event processing loop.
	 * 
	 */

	final void disposeInterrupted() {

		if (deleted) {
			final String msg = "Interp.disposeInterrupted() invoked for " + "a deleted interp";

			throw new TclRuntimeError(msg);
		}

		if (interruptedEvent == null) {
			final String msg = "Interp.disposeInterrupted() invoked for "
					+ "an interp that was not interrupted via setInterrupted()";

			throw new TclRuntimeError(msg);
		}

		// If the interruptedEvent has not been processed yet,
		// then remove it from the Tcl event queue.

		if ((interruptedEvent != null) && !interruptedEvent.wasProcessed) {
			getNotifier().deleteEvents(interruptedEvent);
		}

		// Remove each after event from the Tcl event queue.
		// It is not possible to remove events from the Tcl
		// event queue directly since an event does not
		// know which interp it was registered for. This
		// logic loops of pending after events and deletes
		// each one from the Tcl event queue. Note that
		// an interrupted interp only raises the interrupted
		// exception once, so it is legal to execute Tcl code
		// here to cleanup after events.

		try {
			eval("after info", 0);
			TclObject tobj = getResult();
			tobj.preserve();
			int len = TclList.getLength(this, tobj);
			for (int i = 0; i < len; i++) {
				TclObject evt = TclList.index(this, tobj, i);
				String cmd = "after cancel " + evt;
				eval(cmd, 0);
			}
			tobj.release();
		} catch (TclException te) {
			// ignored
		}

		// Actually dispose of the interp. After this dispose
		// call is invoked, it should not be possible to invoke
		// commands in this interp.

		dispose();
	}

	/**
	 * 
	 * Set new value for maxNestingDepth
	 * 
	 * @param depth
	 *            If > 0, set as new depth; otherwise don't set new value
	 * @return old value, or current value if depth <= 0
	 */

	public int setMaxNestingDepth(int depth) {
		int old = this.maxNestingDepth;

		if (depth > 0) {
			this.maxNestingDepth = depth;
		}

		return old;
	}

	/**
	 * setMaxNestingDepth --
	 * 
	 * @return value of maxNestingDepth
	 */

	public int getMaxNestingDepth() {
		return maxNestingDepth;
	}

	/**
	 * Debug print info about the interpreter.
	 */

	public String toString() {
		StringBuilder buffer = new StringBuilder();

		String info = super.toString();

		// Trim "tcl.lang.Interp@9b688e" to "Interp@9b688e"

		if (info.startsWith("tcl.lang.Interp")) {
			info = info.substring(9);
		}

		buffer.append(info);
		buffer.append(' ');
		buffer.append("allocated in \"" + cThreadName + "\"");

		return buffer.toString();
	}

	/**
	 * Get the name of the executable file to invoke JTcl.  Typically set on the first
	 * call to [info nameofexecutable]
	 * @return name of executable
	 */
	public String getNameOfExecutable() {
		return nameOfExecutable;
	}

	/**
	 * Set the name of the executable file to invoke JTcl.  Typically set on the first
	 * call to [info nameofexecutable]
	 */
	public void setNameOfExecutable(String name) {
		nameOfExecutable = name;
	}

	/**
	 * Get the name of the shell class name to invoke JTcl.  Set by default as
	 * the constructer of Interp.  If shellClassName was set as null, return
	 * the name of "tcl.lang.NoInteractiveShell"
	 * @return shell class name
	 */
	public String getShellClassName() {
		return shellClassName == null ? "tcl.lang.NonInteractiveShell" : shellClassName;
	}
	
	/**
	 * Set the name of the shell class name to invoke JTcl.  Can be null, which causes
	 * "tcl.lang.NoInteractiveShell" to be returned by getshellClassName()
	 */
	public void setShellClassName(String name) {
		shellClassName = name;
	}
	
}
