/*
 * LsetCmd.java
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 * Copyright (c) 1998-1999 by Scriptics Corporation.
 * Copyright (c) 2000 Christian Krone.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: LsetCmd.java,v 1.5 2009/07/08 22:51:04 rszulgo Exp $
 *
 */

package tcl.lang.cmd;

import java.util.ArrayList;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TCL;
import tcl.lang.TclException;
import tcl.lang.TclList;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;
import tcl.lang.TclString;
import tcl.lang.Util;
import tcl.lang.Var;

/**
 * This class implements the built-in "lset" command in Tcl 8.4.19
 */

public class LsetCmd implements Command {

	/**
	 * This procedure is invoked to process the "lset" Tcl command. See the user
	 * documentation for details on what it does.
	 * 
	 * Results: None.
	 * 
	 * Side effects: See the user documentation.
	 * 
	 * @see tcl.lang.Command#cmdProc(tcl.lang.Interp, tcl.lang.TclObject[])
	 */

	public void cmdProc(Interp interp, TclObject[] objv) throws TclException {

		TclObject list; // the list being altered.
		TclObject finalValue; // Value finally assigned to the variable.

		// Check parameter count.
		if (objv.length < 3) {
			throw new TclNumArgsException(interp, 1, objv, "listVar index ?index...? value");
		}

		// Look up the list variable's value.
		list = Var.getVar(interp, objv[1].toString(), null, TCL.LEAVE_ERR_MSG);
		if (list == null) {
			throw new TclException(interp, "Cannot find variable name " + objv[1].toString());
		}

		// Substitute the value in the value. Return either the value or else an
		// unshared copy of it.

		if (objv.length == 4) {
			finalValue = list(interp, list, objv[2], objv[3]);
		} else {
			TclObject[] indices = new TclObject[objv.length - 3];
			// check indices count
			if (objv.length > 4) {
				// many indices
				for (int i = 0; i < objv.length - 3; i++) {
					indices[i] = objv[i + 2];
				}
			}
			finalValue = flat(interp, list, objv.length - 3, indices, objv[objv.length - 1]);
		}

		// If substitution has failed, bail out.

		if (finalValue == null) {
			throw new TclException(interp, "Substitution has failed!");
		}

		// Finally, update the variable so that traces fire.

		list = Var.setVar(interp, objv[1].toString(), (String) null, finalValue, TCL.LEAVE_ERR_MSG);

		finalValue.release();
		if (list == null) {
			throw new TclException(interp, "Cannot update variable");
		}

		// Return the new value of the variable as the interpreter result.

		interp.setResult(list);
	}

	/**
	 * flat (TclLsetFlat) --
	 * 
	 * Core of the 'lset' command when objc>=5. Objv[2], ... , objv[objc-2]
	 * contain scalar indices.
	 * 
	 * Side effects: Surgery is performed on the list value to produce the
	 * result.
	 * 
	 * On entry, the reference count of the variable value does not reflect any
	 * references held on the stack. The first action of this function is to
	 * determine whether the object is shared, and to duplicate it if it is. The
	 * reference count of the duplicate is incremented. At this point, the
	 * reference count will be 1 for either case, so that the object will appear
	 * to be unshared.
	 * 
	 * If an error occurs, and the object has been duplicated, the reference
	 * count on the duplicate is decremented so that it is now 0: this dismisses
	 * any memory that was allocated by this procedure.
	 * 
	 * If no error occurs, the reference count of the original object is
	 * incremented if the object has not been duplicated, and nothing is done to
	 * a reference count of the duplicate. Now the reference count of an
	 * unduplicated object is 2 (the returned object, plus the one stored in the
	 * variable). The reference count of a duplicate object is 1, reflecting
	 * that the returned object is the only active reference. The caller is
	 * expected to store the returned value back in the variable and decrement
	 * its reference count.
	 * 
	 * list (Tcl_LsetList) and related methods maintain a linked list of
	 * TclObject's whose string representations must be spoilt by threading via
	 * 'ptr2' On entry to list (Tcl_LsetList), the values of 'ptr2' are
	 * immaterial; on exit, the 'ptr2' is set to null.
	 * 
	 * @param interp
	 *            Tcl interpreter
	 * @param list
	 *            The list being modified
	 * @param indexCount
	 *            Number of index args
	 * @param indexArray
	 *            Index args
	 * @param value
	 *            Value arg to 'lset'
	 * 
	 * @return Returns the new value of the list variable, or NULL if an error
	 *         occurs.
	 * 
	 * @see Tcl 8.4.19
	 * 
	 * @author rszulgo
	 * @since 2009-07-07
	 */

	private static final TclObject flat(Interp interp, TclObject list, int indexCount, TclObject[] indexArray,
			TclObject value) throws TclException {

		int duplicated; 		// Flag == 1 if the obj has been duplicated, 0 otherwise 
		TclObject retValue; 	// The list to be returned 
		int elemCount; 			// Length of one sublist being changed 
		ArrayList elems; 		// The elements of a sublist 
		TclObject subList; 		// The current sublist 
		int result = 0; 		// Status return from library calls 
		int index = 0; 			// Index of the element to replace in the current sublist
		ArrayList chainList = new ArrayList();		// check track of sub-lists, in order to invalidate string reps

		// If there are no indices, then simply return the new value
		if (indexCount == 0) {
			value.preserve();
			return value;
		}

		// If the list is shared, make a private copy.
		if (list.isShared()) {
			duplicated = 1;
			list = list.duplicate();
			list.preserve();
		} else {
			duplicated = 0;
		}

		// Anchor the linked list of TclObject's whose string reps must be
		// invalidated if the operation succeeds.
		retValue = list;

		// Handle each index arg by diving into the appropriate sublist
		for (int i = 0;; ++i) {
			// Take the sublist apart.
			try {
				elems = TclList.getElementsList(interp, list);
				elemCount = elems.size();
			} catch (TclException e) {
				// If we can't get elements of list, we break the loop and then
				// prepare for return from method.
				break;
			}


			// Determine the index of the requested element.
			index = Util.getIntForIndex(interp, indexArray[i], (elemCount - 1));

			// Check that the index is in range.
			if (index < 0 || index >= elemCount) {
				interp.setResult(TclString.newInstance("list index out of range"));
				throw new TclException(interp, "list index out of range");
			}

			// Break the loop after extracting the innermost sublist
			if (i >= indexCount - 1) {
				result = TCL.OK;
				break;
			}

			// Extract the appropriate sublist, and make sure that it is
			// unshared.
			subList = (TclObject) elems.get(index);
			if (subList.isShared()) {
				subList = subList.duplicate();
				try {
					TclList.setElement(interp, list, index, subList);
				} catch (TclException e) {
					/*
					 * We actually shouldn't be able to get here. If we do, it
					 * would result in leaking subList, but everything's been
					 * validated already; the error exit from TclList.setElement
					 * should never happen.
					 */
					break;
				}
			}

			// Chain the current sublist onto the linked list of TclObject's
			// whose string reps must be spoilt.
			chainList.add(list);
			list = subList;

		}

		// Store the result in the list element

		if (result == TCL.OK) {
			try {
				TclList.lsetElement(interp, list, index, value);

				// Spoil all the string reps

				int chainListLen = chainList.size();
				for (Object aChainList : chainList) {
					TclObject tobj = (TclObject) aChainList;
					tobj.invalidateStringRep();
				}
				list.invalidateStringRep();

				// Return the new list if everything worked.

				if (duplicated == 0) {
					retValue.preserve();
				}

				return retValue;
				
			} catch (TclException e) {
				// Clean up the one dangling reference, re-throw exception
				if (duplicated != 0) {
					retValue.release();
				}
				throw e;
			}
		}

		// Clean up the one dangling reference otherwise

		if (duplicated != 0) {
			retValue.release();
		}

		return null;
	}

	/**
	 * list TclLsetList --
	 * 
	 * Core of the 'lset' command when objc == 4. Objv[2] may be either a scalar
	 * index or a list of indices.
	 * 
	 * Side effects: Surgery is performed on the list value to produce the
	 * result.
	 * 
	 * On entry, the reference count of the variable value does not reflect any
	 * references held on the stack. The first action of this function is to
	 * determine whether the object is shared, and to duplicate it if it is. The
	 * reference count of the duplicate is incremented. At this point, the
	 * reference count will be 1 for either case, so that the object will appear
	 * to be unshared.
	 * 
	 * If an error occurs, and the object has been duplicated, the reference
	 * count on the duplicate is decremented so that it is now 0: this dismisses
	 * any memory that was allocated by this procedure.
	 * 
	 * If no error occurs, the reference count of the original object is
	 * incremented if the object has not been duplicated, and nothing is done to
	 * a reference count of the duplicate. Now the reference count of an
	 * unduplicated object is 2 (the returned object, plus the one stored in the
	 * variable). The reference count of a duplicate object is 1, reflecting
	 * that the returned object is the only active reference. The caller is
	 * expected to store the returned value back in the variable and decrement
	 * its reference count.
	 * 
	 * flat method and related methods maintain a linked list of TclObject's
	 * whose string representations must be spoilt by threading via 'ptr2'. On
	 * entry to list method, the values of 'ptr2' are immaterial; on exit, the
	 * 'ptr2' is set to null.
	 * 
	 * @param interp
	 *            Tcl interpreter
	 * @param list
	 *            the list being modified
	 * @param indexArg
	 *            Index or index-list arg to 'lset'
	 * @param value
	 *            Value arg to 'lset'
	 * 
	 * @return Returns the new value of the list variable, or NULL if an error
	 *         occurs.
	 * 
	 * @see Tcl 8.4.19
	 * 
	 * @author rszulgo
	 * @since 2009-07-07
	 */
	private static final TclObject list(Interp interp, TclObject list, TclObject indexArg, TclObject value)
			throws TclException {

		int indexCount; 		// Number of indices in the index list 
		TclObject[] indices;	// Vector of indices in the index list 
		int duplicated; 		// Flag == 1 if the obj has been duplicated, 0 otherwise 
		TclObject retValue; 	// The list to be returned 
		int index = 0; 			// Current index in the list - discarded 
		int result = 0; 		// Status return from library calls 
		TclObject subList; 		// The current sublist 
		int elemCount; 			// Count of elements in the current sublist 
		ArrayList elems; 		// Elements of current sublist 
		ArrayList chainList = new ArrayList();		// check track of sub-lists, in order to invalidate string reps

		// Determine whether the index arg designates a list or a single index.
		// We have to be careful about the order of the checks to avoid repeated
		// shimmering; see TIP #22 and #23 for details.
		
		if (!indexArg.isListType()) {
			try {
				index = Util.getIntForIndex(null, indexArg, 0);
				return flat(interp, list, 1, TclList.getElements(interp, indexArg), value);
			} catch (TclException e) {
				try {
					indices = TclList.getElements(null, indexArg);
					indexCount = indices.length;
				} catch (TclException ex) {
					// indexArgPtr designates something that is neither an index
					// nor a well formed list. Report the error via exception
					// throw from Util.getIntForIndex
					interp.setResult(TclString.newInstance(e.getMessage()));
					throw e;
				}
			}
			
		} else {
			
			// indexArg designates a single index.
			try {
				indices = TclList.getElements(null, indexArg);
				indexCount = indices.length;
			} catch (TclException e) {
				// indexArgPtr designates something that is neither an index nor
				// a well formed list. Report the error via TclLsetFlat.
				return flat(interp, list, 1, TclList.getElements(interp, indexArg), value);
			}
		}

		// At this point, we know that arg designates a well formed list, and
		// the 'else if' above has parsed it into indexCount and indices. If
		// there are no indices, simply return 'valuePtr'

		if (indexCount == 0) {
			value.preserve();
			return value;
		}

		// Duplicate the list arg if necessary.
		if (list.isShared()) {
			duplicated = 1;
			list = list.duplicate();
			list.preserve();
		} else {
			duplicated = 0;
		}

		/*
		 * It would be tempting simply to go off to TclLsetFlat to finish the
		 * processing. Alas, it is also incorrect! The problem is that
		 * 'indexArgPtr' may designate a sublist of 'listPtr' whose value is to
		 * be manipulated. The fact that 'listPtr' is itself unshared does not
		 * guarantee that no sublist is. Therefore, it's necessary to replicate
		 * all the work here, expanding the index list on each trip through the
		 * loop.
		 * 
		 * Anchor the linked list of Tcl_Obj's whose string reps must be
		 * invalidated if the operation succeeds.
		 */

		retValue = list;

		// Handle each index arg by diving into the appropriate sublist
		for (int i = 0;; ++i) {

			// Take the sublist apart.
			try {
				elems = TclList.getElementsList(interp, list);
				elemCount = elems.size();
			} catch (TclException e) {
				// If we can't get elements of list, we break the loop and then
				// prepare for return from method.
				break;
			}

			// Reconstitute the index array
			try {
				indices = TclList.getElements(interp, indexArg);
				indexCount = indices.length;
			} catch (TclException e) {
				// Shouldn't be able to get here, because we already parsed the
				// thing successfully once.
				break;
			}

			// Determine the index of the requested element.

			index = Util.getIntForIndex(interp, indices[i], (elemCount - 1));

			// Check that the index is in range.
			if (index < 0 || index >= elemCount) {
				interp.setResult(TclString.newInstance("list index out of range"));
				throw new TclException(interp, "list index out of range");
			}

			// Break the loop after extracting the innermost sublist
			if (i >= indexCount - 1) {
				result = TCL.OK;
				break;
			}

			// Extract the appropriate sublist, and make sure that it is
			// unshared.
			subList = (TclObject) elems.get(index);
			if (subList.isShared()) {
				subList = subList.duplicate();
				try {
					TclList.setElement(interp, list, index, subList);
				} catch (TclException e) {
					/*
					 * We actually shouldn't be able to get here, because we've
					 * already checked everything that TclList.setElement
					 * checks. If we were to get here, it would result in
					 * leaking subList.
					 */
					break;
				}
			}

			// Chain the current sublist onto the linked list of TclObject's
			// whose string reps must be spoilt.
			chainList.add(list);
			list = subList;
		}

		// Store the new element into the correct slot in the innermost sublist.
		if (result == TCL.OK) {
			try {
				TclList.lsetElement(interp, list, index, value);

				// Spoil all the string reps
				
				int chainListLen = chainList.size();
				for (Object aChainList : chainList) {
					TclObject tobj = (TclObject) aChainList;
					tobj.invalidateStringRep();
				}
				list.invalidateStringRep();

				// Return the new list if everything worked.

				if (duplicated == 0) {
					retValue.preserve();
				}

				return retValue;

			} catch (TclException e) {
				// Clean up the one dangling reference, re-throw exception
				if (duplicated != 0) {
					retValue.release();
				}
				throw e;
			}
		}

		// Clean up the one dangling reference otherwise

		if (duplicated != 0) {
			retValue.release();
		}

		return null;
	}
}