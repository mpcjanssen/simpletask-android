/*
 * TclByteArray.java
 *
 *	This class contains the implementation of the Jacl binary data object.
 *
 * Copyright (c) 1999 Christian Krone.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: TclByteArray.java,v 1.4 2003/03/08 02:05:06 mdejong Exp $
 *
 */

package tcl.lang;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;

import tcl.lang.cmd.EncodingCmd;

/**
 * This class implements the binary data object type in Tcl.
 */
public class TclByteArray implements InternalRep {

	/**
	 * The number of bytes used in the byte array. The following structure is
	 * the internal rep for a ByteArray object. Keeps track of how much memory
	 * has been used. This can be different from how much has been allocated for
	 * the byte array to enable growing and shrinking of the ByteArray object
	 * with fewer allocations.
	 */
	private int used;

	/**
	 * Internal representation of the binary data.
	 */
	private byte[] bytes;

	/**
	 * Create a new empty Tcl binary data.
	 */
	private TclByteArray() {
		used = 0;
		bytes = new byte[0];
	}

	/**
	 * Create a new Tcl binary data.
	 */
	private TclByteArray(byte[] b) {
		used = b.length;
		bytes = new byte[used];
		System.arraycopy(b, 0, bytes, 0, used);
	}

	/**
	 * Create a new Tcl binary data.
	 */
	private TclByteArray(byte[] b, int position, int length) {
		used = length;
		bytes = new byte[used];
		System.arraycopy(b, position, bytes, 0, used);
	}

	/**
	 * Create a new Tcl binary data.
	 */
	private TclByteArray(char[] c) {
		used = c.length;
		bytes = new byte[used];
		for (int ix = 0; ix < used; ix++) {
			bytes[ix] = (byte) c[ix];
		}
	}

	/**
	 * Returns a duplicate of the current object.
	 * 
	 */
	public InternalRep duplicate() {
		return new TclByteArray(bytes, 0, used);
	}

	/**
	 * Implement this no-op for the InternalRep interface.
	 */

	public void dispose() {
	}

	/**
	 * Called to query the string representation of the Tcl object. This method
	 * is called only by TclObject.toString() when TclObject.stringRep is null.
	 * 
	 * @return the string representation of the TclByteArray, calculated by
	 *         copying each byte into the lower byte of each character (Tcl's
	 *         'identity' encoding)
	 */
	@Override
	public String toString() {
		char[] c = new char[used];
		for (int ix = 0; ix < used; ix++) {
			c[ix] = (char) (bytes[ix] & 0xff);
		}
		return new String(c);
	}

	/**
	 * Creates a new instance of a TclObject with a TclByteArray internal rep.
	 * 
	 * @return the TclObject with the given byte array value.
	 */

	public static TclObject newInstance(byte[] b, int position, int length) {
		return new TclObject(new TclByteArray(b, position, length));
	}

	/**
	 * Creates a new instance of a TclObject with a TclByteArray internal rep.
	 * 
	 * @return the TclObject with the given byte array value.
	 */

	public static TclObject newInstance(byte[] b) {
		return new TclObject(new TclByteArray(b));
	}

	/**
	 * Creates a new instance of a TclObject with an empty TclByteArray internal
	 * rep.
	 * 
	 * @return the TclObject with the empty byte array value.
	 */

	public static TclObject newInstance() {
		return new TclObject(new TclByteArray());
	}

	/**
	 * Called to convert the other object's internal rep to a ByteArray.
	 * 
	 * @param interp
	 *            current interpreter.
	 * @param tobj
	 *            the TclObject to convert to use the ByteArray internal rep.
	 * @exception TclException
	 *                if the object doesn't contain a valid ByteArray.
	 */
	static void setByteArrayFromAny(Interp interp, TclObject tobj) {
		InternalRep rep = tobj.getInternalRep();

		if (!(rep instanceof TclByteArray)) {
			char[] c = tobj.toString().toCharArray();
			tobj.setInternalRep(new TclByteArray(c));
		}
	}

	/**
	 * 
	 * This method changes the length of the byte array for this object. Once
	 * the caller has set the length of the array, it is acceptable to directly
	 * modify the bytes in the array up until Tcl_GetStringFromObj() has been
	 * called on this object.
	 * 
	 * Results: The new byte array of the specified length.
	 * 
	 * Side effects: Allocates enough memory for an array of bytes of the
	 * requested size. When growing the array, the old array is copied to the
	 * new array; new bytes are undefined. When shrinking, the old array is
	 * truncated to the specified length.
	 * 
	 * @param interp
	 *            the current interpreter
	 * @param tobj
	 *            object in which to change the byte array length. Object is
	 *            converted to a TclByteArray
	 * @param length
	 *            new length of the array
	 * 
	 * @return the byte array
	 */

	public static byte[] setLength(Interp interp, TclObject tobj, int length) {
		if (tobj.isShared()) {
			throw new TclRuntimeError("TclByteArray.setLength() called with shared object");
		}
		setByteArrayFromAny(interp, tobj);
		TclByteArray tbyteArray = (TclByteArray) tobj.getInternalRep();

		if (length > tbyteArray.bytes.length) {
			byte[] newBytes = new byte[length];
			System.arraycopy(tbyteArray.bytes, 0, newBytes, 0, tbyteArray.used);
			tbyteArray.bytes = newBytes;
		}
		tobj.invalidateStringRep();
		tbyteArray.used = length;
		return tbyteArray.bytes;
	}

	/**
	 * Queries the length of the byte array. If tobj is not a byte array object,
	 * an attempt will be made to convert it to a byte array.
	 * 
	 * @param interp
	 *            current interpreter.
	 * @param tobj
	 *            the TclObject to use as a byte array.
	 * @return the length of the byte array.
	 * @exception TclException
	 *                if tobj is not a valid byte array.
	 */
	public static final int getLength(Interp interp, TclObject tobj) {
		setByteArrayFromAny(interp, tobj);

		TclByteArray tbyteArray = (TclByteArray) tobj.getInternalRep();
		return tbyteArray.used;
	}

	/**
	 * Returns the bytes of a ByteArray object. If tobj is not a ByteArray
	 * object, an attempt will be made to convert it to a ByteArray.
	 * <p>
	 * 
	 * @param interp
	 *            the current interpreter.
	 * @param tobj
	 *            the byte array object.
	 * @return a byte array.
	 * @exception TclException
	 *                if tobj is not a valid ByteArray.
	 */
	public static byte[] getBytes(Interp interp, TclObject tobj) {
		setByteArrayFromAny(interp, tobj);
		TclByteArray tbyteArray = (TclByteArray) tobj.getInternalRep();
		return tbyteArray.bytes;
	}

	/**
	 * Interpret the bytes in the byte array according to the specified tcl
	 * encoding and return the corresponding string.
	 * 
	 * @param interp
	 *            the current interpreter
	 * @param tobj
	 *            the object, which is converted to a TclByteArray if necessary
	 * @param tclEncoding
	 *            Tcl encoding in which to decode to String. 'identity',
	 *            'binary' and null all imply that the string will be made of
	 *            chars whose lower byte is the corresponding byte from the
	 *            array.
	 * @return the decoded string
	 */
	public static String decodeToString(Interp interp, TclObject tobj, String tclEncoding) {
		setByteArrayFromAny(interp, tobj);
		TclByteArray tbyteArray = (TclByteArray) tobj.getInternalRep();
		if (tclEncoding == null || tclEncoding.equals("identity") || tclEncoding.equals("binary")) {
			return tobj.toString();
		}
		String javaEncoding = EncodingCmd.getJavaName(tclEncoding);
		CharsetDecoder csd = Charset.forName(javaEncoding).newDecoder();
		csd.onMalformedInput(CodingErrorAction.IGNORE);
		csd.onUnmappableCharacter(CodingErrorAction.REPLACE);
		CharBuffer cb = null;
		try {
			cb = csd.decode(ByteBuffer.wrap(tbyteArray.bytes, 0, tbyteArray.used));
		} catch (CharacterCodingException e) {
		}
		if (cb == null)
			return "";
		else
			return cb.toString();
	}

}
