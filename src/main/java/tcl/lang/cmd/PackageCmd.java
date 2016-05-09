/* 
 * PackageCmd.java --
 *
 *	This class implements the built-in "package" command in Tcl.
 *
 * Copyright (c) 1997 by Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 *
 * RCS: @(#) $Id: PackageCmd.java,v 1.7 2006/01/26 19:49:18 mdejong Exp $
 */

package tcl.lang.cmd;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TCL;
import tcl.lang.TclException;
import tcl.lang.TclIndex;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;
import tcl.lang.TclRuntimeError;
import tcl.lang.Util;

public class PackageCmd implements Command {

	private static final String[] validCmds = { "forget", "ifneeded", "names",
			"present", "provide", "require", "unknown", "vcompare", "versions",
			"vsatisfies" };

	static final private int OPT_FORGET = 0;
	static final private int OPT_IFNEEDED = 1;
	static final private int OPT_NAMES = 2;
	static final private int OPT_PRESENT = 3;
	static final private int OPT_PROVIDE = 4;
	static final private int OPT_REQUIRE = 5;
	static final private int OPT_UNKNOWN = 6;
	static final private int OPT_VCOMPARE = 7;
	static final private int OPT_VERSIONS = 8;
	static final private int OPT_VSATISFIES = 9;

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * pkgProvide --
	 * 
	 * This procedure is invoked to declare that a particular version of a
	 * particular package is now present in an interpreter. There must not be
	 * any other version of this package already provided in the interpreter.
	 * 
	 * Results: Normally does nothing; if there is already another version of
	 * the package loaded then an error is raised.
	 * 
	 * Side effects: The interpreter remembers that this package is available,
	 * so that no other version of the package may be provided for the
	 * interpreter.
	 * 
	 * ----------------------------------------------------------------------
	 */

	public static void pkgProvide(Interp interp, // Interpreter in which package
			// is now
			// available.
			String pkgName, // Name of package.
			String version) // Version string for package.
			throws TclException {
		Package pkg;

		// Validate the version string that was passed in.

		checkVersion(interp, version);
		pkg = findPackage(interp, pkgName);
		if (pkg.version == null) {
			pkg.version = version;
			return;
		}
		if (compareVersions(pkg.version, version, null) != 0) {
			throw new TclException(interp,
					"conflicting versions provided for package \"" + pkgName
							+ "\": " + pkg.version + ", then " + version);
		}
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * pkgRequire --
	 * 
	 * This procedure is called by code that depends on a particular version of
	 * a particular package. If the package is not already provided in the
	 * interpreter, this procedure invokes a Tcl script to provide it. If the
	 * package is already provided, this procedure makes sure that the caller's
	 * needs don't conflict with the version that is present.
	 * 
	 * Results: If successful, returns the version string for the currently
	 * provided version of the package, which may be different from the
	 * "version" argument. If the caller's requirements cannot be met (e.g. the
	 * version requested conflicts with a currently provided version, or the
	 * required version cannot be found, or the script to provide the required
	 * version generates an error), a TclException is raised.
	 * 
	 * Side effects: The script from some previous "package ifneeded" command
	 * may be invoked to provide the package.
	 * 
	 * ----------------------------------------------------------------------
	 */

	public static String pkgRequire(Interp interp, // Interpreter in which
			// package is now
			// available.
			String pkgName, // Name of desired package.
			String version, // Version string for desired version;
			// null means use the latest version
			// available.
			boolean exact) // true means that only the particular
			// version given is acceptable. false means
			// use the latest compatible version.
			throws TclException {
		VersionSatisfiesResult vsres;
		Package pkg;
		PkgAvail avail, best;
		String script;
		StringBuffer sbuf;
		int pass, result;

		// Do extra check to make sure that version is not
		// null when the exact flag is set to true.

		if (version == null && exact) {
			throw new TclException(interp,
					"conflicting arguments : version == null and exact == true");
		}

		// Before we can compare versions the version string
		// must be verified but if it is null we are just looking
		// for the latest version so skip the check in this case.

		if (version != null) {
			checkVersion(interp, version);
		}

		// It can take up to three passes to find the package: one pass to
		// run the "package unknown" script, one to run the "package ifneeded"
		// script for a specific version, and a final pass to lookup the
		// package loaded by the "package ifneeded" script.

		vsres = new VersionSatisfiesResult();
		for (pass = 1;; pass++) {
			pkg = findPackage(interp, pkgName);
			if (pkg.version != null) {
				break;
			}

			// The package isn't yet present. Search the list of available
			// versions and invoke the script for the best available version.

			best = null;
			for (avail = pkg.avail; avail != null; avail = avail.next) {
				if ((best != null)
						&& (compareVersions(avail.version, best.version, null) <= 0)) {
					continue;
				}
				if (version != null) {
					result = compareVersions(avail.version, version, vsres);
					if ((result != 0) && exact) {
						continue;
					}
					if (!vsres.satisfies) {
						continue;
					}
				}
				best = avail;
			}
			if (best != null) {
				// We found an ifneeded script for the package. Be careful while
				// executing it: this could cause reentrancy, so (a) protect the
				// script itself from deletion and (b) don't assume that best
				// will still exist when the script completes.

				script = best.script;
				try {
					interp.eval(script, TCL.EVAL_GLOBAL);
				} catch (TclException e) {
					interp.addErrorInfo("\n    (\"package ifneeded\" script)");

					// Throw the error with new info added to errorInfo.

					throw e;
				}
				interp.resetResult();
				pkg = findPackage(interp, pkgName);
				break;
			}

			// Package not in the database. If there is a "package unknown"
			// command, invoke it (but only on the first pass; after that,
			// we should not get here in the first place).

			if (pass > 1) {
				break;
			}
			script = interp.packageUnknown;
			if (script != null) {
				sbuf = new StringBuffer();
				try {
					Util.appendElement(interp, sbuf, script);
					Util.appendElement(interp, sbuf, pkgName);
					if (version == null) {
						Util.appendElement(interp, sbuf, "");
					} else {
						Util.appendElement(interp, sbuf, version);
					}
					if (exact) {
						Util.appendElement(interp, sbuf, "-exact");
					}
				} catch (TclException e) {
					throw new TclRuntimeError("unexpected TclException: " + e);
				}
				try {
					interp.eval(sbuf.toString(), TCL.EVAL_GLOBAL);
				} catch (TclException e) {
					interp.addErrorInfo("\n    (\"package unknown\" script)");

					// Throw the first exception.

					throw e;
				}
				interp.resetResult();
			}
		}
		if (pkg.version == null) {
			sbuf = new StringBuffer();
			sbuf.append("can't find package " + pkgName);
			if (version != null) {
				sbuf.append(" " + version);
			}
			throw new TclException(interp, sbuf.toString());
		}

		// At this point we know that the package is present. Make sure that the
		// provided version meets the current requirement.

		if (version == null) {
			return pkg.version;
		}

		result = compareVersions(pkg.version, version, vsres);
		if ((vsres.satisfies && !exact) || (result == 0)) {
			return pkg.version;
		}

		// If we have a version conflict we throw a TclException.

		throw new TclException(interp, "version conflict for package \""
				+ pkgName + "\": have " + pkg.version + ", need " + version);
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * Tcl_PkgPresent -> pkgPresent
	 * 
	 * Checks to see whether the specified package is present. If it is not then
	 * no additional action is taken.
	 * 
	 * Results: If successful, returns the version string for the currently
	 * provided version of the package, which may be different from the
	 * "version" argument. If the caller's requirements cannot be met (e.g. the
	 * version requested conflicts with a currently provided version), a
	 * TclException is raised.
	 * 
	 * Side effects: None.
	 * 
	 * ----------------------------------------------------------------------
	 */
	static String pkgPresent(Interp interp, // Interpreter in which package is
			// now
			// available.
			String pkgName, // Name of desired package.
			String version, // Version string for desired version;
			// null means use the latest version
			// available.
			boolean exact) // true means that only the particular
			// version given is acceptable. false means
			// use the latest compatible version.
			throws TclException {
		Package pkg;
		VersionSatisfiesResult vsres = new VersionSatisfiesResult();
		int result;

		pkg = (Package) interp.packageTable.get(pkgName);
		if (pkg != null) {
			if (pkg.version != null) {

				// At this point we know that the package is present. Make sure
				// that the provided version meets the current requirement.

				if (version == null) {
					return pkg.version;
				}
				result = compareVersions(pkg.version, version, vsres);
				if ((vsres.satisfies && !exact) || (result == 0)) {
					return pkg.version;
				}
				throw new TclException(interp,
						"version conflict for package \"" + pkgName
								+ "\": have " + pkg.version + ", need "
								+ version);
			}
		}

		if (version != null) {
			throw new TclException(interp, "package " + pkgName + " " + version
					+ " is not present");
		} else {
			throw new TclException(interp, "package " + pkgName
					+ " is not present");
		}
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * cmdProc --
	 * 
	 * This procedure is invoked to process the "package" Tcl command. See the
	 * user documentation for details on what it does.
	 * 
	 * Side effects: |>None.<|
	 * 
	 * ----------------------------------------------------------------------
	 */

	public void cmdProc(Interp interp, // The current interpreter.
			TclObject[] objv) // Command arguments.
			throws TclException // Thrown if an error occurs.
	{
		VersionSatisfiesResult vsres;
		Package pkg;
		PkgAvail avail;
		PkgAvail prev;
		String version;
		String pkgName;
		String key;
		String cmd;
		String ver1, ver2;
		StringBuffer sbuf;
		Enumeration e;
		int i, opt, exact;
		boolean once;

		if (objv.length < 2) {
			throw new TclNumArgsException(interp, 1, objv,
					"option ?arg arg ...?");
		}
		opt = TclIndex.get(interp, objv[1], validCmds, "option", 0);
		switch (opt) {
		case OPT_FORGET: {
			// Forget takes 0 or more arguments.

			for (i = 2; i < objv.length; i++) {
				// We do not need to check to make sure
				// package name is "" because it would not
				// be in the hash table so name will be ignored.

				pkgName = objv[i].toString();
				pkg = (Package) interp.packageTable.get(pkgName);

				// If this package does not exist, go to next one.

				if (pkg == null) {
					continue;
				}
				interp.packageTable.remove(pkgName);
				while (pkg.avail != null) {
					avail = pkg.avail;
					pkg.avail = avail.next;
					avail = null;
				}
				pkg = null;
			}
			return;
		}
		case OPT_IFNEEDED: {
			if ((objv.length < 4) || (objv.length > 5)) {
				throw new TclNumArgsException(interp, 1, objv,
						"ifneeded package version ?script?");
			}
			pkgName = objv[2].toString();
			version = objv[3].toString();

			// Verify that this version string is valid.

			checkVersion(interp, version);
			if (objv.length == 4) {
				pkg = (Package) interp.packageTable.get(pkgName);
				if (pkg == null)
					return;

			} else {
				pkg = findPackage(interp, pkgName);
			}
			for (avail = pkg.avail, prev = null; avail != null; prev = avail, avail = avail.next) {
				if (compareVersions(avail.version, version, null) == 0) {
					if (objv.length == 4) {
						// If doing a query return current script.

						interp.setResult(avail.script);
						return;
					}

					// We matched so we must be setting the script.

					break;
				}
			}

			// When we do not match on a query return nothing.

			if (objv.length == 4) {
				return;
			}
			if (avail == null) {
				avail = new PkgAvail();
				avail.version = version;
				if (prev == null) {
					avail.next = pkg.avail;
					pkg.avail = avail;
				} else {
					avail.next = prev.next;
					prev.next = avail;
				}
			}
			avail.script = objv[4].toString();
			return;
		}
		case OPT_NAMES: {
			if (objv.length != 2) {
				throw new TclNumArgsException(interp, 1, objv, "names");
			}

			try {
				sbuf = new StringBuffer();
				once = false;
				for (Object o : interp.packageTable.entrySet()) {
					Map.Entry entry = (Map.Entry) o;
					key = (String) entry.getKey();
					pkg = (Package) entry.getValue();
					once = true;
					if ((pkg.version != null) || (pkg.avail != null)) {
						Util.appendElement(interp, sbuf, key);
					}
				}
				if (once) {
					interp.setResult(sbuf.toString());
				}
			} catch (TclException ex) {
				throw new TclRuntimeError("unexpected TclException: " + ex);
			}
			return;
		}
		case OPT_PRESENT: {
			if (objv.length < 3) {
				throw new TclNumArgsException(interp, 2, objv,
						"?-exact? package ?version?");
			}
			if (objv[2].toString().equals("-exact")) {
				exact = 1;
			} else {
				exact = 0;
			}

			version = null;
			if (objv.length == (4 + exact)) {
				version = objv[3 + exact].toString();
				checkVersion(interp, version);
			} else if ((objv.length != 3) || (exact == 1)) {
				throw new TclNumArgsException(interp, 2, objv,
						"?-exact? package ?version?");
			}
			if (exact == 1) {
				version = pkgPresent(interp, objv[3].toString(), version, true);
			} else {
				version = pkgPresent(interp, objv[2].toString(), version, false);
			}
			interp.setResult(version);
			break;
		}
		case OPT_PROVIDE: {
			if ((objv.length < 3) || (objv.length > 4)) {
				throw new TclNumArgsException(interp, 1, objv,
						"provide package ?version?");
			}
			if (objv.length == 3) {
				pkg = (Package) interp.packageTable.get(objv[2].toString());
				if (pkg != null) {
					if (pkg.version != null) {
						interp.setResult(pkg.version);
					}
				}
				return;
			}
			pkgProvide(interp, objv[2].toString(), objv[3].toString());
			return;
		}
		case OPT_REQUIRE: {
			if ((objv.length < 3) || (objv.length > 5)) {
				throw new TclNumArgsException(interp, 1, objv,
						"require ?-exact? package ?version?");
			}
			if (objv[2].toString().equals("-exact")) {
				exact = 1;
			} else {
				exact = 0;
			}
			version = null;
			if (objv.length == (4 + exact)) {
				version = objv[3 + exact].toString();
				checkVersion(interp, version);
			} else if ((objv.length != 3) || (exact == 1)) {
				throw new TclNumArgsException(interp, 1, objv,
						"require ?-exact? package ?version?");
			}
			if (exact == 1) {
				version = pkgRequire(interp, objv[3].toString(), version, true);
			} else {
				version = pkgRequire(interp, objv[2].toString(), version, false);
			}
			interp.setResult(version);
			return;
		}
		case OPT_UNKNOWN: {
			if (objv.length > 3) {
				throw new TclNumArgsException(interp, 1, objv,
						"unknown ?command?");
			}
			if (objv.length == 2) {
				if (interp.packageUnknown != null) {
					interp.setResult(interp.packageUnknown);
				}
			} else if (objv.length == 3) {
				interp.packageUnknown = null;
				cmd = objv[2].toString();
				if (cmd.length() > 0) {
					interp.packageUnknown = cmd;
				}
			}
			return;
		}
		case OPT_VCOMPARE: {
			if (objv.length != 4) {
				throw new TclNumArgsException(interp, 1, objv,
						"vcompare version1 version2");
			}
			ver1 = objv[2].toString();
			ver2 = objv[3].toString();
			checkVersion(interp, ver1);
			checkVersion(interp, ver2);
			interp.setResult(compareVersions(ver1, ver2, null));
			return;
		}
		case OPT_VERSIONS: {
			if (objv.length != 3) {
				throw new TclNumArgsException(interp, 1, objv,
						"versions package");
			}
			pkg = (Package) interp.packageTable.get(objv[2].toString());
			if (pkg != null) {
				try {
					sbuf = new StringBuffer();
					once = false;
					for (avail = pkg.avail; avail != null; avail = avail.next) {
						once = true;
						Util.appendElement(interp, sbuf, avail.version);
					}
					if (once) {
						interp.setResult(sbuf.toString());
					}
				} catch (TclException ex) {
					throw new TclRuntimeError("unexpected TclException: " + ex);
				}
			}
			return;
		}
		case OPT_VSATISFIES: {
			if (objv.length != 4) {
				throw new TclNumArgsException(interp, 1, objv,
						"vsatisfies version1 version2");
			}

			ver1 = objv[2].toString();
			ver2 = objv[3].toString();
			checkVersion(interp, ver1);
			checkVersion(interp, ver2);
			vsres = new VersionSatisfiesResult();
			compareVersions(ver1, ver2, vsres);
			interp.setResult(vsres.satisfies);
			return;
		}
		default: {
			throw new TclRuntimeError("TclIndex.get() error");
		}
		} // end switch(opt)
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * findPackage --
	 * 
	 * This procedure finds the Package record for a particular package in a
	 * particular interpreter, creating a record if one doesn't already exist.
	 * 
	 * Results: The return value is a ref to the Package record for the package.
	 * 
	 * Side effects: A new Package record may be created.
	 * 
	 * ----------------------------------------------------------------------
	 */

	private static Package findPackage(Interp interp, // Interpreter to use for
			// package lookup.
			String pkgName) // Name of package to find.
			throws TclException {
		Package pkg;

		// check package name to make sure it is not null or "".

		if (pkgName == null || pkgName.length() == 0) {
			throw new TclException(interp, "expected package name but got \"\"");
		}

		pkg = (Package) interp.packageTable.get(pkgName);
		if (pkg == null) {
			// We should add a package with this name.

			pkg = new Package();
			interp.packageTable.put(pkgName, pkg);
		}
		return pkg;
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * checkVersion --
	 * 
	 * This procedure checks to see whether a version number has valid syntax.
	 * 
	 * Results: If string is not properly formed version number then a
	 * TclException is raised.
	 * 
	 * Side effects: None.
	 * 
	 * ----------------------------------------------------------------------
	 */

	private static void checkVersion(Interp interp, // Used for error reporting.
			String version) // Supposedly a version number, which is
			// groups of decimal digits separated
			// by dots.
			throws TclException {
		int i, len;
		char c, prevChar;
		boolean error = true;

		try {
			if ((version == null) || (version.length() == 0)) {
				version = "";
				return;
			}
			if (!Character.isDigit(version.charAt(0))) {
				return;
			}
			len = version.length();
			for (prevChar = version.charAt(0), i = 1; i < len; i++) {
				c = version.charAt(i);
				if (!Character.isDigit(c) && ((c != '.') || (prevChar == '.'))) {
					return;
				}
				prevChar = c;
			}
			if (prevChar != '.') {
				error = false;
				return;
			}
		} finally {
			if (error) {
				throw new TclException(interp,
						"expected version number but got \"" + version + "\"");
			}
		}
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * compareVersions --
	 * 
	 * This procedure compares two version numbers.
	 * 
	 * Results: This function will return a -1 if v1 is less than v2, 0 if the
	 * two version numbers are the same, and 1 if v1 is greater than v2. If the
	 * sat argument is not null then then its VersionSatisfiesResult.satisifes
	 * field will be true if v2 >= v1 and both numbers have the same major
	 * number or false otherwise.
	 * 
	 * Side effects: None.
	 * 
	 * ----------------------------------------------------------------------
	 */

	private static int compareVersions(String v1, // Versions strings. (e.g.
			// 2.1.3)
			String v2, VersionSatisfiesResult vsres)

	{
		int i;
		int max;
		int n1 = 0;
		int n2 = 0;
		boolean thisIsMajor = true;
		String[] v1ns;
		String[] v2ns;

		// Each iteration of the following loop processes one number from
		// each string, terminated by a ".". If those numbers don't match
		// then the comparison is over; otherwise, we loop back for the
		// next number.

		// This should never happen because null strings would not
		// have gotten past the version verify.

		if ((v1 == null) || (v2 == null)) {
			throw new TclRuntimeError("null version in package version compare");
		}
		v1ns = split(v1, '.');
		v2ns = split(v2, '.');

		// We are sure there is at least one string in each array so
		// this should never happen.

		if (v1ns.length == 0 || v2ns.length == 0) {
			throw new TclRuntimeError("version length is 0");
		}
		if (v1ns.length > v2ns.length) {
			max = v1ns.length;
		} else {
			max = v2ns.length;
		}

		for (i = 0; i < max; i++) {
			n1 = n2 = 0;

			// Grab number from each version ident if version spec
			// ends the use a 0 as value.

			try {
				if (i < v1ns.length) {
					n1 = Integer.parseInt(v1ns[i]);
				}
				if (i < v2ns.length) {
					n2 = Integer.parseInt(v2ns[i]);
				}
			} catch (NumberFormatException ex) {
				throw new TclRuntimeError(
						"NumberFormatException for package versions \"" + v1
								+ "\" or \"" + v2 + "\"");
			}

			// Compare and go on to the next version number if the
			// current numbers match.

			if (n1 != n2) {
				break;
			}
			thisIsMajor = false;
		}
		if (vsres != null) {
			vsres.satisfies = ((n1 == n2) || ((n1 > n2) && !thisIsMajor));
		}
		if (n1 > n2) {
			return 1;
		} else if (n1 == n2) {
			return 0;
		} else {
			return -1;
		}
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * split --
	 * 
	 * Util function used in version compare to split a string on a single char
	 * it is only used in the version compare function.
	 * 
	 * Results: |>None.<|
	 * 
	 * Side effects: |>None.<|
	 * 
	 * ----------------------------------------------------------------------
	 */

	static String[] split(String in, char splitchar) {
		ArrayList words;
		int i;
		int len;
		char[] str;
		int wordstart = 0;

		// Create an array that is as big as the input
		// str plus one for an extra split char.

		len = in.length();
		str = new char[len + 1];
		in.getChars(0, len, str, 0);
		str[len++] = splitchar;
		words = new ArrayList(5);

		for (i = 0; i < len; i++) {

			// Compare this char to the split char
			// if they are the same the we need to
			// add the last word to the array.

			if (str[i] == splitchar) {
				if (wordstart <= (i - 1)) {
					words.add(new String(str, wordstart, i - wordstart));
				}
				wordstart = (i + 1);
			}
		}

		// Create an array that is as big as the number
		// of elements in the vector, copy over and return.

		String[] ret = { (String) null };
		ret = (String[]) words.toArray(ret);
		return ret;
	}

	// If compare versions is called with a third argument then one of
	// these structures needs to be created and passed in

	static class VersionSatisfiesResult {
		boolean satisfies = false;
	}

	// Each invocation of the "package ifneeded" command creates a class
	// of the following type, which is used to load the package into the
	// interpreter if it is requested with a "package require" command.

	static class PkgAvail {
		String version = null; // Version string.
		String script = null; // Script to invoke to provide this package
		// version
		PkgAvail next = null; // Next in list of available package versions
	}

	// For each package that is known in any way to an interpreter, there
	// is one record of the following type. These records are stored in
	// the "packageTable" hash table in the interpreter, keyed by
	// package name such as "Tk" (no version number).

	static class Package {
		String version = null; // Version that has been supplied in this
		// interpreter via "package provide"
		// null means the package doesn't
		// exist in this interpreter yet.

		PkgAvail avail = null; // First in list of all available package
		// versions
	}

} // end of class PackageCmd
