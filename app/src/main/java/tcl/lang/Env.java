/* 
 * Env.java --
 *
 *	This class is used to create and manage the environment array
 *	used by the Tcl interpreter.
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 *
 * RCS: @(#) $Id: Env.java,v 1.2 1999/08/07 05:46:26 mo Exp $
 */

package tcl.lang;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

/**
 * This class manages the environment array for Tcl interpreters.
 */

class Env  {
	

	/***
	 * This method is called to initialize an interpreter with it's initial
	 * values for the env array.
	 * 
	 * Results: None.
	 * 
	 * Side effects: The env array in the interpreter is created and populated.
     *
     *  @param interp 
	 */

	static void initialize(Interp interp) {
		// For a few standrad environment vairables that Tcl users
		// often assume aways exist (even if they shouldn't), we will
		// try to create those expected variables with the common unix
		// names.

		try {
			interp.setVar("env", "CLASSPATH", Util.tryGetSystemProperty(
					"java.class.path", ""), TCL.GLOBAL_ONLY);
		} catch (TclException e) {
			// Ignore errors.
		}

		try {
			interp.setVar("env", "HOME", Util.tryGetSystemProperty("user.home",
					""), TCL.GLOBAL_ONLY);
		} catch (TclException e) {
			// Ignore errors.
		}

		try {
			interp.setVar("env", "USER", Util.tryGetSystemProperty("user.name",
					""), TCL.GLOBAL_ONLY);
		} catch (TclException e) {
			// Ignore errors.
		}

		// Now we will populate the rest of the env array with the
		// properties recieved from the System classes. This makes for
		// a nice shortcut for getting to these useful values.

		try {

			Properties props = System.getProperties();
			Enumeration list = props.propertyNames();
			while (list.hasMoreElements()) {
				String key = (String) list.nextElement();
				try {
					interp.setVar("env", key, props.getProperty(key),
							TCL.GLOBAL_ONLY);
				} catch (TclException e1) {
					// Ignore errors.
				}
			}
			
			// populate with the actual environment.  Since Java 1.6 doesn't allow us 
			// to modify the environment any changes won't really be propagated 
			// to the env.  This could be implemented later with a tcl.lang.Resolver
			// and some JNI code
			for (Entry<String, String> env : System.getenv().entrySet()) {
				interp.setVar("env", env.getKey(), env.getValue(), TCL.GLOBAL_ONLY);
			}
		} catch (SecurityException e2) {
			// We are inside a browser and we can't access the list of
			// property names. That's fine. Life goes on ....
		} catch (Exception e3) {
			// We are inside a browser and we can't access the list of
			// property names. That's fine. Life goes on ....

			System.out.println("Exception while initializing env array");
			System.out.println(e3);
			System.out.println("");
		}
	}
	
	/**
	 * Replacement for System.getenv().  Returns JTcl's view of the environment,
	 * as stored in the ::env() array.  
	 * 
	 * JTcl modifications to values in the env array are not visible to
	 * System.getenv() due to Java API restrictions.
	 * 
	 * @param interp the current interpreter
	 * @return an unmodifiable Map<String, String> view of JTcl's ::env array.
	 */
	public static Map<String, String> getenv(Interp interp) {
		Var [] retArray = null;
		try {
			retArray = Var.lookupVar(interp, "env", null, TCL.GLOBAL_ONLY, null, false, false);
		} catch (TclException e) {
			// throwException is false;  it's not being thrown
		}
		if (retArray==null || retArray[0]==null || ! retArray[0].isVarArray()) {
			// revert to System.getenv()
			try {
				return System.getenv();
			} catch (SecurityException e) {
				// System access exception, just return empty map
				return Collections.unmodifiableMap(new HashMap<String,String>());
			}
		} else {
			HashMap<String, String> env = new HashMap<>();
			Map<String, Var> arrayMap = retArray[0].getArrayMap();
			for (Entry<String, Var> envVar : arrayMap.entrySet()) {
				if (! envVar.getValue().isVarUndefined()) {
					String value = envVar.getValue().getValue().toString();
					if (value!=null) {
						env.put(envVar.getKey(), value);
					}
				}
			}
			return Collections.unmodifiableMap(env);
		}
	}

} // end Env
