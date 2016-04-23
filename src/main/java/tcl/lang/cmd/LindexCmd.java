/*
 * LindexCmd.java - -
 *
 *	Implements the built-in "lindex" Tcl command.
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: LindexCmd.java,v 1.5 2006/06/08 07:44:51 mdejong Exp $
 *
 */

package tcl.lang.cmd;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TclException;
import tcl.lang.TclList;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;
import tcl.lang.TclRuntimeError;
import tcl.lang.Util;

/*
 * This class implements the built-in "lindex" command in Tcl.
 */

public class LindexCmd implements Command {

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * cmdProc --
	 * 
	 * This procedure is invoked as part of the Command interface to process the
	 * "lindex" Tcl command. See the user documentation for details on what it
	 * does.
	 * 
	 * Results: None.
	 * 
	 * Side effects: See the user documentation.
	 * 
	 * ----------------------------------------------------------------------
	 */

	public void cmdProc(Interp interp, // Current interpreter.
			TclObject[] objv) // Argument objects.
			throws TclException // A standard Tcl exception.
	{
		TclObject elem; // The element being extracted

		if (objv.length < 2) {
			throw new TclNumArgsException(interp, 1, objv, "list ?index...?");
		}

		// If objv.length == 3, then objv[2] may be either a single index or
		// a list of indices: go to TclLindexList to determine which.
		// If objv.length >= 4, or objv.length == 2, then objv[2 ..
		// objv.length-2 ] are all
		// single indices and processed as such in TclLindexFlat.

		if (objv.length == 3) {
			elem = TclLindexList(interp, objv[1], objv, 2);
		} else {
			elem = TclLindexFlat(interp, objv[1], objv.length - 2, objv, 2);
		}

		// Set the interpreter's object result to the last element extracted

		if (elem == null) {
			throw new TclRuntimeError("unexpected null result");
		} else {
			interp.setResult(elem);
			elem.release();
		}
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * TclLindexList --
	 * 
	 * This procedure handles the 'lindex' command when objc==3.
	 * 
	 * Results: Returns a pointer to the object extracted, or null if an error
	 * occurred.
	 * 
	 * Side effects: None.
	 * 
	 * If objv[1] can be parsed as a list, TclLindexList handles extraction of
	 * the desired element locally. Otherwise, it invokes TclLindexFlat to treat
	 * objv[1] as a scalar.
	 * 
	 * The reference count of the returned object includes one reference
	 * corresponding to the pointer returned. Thus, the calling code will
	 * usually do something like: Tcl_SetObjResult( interp, result );
	 * Tcl_DecrRefCount( result );
	 * 
	 * ----------------------------------------------------------------------
	 */

	public static TclObject TclLindexList(Interp interp, // Tcl interpreter
			TclObject list, // List being unpacked
			TclObject[] objv, // Array containing arg object
			int argIndex) // Index of arg object in objv
			throws TclException {
		int listLen; // Length of the list being manipulated.
		int index; // Index into the list
		int i; // Current index number
		TclObject[] indices; // Array of list indices
		TclObject oldList; // Temp location to preserve the list
		// pointer when replacing it with a sublist

		// Determine whether arg designates a list or a single index.
		// We have to be careful about the order of the checks to avoid
		// repeated shimmering; see TIP#22 and TIP#33 for the details.

		TclObject arg = objv[argIndex];
		boolean isListType = arg.isListType();
		boolean isValidIndex = false;

		if (!isListType) {
			try {
				index = Util.getIntForIndex(null, arg, 0);
				isValidIndex = true;
			} catch (TclException ex) {
			}
		}
		if (!isListType && isValidIndex) {
			// arg designates a single index.
			return TclLindexFlat(interp, list, 1, objv, argIndex);
		}

		indices = null;
		try {
			indices = TclList.getElements(null, arg);
		} catch (TclException ex) {
		}

		if (indices == null) {
			// arg designates something that is neither an index nor a
			// well-formed list. Report the error via TclLindexFlat.

			return TclLindexFlat(interp, list, 1, objv, argIndex);
		}

		// Record the reference to the list that we are maintaining in
		// the activation record.

		list.preserve();

		// arg designates a list, and the code above has parsed it
		// into indices.

		for (i = 0; i < indices.length; i++) {
			// Convert the current list/sublist to a list rep if necessary.
			try {
				listLen = TclList.getLength(interp, list);
			} catch (TclException te) {
				list.release();
				throw te;
			}

			// Get the index from indices[i]

			try {
				index = Util.getIntForIndex(interp, indices[i], listLen - 1);
			} catch (TclException te) {
				list.release();
				throw te;
			}

			if (index < 0 || index >= listLen) {
				// Index is out of range, return empty string result
				list.release();
				list = interp.checkCommonString(null);
				list.preserve();
				return list;
			}

			// Get list element at index.

			oldList = list;
			list = TclList.index(interp, oldList, index);
			list.preserve();
			oldList.release();
		}

		// Return the last object extracted. Its reference count will include
		// the reference being returned.

		return list;
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * TclLindexFlat --
	 * 
	 * This procedure handles the 'lindex' command, given that the arguments to
	 * the command are known to be a flat list.
	 * 
	 * Results: Returns a standard Tcl result.
	 * 
	 * Side effects: None.
	 * 
	 * This procedure is called from either tclExecute.c or Tcl_LindexObjCmd
	 * whenever either is presented with objc == 2 or objc >= 4. It is also
	 * called from TclLindexList for the objc==3 case once it is determined that
	 * objv[2] cannot be parsed as a list.
	 * 
	 * ----------------------------------------------------------------------
	 */

	static TclObject TclLindexFlat(Interp interp, // Tcl interpreter
			TclObject list, // Tcl object representing the list
			int indexCount, // Count of indices
			TclObject[] indexArray,
			// Array of Tcl objects
			// representing the indices in the
			// list
			int indexArrayOffset) // Offset from start of indexArray
			throws TclException {
		int i; // Current list index
		int listLen; // Length of the current list being
		// processed
		int index; // Parsed version of the current element
		// of indexArray
		TclObject oldList; // Temporary to hold list so that
		// its ref count can be decremented.

		// Record the reference to the 'list' object that we are
		// maintaining in the activation record.

		list.preserve();

		final int endIndex = indexArrayOffset + indexCount;
		for (i = indexArrayOffset; i < endIndex; i++) {
			// Convert the current list/sublist to a list rep if necessary.
			try {
				listLen = TclList.getLength(interp, list);
			} catch (TclException te) {
				list.release();
				throw te;
			}

			// Get the index from objv[i]

			try {
				index = Util.getIntForIndex(interp, indexArray[i], listLen - 1);
			} catch (TclException te) {
				list.release();
				throw te;
			}

			if (index < 0 || index >= listLen) {
				// Index is out of range, return empty string result
				list.release();
				list = interp.checkCommonString(null);
				list.preserve();
				return list;
			}

			// Make sure list still refers to a list object.
			// It might have been converted to something else above
			// if objv[1] overlaps with one of the other parameters.

			try {
				listLen = TclList.getLength(interp, list);
			} catch (TclException te) {
				list.release();
				throw te;
			}

			// Get list element at index.

			oldList = list;
			list = TclList.index(interp, oldList, index);
			list.preserve();
			oldList.release();
		}

		return list;
	}

} // end LindexCmd class

