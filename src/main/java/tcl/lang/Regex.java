/*
 * Rejex.java --
 *
 * 	This file contains is a gasket class between
 *  TCL's concept of regular expressions and Java's
 *  Pattern and Matcher classes.  
 *
 * Copyright (c) 2009 Radoslaw Szulgo (radoslaw@szulgo.pl)
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: Regex.java,v 1.12 2010/02/21 18:30:44 mdejong Exp $
 */

package tcl.lang;

import java.lang.StringBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * The Regex class can be used to match a TCL-style regular expression
 *  against a string and optionally replace the matched parts with new strings.
 *  It serves as a gasket between TCL regular expression style and 
 *  java.util.regex.* style.
 * 
 * Known problems: FIXME
 *
 *   - Most important: TCL always attempts to match the longest string
 *     starting from the outermost levels to the inner levels of parens.
 *     With alternation (|) TCL chooses the longest match of all the branches.
 *     Java, on the other hand, evaluates the RE from left to right, and returns
 *     the first successful match, even if it's not the longest.  This class
 *     follows the Java rules, because there doesn't appear to be a way to
 *     influence Matcher's behavior to choose the longest.  Probably the real solution
 *     is to write a custom regex engine that performs according to TCL rules.
 *
 *   - BRE's are not supported (embedded option 'b' causes PatternSyntaxException)
 *
 *   - ERE's are not supported, unless they are 'ARE'-compatible and are
 *     not explicitly requested with the 'e' embedded option ('e' causes
 *     PatternSyntaxException)
 *
 *   - getInfo(), used by 'regexp -about', doesn't provide flag information.
 *     But the test cases in reg.test that use 'regexp -about', and 
 *     the behavior during 'regexp -about' compile errors is adjusted.
 *
 *   - Some syntax errors that would occur in C TCL don't occur here because
 *     Java is more forgiving of bad RE syntax
 *
 * @author Radoslaw Szulgo (radoslaw@szulgo.pl)
 * @version 1.0, 2009/08/05
 * @author Dan Bodoh (dan.bodoh@gmail.com)
 * @version 1.1, 2010/05/29
 * 
 * @see java.util.regex.Matcher
 * @see java.util.regex.Pattern
 */

public class Regex {
    // note these flags are Octal, just like TCL code

    /** BRE flag (convenience) */
    public static final int TCL_REG_BASIC=    000000;
    /** ERE flag */
    public static final int TCL_REG_EXTENDED= 000001;
    /** advanced features in ARE flag */
    public static final int TCL_REG_ADVF=     000002;
    /** AREs (also EREs) flag */
    public static final int TCL_REG_ADVANCED= TCL_REG_EXTENDED|TCL_REG_ADVF;
    /** regex is a literal flag */
    public static final int TCL_REG_QUOTE=    000004;
    /** ignore case flag */
    public static final int TCL_REG_NOCASE=   000010;
    /** don't care about subexpressions flag */
    public static final int TCL_REG_NOSUB=    000020;
    /** Expanded - comments and whitespace flag */
    public static final int TCL_REG_EXPANDED= 000040;
    /** \n doesn't match . or [^ ] flag */
    public static final int TCL_REG_NLSTOP=   000100;
    /** ^ matches after \n $ before flag */
    public static final int TCL_REG_NLANCH=   000200;
    /** Newlines are line terminators flag */
    public static final int TCL_REG_NEWLINE=  TCL_REG_NLSTOP|TCL_REG_NLANCH;
    /** report details on partial/limited matches flag */
    public static final int TCL_REG_CANMATCH= 001000;

	// Pattern object

	private Pattern pattern;

	// Matcher object

	private Matcher matcher;

	// Flags of Pattern object

	private int flags;

	// Regular Expression string

	private String regexp;

	// Input string

	private String string;

	// Count of matches

	private int count;

	// Offset of the input string

	private int offset;

    // has match() been called at least once?
    
    private boolean matchCalled = false;

    // TCL to Java regex map for character classes, equivalences, collating
    
    private static final Map bracketMap;
    static {
        // initialize bracketMap
        Map br = new HashMap();

        br.put("[:alnum:]","\\p{Alnum}");
        br.put("[:alpha:]","\\p{Alpha}");
        br.put("[:ascii:]","\\p{ASCII}");
        br.put("[:blank:]","\\p{Blank}");
        br.put("[:cntrl:]","\\p{Cntrl}");
        br.put("[:digit:]","\\d");
        br.put("[:graph:]","\\p{Graph}");
        br.put("[:lower:]","\\p{Lower}");
        br.put("[:print:]","\\p{Print}");
        br.put("[:punct:]","\\p{Punct}");
        br.put("[:space:]","\\s");
        br.put("[:upper:]","\\p{Upper}");
        br.put("[:word:]","\\w");
        br.put("[:xdigit:]","\\p{XDigit}");
        br.put("[:<:]","(?=\\w)(?<=\\W|^)");
        br.put("[:>:]","(?=\\W|$)(?<=\\w)");
        br.put("[.\\.]","\\\\"); // mentioned in docs, so special case it
        br.put("[.-.]","\\-"); // mentioned in docs, so special case it
        br.put("[.].]","\\]"); // mentioned in docs, so special case it
        br.put("[.^.]","\\^"); // mentioned in docs, so special case it

        // These aren't in docs, but they are in TCL regex code and
        // in test case reg.test
        br.put("[.NUL.]",		"\\00");
        br.put("[.SOH.]",		"\\001");
        br.put("[.STX.]",		"\\002");
        br.put("[.ETX.]",		"\\003");
        br.put("[.EOT.]",		"\\004");
        br.put("[.ENQ.]",		"\\005");
        br.put("[.ACK.]",		"\\006");
        br.put("[.BEL.]",		"\\007");
        br.put("[.alert.]",		"\\007");
        br.put("[.BS.]",		"\\010");
        br.put("[.backspace.]",	"\\b");
        br.put("[.HT.]",		"\\011");
        br.put("[.tab.]",		"\\t");
        br.put("[.LF.]",		"\\012");
        br.put("[.newline.]",		"\\n");
        br.put("[.VT.]",		"\\013");
        br.put("[.vertical-tab.]",	"\\013");
        br.put("[.FF.]",		"\\014");
        br.put("[.form-feed.]",	"\\f");
        br.put("[.CR.]",		"\\015");
        br.put("[.carriage-return.]",	"\\r");
        br.put("[.SO.]",		"\\016");
        br.put("[.SI.]",		"\\017");
        br.put("[.DLE.]",		"\\020");
        br.put("[.DC1.]",		"\\021");
        br.put("[.DC2.]",		"\\022");
        br.put("[.DC3.]",		"\\023");
        br.put("[.DC4.]",		"\\024");
        br.put("[.NAK.]",		"\\025");
        br.put("[.SYN.]",		"\\026");
        br.put("[.ETB.]",		"\\027");
        br.put("[.CAN.]",		"\\030");
        br.put("[.EM.]",		"\\031");
        br.put("[.SUB.]",		"\\032");
        br.put("[.ESC.]",		"\\033");
        br.put("[.IS4.]",		"\\034");
        br.put("[.FS.]",		"\\034");
        br.put("[.IS3.]",		"\\035");
        br.put("[.GS.]",		"\\035");
        br.put("[.IS2.]",		"\\036");
        br.put("[.RS.]",		"\\036");
        br.put("[.IS1.]",		"\\037");
        br.put("[.US.]",		"\\037");
        br.put("[.space.]",		"\\ ");
        br.put("[.exclamation-mark.]","!");
        br.put("[.quotation-mark.]",	"\"");
        br.put("[.number-sign.]",	"\\#");
        br.put("[.dollar-sign.]",	"\\$");
        br.put("[.percent-sign.]",	"\\%");
        br.put("[.ampersand.]",	"&");
        br.put("[.apostrophe.]",	"\\'");
        br.put("[.left-parenthesis.]","\\(");
        br.put("[.right-parenthesis.]", "\\)");
        br.put("[.asterisk.]",	"\\*");
        br.put("[.plus-sign.]",	"\\+");
        br.put("[.comma.]",		",");
        br.put("[.hyphen.]",		"\\-");
        br.put("[.hyphen-minus.]",	"\\-");
        br.put("[.period.]",		"\\.");
        br.put("[.full-stop.]",	"\\.");
        br.put("[.slash.]",		"/");
        br.put("[.solidus.]",		"/");
        br.put("[.zero.]",		"0");
        br.put("[.one.]",		"1");
        br.put("[.two.]",		"2");
        br.put("[.three.]",		"3");
        br.put("[.four.]",		"4");
        br.put("[.five.]",		"5");
        br.put("[.six.]",		"6");
        br.put("[.seven.]",		"7");
        br.put("[.eight.]",		"8");
        br.put("[.nine.]",		"9");
        br.put("[.colon.]",		"\\:");
        br.put("[.semicolon.]",	";");
        br.put("[.less-than-sign.]",	"<");
        br.put("[.equals-sign.]",	"=");
        br.put("[.greater-than-sign.]", ">");
        br.put("[.question-mark.]",	"\\?");
        br.put("[.commercial-at.]",	"@");
        br.put("[.left-square-bracket.]", "\\[");
        br.put("[.backslash.]",	"\\\\");
        br.put("[.reverse-solidus.]",	"\\\\");
        br.put("[.right-square-bracket.]", "\\]");
        br.put("[.circumflex.]",	"\\^");
        br.put("[.circumflex-accent.]", "\\^");
        br.put("[.underscore.]",	"_");
        br.put("[.low-line.]",	"_");
        br.put("[.grave-accent.]",	"`");
        br.put("[.left-brace.]",	"\\{");
        br.put("[.left-curly-bracket.]", "\\{");
        br.put("[.vertical-line.]",	"|");
        br.put("[.right-brace.]",	"\\}");
        br.put("[.right-curly-bracket.]", "\\}");
        br.put("[.tilde.]",		"~");
        br.put("[.DEL.]",		"\\0177");
        bracketMap = Collections.unmodifiableMap(br);
   }


	/**
	 * Stores params in object and compiles given regexp.
	 * Additional param 'flags' is a bitwise-or of Regex.TCL_REG_* flags
	 * Note that 'flags = flags | TCL_REG_ADVANCED'  internally prior
     * to any processing of embedded options.
	 * 
	 * @param regexp
	 *            TCL-style regular expression
	 * @param string
	 *            input string
	 * @param offset
	 *            offset of the input string where matching starts
	 * @param flags
	 *            Regex.TCL_REG_* flags of pattern object that compiles regexp
	 * @throws PatternSyntaxException
	 *             when there is an error during regexp compilation
	 */

	public Regex(String regexp, String string, int offset, int flags)
			throws PatternSyntaxException {
        
        this.initialize(regexp, string, offset, flags|TCL_REG_ADVANCED);
	}

	/**
	 * Stores params in object and compiles given regexp.
     * Flags are set to TCL_REG_ADVANCED prior
     * to any processing of embedded options.
	 * 
	 * @param regexp
	 *            TCL-style regular expression
	 * @param string
	 *            input string
	 * @param offset
	 *            offset of the input string where matching starts
	 * @throws PatternSyntaxException
	 *             when there is an error during regexp compilation
	 */

	public Regex(String regexp, String string, int offset)
			throws PatternSyntaxException {
        this.initialize(regexp, string, offset, TCL_REG_ADVANCED);
	}

	/**
	 * Stores params in object and compiles given regexp. This constructor
     * is used to support testregexp, which has direct access to flags.
	 * 
	 * @param regexp
	 *            TCL-style regular expression
	 * @param string
	 *            input string
	 * @param offset
	 *            offset of the input string where matching starts
	 * @param flags
	 *            Regex.TCL_REG_* flags of pattern object that compiles regexp
     * @param xflags
     *            Flag string from reg.test (for testregexp)
	 * @throws PatternSyntaxException
	 *             when there is an error during regexp compilation
	 */

	public Regex(String regexp, String string, int offset, int flags, String xflags)
			throws PatternSyntaxException {

        this.flags = TCL_REG_ADVANCED|flags;
        this.parseFlagString(xflags,false);
        this.initialize(regexp, string, offset, this.flags);
    }

    /**
     * Initialize Regex object 
     */
    private void initialize(String regexp, String string, int offset, int flags) 
        throws PatternSyntaxException {

        this.flags = flags;
        this.regexp = regexp;
        this.string = string;
		this.count = 0;

		// Record the offset in the string where a matching op should
		// begin, it is possible that the passed in offset is larger
		// than the actual length of the string.
        this.offset = offset;

		this.pattern = this.compile(regexp);
		this.matcher = pattern.matcher(this.string);
    }

	/**
     * Attempts to match the input string against the regular expression.
     * On the first call, it attempts matching starting at the offset specified in the
     * constructor.  On subsequent calls, it attempts matching where the 
     * previous match succeeded.
	 * 
     * @return true if a match is made, false otherwise
	 */
    public boolean match() {
        boolean found;
        if (matchCalled) {
            found = matcher.find();
        }
        else {
            matchCalled = true;
            int off = this.offset;
            if (off > string.length()) 
                off = string.length();
            found = matcher.find(off);
        }
        if (found) ++this.count;
        return found;
    }

	/**
	 * Replaces the subsequence(s) of the input sequence that match the
	 * pattern with the given TCL-style replacement string.
	 * 
	 * @param tclSubSpec TCL-regsub-style replacement string
     * @param all  If true, all matches are replaced.  If false, only the
     *             first match is replaced
	 * @return The string constructed by replacing the matching
	 *         subsequence(s) by the replacement string, substituting captured
	 *         subsequences as needed
	 */
	public String replace(String tclSubSpec, boolean all) {
		StringBuffer sb = new StringBuffer(string.length());

        boolean found = match();
        String javaSubSpec = null;
        if (found) 
            javaSubSpec = Regex.parseSubSpec(tclSubSpec);

        while (found) {
            matcher.appendReplacement(sb, javaSubSpec);
            found = all && match();  // if all is false, drop out
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

	/**
	 * Returns a list containing information about the regular expression. The
	 * first element of the list is a subexpression count. The second element is
	 * a should be a list of property names that describe various attributes of the regular
	 * expression. Currently, that property name list is empty.
	 * 
	 * Primarily intended for debugging purposes.
	 * 
	 * @param interp
	 *            current Jacl interpreter object
	 * @return A list containing information about the regular expression.
	 * @throws TclException
	 */

	public TclObject getInfo(Interp interp) throws TclException {
		TclObject props = TclList.newInstance();
		String groupCount = String.valueOf(matcher.groupCount());
		int f = pattern.flags();

		TclList.append(interp, props, TclString.newInstance(groupCount));

        // For now, provie an empty list.
        // Perhaps we should emulate TclRegAbout() in the future?
		TclList.append(interp, props, TclString.newInstance(""));

		return props;
	}

	/**
	 *------------------------------------------------------------------------
	 * -----
	 * 
	 * parseSubSpec --
	 * 
	 * Parses the replacement string (subSpec param) which is in Tcl's form.
	 * This method replaces Tcl's '&' and '\N' where 'N' is a number 0-9. to
	 * Java's reference characters. This method also quotes any characters that
	 * have special meaning to Java's regular expression APIs.
	 * 
	 * The replacement string (subSpec param) may contain references to
	 * subsequences captured during the previous match: Each occurrence of $g
	 * will be replaced by the result of evaluating group(g). The first number
	 * after the $ is always treated as part of the group reference. Subsequent
	 * numbers are incorporated into g if they would form a legal group
	 * reference. Only the numerals '0' through '9' are considered as potential
	 * components of the group reference. If the second group matched the string
	 * "foo", for example, then passing the replacement string "$2bar" would
	 * cause "foobar" to be appended to the string buffer. A dollar sign ($) may
	 * be included as a literal in the replacement string by preceding it with a
	 * backslash (\$).
	 * 
	 * Results: None.
	 * 
	 * Side effects: None.
	 * 
	 * @param subSpec
	 *            The replacement string
	 * @return The replacement string in Java's form
	 *         ----------------------------
	 *         -------------------------------------------------
	 */

	protected static String parseSubSpec(String subSpec) {
		boolean escaped = false;

		StringBuilder sb = new StringBuilder();
		final int len = subSpec.length();

		for (int i = 0; i < len; i++) {
			char c = subSpec.charAt(i);

			if (c == '&') {
				// & indicates a whole match spec

				if (escaped) {
					sb.append(c);
					escaped = false;
				} else {
					sb.append("$0");
				}
			} else if (escaped && (c == '0')) {
				// \0 indicates a whole match spec

				escaped = false;
				sb.append("$0");
			} else if (escaped && (c >= '1' && c <= '9')) {
				// \N indicates a sub match spec

				escaped = false;
				sb.append('$');
				sb.append(c);
			} else if (c == '$') {
				// Dollar sign literal in the Tcl subst
				// spec must be escaped so that $0 is
				// not seen as a replacement spec by
				// the Java regexp API

				if (escaped) {
					sb.append("\\\\");
					escaped = false;
				}

				sb.append("\\$");
			} else if (c == '\\') {
				if (escaped) {
					sb.append("\\\\");
					escaped = false;
				} else {
					escaped = true;
				}
			} else {
				if (escaped) {
					// The previous character was an escape, so
					// emit it now before appending this char

					sb.append("\\\\");
					escaped = false;
				}

				sb.append(c);
			}
		}
		if (escaped) {
			// The last character was an escape
			sb.append("\\\\");
		}

		return sb.toString();
	}

	/**
	 * 
	 * @return the number of capturing groups in the last successful match()
	 * @see java.util.regex.Matcher#groupCount()
	 */
	public int groupCount() {
		return matcher.groupCount();
	}

	/**
	 * @return the index of the first character matched 
	 * @see java.util.regex.Matcher#start()
	 */
	public int start() {
		return matcher.start();
	}

	/**
	 * @param group
	 *            The index of a capturing group in this matcher's pattern
	 * @return the start index of the subsequence captured by the given
	 * group during the previous match operation.
	 * @see java.util.regex.Matcher#start(int)
	 */

	public int start(int group) {
		return matcher.start(group);
	}

	/**
	 * @return Returns the index of the last character matched, plus one.
	 * @see java.util.regex.Matcher#end()
	 */
	public int end() {
		return matcher.end();
	}

	/**
	 * @param group
	 *            The index of a capturing group in this matcher's pattern
	 * @return The index of the last character captured by the group, plus one,
	 *         or -1 if the match was successful but the group itself did not
	 *         match anything
	 * @see java.util.regex.Matcher#end(int)
	 */

	public int end(int group) {
		return matcher.end(group);
	}

	/**
	 * @return The (possibly empty) subsequence matched by the previous match,
	 *         in string form.
	 */
	public String group() {
		return matcher.group();
	}

	/**
	 * Returns the input subsequence captured by the given group during the
	 * previous match operation.
	 * 
	 * @param group
	 *            The index of a capturing group in this matcher's pattern
	 * @return The (possibly empty) subsequence captured by the group during the
	 *         previous match, or null if the group failed to match part of the
	 *         input
	 * @see java.util.regex.Matcher#group(int)
	 */

	public String group(int group) {
		return matcher.group(group);
	}


	/**
	 * @return the TCL regexp string
	 */
	String getRegexp() {
		return regexp;
	}

	/**
	 * @return the input string
	 */

	String getString() {
		return string;
	}

	/**
	 * @return the count of correctly matched subsequences of the input string
	 */

	public int getCount() {
		return count;
	}

	/**
	 * @return the offset of the input string
	 */

	public int getOffset() {
		return offset;
	}


    /**
     * @param b string buffer to search for octal or hex digits
     * @param index index to start from
     * @param hex true if search for hex, false for octal
     * @return index+1 of first non-hex (octal) character
     */
    private static int endOfDigits(StringBuffer b, int index, boolean hex) {
        int i;
        for (i=index; i<b.length(); i++) {
            switch (b.charAt(i)) {
                case '0': case '1': case '2': case '3': 
                case '4': case '5': case '6': case '7':
                    continue;

                case '8': case '9': case 'A': case 'a':
                case 'B': case 'b': case 'C': case 'c':
                case 'D': case 'd': case 'E': case 'e':
                case 'F': case 'f': 
                    if (hex) {
                        continue;
                    }
                    else return i;

                default:
                    return i;
            }
        }
        return b.length();
    }

	/**
	 * Return a regexp pattern syntax error message in a format expected by Tcl,
     * primarily to make TCL tests to pass.
     *
     * @param ex A PatternSyntaxException thrown from the Regex constructor
     *
     * @return an error message string that looks like TCL's C implementation
     *
	 */

	public static String getPatternSyntaxMessage(PatternSyntaxException ex) {
		String prefix = "couldn't compile regular expression pattern: ";
		String suffix = null;

		String msg = ex.getMessage();
		int index = ex.getIndex();
		String regexp = ex.getPattern();

		// Either '(' or '[' without a closing ')' or ']'

		if (msg.contains("Unclosed group near")) {
			suffix = "parentheses () not balanced";
		} else if (msg.contains("Unclosed character class near")) {
			suffix = "brackets [] not balanced";
		} else if (msg.contains("Dangling meta character")) {
            suffix = "quantifier operand invalid";
        } else if (msg.contains("Unclosed counted closure")) {
            suffix = "braces {} not balanced";
        }


		if (suffix == null) {
			suffix = ex.getMessage();
		}

		return prefix + suffix;
	}

    /**
     * Convert this.flags to Java's Pattern flags
     *
     * @return flags for Pattern.compile() from this object's flags member
     */
    protected int getJavaFlags() {
        int javaFlags = 0;
        if ((this.flags & TCL_REG_NOCASE) == TCL_REG_NOCASE) {
            javaFlags |= Pattern.CASE_INSENSITIVE;
        } else {
            javaFlags &= ~Pattern.CASE_INSENSITIVE;
        }
        if ((this.flags & TCL_REG_NLSTOP) == TCL_REG_NLSTOP) {
            javaFlags &= ~Pattern.DOTALL;
        } else {
            javaFlags |= Pattern.DOTALL;
        }
        return javaFlags;
    }

    /**
     * Parse up a embedded options string or an xflags string
     * and modify this.flags.
     *
     * @param optionString contains embedded options or xflags
     * @param isEmbed if true, string will be ignored
     *                          if it does not start with (?,
     *                          and anything after ')' will be ignored.
     *
     * @return index+1 of ')' or last character of string
     * @throws PatternSyntaxException if flag string contains unknown or 
     *                                unsupported flags
     */
    protected int parseFlagString(String optionString, boolean isEmbed)
        throws PatternSyntaxException {

        int len = optionString.length();
        boolean validFlag = true;
        boolean has_q = false;
        boolean has_eaxn = false;

        if (isEmbed) {
            if (len < 3) return 0; // can't contain (? and )
            if ((this.flags & TCL_REG_QUOTE)!=0) return 0;
        } else {
            if ( (this.flags & TCL_REG_EXPANDED) != 0) {
                has_eaxn = true;
            }
            if ((this.flags & (TCL_REG_NLSTOP|TCL_REG_NLANCH)) ==
                 (TCL_REG_NLSTOP|TCL_REG_NLANCH)) {
                has_eaxn = true;
            }
        }
        for (int i=0; i<len; i++) {
            char c = optionString.charAt(i);
            if (isEmbed) {
                if (i==0 && c!='(') return 0;
                if (i==1 && c!='?') return 0;
                if (i==2 && c==':') return 0; // not embedded flags
                if (i==2 && c=='=') return 0; // not embedded flags
                if (i>1 && c==')')  return (i+1);
                if (i<2) continue;
            }
            switch (c) {
                case 'a':
                    if (isEmbed) validFlag = false;
                    else {
                        this.flags |= TCL_REG_ADVF;
                        has_eaxn = true;
                    }
                    break;
                case 'b':
                    this.flags &= ~TCL_REG_ADVANCED;
                    break;
                case 'c':
                    this.flags &= ~TCL_REG_NOCASE;
                    break;
                case 'e':
                    this.flags &= ~TCL_REG_ADVANCED;
                    this.flags |=  TCL_REG_EXTENDED;
                    has_eaxn = true;
                    break;
                case 'i':
                    this.flags |= TCL_REG_NOCASE;
                    break;
                case 'n':
                    has_eaxn = true;
                    // fall thru
                case 'm':
                    this.flags |= TCL_REG_NLSTOP|TCL_REG_NLANCH;
                    break;
                case 'p':
                    this.flags |= TCL_REG_NLSTOP;
                    this.flags &= ~TCL_REG_NLANCH;
                    break;
                case 'q':
                    this.flags |= TCL_REG_QUOTE;
                    has_q = true;
                    break;
                case 's':
                    if (isEmbed)
                        this.flags &= ~(TCL_REG_NLSTOP|TCL_REG_NLANCH);
                    // else match BOS only FIXME
                    break;
                case 't':
                    if (isEmbed)
                        this.flags &= ~TCL_REG_EXPANDED;
                    else
                        this.flags |= TCL_REG_CANMATCH;
                    break;
                case 'w':
                    this.flags &= ~TCL_REG_NLSTOP;
                    this.flags |= TCL_REG_NLANCH;
                    break;
                case 'x':
                    this.flags |= TCL_REG_EXPANDED;
                    has_eaxn = true;
                    break;
                case '+':
                case '^':
                case '$':
                    /* These xflags are not supported */
                    if (isEmbed) {
                        validFlag = false;
                        break;
                    }
                    throw new PatternSyntaxException("Unsupported xflag "+c, optionString,0);
                default:
                    if (isEmbed) {
                        validFlag = false;
                    } 
                    // just ignore any xflags we don't understand for now
                    break;
            }
            if (! validFlag) {
                throw new PatternSyntaxException("Unknown flag "+c,optionString,i);
            }
        }
        if (!isEmbed && has_q && has_eaxn) {
            // reg.test thinks these are incompatible, so we can comply
            throw new PatternSyntaxException("Incompatible xflags: q with e, a, x or n",optionString,0);
        }
        return len;
    }


    /**
     * @throws PatternSyntaxException if a flag is unsupported
     */
    protected void testForUnsupportedFlags() throws PatternSyntaxException {
        if ((this.flags & TCL_REG_ADVANCED)==0) 
         throw new PatternSyntaxException("BREs not supported",this.regexp,0);
        if ((this.flags & TCL_REG_ADVANCED)==TCL_REG_EXTENDED) 
         throw new PatternSyntaxException("EREs not supported",this.regexp,0);
        if ((this.flags & TCL_REG_ADVANCED)==TCL_REG_ADVF) 
         throw new PatternSyntaxException("RREs not supported",this.regexp,0);
        if ((this.flags & TCL_REG_CANMATCH)!=0) 
         throw new PatternSyntaxException("TCL_REG_CANMATCH not supported",
            this.regexp,0);
    }

    /**
     * @param c character to test
     * @return true if c is a whitespace character that can be
     *         attributed to a -expanded comment
     */
    private static boolean isCommentWhitespace(StringBuffer sb, int index, boolean inBrackets, int flags) {
        if ((flags & TCL_REG_EXPANDED)==0) return false;
        if (inBrackets) return false;
        if (index >= sb.length()) return false;
        return Character.isWhitespace(sb.charAt(index));
    }
        
	/**
     * Rewrite TCL regex into a Java regex, and compiles it to a Java Pattern.
     * There are some know problems, see Javadoc at top of file
     *
	 * @param tclRegex The TCL regular expression to be compiled
     * @return a Pattern object containing the compiled regex
     * @throws PatternSyntaxException if the expression's syntax is invalid
     *                                 or unsupported
     *
     * @see tcl.lang.Regex
	 */
    protected Pattern compile (String tclRegex) throws PatternSyntaxException {
        StringBuffer regexsb = new StringBuffer(tclRegex);
        int index = 0;
        int bracketExprIndex = -1;
        boolean inBrackets = false;
        Stack groupStack = new Stack();
        Stack openSubExpressions = new Stack();
        Vector validSubExpression = new Vector();

        /***************************************************************
        * possible ***: or ***= at beginning?
        *****************************************************************/
        if (regexsb.length()>=4 && '*' == regexsb.charAt(0) &&
            (this.flags & TCL_REG_QUOTE)!=TCL_REG_QUOTE) {

            String s = regexsb.substring(0,4);
            if ("***:".equals(s)) {
                regexsb.delete(0,4);
                this.flags |= TCL_REG_ADVANCED;
            } else if ("***=".equals(s)) {
                regexsb.delete(0,4);
                this.flags |= TCL_REG_QUOTE;
            }
        }

        /***************************************************************
         * Parse the embedded options, modifying this.flags,
         * and if any exist, remove them
         ***************************************************************/
        int endOfFlags = parseFlagString(regexsb.toString(),true);
        if (endOfFlags > 0) {
            regexsb.delete(0,endOfFlags);
        }

        /** Make sure all the flags that have been specified are actually 
         * supported
         */
        testForUnsupportedFlags();

        /**************************************************************
         * TCL's empty regex appears to match in front of every 
         * character, but not after last character of string like Java's
         * empty regex.  See test case regexpComp-21.10.
         **************************************************************/
        if (regexsb.length() == 0 ) {
            regexsb.insert(0,"^|(?!$)");
            index = regexsb.length();
        }

        /***************************************************************
        * Loop through all characters in regex, processing TCL
        * regex things to Java regex things
        *****************************************************************/
        while ( index < regexsb.length() && ((this.flags & TCL_REG_QUOTE)!=TCL_REG_QUOTE))  {

            /************************************************************** 
             * Strip out (?#) comments
             **************************************************************/
            if (! inBrackets && regexsb.charAt(index)=='(' &&
                (index+3) < regexsb.length() && regexsb.charAt(index+1)=='?' &&
                regexsb.charAt(index+2)=='#') {

                int end=index+3;
                while (end < regexsb.length() && regexsb.charAt(end)!=')') {
                    ++end;
                }
                regexsb.delete(index,end+1);  // delete comment
                ++index;  // go to next character
                continue;
            }

            /****************************************************************
             * Delete -expanded style comments and space.  Java Pattern.COMMENTS
             * and TCL's -expanded are a bit different; TCL doesn't touch
             * anything inside of brackets, while Java does. SO delete
             * comments here, and don't use the Pattern.COMMENTS flag.
             ****************************************************************/
            if (! inBrackets && ((this.flags & TCL_REG_EXPANDED)!=0)) {
                int end = index;
                while (isCommentWhitespace(regexsb, end, inBrackets, this.flags)) {
                    ++end;
                }
                if (end < regexsb.length() && regexsb.charAt(end)=='#') {
                    ++end;
                    while ((end < regexsb.length()) &&
                           regexsb.charAt(end)!='\n') {
                        ++end;
                    }
                    if (end < regexsb.length()) end++;
                } 
                if (end > index) {
                    regexsb.delete(index,end);
                    continue; // go to next regex character
                }
                // else drop through for more processing
            }
            /***************************************************************
             * Count subexpressions
             ***************************************************************/
            if (! inBrackets && regexsb.charAt(index)=='(') { 
                boolean isCaptureGroup;
                if (index+1 < regexsb.length() && regexsb.charAt(index+1)!='?') {
                    /* capturing group */
                    isCaptureGroup = true;
                    /* openSubExpression and validSubExpression are 0-based,
                     * even though back refs are 1-based
                     */
                    openSubExpressions.push(validSubExpression.size());
                    validSubExpression.add(false);
                } else {
                    isCaptureGroup = false;
                }
                groupStack.push(isCaptureGroup);
                
                ++index;
                continue; // next regex char, since we don't do anything with '('
            }
            if (! inBrackets && regexsb.charAt(index)==')') { 
                if (groupStack.empty()) {
                    throw new PatternSyntaxException("Unbalanced parentheses", 
                                                      regexsb.toString(), index);
                }
                boolean isCaptureGroup = (Boolean) groupStack.pop();
                if (isCaptureGroup) {
                    int closedExpr = (Integer) openSubExpressions.pop();
                    validSubExpression.set(closedExpr, true);
                }
                ++index;
                continue; // next regex char, since we don't do anything with ')'
            }

            /***************************************************************
             * A '[' puts us inside a bracket expression
             *****************************************************************/
            if (! inBrackets && '[' == regexsb.charAt(index)) {
                bracketExprIndex = 0;
                inBrackets = true;
                ++index;
                continue; // goto next character of regex
            }

            /***************************************************************
             * See if ] will get us out of bracket expression
             *****************************************************************/
            if (inBrackets) {
                ++bracketExprIndex;
                if (regexsb.charAt(index)==']') {
                    if (bracketExprIndex > 2) {
                        bracketExprIndex = -1;  // end of bracket expr
                        inBrackets = false;
                    } else if (bracketExprIndex==2 && regexsb.charAt(index-1)!='^') {
                        bracketExprIndex = -1;  // end of bracket expr
                        inBrackets = false;
                    }
                    ++index;
                    continue; // goto next character of regex
                }
                // else drop through for more processing
            }

            /**************************************************************
             * TCL's TCL_REG_NLSTOP not exactly like Java's Pattern.DOTALL
             * mode.  Java doesn't stop at [^ ], so it needs to be explicitly
             * added to the (inverted) character class.
             **************************************************************/
            if (bracketExprIndex==1 && (this.flags & TCL_REG_NLSTOP)!=0 &&
                regexsb.charAt(index)=='^') {

                regexsb.insert(index+1,"\\n");
                index += 3; // ^ then \\ then n
                continue;  // continue for more regex processing
            }

            /***************************************************************
             * TCL_REG_NLANCH (-line and -lineanchor) is not exactly like 
             * Java's Pattern.MULTILINE mode.  
             * In Java, ^ will not match after a \n that is the last character 
             * of string,  although TCL does.  So instead of using 
             * Pattern.MULTILINE mode when TCL_REG_NLANCH is requested,
             * the ^ and $ will be converted to TCL_REG_NLANCH-like expressions.
             *****************************************************************/
            if (! inBrackets && 
                    ((this.flags & TCL_REG_NLANCH)==TCL_REG_NLANCH)) {
                String replace = null;
                if ('^' == regexsb.charAt(index)) {
                    // match at beginning of string or when prev
                    // character is a newline
                    if (this.offset==0)
                        replace = "(?:^|(?<=\\n))";
                    else
                        // don't match at beginning - see below
                        replace = "(?<=\\n)";
                } 
                if ('$' == regexsb.charAt(index)) {
                    // match at end of string or when next
                    // character is a newline
                    replace = "(?:$|(?=\\n))";
                }
                if (replace != null) {
                    regexsb.replace(index,index+1,replace);
                    index+=replace.length(); // skip replacement
                    continue; // go to next character of regex
                }
                // else fall through for more regex processing
            } 
            /** TCL doesn't allow '^' to match at any offset > 0.  Java
             * doesn't either, except when the string is zero-length.
             * If offset > 0, we'll just replace '^' with a zero-length
             * lookahead sequence that can't match.  Note that
             * the TCL_REG_NLANCH case is taken care of above.
             */
            if (this.offset > 0 && 
                ! inBrackets && 
                regexsb.charAt(index)=='^' &&
                ((this.flags & TCL_REG_NLANCH)==0)) {

                String replace = "(?=_)(?!_)";  // pos and neg lookahead for '_' can't match
                regexsb.replace(index, index+1, replace);
                index += replace.length();
                continue; // to next regex character
            }
            
            /***************************************************************
             * convert [: :], [. .]  [==]
             * Since '[' are caught above, this '[' must already be inside of
             * a bracket expression
             *****************************************************************/
            if ('[' == regexsb.charAt(index)) {
                char c2;
                if (index+1 < regexsb.length())
                    c2 = regexsb.charAt(index+1);
                else c2='?';  // just make next test fail

                if (c2 == ':' || c2=='.' || c2=='=') {
                    if (! inBrackets) {
                        throw new PatternSyntaxException(
                                        " [::] [..] [==] not allowed outside of a bracket expression",
                                        regexsb.toString(),index);
                    }
                    StringBuilder charClass = new StringBuilder("[");
                    charClass.append(c2);
                    // suck up everything into charClass until next ']'
                    int i = index+2;
                    while (i < regexsb.length()) {
                        char c = regexsb.charAt(i);
                        charClass.append(c);
                        if (c==']') break;
                        i++;
                    }
                    // replacements are stored in 'bracketMap' Hashtable
                    String replacement = (String)bracketMap.get(charClass.toString());

                    /* collapse single-char collating and equivalence into character class */
                    /* Those characters needing escape are part of bracketMap */
                    if (replacement==null && (c2=='.' || c2=='=') &&
                        charClass.length()==5) {
                        replacement = charClass.substring(2,3);
                    }
                    if (replacement != null) {
                        // special case [[:<:]] and [[:>:]] - must get rid of outside brackets
                        if (c2==':' && (regexsb.charAt(index+2)=='>' ||
                                        regexsb.charAt(index+2)=='<')) {
                            --index;
                            regexsb.replace(index,index+7, replacement);
                        } else {
                            regexsb.replace(index, index+charClass.length(), replacement);
                        }
                        
                        index += replacement.length();
                        
                    } else {
                        throw new PatternSyntaxException(charClass.toString() + 
                                        " not supported",regexsb.toString(),index);
                    }
                    continue; // goto next regex character
                } else {
                    // next character was not : . or =, let's assume we have to
                    // it's a literal [ and escape it
                    regexsb.insert(index,'\\');
                    index += 2;  // skip new \ and [
                    continue;  // goto next regex character
                }
                // else it's not a [ , so drop through and continue processing
            }


            /***************************************************************
             * Escape literal braces
             *****************************************************************/
            if ('{' == regexsb.charAt(index) ) {
                // does it look like it's not a bounds?
                ++index;
                // may as well get rid of comment whitespace while we are searching
                while (isCommentWhitespace(regexsb, index, inBrackets, this.flags))
                    regexsb.delete(index,index+1);

                if (index >= regexsb.length() ||
                    ! Character.isDigit(regexsb.charAt(index))) {

                    regexsb.insert(index-1,"\\");  //escape it
                    ++index;
                }
                continue; // goto next regex character
            }

            /***************************************************************
             * Convert TCL escapes that don't exist in Java to Java equivalent
             *****************************************************************/
            if ('\\' == regexsb.charAt(index)) {
                ++index; // point to escaped character
                String escapeReplacement = null;

                // for speed, escapes are replaced with a 'switch' and hardcoded replacements, 
                // instead of using a Hashtable or some such cleaner looking method

                switch (regexsb.charAt(index)) {
                
                case '0':
                    // \0 -> \00,  \0y -> \00y, \0yz -> \00yz
                    escapeReplacement = "00";
                    break;
                case '1': case '2': case '3': 
                case '4': case '5': case '6': case '7': 
                    // Is this a back reference or an octal?
                    int endOctal = endOfDigits(regexsb, index, false);
                    int octalLen = endOctal - index;

                    escapeReplacement = null;
                    // always a back ref if it's length 1 and not a '0'
                    int value = Integer.parseInt(regexsb.substring(index,endOctal),8);
                    if (value > validSubExpression.size()) {
                        if (octalLen >= 2) {
                            escapeReplacement = "0" + regexsb.substring(index,index+1);
                         } else {
                             throw new PatternSyntaxException("invalid backreference number",
                                                             regexsb.toString(), index);
                         }
                    } else {
                        boolean isValid = (Boolean) validSubExpression.get(value - 1);
                        if (! isValid) {
                            // not in Docs, but test case reg-14.18 and reg-15.9 do this
                            throw new PatternSyntaxException("invalid backreference number",
                                                         regexsb.toString(), index);
                         }
                    }
                    if (escapeReplacement==null) {
                        index += octalLen;
                    }
                    break;
                case 'A':
                    // Java's \A only matches exactly at the beginning of the string, at
                    // offset 0.  Fortunately, Java's \G acts like TCL's \A
                    escapeReplacement = "G";
                    break;
                case 'B':
                    escapeReplacement = "\\";
                    break;
                case 'b':
                    escapeReplacement = "x08";
                    break;
                case 'c':
                    // Java \ch != \cH, but TCL is
                    ++index;
                    if ( index < regexsb.length()) {
                        regexsb.setCharAt(index,Character.toUpperCase(regexsb.charAt(index)));
                        ++index;
                    } else {
                        escapeReplacement = null;
                    }
                    break;
                case 'M':
                    // \M -> (?=\W|$)(?<=\w) (look ahead for a \W, behind for \w)
                    if (inBrackets) throw new 
                        PatternSyntaxException("\\M illegal within a bracket expression",
                                               regexsb.toString(), index);
                    escapeReplacement = (String)bracketMap.get("[:>:]");
                    --index;  
                    regexsb.delete(index,index+1); // delete the backslash
                    break;
                case 'm':
                    // \m -> (?=\w)(?<=\W|^) (look ahead for a \w, behind for \W)
                    if (inBrackets) throw new 
                        PatternSyntaxException("\\m illegal within a bracket expression",
                                               regexsb.toString(), index);
                    escapeReplacement = (String)bracketMap.get("[:<:]");
                    --index;  
                    regexsb.delete(index,index+1); // delete the backslash
                    break;
                case 'U':
                    // \Ustuvwxyz -> \\uwxyz if 16 bits
                    if ("0000".equals(regexsb.substring(index+1,index+5))) {
                        regexsb.setCharAt(index,'u');
                        regexsb.delete(index+1, index+5);
                        index += 4;  // skip past original lower 16-bits of unicode value
                        escapeReplacement = null; // don't use escapeReplacement
                    } else {
                        throw new PatternSyntaxException("32-bit unicode value not supported",
                                               regexsb.toString(), index);
                    }
                    break;

                case 'v':
                    escapeReplacement = "x0b";
                    break;

                case 'x':
                    // \xhhh -> \xhh (TCL can have any number of hex digits, Java has 2)
                    ++index;  // point at first hex digit
                    int endHex = endOfDigits(regexsb, index, true);
                    int hexLen = endHex - index;
                    if (hexLen==0 || hexLen==2) {
                        // do nothing, skip past hex digits
                        index += hexLen;
                    } else if (hexLen==1) {
                        // insert a leading 0
                        regexsb.insert(index,'0');
                        index += 2; // skip past inserted '0' and next digit
                    } else {
                        // lop off all but last two hex digits
                        regexsb.delete(index,endHex-2);
                        index += 2;  // and skip those two digits
                    }
                    escapeReplacement = null; // don't use replacement
                    break;
    
                case 'Y':
                    // \Y -> \b
                    if (inBrackets) throw new 
                        PatternSyntaxException("\\Y illegal within a bracket expression",
                                               regexsb.toString(), index);
                    escapeReplacement = "B";
                    break;

                case 'y':
                    // \y -> \b
                    if (inBrackets) throw new 
                        PatternSyntaxException("\\y illegal within a bracket expression",
                                               regexsb.toString(), index);
                    escapeReplacement = "b";
                    break;

                case 'Z':
                    // Java's \Z only matches exactly at the beginning of the if there,
                    // is no preceding \n; but Java's \z acts like TCL's \Z
                    escapeReplacement = "z";
                    break;
                case 'p':
                case 'G':
                case 'z':
                case 'Q':
                case 'E':
                    // These escapes conflict with Java escapes, and are illegal anyway in TCL
                    throw new PatternSyntaxException("Illegal escape",regexsb.toString(), index);

                default:
                    escapeReplacement = null;
                    index++;  //skip past this escaped character that we aren't going to touch
                    break;
                }

                if (escapeReplacement != null) {
                    regexsb.replace(index,index+1,escapeReplacement);
                    index += escapeReplacement.length();  // skip past replacement
                }
                continue; // go to next character in regex
            }

            // if we got here, we are ignoring this character.   So increment
            // index and go onto next regex character
            ++index;  
        }

        // insert Java escape around regex if it was quoted
        if ((this.flags & TCL_REG_QUOTE) == TCL_REG_QUOTE) {
            regexsb.insert(0,"\\Q");
            regexsb.append("\\E");
        }
        //System.out.println("In : " + tclRegex + "\nOut: " + regexsb.toString());
        return Pattern.compile(regexsb.toString(), getJavaFlags());
    }

} // end of class Regex

