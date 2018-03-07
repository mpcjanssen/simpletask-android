/*
 * TclList.java
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: TclList.java,v 1.16 2009/07/08 14:12:35 rszulgo Exp $
 *
 */

package tcl.lang;

import java.util.ArrayList;

/**
 * This class implements the list object type in Tcl.
 */
public class TclList implements InternalRep {

	/**
	 * Internal representation of a list value.
	 */
	private ArrayList<TclObject> alist;

	/**
	 * Create a new empty Tcl List.
	 */
	private TclList() {
		alist = new ArrayList<>();

		if (TclObject.saveObjRecords) {
			String key = "TclList";
			Integer num = (Integer) TclObject.objRecordMap.get(key);
			if (num == null) {
				num = 1;
			} else {
				num = num.intValue() + 1;
			}
			TclObject.objRecordMap.put(key, num);
		}
	}

	/**
	 * Create a new empty Tcl List, with the list pre-allocated to the given
	 * size.
	 * 
	 * @param size
	 *            the number of slots pre-allocated in the alist.
	 */
	private TclList(int size) {
		alist = new ArrayList<>(size);

		if (TclObject.saveObjRecords) {
			String key = "TclList";
			Integer num = (Integer) TclObject.objRecordMap.get(key);
			if (num == null) {
				num = 1;
			} else {
				num = num.intValue() + 1;
			}
			TclObject.objRecordMap.put(key, num);
		}
	}

	/**
	 * Called to free any storage for the type's internal rep.
	 */
	public void dispose() {
		final int size = alist.size();
		for (TclObject anAlist : alist) {
			anAlist.release();
		}
		alist.clear();
	}

	/**
	 * DupListInternalRep -> duplicate
	 * 
	 * Returns a dupilcate of the current object.
	 * 
	 */
	public InternalRep duplicate() {
		int size = alist.size();
		TclList newList = new TclList(size);

		for (int i = 0; i < size; i++) {
			TclObject tobj = (TclObject) alist.get(i);
			tobj.preserve();
			newList.alist.add(tobj);
		}

		if (TclObject.saveObjRecords) {
			String key = "TclList.duplicate()";
			Integer num = (Integer) TclObject.objRecordMap.get(key);
			if (num == null) {
				num = 1;
			} else {
				num = num.intValue() + 1;
			}
			TclObject.objRecordMap.put(key, num);
		}

		return newList;
	}

	/**
	 * Called to query the string representation of the Tcl object. This method
	 * is called only by TclObject.toString() when TclObject.stringRep is null.
	 * 
	 * @return the string representation of the Tcl object.
	 */
	public String toString() {
		final int size = alist.size();
		if (size == 0) {
			return "";
		}
		int est = size * 4;

		StringBuffer sbuf = new StringBuffer((est > 64) ? est : 64);
		try {
			for (TclObject elm : alist) {
				if (elm != null) {
					Util.appendElement(null, sbuf, elm.toString());
				} else {
					Util.appendElement(null, sbuf, "");
				}
			}
		} catch (TclException e) {
			throw new TclRuntimeError("unexpected TclException: " + e);
		}

		return sbuf.toString();
	}

	/**
	 * Creates a new instance of a TclObject with a TclList internal rep.
	 * 
	 * @return the TclObject with the given list value.
	 */

	public static TclObject newInstance() {
		return new TclObject(new TclList());
	}

	/**
	 * copy (TclListObjCopy) --
	 * 
	 * Makes a "pure list" copy of a list value. This provides for the C level a
	 * counterpart of the [lrange $list 0 end] command, while using internals
	 * details to be as efficient as possible.
	 * 
	 * Side effects: None.
	 * 
	 * @param interp
	 *            Used to report errors if not null.
	 * @param list
	 *            List object for which an element array is to be returned.
	 * 
	 * @return Normally returns a new TclObject, that contains the same list
	 *         value as 'list' does. The returned TclObject has a refCount of
	 *         zero. If 'list' does not hold a list, null is returned, and if
	 *         'interp' is non-null, an error message is recorded there.
	 */

	public static TclObject copy(Interp interp, TclObject list) {
		TclObject copy;

		if (!list.isListType()) {
			try {
				setListFromAny(interp, list);
			} catch (TclException e) {
				return null;
			}
		}

		copy = newInstance();
		copy.invalidateStringRep();
		copy = list.duplicate();
		return copy;
	}

	/**
	 * Called to convert the other object's internal rep to list.
	 * 
	 * @param interp
	 *            current interpreter.
	 * @param tobj
	 *            the TclObject to convert to use the List internal rep.
	 * @exception TclException
	 *                if the object doesn't contain a valid list.
	 */
	private static void setListFromAny(Interp interp, TclObject tobj)
			throws TclException {
		TclList tlist = new TclList();
		splitList(interp, tlist.alist, tobj.toString());
		tobj.setInternalRep(tlist);

		if (TclObject.saveObjRecords) {
			String key = "TclString -> TclList";
			Integer num = (Integer) TclObject.objRecordMap.get(key);
			if (num == null) {
				num = 1;
			} else {
				num = num.intValue() + 1;
			}
			TclObject.objRecordMap.put(key, num);
		}
	}

	/**
	 * Splits a list (in string rep) up into its constituent fields.
	 * 
	 * @param interp
	 *            current interpreter.
	 * @param alist
	 *            store the list elements in this ArraryList.
	 * @param s
	 *            the string to convert into a list.
	 * @exception TclException
	 *                if the object doesn't contain a valid list.
	 */
	private static final void splitList(Interp interp, ArrayList<TclObject> alist, String s)
			throws TclException {
		int len = s.length();
		int i = 0;
		FindElemResult res = new FindElemResult();

		while (i < len) {
			if (!Util.findElement(interp, s, i, len, res)) {
				break;
			} else {
				TclObject tobj = TclString.newInstance(res.elem);
				tobj.preserve();
				alist.add(tobj);
			}
			i = res.elemEnd;
		}
	}

	/**
	 * Tcl_ListObjAppendElement -> TclList.append()
	 * 
	 * Appends a TclObject element to a list object.
	 * 
	 * @param interp
	 *            current interpreter.
	 * @param tobj
	 *            the TclObject to append an element to.
	 * @param elemObj
	 *            the element to append to the object.
	 * @exception TclException
	 *                if tobj cannot be converted into a list.
	 */
	public static final void append(Interp interp, TclObject tobj,
			TclObject elemObj) throws TclException {
		if (tobj.isShared()) {
			throw new TclRuntimeError(
					"TclList.append() called with shared object");
		}
		if (!tobj.isListType()) {
			setListFromAny(interp, tobj);
		}
		tobj.invalidateStringRep();

		elemObj.preserve();
		((TclList) tobj.getInternalRep()).alist.add(elemObj);
	}

	/**
	 * TclList.append()
	 * 
	 * Appends multiple TclObject elements to a list object.
	 * 
	 * @param interp
	 *            current interpreter.
	 * @param tobj
	 *            the TclObject to append elements to.
	 * @param objv
	 *            array containing elements to append.
	 * @param startIdx
	 *            index to start appending values from
	 * @param endIdx
	 *            index to stop appending values at
	 * @exception TclException
	 *                if tobj cannot be converted into a list.
	 */
	public static final void append(Interp interp, TclObject tobj,
			TclObject[] objv, final int startIdx, final int endIdx)
			throws TclException {
		if (tobj.isShared()) {
			throw new TclRuntimeError(
					"TclList.append() called with shared object");
		}
		if (!tobj.isListType()) {
			setListFromAny(interp, tobj);
		}
		tobj.invalidateStringRep();

		ArrayList<TclObject> alist = ((TclList) tobj.getInternalRep()).alist;

		for (int i = startIdx; i < endIdx; i++) {
			TclObject elemObj = objv[i];
			elemObj.preserve();
			alist.add(elemObj);
		}
	}

	/**
	 * Queries the length of the list. If tobj is not a list object, an attempt
	 * will be made to convert it to a list.
	 * 
	 * @param interp
	 *            current interpreter.
	 * @param tobj
	 *            the TclObject to use as a list.
	 * @return the length of the list.
	 * @exception TclException
	 *                if tobj is not a valid list.
	 */
	public static final int getLength(Interp interp, TclObject tobj)
			throws TclException {
		if (!tobj.isListType()) {
			setListFromAny(interp, tobj);
		}

		TclList tlist = (TclList) tobj.getInternalRep();
		return tlist.alist.size();
	}
	
	/**
	 * Returns a TclObject array of the elements in a list object. If tobj is
	 * not a list object, an attempt will be made to convert it to a list.
	 * <p>
	 * 
	 * The objects referenced by the returned array should be treated as
	 * readonly and their ref counts are _not_ incremented; the caller must do
	 * that if it holds on to a reference.
	 * 
	 * @param interp
	 *            the current interpreter.
	 * @param tobj
	 *            the list to sort.
	 * @return a TclObject array of the elements in a list object.
	 * @exception TclException
	 *                if tobj is not a valid list.
	 */
	public static TclObject[] getElements(Interp interp, TclObject tobj)
	throws TclException {
		if (!tobj.isListType()) {
			setListFromAny(interp, tobj);
		}
		TclList tlist = (TclList) tobj.getInternalRep();
		
		int size = tlist.alist.size();
		TclObject objArray[] = new TclObject[size];
		for (int i = 0; i < size; i++) {
			objArray[i] = (TclObject) tlist.alist.get(i);
		}
		return objArray;
	}

	/**
	 * Returns an ArrayList of TclObject  elements in a list object. If tobj is
	 * not a list object, an attempt will be made to convert it to a list.
	 * <p>
	 * 
	 * Note that the actual internal ArrayList of a TclList object is returned,
	 * so be careful when changing the list.
	 * 
	 * @param interp
	 *            the current interpreter.
	 * @param tobj
	 *            the list to sort.
	 * @return an ArrayList of elements in a list object.
	 * @exception TclException
	 *                if tobj is not a valid list.
	 */
	public static ArrayList getElementsList(Interp interp, TclObject tobj)
			throws TclException {
		if (!tobj.isListType()) {
			setListFromAny(interp, tobj);
		}
		TclList tlist = (TclList) tobj.getInternalRep();

		return tlist.alist;
	}
	
	/**
	 * TclListObjSetElement --
	 * 
	 * Set a single element of a list to a specified value
	 * 
	 * Side effects: Tcl_Panic if listPtr designates a shared object. Otherwise,
	 * attempts to convert it to a list with a non-shared internal rep.
	 * Decrements the ref count of the object at the specified index within the
	 * list, replaces with the object designated by valuePtr, and increments the
	 * ref count of the replacement object.
	 * 
	 * It is the caller's responsibility to invalidate the string representation
	 * of the object.
	 * 
	 * @param interp
	 *            Tcl interpreter; used for error reporting if not null
	 * @param list
	 *            List object in which element should be stored
	 * @param index
	 *            Index of element to store
	 * @param value
	 *            Tcl object to store in the designated list element
	 * 
	 *           
	 * @throws TclException
	 */
	
	public static void setElement(Interp interp, TclObject list, int index,
			TclObject value) throws TclException {
		
		TclList listRep; 	// Internal representation of the list being modified.
		TclObject[] elems; 	// Pointers to elements of the list.
		int elemCount;
		
		// Ensure that the list parameter designates an unshared list.
		
		if (list.isShared()) {
			throw new TclRuntimeError(
			"TclListObjSetElement called with shared object");
		}
		
		if (!list.isListType()) {
			if (list.toString().length() == 0) {
				interp.setResult(TclString
						.newInstance("list index out of range"));
				throw new TclException(0);
			}
			
			try {
				setListFromAny(interp, list);
			} catch (TclException e) {
				throw e;
			}
		}
		
		/*
		 * !!!!!!!!!!!!!!! unchecked!
		 */
		listRep = (TclList) list.getInternalRep();
		elems = (TclObject[]) listRep.alist.toArray(new TclObject[listRep.alist
		                                                          .size()]);
		elemCount = listRep.alist.size();
		
		// Ensure that the index is in bounds.
		
		if (index < 0 || index >= elemCount) {
			if (interp != null) {
				interp.setResult(TclString
						.newInstance("list index out of range"));
			}
			throw new TclRuntimeError("list index out of range");
		}
		
		// If the internal rep is shared, replace it with an unshared copy.
		
		if (list.getRefCount() > 1) {
			TclList oldListRep = listRep;
			TclObject[] oldElems = elems;
			
			listRep = new TclList(elemCount);
			
			if (listRep == null) {
				throw new TclRuntimeError("Not enough memory to allocate list");
			}
			
			elems = (TclObject[]) listRep.alist.toArray();
			
			for (int i = 0; i < elemCount; i++) {
				elems[i] = oldElems[i];
				elems[i].preserve();
			}
			
			listRep.duplicate();
			list.setInternalRep(listRep);
			oldListRep.dispose();
		}
		
		/*
		 * NOTE: A reference to the new list element will be added in the replace() method.
		 */
		
		// Remove a reference from the old list element.
		
		elems[index].refCount--;
		
		// Stash the new object in the list.
		
		elems[index] = value;
		TclList.replace(interp, list, 0, TclList.getLength(interp, list),
				elems, 0, elems.length - 1);
		
	}

	/**
	 * TclListObjSetElement --
	 * 
	 * Set a single element of a list to a specified value in place (LsetCmd) 
	 * 
	 * 
	 * Side effects: Tcl_Panic if listPtr designates a shared object. Otherwise,
	 * attempts to convert it to a list with a non-shared internal rep.
	 * Decrements the ref count of the object at the specified index within the
	 * list, replaces with the object designated by valuePtr, and increments the
	 * ref count of the replacement object.
	 * 
	 * It is the caller's responsibility to invalidate the string representation
	 * of the object.  This method also avoids extra toArray() of the internal
	 * ArrayList.
	 * 
	 * @param interp
	 *            Tcl interpreter; used for error reporting if not null
	 * @param list
	 *            List object in which element should be stored
	 * @param index
	 *            Index of element to store
	 * @param value
	 *            Tcl object to store in the designated list element
	 * 
	 *           
	 * @throws TclException
	 */

	public static void lsetElement(Interp interp, TclObject list, int index,
			TclObject value) throws TclException {

		TclList listRep; 	// Internal representation of the list being modified.
		TclObject[] elems; 	// Pointers to elements of the list.
		int elemCount;

		// Ensure that the list parameter designates an unshared list.

		if (list.isShared()) {
			throw new TclRuntimeError(
					"TclListObjSetElement called with shared object");
		}

		if (!list.isListType()) {
			if (list.toString().length() == 0) {
				interp.setResult(TclString
						.newInstance("list index out of range"));
				throw new TclException(0);
			}

			try {
				setListFromAny(interp, list);
			} catch (TclException e) {
				throw e;
			}
		}

		/*
		 * !!!!!!!!!!!!!!! unchecked!
		 */
		listRep = (TclList) list.getInternalRep();
		ArrayList elemList = listRep.alist;
		elemCount = elemList.size();

		// Ensure that the index is in bounds.

		if (index < 0 || index >= elemCount) {
			if (interp != null) {
				interp.setResult(TclString
						.newInstance("list index out of range"));
			}
			throw new TclRuntimeError("list index out of range");
		}

		// If the internal rep is shared, replace it with an unshared copy.

		if (list.getRefCount() > 1) {
			TclList oldListRep = listRep;

			listRep = new TclList(elemCount);

			if (listRep == null) {
				throw new TclRuntimeError("Not enough memory to allocate list");
			}

			listRep.duplicate();
			list.setInternalRep(listRep);
			oldListRep.dispose();
			elemList = listRep.alist;
		}

		// Remove a reference from the old list element.

		((TclObject)elemList.get(index)).refCount--;

		// Stash the new object in the list.

		elemList.set(index, value);
		value.preserve();

	}

	/**
	 * This procedure returns a pointer to the index'th object from the list
	 * referenced by tobj. The first element has index 0. If index is negative
	 * or greater than or equal to the number of elements in the list, a null is
	 * returned. If tobj is not a list object, an attempt will be made to
	 * convert it to a list.
	 * 
	 * @param interp
	 *            current interpreter.
	 * @param tobj
	 *            the TclObject to use as a list.
	 * @param index
	 *            the index of the requested element.
	 * @return the the requested element.
	 * @exception TclException
	 *                if tobj is not a valid list.
	 */
	public static final TclObject index(Interp interp, TclObject tobj, int index)
			throws TclException {
		if (!tobj.isListType()) {
			setListFromAny(interp, tobj);
		}

		TclList tlist = (TclList) tobj.getInternalRep();
		if (index < 0 || index >= tlist.alist.size()) {
			return null;
		} else {
			return (TclObject) tlist.alist.get(index);
		}
	}

	/**
	 * This procedure inserts the elements in elements[] into the list at the
	 * given index. If tobj is not a list object, an attempt will be made to
	 * convert it to a list.
	 * 
	 * @param interp
	 *            current interpreter.
	 * @param tobj
	 *            the TclObject to use as a list.
	 * @param index
	 *            the starting index of the insertion operation. <=0 means the
	 *            beginning of the list. >= TclList.getLength(tobj) means the
	 *            end of the list.
	 * @param elements
	 *            the element(s) to insert.
	 * @param from
	 *            insert elements starting from elements[from] (inclusive)
	 * @param to
	 *            insert elements up to elements[to] (inclusive)
	 * @exception TclException
	 *                if tobj is not a valid list.
	 */
	public static final void insert(Interp interp, TclObject tobj, int index,
			TclObject elements[], int from, int to) throws TclException {
		if (tobj.isShared()) {
			throw new TclRuntimeError(
					"TclList.insert() called with shared object");
		}
		replace(interp, tobj, index, 0, elements, from, to);
	}

	/**
	 * This procedure replaces zero or more elements of the list referenced by
	 * tobj with the objects from an TclObject array. If tobj is not a list
	 * object, an attempt will be made to convert it to a list.
	 * 
	 * @param interp
	 *            current interpreter.
	 * @param tobj
	 *            the TclObject to use as a list.
	 * @param index
	 *            the starting index of the replace operation. <=0 means the
	 *            beginning of the list. >= TclList.getLength(tobj) means the
	 *            end of the list.
	 * @param count
	 *            the number of elements to delete from the list. <=0 means no
	 *            elements should be deleted and the operation is equivalent to
	 *            an insertion operation.
	 * @param elements
	 *            the element(s) to insert.
	 * @param from
	 *            insert elements starting from elements[from] (inclusive)
	 * @param to
	 *            insert elements up to elements[to] (inclusive)
	 * @exception TclException
	 *                if tobj is not a valid list.
	 */
	public static final void replace(Interp interp, TclObject tobj, int index,
			int count, TclObject elements[], int from, int to)
			throws TclException {
		if (tobj.isShared()) {
			throw new TclRuntimeError(
					"TclList.replace() called with shared object");
		}
		if (!tobj.isListType()) {
			setListFromAny(interp, tobj);
		}
		tobj.invalidateStringRep();
		TclList tlist = (TclList) tobj.getInternalRep();

		int size = tlist.alist.size();
		int i;

		if (index >= size) {
			// Append to the end of the list. There is no need for deleting
			// elements.
			index = size;
		} else {
			if (index < 0) {
				index = 0;
			}
			if (count > size - index) {
				count = size - index;
			}
			for (i = 0; i < count; i++) {
				TclObject obj = (TclObject) tlist.alist.get(index);
				// obj.release();
				tlist.alist.remove(index);
			}
		}
		for (i = from; i <= to; i++) {
			elements[i].preserve();
			tlist.alist.add(index++, elements[i]);
		}
	}



	/**
	 * Sorts the list according to the sort mode and (optional) sort command. If
	 * tobj is not a list object, an attempt will be made to convert it to a
	 * list.
	 * 
	 * @param interp
	 *            the current interpreter.
	 * @param tobj
	 *            the list to sort.
	 * @param sortMode
	 *            the sorting mode.
	 * @param sortIncreasing
	 *            true if to sort the elements in increasing order.
	 * @param unique
	 *            true if only the last set of duplicate elements found in the
	 *            list have to be retained
	 * @param command
	 *            the command to compute the order of two elements.
	 * @exception TclException
	 *                if tobj is not a valid list.
	 */

	public static void sort(Interp interp, TclObject tobj, int sortMode,
			int sortIndex,  boolean sortIncreasing, boolean unique,
			String command) throws TclException {
		if (!tobj.isListType()) {
			setListFromAny(interp, tobj);
		}
		tobj.invalidateStringRep();
		TclList tlist = (TclList) tobj.getInternalRep();

		TclObject objArray[] = TclList.getElements(interp, tobj);
		tlist.alist.clear();

		QSort s = new QSort();
		s.sort(interp, objArray, sortMode, sortIndex, sortIncreasing, command);

		for (int i = 0; i < objArray.length; i++) {
			if (unique && i < objArray.length-1) {
				TclObject o1 = objArray[i];
				TclObject o2 = objArray[i+1];
				if (s.compare(o1, o2)==0) {
					/* don't add o1 now, it is not unique */
					continue;
				}
			}
			tlist.alist.add(objArray[i]);
			objArray[i] = null;
		}
	}
}
