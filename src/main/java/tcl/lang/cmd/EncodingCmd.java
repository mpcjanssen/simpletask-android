/*
 * EncodingCmd.java --
 *
 * Copyright (c) 2001 Bruce A. Johnson
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: EncodingCmd.java,v 1.3 2006/07/07 23:36:00 mdejong Exp $
 *
 */

package tcl.lang.cmd;


import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TclByteArray;
import tcl.lang.TclException;
import tcl.lang.TclIO;
import tcl.lang.TclIndex;
import tcl.lang.TclList;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;
import tcl.lang.TclRuntimeError;
import tcl.lang.TclString;
import tcl.lang.channel.Channel;

/**
 * This class implements the built-in "encoding" command in Tcl.
 * It's also the home for the global systemEncoding property,
 * and translates between TCL encoding names and Java Encoding names.
 */

public class EncodingCmd implements Command {

	/**
	 * The encoding value set and returned by 'encoding system'
	 */
	public static String systemTclEncoding = null;
	/**
	 * The java equivalent name (either java.nio or java.io/java.lang) of the value
	 * returned for 'encoding system'
	 */
	public static String systemJavaEncoding = null;

	/**
	 * Encapsulates both the tcl and the java
	 * name for an encoding
	 */
	static class EncodingMap {
		String tclName;
		String javaName;
		int bytesPerChar;

		public EncodingMap(String tclName, String javaName, int bytesPerChar) {
			this.tclName = tclName;
			this.javaName = javaName;
			this.bytesPerChar = bytesPerChar;
		}
	}


	/**
	 * Hashtable of all supported encodings, containing both java names
	 * and tcl names.  "tcl," is prepended to Tcl names for the index; "java,"
	 * is prepended to the java names.  The java names can be either the old-style
	 * java.io/java.lang names, or the new style java.nio names.
	 */
	static Hashtable<String, EncodingMap> encodeHash;

	static EncodingMap[] encodings = { 
		    new EncodingMap("identity", "UTF8", 1),  // this is not really true; convertfrom/convertto handle it differently
		    new EncodingMap("symbol", "ASCII",1), // not really true; handled as a special case
			new EncodingMap("utf-8", "UTF8", 1),
			new EncodingMap("utf-16", "UTF16", 2),
			new EncodingMap("unicode", "ISO-10646-UCS-2", 2),
			new EncodingMap("ascii", "ASCII", 1),
			new EncodingMap("big5", "Big5", 0),
			new EncodingMap("cp1250", "Cp1250", 1),
			new EncodingMap("cp1251", "Cp1251", 1),
			new EncodingMap("ansi-1251", "Cp1251", 1),
			new EncodingMap("cp1252", "Cp1252", 1),
			new EncodingMap("cp1253", "Cp1253", 1),
			new EncodingMap("cp1254", "Cp1254", 1),
			new EncodingMap("cp1255", "Cp1255", 1),
			new EncodingMap("cp1256", "Cp1256", 1),
			new EncodingMap("cp1257", "Cp1257", 1),
			new EncodingMap("cp1258", "Cp1258", 1),
			new EncodingMap("cp437", "Cp437", 1),
			new EncodingMap("cp737", "Cp737", 1),
			new EncodingMap("cp775", "Cp775", 1),
			new EncodingMap("cp850", "Cp850", 1),
			new EncodingMap("cp852", "Cp852", 1),
			new EncodingMap("cp855", "Cp855", 1),
			new EncodingMap("cp857", "Cp857", 1),
			new EncodingMap("cp860", "Cp860", 1),
			new EncodingMap("cp861", "Cp861", 1),
			new EncodingMap("cp862", "Cp862", 1),
			new EncodingMap("cp863", "Cp863", 1),
			new EncodingMap("cp864", "Cp864", 1),
			new EncodingMap("cp865", "Cp865", 1),
			new EncodingMap("cp866", "Cp866", 1),
			new EncodingMap("cp869", "Cp869", 1),
			new EncodingMap("cp874", "Cp874", 1),
			new EncodingMap("cp932", "Cp943", 0),  // this mapping reduces the number of encoding.test errors
			new EncodingMap("cp936", "Cp936", 0),
			new EncodingMap("cp949", "Cp949", 0),
			new EncodingMap("cp950", "Cp950", 0),
			new EncodingMap("euc-cn", "EUC_cn", 0),
			new EncodingMap("euc-jp", "EUC_jp", 0),
			new EncodingMap("euc-kr", "EUC_kr", 0),
			new EncodingMap("iso2022", "ISO-2022-JP-2", -1),  // C Tcl's mapping files looks like ISO2022-JP-2
			new EncodingMap("iso2022-jp", "ISO-2022-JP-2", -1),  // C Tcl's mapping file looks like ISO2022-JP-2
			new EncodingMap("iso2022-kr", "ISO2022KR", -1),
			new EncodingMap("iso8859-1", "ISO-8859-1", 1),
			new EncodingMap("ansi_x3.4-1968", "ISO-8859-1", 1),
			new EncodingMap("iso8859-2", "ISO-8859-2", 1),
			new EncodingMap("iso8859-3", "ISO-8859-3", 1),
			new EncodingMap("iso8859-4", "ISO-8859-4", 1),
			new EncodingMap("iso8859-5", "ISO-8859-5", 1),
			new EncodingMap("iso8859-6", "ISO-8859-6", 1),
			new EncodingMap("iso8859-7", "ISO-8859-7", 1),
			new EncodingMap("iso8859-8", "ISO-8859-8", 1),
			new EncodingMap("iso8859-9", "ISO-8859-9", 1),
			new EncodingMap("iso8859-10", "ISO-8859-10", 1),
			new EncodingMap("iso8859-11", "ISO-8859-11", 1),
			new EncodingMap("iso8859-12", "ISO-8859-12", 1),
			new EncodingMap("iso8859-13", "ISO-8859-13", 1),
			new EncodingMap("iso8859-14", "ISO-8859-14", 1),
			new EncodingMap("iso8859-15", "ISO-8859-15", 1),
			new EncodingMap("jis0201", "JIS0201", 1),
			new EncodingMap("jis0208", "JIS0208", 2),
			new EncodingMap("jis0212", "JIS0212", 2),
			new EncodingMap("koi8-r", "KOI8_r", 1),
			new EncodingMap("macCentEuro", "MacCentEuro", 1),
			new EncodingMap("macCroatian", "MacCroatian", 1),
			new EncodingMap("macCyrillic", "MacCyrillic", 1),
			new EncodingMap("macDingbats", "MacDingbats", 1),
			new EncodingMap("macGreek", "MacGreek", 1),
			new EncodingMap("macIceland", "MacIceland", 1),
			new EncodingMap("macJapan", "MacJapan", 0),
			new EncodingMap("macRoman", "MacRoman", 1),
			new EncodingMap("macRomania", "MacRomania", 1),
			new EncodingMap("macSymbol", "MacSymbol", 1),
			new EncodingMap("macThai", "MacThai", 1),
			new EncodingMap("macTurkish", "MacTurkish", 1),
			new EncodingMap("macUkraine", "MacUkraine", 1),
			new EncodingMap("shiftjis","Shift_JIS", 0) };

	static {
		// Store entries in a Hashtable, so that access from
		// multiple threads will be synchronized.

		encodeHash = new Hashtable<>();

		for (EncodingMap map : encodings) {
			String tclKey = "tcl," + map.tclName;
			String javaKey = "java," + map.javaName;

			encodeHash.put(tclKey, map);
			encodeHash.put(javaKey, map);
		}

		// Determine default system encoding, use
		// "iso8859-1" if default is not known.

		// use Java 1.5 API.  Since the EncodingMap
		// uses some of the Java historical names, look through
		// all the aliases to find a match to the defaultCharset
		
		Charset defaultCharset = Charset.defaultCharset();
		Set<String> aliases = defaultCharset.aliases();
		ArrayList<String> all = new ArrayList<>(aliases.size() + 1);
		all.add(defaultCharset.name());
		all.addAll(aliases);
		
		Iterator<String> iterator = all.iterator();
		
		String enc = null;
		while (iterator.hasNext()) {
			enc = iterator.next();
			// Lookup EncodingMap for this Java encoding name
			String key = "java," + enc;
			EncodingMap map = (EncodingMap) encodeHash.get(key);
			if (map == null) {
				enc = null;
			} else {
				systemTclEncoding = map.tclName;
				systemJavaEncoding = map.javaName;	
				break;
			}
		}

		// Default to "iso8859-1" if the encoding is not
		// in the supported encoding table.

		if (enc == null || enc.length() == 0) {
			systemTclEncoding = "iso8859-1";
			systemJavaEncoding = "ISO-8859-1";
		}
	}

	/**
	 * This table maps Tcl's built-in 'symbol' encoding to Unicode characters; it is derived from symbol.enc in the Tcl source
	 * It covers the symbols from 0x40 to 0x7A
	 */
	static final private char [] symbolEncoding0x40to0x7A = {	
		'\u2245','\u0391','\u0392','\u03A7','\u0394','\u0395','\u03A6','\u0393','\u0397','\u0399','\u03D1','\u039A','\u039B','\u039C','\u039D','\u039F',
		'\u03A0','\u0398','\u03A1','\u03A3','\u03A4','\u03A5','\u03C2','\u03A9','\u039E','\u03A8','\u0396','\u005B','\u2234','\u005D','\u22A5','\u005F',
		'\uF8E5','\u03B1','\u03B2','\u03C7','\u03B4','\u03B5','\u03C6','\u03B3','\u03B7','\u03B9','\u03D5','\u03BA','\u03BB','\u03BC','\u03BD','\u03BF',
		'\u03C0','\u03B8','\u03C1','\u03C3','\u03C4','\u03C5','\u03D6','\u03C9','\u03BE','\u03C8','\u03B6'};
	
	/**
	 * This table maps Tcl's built-in 'symbol' encoding to Unicode characters; it is derived from symbol.enc in the Tcl source
	 * It covers the symbols from 0xA0 to 0xFF
	 */
	static final private char [] symbolEncoding0xA0to0xFF = {	
		'\u0000','\u03D2','\u2032','\u2264','\u2044','\u221E','\u0192','\u2663','\u2666','\u2665','\u2660','\u2194','\u2190','\u2191','\u2192','\u2193',
		'\u00B0','\u00B1','\u2033','\u2265','\u00D7','\u221D','\u2202','\u2022','\u00F7','\u2260','\u2261','\u2248','\u2026','\uF8E6','\uF8E7','\u21B5',
		'\u2135','\u2111','\u211C','\u2118','\u2297','\u2295','\u2205','\u2229','\u222A','\u2283','\u2287','\u2284','\u2282','\u2286','\u2208','\u2209',
		'\u2220','\u2207','\u00AE','\u00A9','\u2122','\u220F','\u221A','\u22C5','\u00AC','\u2227','\u2228','\u21D4','\u21D0','\u21D1','\u21D2','\u21D3',
		'\u22C4','\u2329','\uF8E8','\uF8E9','\uF8EA','\u2211','\uF8EB','\uF8EC','\uF8ED','\uF8EE','\uF8EF','\uF8F0','\uF8F1','\uF8F2','\uF8F3','\uF8F4',
		'\uF8FF','\u232A','\u222B','\u2320','\uF8F5','\u2321','\uF8F6','\uF8F7','\uF8F8','\uF8F9','\uF8FA','\uF8FB','\uF8FC','\uF8FD','\uF8FE','\u0000'};
	
		
	static final private String validCmds[] = { "convertfrom", "convertto",
			"names", "system", };

	static final int OPT_CONVERTFROM = 0;
	static final int OPT_CONVERTTO = 1;
	static final int OPT_NAMES = 2;
	static final int OPT_SYSTEM = 3;

	/**
	 * This procedure is invoked to process the "encoding" Tcl command. See the
	 * user documentation for details on what it does.
	 * 
	 * @param interp
	 *            the current interpreter.
	 * @param objv
	 *            command arguments.
	 */

	public void cmdProc(Interp interp, TclObject[] objv) throws TclException {
		if (objv.length < 2) {
			throw new TclNumArgsException(interp, 1, objv, "option ?arg ...?");
		}

		int index = TclIndex.get(interp, objv[1], validCmds, "option", 0);

		switch (index) {
		case OPT_CONVERTTO:
		case OPT_CONVERTFROM: {
			String tclEncoding, javaEncoding;
			TclObject data;

			if (objv.length == 3) {
				tclEncoding = systemTclEncoding;
				data = objv[2];
			} else if (objv.length == 4) {
				tclEncoding = objv[2].toString();
				data = objv[3];
			} else {
				throw new TclNumArgsException(interp, 2, objv,
						"?encoding? data");
			}

			javaEncoding = getJavaName(tclEncoding);

			if (javaEncoding == null) {
				throw new TclException(interp, "unknown encoding \""
						+ tclEncoding + "\"");
			}

			try {
				if (index == OPT_CONVERTFROM) {
					// this doesn't preserve tclbytearray on identity
					switch (tclEncoding) {
						case "identity":
							// preserve the original bytes as a TclByteArray
							TclByteArray.getLength(interp, data);
							interp.setResult(data);
							break;
						case "symbol":
							char[] cbuf = new char[TclByteArray.getLength(interp, data)];
							decodeSymbol(TclByteArray.getBytes(interp, data), cbuf, 0, cbuf.length);
							interp.setResult(new String(cbuf));
							break;
						default:
							interp.setResult(TclByteArray.decodeToString(interp, data, tclEncoding));
							break;
					}
				} else /* OPT_CONVERTTO */ {
					switch (tclEncoding) {
						case "identity":
							if (data.isByteArrayType())
								interp.setResult(data);
							else {
								// convert to TclByteArray, using modified UTF8, where \u0000 is 0xc0 0x80
								// We'll use a DataOutputStream.writeUTF() to do this for us
								ByteArrayOutputStream bos = new ByteArrayOutputStream(data.toString().length() * 3);
								DataOutputStream dos = new DataOutputStream(bos);
								dos.writeUTF(data.toString());

								byte[] bytes = bos.toByteArray();
								interp.setResult(TclByteArray.newInstance(bytes, 2, bytes.length - 2));
							}
							break;
						case "symbol":
							byte[] bbuf = encodeSymbol(data.toString().toCharArray(), 0, data.toString().length());
							interp.setResult(TclByteArray.newInstance(bbuf, 0, bbuf.length));
							break;
						default:
							TclObject rv = TclByteArray.newInstance(data.toString().getBytes(javaEncoding));
							interp.setResult(rv);
							break;
					}
				}

			} catch (UnsupportedEncodingException ex) {
				throw new TclException(interp,"Encoding.cmdProc() error: "
						+ "unsupported java encoding \"" + javaEncoding + "\"");
			} catch (IOException e) {
				 throw new TclException(interp, e.getMessage());
			}

			break;
		}
		case OPT_NAMES: {
			if (objv.length > 2) {
				throw new TclNumArgsException(interp, 2, objv, null);
			}

			TclObject list = TclList.newInstance();
			for (EncodingMap map : encodings) {
				// Encodings that exists in the table but
				// is not supported by the runtime should
				// not be returned.

				if (isSupported(map.javaName)) {
					TclList.append(interp, list, TclString
							.newInstance(map.tclName));
				}
			}
			interp.setResult(list);
			break;
		}
		case OPT_SYSTEM: {
			if (objv.length > 3)
				throw new TclNumArgsException(interp, 2, objv, "?encoding?");

			if (objv.length == 2) {
				interp.setResult(systemTclEncoding);
			} else {
				String tclEncoding = objv[2].toString();
				String javaEncoding = EncodingCmd.getJavaName(tclEncoding);

				if (javaEncoding == null) {
					throw new TclException(interp, "unknown encoding \""
							+ tclEncoding + "\"");
				}
				if (! isSupported(javaEncoding))
					throw new TclException(interp,"Encoding.cmdProc() error: "
						+ "unsupported java encoding \"" + javaEncoding + "\"");

				systemTclEncoding = tclEncoding;
				systemJavaEncoding = javaEncoding;
				
				/* TCL probably does this differently, but it's the easiest way to implement in JTCL */
				if (interp.systemEncodingChangesStdoutStderr) {
					Channel chan = TclIO.getChannel(interp, "stdout");
					if (chan!=null) chan.setEncoding(systemJavaEncoding);
					chan = TclIO.getChannel(interp, "stderr");
					if (chan!=null) chan.setEncoding(systemJavaEncoding);
					interp.systemEncodingChangesStdoutStderr = true; // reset it because getChannel() cleared it
				}
			}

			break;
		}
		default: {
			throw new TclRuntimeError("Encoding.cmdProc() error: "
					+ "incorrect index returned from TclIndex.get()");
		}
		}
	}


	/**
	 * Given a Java encoding name return the average bytes per char
	 * 
	 * @param name
	 *            Java name of encoding as returned from getJavaName(
	 * @return the average bytes per character
	 */
	public static int getBytesPerChar(String name) {
		String key = "java," + name;
		EncodingMap map = (EncodingMap) encodeHash.get(key);
		if (map == null) {
			throw new RuntimeException("Invalid encoding \"" + name + "\"");
		}
		return map.bytesPerChar;
	}


	/**
	 * Given a Tcl encoding name, return the Java encoding name
	 * 
	 * @param name
	 *            Tcl name for encoding
	 * @return java name for encoding, which may be either the new-style
	 *         java.nio name, or the old style java.io/java.lang name
	 */
	public static String getJavaName(String name) {
		String key = "tcl," + name;
		EncodingMap map = (EncodingMap) encodeHash.get(key);
		if (map == null) {
			return null;
		}
		return map.javaName;
	}

	/**
	 * Given a Java encoding name, return the Tcl encoding name
	 * 
	 * @param name Java name, as specified from getJavaName()
	 * @return Tcl name for the encoding
	 */
	static String getTclName(String name) {
		String key = "java," + name;
		EncodingMap map = (EncodingMap) encodeHash.get(key);
		if (map == null) {
			return null;
		}
		return map.tclName;
	}

	/**
	 * 
	 * @param name
	 *            java encoding name, as from getJavaName()
	 * @return true if the encoding is supported in this JRE
	 */
	static boolean isSupported(String name) {
		String key = "java," + name;
		EncodingMap map = (EncodingMap) encodeHash.get(key);
		if (map == null) {
			return false;
		}

		// Load the encoding at runtime
		Charset cs;
		try {
			cs = Charset.forName(name);
		} catch (IllegalCharsetNameException ex) {
			// This should never happen
			throw new TclRuntimeError("illegal charset name \"" + name + "\"");
		} catch (UnsupportedCharsetException ex) {
			// This can happen when a western install does
			// not support international encodings.

			return false;
		}

		// Return true when charset can encode and decode.
		// All charsets can decode, but some special case
		// charsets may not support the encode operation.

		return cs.canEncode();
	}
	
	/**
	 * Decode bytes encoded in Tcl's symbol encoding into a character array.
	 * We probably should have done this by registering a Java CharSet,
	 * but that's a lot of code for this not-often-used encoding.
	 * 
	 * @param bbuf binary data that contains a symbol encodes string
	 * @param cbuf character buffer to fill with data s
	 * @param off offset to filling cbuf
	 * @param len number of characters to add to cbuf
	 */
	public static void decodeSymbol(byte [] bbuf, char[] cbuf, int off, int len) {
		for (int i=0; i<len; i++) {
			int byteVal = bbuf[i] & 0xff;
			if (byteVal < 0x40) { 
				cbuf[i+off] = (char)byteVal;
			} else if (byteVal < 0x7b) {
				cbuf[i+off] = symbolEncoding0x40to0x7A[byteVal-0x40];
			} else if (byteVal < 0xA0) {
				cbuf[i+off] = (char)byteVal;
			} else {
				cbuf[i+off] = symbolEncoding0xA0to0xFF[byteVal-0xA0];
			}
		}
	}
	
	/**
	 * Encode chars into Tcl's symbol encoding into a byte  array.
	 * We probably should have done this by registering a Java CharSet,
	 * but that's a lot of code for this not-often-used encoding.
	 * 
	 * @param cbuf character data to encode in 'symbol' encoding
	 * @param off offset to start encoding
	 * @param len number of characters to encode
	 * @return a byte array of lenght len containing encoded data
	 */
	public static byte [] encodeSymbol(char [] cbuf, int off, int len) {
		byte [] bbuf = new byte[len];
		for (int i=off; i<off+len; i++) {
			if ( (cbuf[i] <= '\u00ff')) {
				bbuf[i-off] = (byte)(cbuf[i] & 0xff);
			} else {
				boolean notFound=true;
				for (int j=0; notFound && j<symbolEncoding0x40to0x7A.length; j++) {
					if (cbuf[i]==symbolEncoding0x40to0x7A[j]) {
						notFound=false;
						bbuf[i-off] = (byte)(j+0x40);
					}
				}
				for (int j=0; notFound && j<symbolEncoding0xA0to0xFF.length; j++) {
					if (cbuf[i]==symbolEncoding0xA0to0xFF[j]) {
						notFound=false;
						bbuf[i-off] = (byte)(j+0xA0);
					}
				}
				if (notFound) {
					bbuf[i-off] = 0x3f;
				}
			}
		}
		return bbuf;
	}
}
