/* 
 * TclToken.java --
 *
 *	For each word of a command, and for each piece of a word such as a
 * 	variable reference, a TclToken is used to describe the word.
 *
 * 	Note: TclToken is designed to be write-once with respect to 
 * 	setting the script and size variables.  Failure to do this 
 * 	may lead to inconsistencies in calls to getTokenString(). 
 *
 * Copyright (c) 1997 by Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 *
 * RCS: @(#) $Id: TclToken.java,v 1.5 2005/10/29 00:27:43 mdejong Exp $
 */

package tcl.lang;

class TclToken {

	// Contains an array the references the script from where the
	// token originates from and an index to the first character
	// of the token inside the script.

	char[] script_array;
	// FIXME: Might be better to rename this as start so that
	// the Java impl looks more like the C impl in source code.
	int script_index;

	// Number of bytes in token.

	int size;

	// Type of token, such as TCL_TOKEN_WORD; See Parse.java
	// for valid types.

	int type;

	// If this token is composed of other tokens, this field
	// tells how many of them there are (including components
	// of components, etc.). The component tokens immediately
	// follow this one.

	int numComponents;

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * Tcl_Token -> TclToken
	 * 
	 * All class variables are accessed directly for speed. The consrructor only
	 * needs to allocate memory for the script.
	 * 
	 * Side effects: None.
	 * 
	 * ----------------------------------------------------------------------
	 */

	TclToken() {
		script_array = null;
		script_index = -1;
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * getTokenString --
	 * 
	 * The token string is a substring in the script. The beginning is
	 * deliminated by script_index, and the offset is the size variable. The
	 * first time this method is called the substring is cached in the str
	 * variable.
	 * 
	 * Note: TclToken is designed to be write-once with respect to setting the
	 * script and size variables. Failure to do this may lead to inconsistencies
	 * in str cache.
	 * 
	 * Results: The String representing the token.
	 * 
	 * Side effects: None.
	 * 
	 * ----------------------------------------------------------------------
	 */

	String getTokenString() {
		final boolean debug = false;

		if (debug && ((script_index + size) > script_array.length)) {
			System.out.println();
			System.out.println("Entered TclToken.getTokenString()");
			System.out.println("hashCode() is " + hashCode());
			System.out.println("script_array.length is " + script_array.length);
			System.out.println("script_index is " + script_index);
			System.out.println("size is " + size);

			System.out.print("the string is \"");
			for (char aScript_array : script_array) {
				System.out.print(aScript_array);
			}
			System.out.println("\"");
		}

		return (new String(script_array, script_index, size));
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * toString --
	 * 
	 * Prints debug information about the TclToken.
	 * 
	 * Results: A String containing the info.
	 * 
	 * Side effects: None.
	 * 
	 * ----------------------------------------------------------------------
	 */

	public String toString() {
		StringBuilder sbuf = new StringBuilder();
		switch (type) {
		case Parser.TCL_TOKEN_WORD: {
			sbuf.append("\n  Token Type: TCL_TOKEN_WORD");
			break;
		}
		case Parser.TCL_TOKEN_SIMPLE_WORD: {
			sbuf.append("\n  Token Type: TCL_TOKEN_SIMPLE_WORD");
			break;
		}
		case Parser.TCL_TOKEN_TEXT: {
			sbuf.append("\n  Token Type: TCL_TOKEN_TEXT");
			break;
		}
		case Parser.TCL_TOKEN_BS: {
			sbuf.append("\n  Token Type: TCL_TOKEN_BS");
			break;
		}
		case Parser.TCL_TOKEN_COMMAND: {
			sbuf.append("\n  Token Type: TCL_TOKEN_COMMAND");
			break;
		}
		case Parser.TCL_TOKEN_VARIABLE: {
			sbuf.append("\n  Token Type: TCL_TOKEN_VARIABLE");
			break;
		}
		case Parser.TCL_TOKEN_SUB_EXPR: {
			sbuf.append("\n  Token Type: TCL_TOKEN_SUB_EXPR");
			break;
		}
		case Parser.TCL_TOKEN_OPERATOR: {
			sbuf.append("\n  Token Type: TCL_TOKEN_OPERATOR");
			break;
		}
		}

		String ts = getTokenString();
		sbuf.append("\n  String:      " + ts);
		sbuf.append("\n  String Size: " + ts.length());
		sbuf.append("\n  ScriptIndex: " + script_index);
		sbuf.append("\n  NumComponents: " + numComponents);
		sbuf.append("\n  Token Size: " + size);
		return sbuf.toString();
	}

} // end TclToken

