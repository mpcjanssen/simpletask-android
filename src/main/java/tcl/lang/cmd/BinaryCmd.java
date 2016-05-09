/*
 * BinaryCmd.java --
 *
 *	Implements the built-in "binary" Tcl command.
 *
 * Copyright (c) 1999 Christian Krone.
 * Copyright (c) 1997 by Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: BinaryCmd.java,v 1.3 2005/07/22 04:47:24 mdejong Exp $
 *
 */

package tcl.lang.cmd;

import java.text.ParsePosition;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TclByteArray;
import tcl.lang.TclDouble;
import tcl.lang.TclException;
import tcl.lang.TclIndex;
import tcl.lang.TclInteger;
import tcl.lang.TclList;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;
import tcl.lang.TclString;

/**
 * This class implements the built-in "binary" command in Tcl.
 */

public class BinaryCmd implements Command {

	static final private String validCmds[] = { "format", "scan", };

	static final private int CMD_FORMAT = 0;
	static final private int CMD_SCAN = 1;

	// The following constants are used by GetFormatSpec to indicate various
	// special conditions in the parsing of a format specifier.

	/**
	 * Use all elements in the argument.
	 */
	static final private int BINARY_ALL = -1;
	/**
	 * No count was specified in format.
	 */
	static final private int BINARY_NOCOUNT = -2;
	/**
	 * End of format was found.
	 */
	static final private char FORMAT_END = ' ';

	/**
	 * This procedure is invoked as part of the Command interface to process the
	 * "binary" Tcl command. See the user documentation for details on what it
	 * does.
	 * 
	 * @see tcl.lang.Command#cmdProc(tcl.lang.Interp, tcl.lang.TclObject[])
	 */
	public void cmdProc(Interp interp, TclObject[] argv) throws TclException {
		int arg; // Index of next argument to consume.
		char[] format = null; // User specified format string.
		char cmd; // Current format character.
		int cursor; // Current position within result buffer.
		int maxPos; // Greatest position within result buffer that
					// cursor has visited.
		int value = 0; // Current integer value to be packed.
						// Initialized to avoid compiler warning.
		int offset, size = 0, length, index;

		if (argv.length < 2) {
			throw new TclNumArgsException(interp, 1, argv, "option ?arg arg ...?");
		}
		int cmdIndex = TclIndex.get(interp, argv[1], validCmds, "option", 0);

		switch (cmdIndex) {
		case CMD_FORMAT: {
			if (argv.length < 3) {
				throw new TclNumArgsException(interp, 2, argv, "formatString ?arg arg ...?");
			}

			// To avoid copying the data, we format the string in two passes.
			// The first pass computes the size of the output buffer. The
			// second pass places the formatted data into the buffer.

			format = argv[2].toString().toCharArray();
			arg = 3;
			length = 0;
			offset = 0;
			ParsePosition parsePos = new ParsePosition(0);

			while ((cmd = GetFormatSpec(format, parsePos)) != FORMAT_END) {
				int count = GetFormatCount(format, parsePos);

				switch (cmd) {
				case 'a':
				case 'A':
				case 'b':
				case 'B':
				case 'h':
				case 'H': {
					// For string-type specifiers, the count corresponds
					// to the number of bytes in a single argument.

					if (arg >= argv.length) {
						missingArg(interp);
					}
					if (count == BINARY_ALL) {
						count = TclByteArray.getLength(interp, argv[arg]);
					} else if (count == BINARY_NOCOUNT) {
						count = 1;
					}
					arg++;
					switch (cmd) {
					case 'a':
					case 'A':
						offset += count;
						break;
					case 'b':
					case 'B':
						offset += (count + 7) / 8;
						break;
					case 'h':
					case 'H':
						offset += (count + 1) / 2;
						break;
					}
					break;
				}
				case 'c':
				case 's':
				case 'S':
				case 'i':
				case 'I':
				case 'f':
				case 'd':
				case 'w':
				case 'W': {
					if (arg >= argv.length) {
						missingArg(interp);
					}
					switch (cmd) {
					case 'c':
						size = 1;
						break;
					case 's':
					case 'S':
						size = 2;
						break;
					case 'i':
					case 'I':
						size = 4;
						break;
					case 'w':
					case 'W':
						size = 8;
						break;
					case 'f':
						size = 4;
						break;
					case 'd':
						size = 8;
						break;
					}

					// For number-type specifiers, the count corresponds
					// to the number of elements in the list stored in
					// a single argument. If no count is specified, then
					// the argument is taken as a single non-list value.

					if (count == BINARY_NOCOUNT) {
						arg++;
						count = 1;
					} else {
						int listc = TclList.getLength(interp, argv[arg++]);
						if (count == BINARY_ALL) {
							count = listc;
						} else if (count > listc) {
							throw new TclException(interp, "number of elements in list" + " does not match count");
						}
					}
					offset += count * size;
					break;
				}
				case 'x': {
					if (count == BINARY_ALL) {
						throw new TclException(interp, "cannot use \"*\"" + " in format string with \"x\"");
					}
					if (count == BINARY_NOCOUNT) {
						count = 1;
					}
					offset += count;
					break;
				}
				case 'X': {
					if (count == BINARY_NOCOUNT) {
						count = 1;
					}
					if ((count > offset) || (count == BINARY_ALL)) {
						count = offset;
					}
					if (offset > length) {
						length = offset;
					}
					offset -= count;
					break;
				}
				case '@': {
					if (offset > length) {
						length = offset;
					}
					if (count == BINARY_ALL) {
						offset = length;
					} else if (count == BINARY_NOCOUNT) {
						alephWithoutCount(interp);
					} else {
						offset = count;
					}
					break;
				}
				default: {
					badField(interp, cmd);
				}
				}
			}
			if (offset > length) {
				length = offset;
			}
			if (length == 0) {
				return;
			}

			// Prepare the result object by preallocating the calculated
			// number of bytes and filling with nulls.

			TclObject resultObj = TclByteArray.newInstance();
			byte[] resultBytes = TclByteArray.setLength(interp, resultObj, length);
			interp.setResult(resultObj);

			// Pack the data into the result object. Note that we can skip
			// the error checking during this pass, since we have already
			// parsed the string once.

			arg = 3;
			cursor = 0;
			maxPos = cursor;
			parsePos.setIndex(0);

			while ((cmd = GetFormatSpec(format, parsePos)) != FORMAT_END) {
				int count = GetFormatCount(format, parsePos);

				if ((count == 0) && (cmd != '@')) {
					if (cmd != 'x') {
						arg++;
					}
					continue;
				}

				switch (cmd) {
				case 'a':
				case 'A': {
					byte pad = (cmd == 'a') ? (byte) 0 : (byte) ' ';
					byte[] bytes = TclByteArray.getBytes(interp, argv[arg++]);
					length = bytes.length;

					if (count == BINARY_ALL) {
						count = length;
					} else if (count == BINARY_NOCOUNT) {
						count = 1;
					}
					if (length >= count) {
						System.arraycopy(bytes, 0, resultBytes, cursor, count);
					} else {
						System.arraycopy(bytes, 0, resultBytes, cursor, length);
						for (int ix = 0; ix < count - length; ix++) {
							resultBytes[cursor + length + ix] = pad;
						}
					}
					cursor += count;
					break;
				}
				case 'b':
				case 'B': {
					char[] str = argv[arg++].toString().toCharArray();
					if (count == BINARY_ALL) {
						count = str.length;
					} else if (count == BINARY_NOCOUNT) {
						count = 1;
					}
					int last = cursor + ((count + 7) / 8);
					if (count > str.length) {
						count = str.length;
					}
					if (cmd == 'B') {
						for (offset = 0; offset < count; offset++) {
							value <<= 1;
							if (str[offset] == '1') {
								value |= 1;
							} else if (str[offset] != '0') {
								expectedButGot(interp, "binary", new String(str));
							}
							if (((offset + 1) % 8) == 0) {
								resultBytes[cursor++] = (byte) value;
								value = 0;
							}
						}
					} else {
						for (offset = 0; offset < count; offset++) {
							value >>= 1;
							if (str[offset] == '1') {
								value |= 128;
							} else if (str[offset] != '0') {
								expectedButGot(interp, "binary", new String(str));
							}
							if (((offset + 1) % 8) == 0) {
								resultBytes[cursor++] = (byte) value;
								value = 0;
							}
						}
					}
					if ((offset % 8) != 0) {
						if (cmd == 'B') {
							value <<= 8 - (offset % 8);
						} else {
							value >>= 8 - (offset % 8);
						}
						resultBytes[cursor++] = (byte) value;
					}
					while (cursor < last) {
						resultBytes[cursor++] = 0;
					}
					break;
				}
				case 'h':
				case 'H': {
					char[] str = argv[arg++].toString().toCharArray();
					if (count == BINARY_ALL) {
						count = str.length;
					} else if (count == BINARY_NOCOUNT) {
						count = 1;
					}
					int last = cursor + ((count + 1) / 2);
					if (count > str.length) {
						count = str.length;
					}
					if (cmd == 'H') {
						for (offset = 0; offset < count; offset++) {
							value <<= 4;
							int c = Character.digit(str[offset], 16);
							if (c < 0) {
								expectedButGot(interp, "hexadecimal", new String(str));
							}
							value |= (c & 0xf);
							if ((offset % 2) != 0) {
								resultBytes[cursor++] = (byte) value;
								value = 0;
							}
						}
					} else {
						for (offset = 0; offset < count; offset++) {
							value >>= 4;
							int c = Character.digit(str[offset], 16);
							if (c < 0) {
								expectedButGot(interp, "hexadecimal", new String(str));
							}
							value |= ((c << 4) & 0xf0);
							if ((offset % 2) != 0) {
								resultBytes[cursor++] = (byte) value;
								value = 0;
							}
						}
					}
					if ((offset % 2) != 0) {
						if (cmd == 'H') {
							value <<= 4;
						} else {
							value >>= 4;
						}
						resultBytes[cursor++] = (byte) value;
					}
					while (cursor < last) {
						resultBytes[cursor++] = 0;
					}
					break;
				}
				case 'c':
				case 's':
				case 'S':
				case 'i':
				case 'I':
				case 'f':
				case 'd':
				case 'w':
				case 'W': {
					TclObject[] listv;

					if (count == BINARY_NOCOUNT) {
						listv = new TclObject[1];
						listv[0] = argv[arg++];
						count = 1;
					} else {
						listv = TclList.getElements(interp, argv[arg++]);
						if (count == BINARY_ALL) {
							count = listv.length;
						}
					}
					for (int ix = 0; ix < count; ix++) {
						cursor = FormatNumber(interp, cmd, listv[ix], resultBytes, cursor);
					}
					break;
				}
				case 'x': {
					if (count == BINARY_NOCOUNT) {
						count = 1;
					}
					for (int ix = 0; ix < count; ix++) {
						resultBytes[cursor++] = 0;
					}
					break;
				}
				case 'X': {
					if (cursor > maxPos) {
						maxPos = cursor;
					}
					if (count == BINARY_NOCOUNT) {
						count = 1;
					}
					if (count == BINARY_ALL || count > cursor) {
						cursor = 0;
					} else {
						cursor -= count;
					}
					break;
				}
				case '@': {
					if (cursor > maxPos) {
						maxPos = cursor;
					}
					if (count == BINARY_ALL) {
						cursor = maxPos;
					} else {
						cursor = count;
					}
					break;
				}
				}
			}
			break;
		}
		case CMD_SCAN: {
			if (argv.length < 4) {
				throw new TclNumArgsException(interp, 2, argv, "value formatString ?varName varName ...?");
			}
			byte[] src = TclByteArray.getBytes(interp, argv[2]);
			length = src.length;
			format = argv[3].toString().toCharArray();
			arg = 4;
			cursor = 0;
			offset = 0;
			ParsePosition parsePos = new ParsePosition(0);

			while ((cmd = GetFormatSpec(format, parsePos)) != FORMAT_END) {
				int count = GetFormatCount(format, parsePos);

				switch (cmd) {
				case 'a':
				case 'A': {
					if (arg >= argv.length) {
						missingArg(interp);
					}
					if (count == BINARY_ALL) {
						count = length - offset;
					} else {
						if (count == BINARY_NOCOUNT) {
							count = 1;
						}
						if (count > length - offset) {
							break;
						}
					}

					size = count;

					// Trim trailing nulls and spaces, if necessary.

					if (cmd == 'A') {
						while (size > 0) {
							if (src[offset + size - 1] != '\0' && src[offset + size - 1] != ' ') {
								break;
							}
							size--;
						}
					}

					interp.setVar(argv[arg++], TclByteArray.newInstance(src, offset, size), 0);

					offset += count;
					break;
				}
				case 'b':
				case 'B': {
					if (arg >= argv.length) {
						missingArg(interp);
					}
					if (count == BINARY_ALL) {
						count = (length - offset) * 8;
					} else {
						if (count == BINARY_NOCOUNT) {
							count = 1;
						}
						if (count > (length - offset) * 8) {
							break;
						}
					}
					StringBuilder s = new StringBuilder(count);
					int thisOffset = offset;

					if (cmd == 'b') {
						for (int ix = 0; ix < count; ix++) {
							if ((ix % 8) != 0) {
								value >>= 1;
							} else {
								value = src[thisOffset++];
							}
							s.append((value & 1) != 0 ? '1' : '0');
						}
					} else {
						for (int ix = 0; ix < count; ix++) {
							if ((ix % 8) != 0) {
								value <<= 1;
							} else {
								value = src[thisOffset++];
							}
							s.append((value & 0x80) != 0 ? '1' : '0');
						}
					}

					interp.setVar(argv[arg++], TclString.newInstance(s.toString()), 0);

					offset += (count + 7) / 8;
					break;
				}
				case 'h':
				case 'H': {
					if (arg >= argv.length) {
						missingArg(interp);
					}
					if (count == BINARY_ALL) {
						count = (length - offset) * 2;
					} else {
						if (count == BINARY_NOCOUNT) {
							count = 1;
						}
						if (count > (length - offset) * 2) {
							break;
						}
					}
					StringBuilder s = new StringBuilder(count);
					int thisOffset = offset;

					if (cmd == 'h') {
						for (int ix = 0; ix < count; ix++) {
							if ((ix % 2) != 0) {
								value >>= 4;
							} else {
								value = src[thisOffset++];
							}
							s.append(Character.forDigit(value & 0xf, 16));
						}
					} else {
						for (int ix = 0; ix < count; ix++) {
							if ((ix % 2) != 0) {
								value <<= 4;
							} else {
								value = src[thisOffset++];
							}
							s.append(Character.forDigit(value >> 4 & 0xf, 16));
						}
					}

					interp.setVar(argv[arg++], TclString.newInstance(s.toString()), 0);

					offset += (count + 1) / 2;
					break;
				}
				case 'c':
				case 's':
				case 'S':
				case 'i':
				case 'I':
				case 'f':
				case 'd':
				case 'w': 
				case 'W': {
					if (arg >= argv.length) {
						missingArg(interp);
					}
					switch (cmd) {
					case 'c':
						size = 1;
						break;
					case 's':
					case 'S':
						size = 2;
						break;
					case 'i':
					case 'I':
						size = 4;
						break;
					case 'f':
						size = 4;
						break;
					case 'd':
						size = 8;
						break;
					case 'w':
					case 'W':
						size = 8;
						break;
					}
					TclObject valueObj;
					if (count == BINARY_NOCOUNT) {
						if (length - offset < size) {
							break;
						}
						valueObj = ScanNumber(src, offset, cmd);
						offset += size;
					} else {
						if (count == BINARY_ALL) {
							count = (length - offset) / size;
						}
						if (length - offset < count * size) {
							break;
						}
						valueObj = TclList.newInstance();
						int thisOffset = offset;
						for (int ix = 0; ix < count; ix++) {
							TclList.append(null, valueObj, ScanNumber(src, thisOffset, cmd));
							thisOffset += size;
						}
						offset += count * size;
					}

					interp.setVar(argv[arg++], valueObj, 0);

					break;
				}
				case 'x': {
					if (count == BINARY_NOCOUNT) {
						count = 1;
					}
					if (count == BINARY_ALL || count > length - offset) {
						offset = length;
					} else {
						offset += count;
					}
					break;
				}
				case 'X': {
					if (count == BINARY_NOCOUNT) {
						count = 1;
					}
					if (count == BINARY_ALL || count > offset) {
						offset = 0;
					} else {
						offset -= count;
					}
					break;
				}
				case '@': {
					if (count == BINARY_NOCOUNT) {
						alephWithoutCount(interp);
					}
					if (count == BINARY_ALL || count > length) {
						offset = length;
					} else {
						offset = count;
					}
					break;
				}
				default: {
					badField(interp, cmd);
				}
				}
			}

			// Set the result to the last position of the cursor.

			interp.setResult(arg - 4);
		}
		}
	}

	/**
	 * This function parses the format strings used in the binary format and
	 * scan commands.
	 * 
	 * Results: Moves the parsePos to the start of the next command. Returns the
	 * current command character or FORMAT_END if the string did not have a
	 * format specifier.
	 * 
	 * @param format
	 *            Format string.
	 * @param parsePos
	 *            Current position in input.
	 * @return
	 */
	private char GetFormatSpec(char[] format, ParsePosition parsePos) {
		int ix = parsePos.getIndex();

		// Skip any leading blanks.

		while (ix < format.length && format[ix] == ' ') {
			ix++;
		}

		// The string was empty, except for whitespace, so fail.

		if (ix >= format.length) {
			parsePos.setIndex(ix);
			return FORMAT_END;
		}

		// Extract the command character.

		parsePos.setIndex(ix + 1);

		return format[ix++];
	}

	/**
	 * This function parses the format strings used in the binary format and
	 * scan commands.
	 * 
	 * Results: Moves the formatPtr to the start of the next command. Returns
	 * the current command count. The count is set to BINARY_ALL if the count
	 * character was '*' or BINARY_NOCOUNT if no count was specified.
	 * 
	 * @param format
	 *            Format string.
	 * @param parsePos
	 *            Current position in input.
	 * @return
	 */
	private int GetFormatCount(char[] format, ParsePosition parsePos) {
		int ix = parsePos.getIndex();

		// Extract any trailing digits or '*'.

		if (ix < format.length && format[ix] == '*') {
			parsePos.setIndex(ix + 1);
			return BINARY_ALL;
		} else if (ix < format.length && Character.isDigit(format[ix])) {
			int length = 1;
			while (ix + length < format.length && Character.isDigit(format[ix + length])) {
				length++;
			}
			parsePos.setIndex(ix + length);
			return Integer.parseInt(new String(format, ix, length));
		} else {
			return BINARY_NOCOUNT;
		}
	}

	/**
	 * This method is called by the binary cmdProc to format a number into a
	 * location pointed at by cursor.
	 * 
	 * @param interp
	 * @param type
	 *            Type of number to format.
	 * @param src
	 *            Number to format.
	 * @param resultBytes
	 * @param cursor
	 * @return
	 * @throws TclException
	 */
	static int FormatNumber(Interp interp, char type, TclObject src, byte[] resultBytes, int cursor)
			throws TclException {
		if (type == 'd') {
			double dvalue = TclDouble.get(interp, src);
			// System.out.println("double value is \"" + dvalue + "\"");
			long lvalue = Double.doubleToLongBits(dvalue);
			// System.out.println("long hex value is \"" +
			// Long.toHexString(lvalue) + "\"");
			for (int ix = 7; ix >= 0; ix--) {
				resultBytes[cursor++] = (byte) (lvalue >> ix * 8);
				// byte b = resultBytes[cursor - 1];
				// System.out.println("index " + ix + " is " +
				// Integer.toHexString(b & 0xff));
			}
		} else if (type == 'f') {
			float fvalue;
			double dvalue = TclDouble.get(interp, src);
			// System.out.println("double value is \"" + dvalue + "\"");
			// Restrict the double value to the valid float range
			if (dvalue == Double.POSITIVE_INFINITY) {
				fvalue = Float.POSITIVE_INFINITY;
			} else if (dvalue == Double.NEGATIVE_INFINITY) {
				fvalue = Float.NEGATIVE_INFINITY;
			} else if (Math.abs(dvalue) > (double) Float.MAX_VALUE) {
				fvalue = (dvalue >= 0.0) ? Float.MAX_VALUE : -Float.MAX_VALUE;
			} else if (Math.abs(dvalue) < (double) Float.MIN_VALUE) {
				fvalue = (dvalue >= 0.0) ? 0.0f : -0.0f;
			} else {
				fvalue = (float) dvalue;
			}
			// System.out.println("float value is \"" + fvalue + "\"");
			int ivalue = Float.floatToIntBits(fvalue);
			// System.out.println("int hex value is \"" +
			// Integer.toHexString(ivalue) + "\"");
			for (int ix = 3; ix >= 0; ix--) {
				resultBytes[cursor++] = (byte) (ivalue >> ix * 8);
				// byte b = resultBytes[cursor - 1];
				// System.out.println("index " + ix + " is " +
				// Integer.toHexString(b & 0xff));
			}
		} else {
			long value = TclInteger.getLong(interp, src);
			switch (type) {
			case 'c':
				resultBytes[cursor++] = (byte) value;
				break;
			case 's':
				resultBytes[cursor++] = (byte) value;
				resultBytes[cursor++] = (byte) (value >> 8);
				break;
			case 'S':
				resultBytes[cursor++] = (byte) (value >> 8);
				resultBytes[cursor++] = (byte) value;
				break;
			case 'i':
				resultBytes[cursor++] = (byte) value;
				resultBytes[cursor++] = (byte) (value >> 8);
				resultBytes[cursor++] = (byte) (value >> 16);
				resultBytes[cursor++] = (byte) (value >> 24);
				break;
			case 'I':
				resultBytes[cursor++] = (byte) (value >> 24);
				resultBytes[cursor++] = (byte) (value >> 16);
				resultBytes[cursor++] = (byte) (value >> 8);
				resultBytes[cursor++] = (byte) value;
				break;
			case 'w':
				resultBytes[cursor++] = (byte) value;
				resultBytes[cursor++] = (byte) (value >> 8);
				resultBytes[cursor++] = (byte) (value >> 16);
				resultBytes[cursor++] = (byte) (value >> 24);
				resultBytes[cursor++] = (byte) (value >> 32);
				resultBytes[cursor++] = (byte) (value >> 40);
				resultBytes[cursor++] = (byte) (value >> 48);
				resultBytes[cursor++] = (byte) (value >> 56);
				break;
			case 'W':
				resultBytes[cursor++] = (byte) (value >> 56);
				resultBytes[cursor++] = (byte) (value >> 48);
				resultBytes[cursor++] = (byte) (value >> 40);
				resultBytes[cursor++] = (byte) (value >> 32);
				resultBytes[cursor++] = (byte) (value >> 24);
				resultBytes[cursor++] = (byte) (value >> 16);
				resultBytes[cursor++] = (byte) (value >> 8);
				resultBytes[cursor++] = (byte) value;
				break;
			}
		}
		return cursor;
	}

	/**
	 * This routine is called by Tcl_BinaryObjCmd to scan a number out of a
	 * buffer.
	 * 
	 * Results: Returns a newly created object containing the scanned number.
	 * This object has a ref count of zero.
	 * 
	 * @param src
	 *            Buffer to scan number.
	 * @param pos
	 *            Position in buffer.
	 * @param type
	 *            Format character from "binary scan"
	 * @return
	 */
	private static TclObject ScanNumber(byte[] src, int pos, int type) {
		switch (type) {
		case 'c': {
			return TclInteger.newInstance((int) src[pos]);
		}
		case 's': {
			short value = (short) ((src[pos] & 0xff) + ((src[pos + 1] & 0xff) << 8));
			return TclInteger.newInstance((int) value);
		}
		case 'S': {
			short value = (short) ((src[pos + 1] & 0xff) + ((src[pos] & 0xff) << 8));
			return TclInteger.newInstance((int) value);
		}
		case 'i': {
			int value = (src[pos] & 0xff) + ((src[pos + 1] & 0xff) << 8) + ((src[pos + 2] & 0xff) << 16)
					+ ((src[pos + 3] & 0xff) << 24);
			return TclInteger.newInstance(value);
		}
		case 'I': {
			int value = (src[pos + 3] & 0xff) + ((src[pos + 2] & 0xff) << 8) + ((src[pos + 1] & 0xff) << 16)
					+ ((src[pos] & 0xff) << 24);
			return TclInteger.newInstance(value);
		}
		case 'w': {
			long value = (src[pos] & 0xffL) + ((src[pos + 1] & 0xffL) << 8) + ((src[pos + 2] & 0xffL) << 16)
					+ ((src[pos + 3] & 0xffL) << 24) 
					+  ((src[pos+4] & 0xffL) << 32) + ((src[pos + 5] & 0xffL) << 40) + ((src[pos + 6] & 0xffL) << 48)
					+ ((src[pos + 7] & 0xffL) << 56);
			return TclInteger.newInstance(value);
		}
		case 'W': {
			long value = (src[pos + 7] & 0xffL) + ((src[pos + 6] & 0xffL) << 8) + ((src[pos + 5] & 0xffL) << 16)
			+ ((src[pos + 4] & 0xffL) << 24)
			+  ((src[pos + 3] & 0xffL) << 32) + ((src[pos + 2] & 0xffL) << 40) + ((src[pos + 1] & 0xffL) << 48)
			+ ((src[pos] & 0xffL) << 56);
			return TclInteger.newInstance(value);
		}
		case 'f': {
			int value = (src[pos + 3] & 0xff) + ((src[pos + 2] & 0xff) << 8) + ((src[pos + 1] & 0xff) << 16)
					+ ((src[pos] & 0xff) << 24);
			return TclDouble.newInstance(Float.intBitsToFloat(value));
		}
		case 'd': {
			long value = (((long) src[pos + 7]) & 0xff) + (((long) (src[pos + 6] & 0xff)) << 8)
					+ (((long) (src[pos + 5] & 0xff)) << 16) + (((long) (src[pos + 4] & 0xff)) << 24)
					+ (((long) (src[pos + 3] & 0xff)) << 32) + (((long) (src[pos + 2] & 0xff)) << 40)
					+ (((long) (src[pos + 1] & 0xff)) << 48) + (((long) (src[pos] & 0xff)) << 56);
			return TclDouble.newInstance(Double.longBitsToDouble(value));
		}
		}
		return null;
	}

	/**
	 * Called whenever a format specifier was detected but there are not enough
	 * arguments specified.
	 * 
	 * @param interp
	 * @throws TclException
	 */
	private static void missingArg(Interp interp) throws TclException {
		throw new TclException(interp, "not enough arguments for all format specifiers");
	}

	/**
	 * Called whenever an invalid format specifier was detected.
	 * 
	 * @param interp
	 * @param cmd
	 * @throws TclException
	 */
	private static void badField(Interp interp, char cmd) throws TclException {
		throw new TclException(interp, "bad field specifier \"" + cmd + "\"");
	}

	/**
	 * Called whenever a letter aleph character (@) was detected but there was
	 * no count specified.
	 * 
	 * @param interp
	 * @throws TclException
	 */
	private static void alephWithoutCount(Interp interp) throws TclException {
		throw new TclException(interp, "missing count for \"@\" field specifier");
	}

	/**
	 * Called whenever a format was found which restricts the valid range of
	 * characters in the specified string, but the string contains at least one
	 * char not in this range.
	 * 
	 * @param interp
	 * @param expected
	 * @param str
	 * @throws TclException
	 */
	private static void expectedButGot(Interp interp, String expected, String str) throws TclException {
		throw new TclException(interp, "expected " + expected + " string but got \"" + str + "\" instead");
	}

}
