/*
 * Expression.java
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: Expression.java,v 1.40 2009/07/10 14:22:00 rszulgo Exp $
 *
 */

package tcl.lang;

import java.util.Date;
import java.util.HashMap;

/**
 * This class handles Tcl expressions.
 */
public class Expression {

	// The token types are defined below. In addition, there is a
	// table associating a precedence with each operator. The order
	// of types is important. Consult the code before changing it.

	public static final int VALUE = 0;
	public static final int OPEN_PAREN = 1;
	public static final int CLOSE_PAREN = 2;
	public static final int COMMA = 3;
	public static final int END = 4;
	public static final int UNKNOWN = 5;

	// Binary operators:

	public static final int MULT = 8;
	public static final int DIVIDE = 9;
	public static final int MOD = 10;
	public static final int PLUS = 11;
	public static final int MINUS = 12;
	public static final int LEFT_SHIFT = 13;
	public static final int RIGHT_SHIFT = 14;
	public static final int LESS = 15;
	public static final int GREATER = 16;
	public static final int LEQ = 17;
	public static final int GEQ = 18;
	public static final int EQUAL = 19;
	public static final int NEQ = 20;
	public static final int BIT_AND = 21;
	public static final int BIT_XOR = 22;
	public static final int BIT_OR = 23;
	public static final int AND = 24;
	public static final int OR = 25;
	public static final int QUESTY = 26;
	public static final int COLON = 27;
	public static final int STREQ = 28;
	public static final int STRNEQ = 29;

	// 8.5 ni and in
	public static final int IN = 30;
	public static final int NI = 31;

	// Unary operators:

	public static final int UNARY_MINUS = 32;
	public static final int UNARY_PLUS = 33;
	public static final int NOT = 34;
	public static final int BIT_NOT = 35;




	/**
	 * Precedence table.  The values for non-operator token types are ignored.
	 */
	public static int precTable[] = { 0, 0, 0, 0, 0, 0, 0, 0, 12, 12, 12, // MULT,
																			// DIVIDE,
																			// MOD
			11, 11, // PLUS, MINUS
			10, 10, // LEFT_SHIFT, RIGHT_SHIFT
			9, 9, 9, 9, // LESS, GREATER, LEQ, GEQ
			8, 8, // EQUAL, NEQ
			7, // BIT_AND
			6, // BIT_XOR
			5, // BIT_OR
			4, // AND
			3, // OR
			2, // QUESTY
			1, // COLON
			8, 8, 8, 8, // STREQ, STRNEQ, IN, NI
			13, 13, 13, 13, // UNARY_MINUS, UNARY_PLUS, NOT, BIT_NOT
	};

	/**
	 *  Mapping from operator numbers to strings; used for error messages.
	 */
	public static String operatorStrings[] = { "VALUE", "(", ")", ",", "END",
			"UNKNOWN", "6", "7", "*", "/", "%", "+", "-", "<<", ">>", "<", ">",
			"<=", ">=", "==", "!=", "&", "^", "|", "&&", "||", "?", ":", "eq",
			"ne", "in", "ni", "-", "+", "!", "~",};

	/**
	 * Maps name of function to its implementation
	 */
	public HashMap<String, MathFunction> mathFuncTable;

	/**
	 * The entire expression, as originally passed to eval et al.
	 */
	private String m_expr;

	/**
	 * Length of the expression.
	 */
	private int m_len;

	/**
	 * Type of the last token to be parsed from the expression. Corresponds to
	 * the characters just before expr.
	 */
	int m_token;

	/**
	 * Position to the next character to be scanned from the expression string.
	 */
	private int m_ind;

	/**
	 * Cache of ExprValue objects. These are cached on a per-interp basis to
	 * speed up most expressions.
	 */
	private ExprValue[] cachedExprValue;
	private int cachedExprIndex = 0;
	private static final int cachedExprLength = 50;

	/**
	 * Evaluate a Tcl expression and set the interp result to the value.
	 * 
	 * @param interp
	 *            the context in which to evaluate the expression.
	 * @param string
	 *            expression to evaluate.
	 * @exception TclException
	 *                for malformed expressions.
	 */

	public void evalSetResult(Interp interp, String string) throws TclException {
		ExprValue value = ExprTopLevel(interp, string);
		switch (value.getType()) {
		case ExprValue.INT:
			interp.setResult(value.getIntValue());
			break;
		case ExprValue.DOUBLE:
			interp.setResult(value.getDoubleValue());
			break;
		case ExprValue.STRING:
			interp.setResult(value.getStringValue());
			break;
		default:
			throw new TclRuntimeError("internal error: expression, unknown");
		}
		releaseExprValue(value);
		return;
	}

	/**
	 * Evaluate an Tcl expression.
	 * 
	 * @param interp
	 *            the context in which to evaluate the expression.
	 * @param string
	 *            expression to evaluate.
	 * @exception TclException
	 *                for malformed expressions.
	 * @return the value of the expression in boolean.
	 */
	public boolean evalBoolean(Interp interp, String string)
			throws TclException {
		ExprValue value = ExprTopLevel(interp, string);
		boolean b = value.getBooleanValue(interp);
		releaseExprValue(value);
		return b;
	}

	/**
	 * Constructor.
	 */
	public Expression() {
		mathFuncTable = new HashMap<>();

		// rand -- needs testing
		// srand -- needs testing
		// hypot -- needs testing
		// fmod -- needs testing
		// try [expr fmod(4.67, 2.2)]
		// the answer should be .27, but I got .2699999999999996

		registerMathFunction("atan2", new Atan2Function());
		registerMathFunction("pow", new PowFunction());
		registerMathFunction("acos", new AcosFunction());
		registerMathFunction("asin", new AsinFunction());
		registerMathFunction("atan", new AtanFunction());
		registerMathFunction("ceil", new CeilFunction());
		registerMathFunction("cos", new CosFunction());
		registerMathFunction("cosh", new CoshFunction());
		registerMathFunction("exp", new ExpFunction());
		registerMathFunction("floor", new FloorFunction());
		registerMathFunction("fmod", new FmodFunction());
		registerMathFunction("hypot", new HypotFunction());
		registerMathFunction("log", new LogFunction());
		registerMathFunction("log10", new Log10Function());
		registerMathFunction("rand", new RandFunction());
		registerMathFunction("sin", new SinFunction());
		registerMathFunction("sinh", new SinhFunction());
		registerMathFunction("sqrt", new SqrtFunction());
		registerMathFunction("srand", new SrandFunction());
		registerMathFunction("tan", new TanFunction());
		registerMathFunction("tanh", new TanhFunction());

		registerMathFunction("abs", new AbsFunction());
		registerMathFunction("double", new DoubleFunction());
		registerMathFunction("int", new IntFunction());
		registerMathFunction("wide", new WideFunction());
		registerMathFunction("round", new RoundFunction());

		m_expr = null;
		m_ind = 0;
		m_len = 0;
		m_token = UNKNOWN;

		cachedExprValue = new ExprValue[cachedExprLength];
		for (int i = 0; i < cachedExprLength; i++) {
			cachedExprValue[i] = new ExprValue(0, null);
		}
	}

	/**
	 * Provides top-level functionality shared by procedures like ExprInt,
	 * ExprDouble, etc.
	 * 
	 * @param interp
	 *            the context in which to evaluate the expression.
	 * @param string
	 *            the expression.
	 * @exception TclException
	 *                for malformed expressions.
	 * @return the value of the expression.
	 */
	private final ExprValue ExprTopLevel(Interp interp, String string)
			throws TclException {

		// Saved the state variables so that recursive calls to expr
		// can work:
		// expr {[expr 1+2] + 3}

		String m_expr_saved = m_expr;
		int m_len_saved = m_len;
		int m_token_saved = m_token;
		int m_ind_saved = m_ind;

		try {
			m_expr = string;
			m_ind = 0;
			m_len = string.length();
			m_token = UNKNOWN;

			ExprValue val = ExprGetValue(interp, -1);
			if (m_token != END) {
				SyntaxError(interp);
			}
			return val;
		} finally {
			m_expr = m_expr_saved;
			m_len = m_len_saved;
			m_token = m_token_saved;
			m_ind = m_ind_saved;
		}
	}

	static void IllegalType(Interp interp, int badType, int operator)
			throws TclException {
		throw new TclException(interp, "can't use "
				+ ((badType == ExprValue.DOUBLE) ? "floating-point value"
						: "non-numeric string") + " as operand of \""
				+ operatorStrings[operator] + "\"");
	}

	void SyntaxError(Interp interp) throws TclException {
		throw new TclException(interp, "syntax error in expression \"" + m_expr
				+ "\"");
	}
	
	void SyntaxError(Interp interp, String whyMsg) throws TclException {
		throw new TclException(interp, "syntax error in expression \"" + m_expr
				+ "\": "+whyMsg);
	}

	static void DivideByZero(Interp interp) throws TclException {
		interp.setErrorCode(TclString
				.newInstance("ARITH DIVZERO {divide by zero}"));
		throw new TclException(interp, "divide by zero");
	}

	public static void IntegerTooLarge(Interp interp) throws TclException {
		interp
				.setErrorCode(TclString
						.newInstance("ARITH IOVERFLOW {integer value too large to represent}"));
		throw new TclException(interp, "integer value too large to represent");
	}

	static void DoubleTooLarge(Interp interp) throws TclException {
		interp
				.setErrorCode(TclString
						.newInstance("ARITH OVERFLOW {floating-point value too large to represent}"));
		throw new TclException(interp,
				"floating-point value too large to represent");
	}

	static void DoubleTooSmall(Interp interp) throws TclException {
		interp
				.setErrorCode(TclString
						.newInstance("ARITH UNDERFLOW {floating-point value too small to represent}"));
		throw new TclException(interp,
				"floating-point value too small to represent");
	}

	static void DomainError(Interp interp) throws TclException {
		interp
				.setErrorCode(TclString
						.newInstance("ARITH DOMAIN {domain error: argument not in valid range}"));
		throw new TclException(interp,
				"domain error: argument not in valid range");
	}

	static void EmptyStringOperandError(Interp interp, int operator)
			throws TclException {
		throw new TclException(interp, "can't use " + "empty string"
				+ " as operand of \"" + operatorStrings[operator] + "\"");
	}

	/**
	 * Given a TclObject, such as the result of a command or variable
	 * evaluation, fill in a ExprValue with the parsed result. If the TclObject
	 * already has an internal rep that is a numeric type, then no need to parse
	 * from the string rep. If the string rep is parsed into a numeric type,
	 * then update the internal rep of the object to the parsed value.
	 */

	public static void ExprParseObject(final Interp interp,
			final TclObject obj, final ExprValue value) throws TclException {
		// If the TclObject already has an integer, boolean,
		// or floating point internal representation, use it.

		if (obj.isIntType()) {
			// A TclObject is a "pure" number if it
			// was created from a primitive type and
			// has no string rep. Pass the string rep
			// along in the ExprValue object if there
			// is one.

			value.setIntValue(obj.ivalue, // Inline TclInteger.get()
					(obj.hasNoStringRep() ? null : obj.toString()));
			return;
		} else if (obj.isDoubleType()) {
			// A TclObject with a double internal rep will
			// never have a string rep that could be parsed
			// as an integer.

			value.setDoubleValue(
			// Inline TclDouble.get()
					((TclDouble) obj.getInternalRep()).value, (obj
							.hasNoStringRep() ? null : obj.toString()));
			return;
		}

		// Otherwise, try to parse a numeric value from the
		// object's string rep.

		ExprParseString(interp, obj, value);

		return;
	}

	/**
	 * TclParseNumber -> ExprParseString
	 * 
	 * Given a TclObject that contains a String to be parsed (from a command or
	 * variable subst), fill in an ExprValue based on the string's numeric
	 * value. The value may be a floating-point, an integer, or a string. If the
	 * string value is converted to a numeric value, then update the internal
	 * rep of the TclObject.
	 * 
	 * @param interp
	 *            the context in which to evaluate the expression.
	 * @param obj
	 *            the TclObject containing the string to parse.
	 * @param value
	 *            the ExprValue object to save the parsed value in.
	 */

	public static void ExprParseString(final Interp interp,
			final TclObject obj, final ExprValue value) {
		char c;
		long ival;
		double dval;
		final String s = obj.toString();
		final int len = s.length();

		// System.out.println("now to ExprParseString ->" + s +
		// "<- of length " + len);

		switch (len) {
		case 0: {
			// Take shortcut when string is of length 0, as the
			// empty string can't represent an int, double, or boolean.

			value.setStringValue("");
			return;
		}
		case 1: {
			// Check for really common strings of length 1
			// that we know will be integers.

			c = s.charAt(0);
			switch (c) {
			case '0':
			case '1':
			case '2':
			case '3':
			case '4':
			case '5':
			case '6':
			case '7':
			case '8':
			case '9':
				ival = (long) (c - '0');
				value.setIntValue(ival, s);
				TclInteger.exprSetInternalRep(obj, ival);
				return;
			default:
				// We know this string can't be parsed
				// as a number, so just treat it as
				// a string. A string of length 1 is
				// very common.

				value.setStringValue(s);
				return;
			}
		}
		case 2: {
			// Check for really common strings of length 2
			// that we know will be integers.

			c = s.charAt(0);
			if (c == '-') {
				c = s.charAt(1);
				switch (c) {
				case '0':
				case '1':
				case '2':
				case '3':
				case '4':
				case '5':
				case '6':
				case '7':
				case '8':
				case '9':
					ival = (long) -(c - '0');
					value.setIntValue(ival, s);
					TclInteger.exprSetInternalRep(obj, ival);
					return;
				}
			}
			break;
		}
		case 3: {
			// Check for really common strings of length 3
			// that we know will be doubles.

			c = s.charAt(1);
			if (c == '.') {
				switch (s) {
					case "0.0":
						dval = 0.0;
						value.setDoubleValue(dval, s);
						TclDouble.exprSetInternalRep(obj, dval);
						return;
					case "0.5":
						dval = 0.5;
						value.setDoubleValue(dval, s);
						TclDouble.exprSetInternalRep(obj, dval);
						return;
					case "1.0":
						dval = 1.0;
						value.setDoubleValue(dval, s);
						TclDouble.exprSetInternalRep(obj, dval);
						return;
					case "2.0":
						dval = 2.0;
						value.setDoubleValue(dval, s);
						TclDouble.exprSetInternalRep(obj, dval);
						return;
				}
			}
			break;
		}
		case 4: {
			if (s.equals("true")) {
				value.setStringValue(s);
				return;
			}
			break;
		}
		case 5: {
			if (s.equals("false")) {
				value.setStringValue(s);
				return;
			}
			break;
		}
		}

		if (looksLikeInt(s, len, 0, false)) {
			// System.out.println("string looks like an int");

			// Note: the Util.strtoul() method handles 32bit unsigned values
			// as well as leading sign characters.

			StrtoulResult res = interp.strtoulResult;
			Util.strtoul(s, 0, 0, res);
			// String token = s.substring(i, res.index);
			// System.out.println("token string from strtoul is \"" + token +
			// "\"");
			// System.out.println("res.errno is " + res.errno);

			if (res.errno == 0) {
				// We treat this string as a number if all the charcters
				// following the parsed number are a whitespace chars.
				// E.g.: " 1", "1", "1 ", and " 1 " are all good numbers

				boolean trailing_blanks = true;

				for (int i = res.index; i < len; i++) {
					if ((c = s.charAt(i)) != ' ' && !Character.isWhitespace(c)) {
						trailing_blanks = false;
						break;
					}
				}

				if (trailing_blanks) {
					ival = res.value;
					// System.out.println("string is an Integer of value " +
					// ival);
					value.setIntValue(ival, s);
					TclInteger.exprSetInternalRep(obj, ival);
					return;
				} else {
					// System.out.println("string failed trailing_blanks test, not an integer");
				}
			}
		} else {
			// System.out.println("string does not look like an int, checking for Double");

			StrtodResult res = interp.strtodResult;
			Util.strtod(s, 0, len, res);

			if (res.errno == 0) {
				// Trailing whitespaces are treated just like the Integer case

				boolean trailing_blanks = true;

				for (int i = res.index; i < len; i++) {
					if ((c = s.charAt(i)) != ' ' && !Character.isWhitespace(c)) {
						trailing_blanks = false;
						break;
					}
				}

				if (trailing_blanks) {
					dval = res.value;
					// System.out.println("string is a Double of value " +
					// dval);
					value.setDoubleValue(dval, s);
					TclDouble.exprSetInternalRep(obj, dval);
					return;
				}

			}
		}

		// System.out.println("string is not a valid number, returning as string \""
		// + s + "\"");

		// Not a valid number. Save a string value (but don't do anything
		// if it's already the value).

		value.setStringValue(s);
		return;
	}

	/**
	 * Parse a "value" from the remainder of the expression.
	 * 
	 * @param interp
	 *            the context in which to evaluate the expression.
	 * @param prec
	 *            treat any un-parenthesized operator with precedence <= this as
	 *            the end of the expression.
	 * @exception TclException
	 *                for malformed expressions.
	 * @return the value of the expression.
	 */
	private ExprValue ExprGetValue(Interp interp, int prec) throws TclException {
		int operator;
		boolean gotOp = false; // True means already lexed the
		// operator (while picking up value
		// for unary operator). Don't lex
		// again.
		ExprValue value, value2 = null;

		// There are two phases to this procedure. First, pick off an
		// initial value. Then, parse (binary operator, value) pairs
		// until done.

		value = ExprLex(interp);

		if (m_token == OPEN_PAREN) {

			// Parenthesized sub-expression.

			value = ExprGetValue(interp, -1);
			if (m_token != CLOSE_PAREN) {
				SyntaxError(interp,"looking for close parenthesis");
			}
		} else {
			if (m_token == MINUS) {
				m_token = UNARY_MINUS;
			}
			if (m_token == PLUS) {
				m_token = UNARY_PLUS;
			}
			if (m_token >= UNARY_MINUS) {

				// Process unary operators.

				operator = m_token;
				value = ExprGetValue(interp, precTable[m_token]);

				if (interp.noEval == 0) {
					evalUnaryOperator(interp, operator, value);
				}
				gotOp = true;
			} else if (m_token == CLOSE_PAREN) {
				// Caller needs to deal with close paren token.
				return null;
			} else if (m_token != VALUE) {
				if (m_token >= MULT)
					SyntaxError(interp,"unexpected operator "+operatorStrings[m_token]);
				else if (m_token == UNKNOWN) 
					SyntaxError(interp,"character not legal in expressions");
				else
					SyntaxError(interp,"premature end of expression");
			}
		}
		if (value == null) {
			SyntaxError(interp);
		}

		
		// Got the first operand. Now fetch (operator, operand) pairs.

		if (!gotOp) {
			value2 = ExprLex(interp);
		}

		while (true) {
			operator = m_token;
			if ((operator < MULT) || (operator >= UNARY_MINUS)) {
				if ((operator == END) || (operator == CLOSE_PAREN)
						|| (operator == COMMA)) {
					return value; // Goto Done
				} else {
					SyntaxError(interp,"extra tokens at end of expression");
				}
			}
			if (precTable[operator] <= prec) {
				return value; // (goto done)
			}

			// If we're doing an AND or OR and the first operand already
			// determines the result, don't execute anything in the
			// second operand: just parse. Same style for ?: pairs.

			if ((operator == AND) || (operator == OR) || (operator == QUESTY)) {

				if (value.isDoubleType()) {
					value.setIntValue(value.getDoubleValue() != 0.0);
				} else if (value.isStringType()) {
					try {
						boolean b = Util.getBoolean(interp, value
								.getStringValue());
						value.setIntValue(b);
					} catch (TclException e) {
						if (interp.noEval == 0) {
							throw e;
						}

						// Must set value.intValue to avoid referencing
						// uninitialized memory in the "if" below; the actual
						// value doesn't matter, since it will be ignored.

						value.setIntValue(0);
					}
				}
				if (((operator == AND) && (value.getIntValue() == 0))
						|| ((operator == OR) && (value.getIntValue() != 0))) {
					interp.noEval++;
					try {
						value2 = ExprGetValue(interp, precTable[operator]);
					} finally {
						interp.noEval--;
					}
					if (operator == OR) {
						value.setIntValue(1);
					}
					continue;
				} else if (operator == QUESTY) {
					// Special note: ?: operators must associate right to
					// left. To make this happen, use a precedence one lower
					// than QUESTY when calling ExprGetValue recursively.

					if (value.getIntValue() != 0) {
						value = ExprGetValue(interp, precTable[QUESTY] - 1);
						if (m_token != COLON) {
							SyntaxError(interp);
						}

						interp.noEval++;
						try {
							value2 = ExprGetValue(interp, precTable[QUESTY] - 1);
						} finally {
							interp.noEval--;
						}
					} else {
						interp.noEval++;
						try {
							value2 = ExprGetValue(interp, precTable[QUESTY] - 1);
						} finally {
							interp.noEval--;
						}
						if (m_token != COLON) {
							SyntaxError(interp);
						}
						value = ExprGetValue(interp, precTable[QUESTY] - 1);
					}
					continue;
				} else {
					value2 = ExprGetValue(interp, precTable[operator]);
				}
			} else {
				value2 = ExprGetValue(interp, precTable[operator]);
			}

			if (value2 == null) {
				SyntaxError(interp);
			}

			if ((m_token < MULT) && (m_token != VALUE) && (m_token != END)
					&& (m_token != COMMA) && (m_token != CLOSE_PAREN)) {
				SyntaxError(interp);
			}

			if (interp.noEval != 0) {
				continue;
			}

			if (operator == COLON) {
				SyntaxError(interp);
			}
			evalBinaryOperator(interp, operator, value, value2);
			releaseExprValue(value2);
		} // end of while(true) loop
	}

	// Evaluate the result of a unary operator ( - + ! ~)
	// when it is applied to a value. The passed in value
	// contains the result.

	public static void evalUnaryOperator(final Interp interp,
			final int operator, final ExprValue value) throws TclException {
		switch (operator) {
		case UNARY_MINUS:
			if (value.isIntType()) {
				value.setIntValue(value.getIntValue() * -1);
			} else if (value.isDoubleType()) {
				value.setDoubleValue(value.getDoubleValue() * -1.0);
			} else {
				IllegalType(interp, value.getType(), operator);
			}
			break;
		case UNARY_PLUS:
			// Unary + operator raises an error for a String,
			// otherwise it tosses out the string rep.

			if (value.isIntOrDoubleType()) {
				value.nullStringValue();
			} else {
				IllegalType(interp, value.getType(), operator);
			}
			break;
		case NOT:
			if (value.isIntType()) {
				// Inlined method does not reset type to INT
				value.optIntUnaryNot();
			} else if (value.isDoubleType()) {
				value.setIntValue(value.getDoubleValue() == 0.0);
			} else if (value.isStringType()) {
				String s = value.getStringValue();
				int s_len = s.length();
				if (s_len == 0) {
					EmptyStringOperandError(interp, operator);
				}
				String tok = getBooleanToken(s);
				// Reject a string like "truea"
				if (tok != null && tok.length() == s_len) {
					if ("true".startsWith(tok) || "on".startsWith(tok)
							|| "yes".startsWith(tok)) {
						value.setIntValue(0);
					} else {
						value.setIntValue(1);
					}
				} else {
					IllegalType(interp, value.getType(), operator);
				}
			} else {
				IllegalType(interp, value.getType(), operator);
			}
			break;
		case BIT_NOT:
			if (value.isIntType()) {
				value.setIntValue(~value.getIntValue());
			} else {
				IllegalType(interp, value.getType(), operator);
			}
			break;
		default:
			throw new TclException(interp, "unknown operator in expression");
		}
	}

	// Evaluate the result of a binary operator (* / + - % << >> ...)
	// when applied to a pair of values. The result is returned in
	// the first (left hand) value. This method will check data
	// types and perform conversions if needed before executing
	// the operation. The value2 argument (right hand) value will
	// not be released, the caller should release it.

	public static void evalBinaryOperator(final Interp interp,
			final int operator, final ExprValue value, // value on left hand
														// side
			final ExprValue value2) // value on right hand side
			throws TclException {
		final boolean USE_INLINED = true;

		int t1 = value.getType();
		int t2 = value2.getType();

		switch (operator) {
		// For the operators below, no strings are allowed and
		// ints get converted to floats if necessary.

		case MULT:
		case DIVIDE:
		case PLUS:
		case MINUS:
			if (t1 == ExprValue.STRING || t2 == ExprValue.STRING) {
				if ((value.getStringValue().length() == 0)
						|| (value2.getStringValue().length() == 0)) {
					EmptyStringOperandError(interp, operator);
				}
				IllegalType(interp, ExprValue.STRING, operator);
			} else if (t1 == ExprValue.DOUBLE) {
				if (t2 == ExprValue.INT) {
					value2.setDoubleValue((double) value2.getIntValue());
					t2 = ExprValue.DOUBLE;
				}
			} else if (t2 == ExprValue.DOUBLE) {
				if (t1 == ExprValue.INT) {
					value.setDoubleValue((double) value.getIntValue());
					t1 = ExprValue.DOUBLE;
				}
			}
			break;

		// For the operators below, only integers are allowed.

		case MOD:
		case LEFT_SHIFT:
		case RIGHT_SHIFT:
		case BIT_AND:
		case BIT_XOR:
		case BIT_OR:
			if (t1 != ExprValue.INT) {
				if (value.getStringValue().length() == 0) {
					EmptyStringOperandError(interp, operator);
				}
				IllegalType(interp, value.getType(), operator);
			} else if (t2 != ExprValue.INT) {
				if (value2.getStringValue().length() == 0) {
					EmptyStringOperandError(interp, operator);
				}
				IllegalType(interp, value2.getType(), operator);
			}

			break;

		// For the operators below, any type is allowed but the
		// two operands must have the same type. Convert integers
		// to floats and either to strings, if necessary.

		case LESS:
		case GREATER:
		case LEQ:
		case GEQ:
		case EQUAL:
		case NEQ:
			if (t1 == t2) {
				// No-op, both operators are already the same type
			} else if (t1 == ExprValue.STRING) {
				if (t2 != ExprValue.STRING) {
					value2.toStringType();
					t2 = ExprValue.STRING;
				}
			} else if (t2 == ExprValue.STRING) {
				if (t1 != ExprValue.STRING) {
					value.toStringType();
					t1 = ExprValue.STRING;
				}
			} else if (t1 == ExprValue.DOUBLE) {
				if (t2 == ExprValue.INT) {
					value2.setDoubleValue((double) value2.getIntValue());
					t2 = ExprValue.DOUBLE;
				}
			} else if (t2 == ExprValue.DOUBLE) {
				if (t1 == ExprValue.INT) {
					value.setDoubleValue((double) value.getIntValue());
					t1 = ExprValue.DOUBLE;
				}
			}
			break;

		// For the 2 operators below, string comparison is always
		// done, so no operand validation is needed.

		case STREQ:
			value.setIntValue(value.getStringValue().equals(
					value2.getStringValue()));
			return;
		case STRNEQ:
			value.setIntValue(!value.getStringValue().equals(
					value2.getStringValue()));
			return;

		case IN:
			TclObject inList = TclString.newInstance(value2.getStringValue());
			for (TclObject element : TclList.getElements(interp, inList)) {
				if (value.getStringValue().equals(
						element.toString())) {
					value.setIntValue(true);
					return;
				}
			}
			value.setIntValue(false);
			return;

		case NI:
			TclObject niList = TclString.newInstance(value2.getStringValue());
			for (TclObject element : TclList.getElements(interp, niList)) {
				if (value.getStringValue().equals(
						element.toString())) {
					value.setIntValue(false);
					return;
				}
			}
			value.setIntValue(true);
			return;

		// For the operators below, no strings are allowed, but
		// no int->double conversions are performed.

		case AND:
		case OR:
			if (t1 == ExprValue.STRING) {
				IllegalType(interp, ExprValue.STRING, operator);
			}
			if (t2 == ExprValue.STRING) {
				boolean b = Util.getBoolean(interp, value2.getStringValue());
				value2.setIntValue(b);
			}
			break;

		// For the operators below, type and conversions are
		// irrelevant: they're handled elsewhere.

		case QUESTY:
		case COLON:
			break;

		// Any other operator is an error.

		default:
			throw new TclException(interp, "unknown operator in expression");
		}

		// Carry out the function of the specified operator.

		switch (operator) {
		case MULT:
			if (t1 == ExprValue.INT) {
				if (USE_INLINED) {
					value.optIntMult(value2);
				} else {
					value.setIntValue(value.getIntValue()
							* value2.getIntValue());
				}
			} else {
				if (USE_INLINED) {
					value.optDoubleMult(value2);
				} else {
					value.setDoubleValue(value.getDoubleValue()
							* value2.getDoubleValue());
				}
			}
			break;
		case DIVIDE:
			if (t1 == ExprValue.INT) {
				long dividend, divisor, quotient;

				if (value2.getIntValue() == 0) {
					DivideByZero(interp);
				}

				// quotient = dividend / divisor
				//
				// When performing integer division, protect
				// against integer overflow. Round towards zero
				// when the quotient is positive, otherwise
				// round towards -Infinity.

				dividend = value.getIntValue();
				divisor = value2.getIntValue();

				if (dividend == TCL.INT_MIN && divisor == -1) {
					// Avoid integer overflow on (TCL.INT_MIN / -1)
					quotient = TCL.INT_MIN;
				} else {
					quotient = dividend / divisor;
					// Round down to a smaller negative number if
					// there is a remainder and the quotient is
					// negative or zero and the signs don't match.
					if (((quotient < 0) || ((quotient == 0) && (((dividend < 0) && (divisor > 0)) || ((dividend > 0) && (divisor < 0)))))
							&& ((quotient * divisor) != dividend)) {
						quotient -= 1;
					}
				}
				value.setIntValue(quotient);
			} else {
				double divisor = value2.getDoubleValue();
				if (divisor == 0.0) {
					DivideByZero(interp);
				}
				value.setDoubleValue(value.getDoubleValue() / divisor);
			}
			break;
		case MOD:
			long dividend,
			divisor,
			remainder;
			boolean neg_divisor = false;

			if (value2.getIntValue() == 0) {
				DivideByZero(interp);
			}

			// remainder = dividend % divisor
			//
			// In Tcl, the sign of the remainder must match
			// the sign of the divisor. The absolute value of
			// the remainder must be smaller than the divisor.
			//
			// In Java, the remainder can be negative only if
			// the dividend is negative. The remainder will
			// always be smaller than the divisor.
			//
			// See: http://mindprod.com/jgloss/modulus.html

			dividend = value.getIntValue();
			divisor = value2.getIntValue();

			if (dividend == TCL.INT_MIN && divisor == -1) {
				// Avoid integer overflow on (TCL.INT_MIN  % -1)
				remainder = 0;
			} else {
				if (divisor < 0) {
					divisor = -divisor;
					dividend = -dividend; // Note: -TCL.INT_MIN  == TCL.INT_MIN
					neg_divisor = true;
				}
				remainder = dividend % divisor;

				// remainder is (remainder + divisor) when the
				// remainder is negative. Watch out for the
				// special case of a TCL.INT_MIN dividend
				// and a negative divisor. Don't add the divisor
				// in that case because the remainder should
				// not be negative.

				if (remainder < 0
						&& !(neg_divisor && (dividend == TCL.INT_MIN))) {
					remainder += divisor;
				}
			}
			if ((neg_divisor && (remainder > 0))
					|| (!neg_divisor && (remainder < 0))) {
				remainder = -remainder;
			}
			value.setIntValue(remainder);
			break;
		case PLUS:
			if (t1 == ExprValue.INT) {
				if (USE_INLINED) {
					value.optIntPlus(value2);
				} else {
					value.setIntValue(value.getIntValue()
							+ value2.getIntValue());
				}
			} else {
				if (USE_INLINED) {
					value.optDoublePlus(value2);
				} else {
					value.setDoubleValue(value.getDoubleValue()
							+ value2.getDoubleValue());
				}
			}
			break;
		case MINUS:
			if (t1 == ExprValue.INT) {
				if (USE_INLINED) {
					value.optIntMinus(value2);
				} else {
					value.setIntValue(value.getIntValue()
							- value2.getIntValue());
				}
			} else {
				if (USE_INLINED) {
					value.optDoubleMinus(value2);
				} else {
					value.setDoubleValue(value.getDoubleValue()
							- value2.getDoubleValue());
				}
			}
			break;
		case LEFT_SHIFT:
			// In Java, a left shift operation will shift bits from 0
			// to 63 places to the left. For an int left operand
			// the right operand value is implicitly (value & 0x1f),
			// so a negative shift amount is in the 0 to 63 range.

			long left_shift_num = value.getIntValue();
			long left_shift_by = value2.getIntValue();
			if (left_shift_by >= 64) {
				left_shift_num = 0;
			} else {
				left_shift_num <<= left_shift_by;
			}
			value.setIntValue(left_shift_num);
			break;
		case RIGHT_SHIFT:
			// In Java, a right shift operation will shift bits from 0
			// to 63 places to the right and propagate the sign bit.
			// For an int left operand, the right operand is implicitly
			// (value & 0x1f), so a negative shift is in the 0 to 63 range.

			long right_shift_num = value.getIntValue();
			long right_shift_by = value2.getIntValue();
			if (right_shift_by >= 64) {
				if (right_shift_num < 0) {
					right_shift_num = -1;
				} else {
					right_shift_num = 0;
				}
			} else {
				right_shift_num >>= right_shift_by;
			}
			value.setIntValue(right_shift_num);
			break;
		case LESS:
			if (t1 == ExprValue.INT) {
				if (USE_INLINED) {
					value.optIntLess(value2);
				} else {
					value.setIntValue(value.getIntValue() < value2
							.getIntValue());
				}
			} else if (t1 == ExprValue.DOUBLE) {
				if (USE_INLINED) {
					value.optDoubleLess(value2);
				} else {
					value.setIntValue(value.getDoubleValue() < value2
							.getDoubleValue());
				}
			} else {
				value.setIntValue(value.getStringValue().compareTo(
						value2.getStringValue()) < 0);
			}
			break;
		case GREATER:
			if (t1 == ExprValue.INT) {
				if (USE_INLINED) {
					value.optIntGreater(value2);
				} else {
					value.setIntValue(value.getIntValue() > value2
							.getIntValue());
				}
			} else if (t1 == ExprValue.DOUBLE) {
				if (USE_INLINED) {
					value.optDoubleGreater(value2);
				} else {
					value.setIntValue(value.getDoubleValue() > value2
							.getDoubleValue());
				}
			} else {
				value.setIntValue(value.getStringValue().compareTo(
						value2.getStringValue()) > 0);
			}
			break;
		case LEQ:
			if (t1 == ExprValue.INT) {
				if (USE_INLINED) {
					value.optIntLessEq(value2);
				} else {
					value.setIntValue(value.getIntValue() <= value2
							.getIntValue());
				}
			} else if (t1 == ExprValue.DOUBLE) {
				if (USE_INLINED) {
					value.optDoubleLessEq(value2);
				} else {
					value.setIntValue(value.getDoubleValue() <= value2
							.getDoubleValue());
				}
			} else {
				value.setIntValue(value.getStringValue().compareTo(
						value2.getStringValue()) <= 0);
			}
			break;
		case GEQ:
			if (t1 == ExprValue.INT) {
				if (USE_INLINED) {
					value.optIntGreaterEq(value2);
				} else {
					value.setIntValue(value.getIntValue() >= value2
							.getIntValue());
				}
			} else if (t1 == ExprValue.DOUBLE) {
				if (USE_INLINED) {
					value.optDoubleGreaterEq(value2);
				} else {
					value.setIntValue(value.getDoubleValue() >= value2
							.getDoubleValue());
				}
			} else {
				value.setIntValue(value.getStringValue().compareTo(
						value2.getStringValue()) >= 0);
			}
			break;
		case EQUAL:
			if (t1 == ExprValue.INT) {
				if (USE_INLINED) {
					value.optIntEq(value2);
				} else {
					value.setIntValue(value.getIntValue() == value2
							.getIntValue());
				}
			} else if (t1 == ExprValue.DOUBLE) {
				if (USE_INLINED) {
					value.optDoubleEq(value2);
				} else {
					value.setIntValue(value.getDoubleValue() == value2
							.getDoubleValue());
				}
			} else {
				value.setIntValue(value.getStringValue().equals(
						value2.getStringValue()));
			}
			break;
		case NEQ:
			if (t1 == ExprValue.INT) {
				if (USE_INLINED) {
					value.optIntNotEq(value2);
				} else {
					value.setIntValue(value.getIntValue() != value2
							.getIntValue());
				}
			} else if (t1 == ExprValue.DOUBLE) {
				if (USE_INLINED) {
					value.optDoubleNotEq(value2);
				} else {
					value.setIntValue(value.getDoubleValue() != value2
							.getDoubleValue());
				}
			} else {
				value.setIntValue(!value.getStringValue().equals(
						value2.getStringValue()));
			}
			break;
		case BIT_AND:
			value.setIntValue(value.getIntValue() & value2.getIntValue());
			break;
		case BIT_XOR:
			value.setIntValue(value.getIntValue() ^ value2.getIntValue());
			break;
		case BIT_OR:
			value.setIntValue(value.getIntValue() | value2.getIntValue());
			break;

		// For AND and OR, we know that the first value has already
		// been converted to an integer. Thus we need only consider
		// the possibility of int vs. double for the second value.

		case AND:
			if (t2 == ExprValue.DOUBLE) {
				value2.setIntValue(value2.getDoubleValue() != 0.0);
			}
			value.setIntValue(((value.getIntValue() != 0) && (value2
					.getIntValue() != 0)));
			break;
		case OR:
			if (t2 == ExprValue.DOUBLE) {
				value2.setIntValue(value2.getDoubleValue() != 0.0);
			}
			value.setIntValue(((value.getIntValue() != 0) || (value2
					.getIntValue() != 0)));
			break;

		}

		return;
	}

	/**
	 * GetLexeme -> ExprLex
	 * 
	 * Lexical analyzer for expression parser: parses a single value, operator,
	 * or other syntactic element from an expression string.
	 * 
	 * Size effects: the "m_token" member variable is set to the value of the
	 * current token.
	 * 
	 * @param interp
	 *            the context in which to evaluate the expression.
	 * @exception TclException
	 *                for malformed expressions.
	 * @return the value of the expression.
	 */
	private ExprValue ExprLex(Interp interp) throws TclException {
		char c, c2;

		while (m_ind < m_len
				&& (((c = m_expr.charAt(m_ind)) == ' ') || Character
						.isWhitespace(c))) {
			m_ind++;
		}
		if (m_ind >= m_len) {
			m_token = END;
			return null;
		}

		// First try to parse the token as an integer or
		// floating-point number. Don't want to check for a number if
		// the first character is "+" or "-". If we do, we might
		// treat a binary operator as unary by
		// mistake, which will eventually cause a syntax error.

		c = m_expr.charAt(m_ind);
		if (m_ind < m_len - 1) {
			c2 = m_expr.charAt(m_ind + 1);
		} else {
			c2 = '\0';
		}

		if ((c != '+') && (c != '-')) {
			if (m_ind == m_len - 1) {
				// Integer shortcut when only 1 character left
				switch (c) {
				case '0':
				case '1':
				case '2':
				case '3':
				case '4':
				case '5':
				case '6':
				case '7':
				case '8':
				case '9':
					m_ind++;
					m_token = VALUE;
					ExprValue value = grabExprValue();
					value.setIntValue(c - '0', String.valueOf(c));
					return value;
				}
			}
			final boolean startsWithDigit = Character.isDigit(c);
			if (startsWithDigit && looksLikeInt(m_expr, m_len, m_ind, false)) {
				StrtoulResult res = interp.strtoulResult;
				Util.strtoul(m_expr, m_ind, 0, res);

				if (res.errno == 0) {
					String token = m_expr.substring(m_ind, res.index);
					m_ind = res.index;
					m_token = VALUE;
					ExprValue value = grabExprValue();
					value.setIntValue(res.value, token);
					return value;
				} else {
					if (res.errno == TCL.INTEGER_RANGE) {
						IntegerTooLarge(interp);
					}
				}
			} else if (startsWithDigit || (c == '.') || (c == 'n')
					|| (c == 'N')) {
				StrtodResult res = interp.strtodResult;
				Util.strtod(m_expr, m_ind, -1, res);
				if (res.errno == 0) {
					String token = m_expr.substring(m_ind, res.index);
					m_ind = res.index;
					m_token = VALUE;
					ExprValue value = grabExprValue();
					value.setDoubleValue(res.value, token);
					return value;
				} else {
					if (res.errno == TCL.DOUBLE_RANGE) {
						if (res.value != 0) {
							DoubleTooLarge(interp);
						} else {
							DoubleTooSmall(interp);
						}
					}
				}
			}
		}

		ParseResult pres;
		ExprValue retval;
		m_ind += 1; // ind is advanced to point to the next token

		switch (c) {
		case '$':
			m_token = VALUE;
			pres = ParseAdaptor.parseVar(interp, m_expr, m_ind, m_len);
			m_ind = pres.nextIndex;

			if (interp.noEval != 0) {
				retval = grabExprValue();
				retval.setIntValue(0);
			} else {
				retval = grabExprValue();
				ExprParseObject(interp, pres.value, retval);
			}
			pres.release();
			return retval;
		case '[':
			m_token = VALUE;
			pres = ParseAdaptor.parseNestedCmd(interp, m_expr, m_ind, m_len);
			m_ind = pres.nextIndex;

			if (interp.noEval != 0) {
				retval = grabExprValue();
				retval.setIntValue(0);
			} else {
				retval = grabExprValue();
				ExprParseObject(interp, pres.value, retval);
			}
			pres.release();
			return retval;
		case '"':
			m_token = VALUE;

			// System.out.println("now to parse from ->" + m_expr +
			// "<- at index "
			// + m_ind);

			pres = ParseAdaptor.parseQuotes(interp, m_expr, m_ind, m_len);
			m_ind = pres.nextIndex;

			// System.out.println("after parse next index is " + m_ind);

			if (interp.noEval != 0) {
				// System.out.println("returning noEval zero value");
				retval = grabExprValue();
				retval.setIntValue(0);
			} else {
				// System.out.println("returning value string ->" +
				// pres.value.toString() + "<-" );
				retval = grabExprValue();
				ExprParseObject(interp, pres.value, retval);
			}
			pres.release();
			return retval;
		case '{':
			m_token = VALUE;
			pres = ParseAdaptor.parseBraces(interp, m_expr, m_ind, m_len);
			m_ind = pres.nextIndex;
			if (interp.noEval != 0) {
				retval = grabExprValue();
				retval.setIntValue(0);
			} else {
				retval = grabExprValue();
				ExprParseObject(interp, pres.value, retval);
			}
			pres.release();
			return retval;
		case '(':
			m_token = OPEN_PAREN;
			return null;

		case ')':
			m_token = CLOSE_PAREN;
			return null;

		case ',':
			m_token = COMMA;
			return null;

		case '*':
			m_token = MULT;
			return null;

		case '/':
			m_token = DIVIDE;
			return null;

		case '%':
			m_token = MOD;
			return null;

		case '+':
			m_token = PLUS;
			return null;

		case '-':
			m_token = MINUS;
			return null;

		case '?':
			m_token = QUESTY;
			return null;

		case ':':
			m_token = COLON;
			return null;

		case '<':
			switch (c2) {
			case '<':
				m_ind += 1;
				m_token = LEFT_SHIFT;
				break;
			case '=':
				m_ind += 1;
				m_token = LEQ;
				break;
			default:
				m_token = LESS;
				break;
			}
			return null;

		case '>':
			switch (c2) {
			case '>':
				m_ind += 1;
				m_token = RIGHT_SHIFT;
				break;
			case '=':
				m_ind += 1;
				m_token = GEQ;
				break;
			default:
				m_token = GREATER;
				break;
			}
			return null;

		case '=':
			if (c2 == '=') {
				m_ind += 1;
				m_token = EQUAL;
			} else {
				m_token = UNKNOWN;
			}
			return null;

		case '!':
			if (c2 == '=') {
				m_ind += 1;
				m_token = NEQ;
			} else {
				m_token = NOT;
			}
			return null;

		case '&':
			if (c2 == '&') {
				m_ind += 1;
				m_token = AND;
			} else {
				m_token = BIT_AND;
			}
			return null;

		case '^':
			m_token = BIT_XOR;
			return null;

		case '|':
			if (c2 == '|') {
				m_ind += 1;
				m_token = OR;
			} else {
				m_token = BIT_OR;
			}
			return null;

		case '~':
			m_token = BIT_NOT;
			return null;

		case 'e':
		case 'n':
		case 'i':
			if (c == 'e' && c2 == 'q') {
				m_ind += 1;
				m_token = STREQ;
				return null;
			} else if (c == 'n' && c2 == 'e') {
				m_ind += 1;
				m_token = STRNEQ;
				return null;
			} else if (c == 'n' && c2 == 'i') {
				m_ind += 1;
				m_token = NI;
				return null;
			} else if (c == 'i' && c2 == 'n') {
				m_ind += 1;
				m_token = IN;
				return null;
			}
			// Fall through to default

		default:
			if (Character.isLetter(c)) {
				// Oops, re-adjust m_ind so that it points to the beginning
				// of the function name or literal.

				m_ind--;

				//
				// Check for boolean literals (true, false, yes, no, on, off)
				// This is kind of tricky since we don't want to pull a
				// partial boolean literal "f" off of the front of a function
				// invocation like expr {floor(1.1)}.
				//

				String substr = m_expr.substring(m_ind);
				boolean is_math_func = false;

				// System.out.println("default char '" + c + "' str is \"" +
				// m_expr + "\" and m_ind " + m_ind + " substring is \"" +
				// substr + "\"");

				final int max = substr.length();
				int i;
				for (i = 0; i < max; i++) {
					c = substr.charAt(i);
					if (!(Character.isLetterOrDigit(c) || c == '_')) {
						break;
					}
				}
				// Skip any whitespace characters too
				for (; i < max; i++) {
					c = substr.charAt(i);
					if (c == ' ' || Character.isWhitespace(c)) {
						continue;
					} else {
						break;
					}
				}
				if ((i < max) && (substr.charAt(i) == '(')) {
					// System.out.println("known to be a math func, char is '" +
					// substr.charAt(i) + "'");
					is_math_func = true;
				}

				if (!is_math_func) {
					String tok = getBooleanToken(substr);
					if (tok != null) {
						m_ind += tok.length();
						m_token = VALUE;
						ExprValue value = grabExprValue();
						value.setStringValue(tok);
						return value;
					}
				}

				return mathFunction(interp);
			}
			m_token = UNKNOWN;
			return null;
		}
	}

	/**
	 * Parses a math function from an expression string, carry out the function,
	 * and return the value computed.
	 * 
	 * @param interp
	 *            current interpreter.
	 * @return the value computed by the math function.
	 * @exception TclException
	 *                if any error happens.
	 */
	ExprValue mathFunction(Interp interp) throws TclException {
		int startIdx = m_ind;
		ExprValue value;
		String funcName;
		MathFunction mathFunc;
		ExprValue[] values = null;
		int numArgs;

		// Find the end of the math function's name and lookup the MathFunc
		// record for the function. Search until the char at m_ind is not
		// alphanumeric or '_'

		for (; m_ind < m_len; m_ind++) {
			if (!(Character.isLetterOrDigit(m_expr.charAt(m_ind)) || m_expr
					.charAt(m_ind) == '_')) {
				break;
			}
		}

		// Get the funcName BEFORE calling ExprLex, so the funcName
		// will not have trailing whitespace.

		funcName = m_expr.substring(startIdx, m_ind);
		mathFunc = (MathFunction) mathFuncTable.get(funcName);

		// Parse errors are thrown BEFORE unknown function names

		ExprLex(interp);
		if (m_token != OPEN_PAREN) {
			if (mathFunc==null)
				SyntaxError(interp,"variable references require preceding $");
			else
				SyntaxError(interp,"expected parenthesis enclosing function arguments");
		}

		// Now test for unknown funcName. Doing the above statements
		// out of order will cause some tests to fail.

		if (mathFunc == null) {
			throw new TclException(interp, "unknown math function \""
					+ funcName + "\"");
		}

		// Scan off the arguments for the function, if there are any.

		numArgs = mathFunc.argTypes.length;

		if (numArgs == 0) {
			ExprLex(interp);
			if (m_token != CLOSE_PAREN) {
				SyntaxError(interp,"missing close parenthesis at end of function call");
			}
		} else {
			values = new ExprValue[numArgs];

			for (int i = 0;; i++) {
				value = ExprGetValue(interp, -1);

				// Handle close paren with no value
				// % expr {srand()}

				if ((value == null) && (m_token == CLOSE_PAREN)) {
					if (i == numArgs)
						break;
					else
						throw new TclException(interp,
								"too few arguments for math function");
				}

				values[i] = value;

				// Check for a comma separator between arguments or a
				// close-paren to end the argument list.

				if (i == (numArgs - 1)) {
					if (m_token == CLOSE_PAREN) {
						break;
					}
					if (m_token == COMMA) {
						throw new TclException(interp,
								"too many arguments for math function");
					} else {
						SyntaxError(interp,"missing close parenthesis at end of function call");
					}
				}
				if (m_token != COMMA) {
					if (m_token == CLOSE_PAREN) {
						throw new TclException(interp,
								"too few arguments for math function");
					} else {
						SyntaxError(interp);
					}
				}
			}
		}

		m_token = VALUE;
		if (interp.noEval != 0) {
			ExprValue rvalue = grabExprValue();
			rvalue.setIntValue(0);
			return rvalue;
		} else {
			// Invoke the function and copy its result back into rvalue.
			ExprValue rvalue = grabExprValue();
			evalMathFunction(interp, funcName, mathFunc, values, true, rvalue);
			return rvalue;
		}
	}

	/**
	 * This procedure will lookup and invoke a math function given the name of
	 * the function and an array of ExprValue arguments. Each ExprValue is
	 * released before the function exits. This method is intended to be used by
	 * other modules that may need to invoke a math function at runtime. It is
	 * assumed that the caller has checked the number of arguments, the type of
	 * the arguments will be adjusted before invocation if needed.
	 * 
	 * The values argument can be null when there are no args to pass.
	 * 
	 * The releaseValues argument should be true when the ExprValue objecys in
	 * the array should be released.
	 */

	public void evalMathFunction(Interp interp, String funcName,
			ExprValue[] values, boolean releaseValues, ExprValue result)
			throws TclException {
		MathFunction mathFunc = (MathFunction) mathFuncTable.get(funcName);
		if (mathFunc == null) {
			throw new TclException(interp, "unknown math function \""
					+ funcName + "\"");
		}
		evalMathFunction(interp, funcName, mathFunc, values, releaseValues,
				result);
	}

	/**
	 * This procedure implements a math function invocation. See the comments
	 * for the function above, note that this method is used when the math
	 * function pointer has already been resolved.
	 * 
	 * The values argument can be null when there are no args to pass.
	 * 
	 * The releaseValues argument should be true when the ExprValue objecys in
	 * the array should be released.
	 */

	void evalMathFunction(Interp interp, String funcName,
			MathFunction mathFunc, ExprValue[] values, boolean releaseValues,
			ExprValue result) throws TclException {
		if (mathFunc.argTypes == null) {
			throw new TclRuntimeError("math function \"" + funcName
					+ "\" has null argTypes");
		}

		// Ensure that arguments match the int/double
		// expectations of the math function.

		int numArgs = mathFunc.argTypes.length;
		int expectedArgs = 0;
		if (values != null) {
			expectedArgs = values.length;
		}

		if (numArgs != expectedArgs) {
			if ((expectedArgs > 0) && (expectedArgs < numArgs)) {
				throw new TclException(interp,
						"too few arguments for math function");
			} else {
				throw new TclException(interp,
						"too many arguments for math function");
			}
		}

		if (values != null) {
			for (int i = 0; i < values.length; i++) {
				ExprValue value = values[i];
				if (value.isStringType()) {
					throw new TclException(interp,
							"argument to math function didn't have numeric value");
				} else if (value.isIntType()) {
					if (mathFunc.argTypes[i] == MathFunction.DOUBLE) {
						value.setDoubleValue((double) value.getIntValue());
					}
				} else {
					if (mathFunc.argTypes[i] == MathFunction.INT) {
						value.setIntValue((long) value.getDoubleValue());
					}
				}
			}
		}

		if (mathFunc instanceof NoArgMathFunction) {
			((NoArgMathFunction) mathFunc).apply(interp, result);
		} else {
			mathFunc.apply(interp, values);

			// Copy result from first argument to passed in ref.
			// It is possible that the caller passed null as
			// the result, leave the value in values[0] in that
			// case. This optimization is only valid when
			// releaseValues is false.

			if (result != null) {
				result.setValue(values[0]);
			}
		}

		if (releaseValues && values != null) {
			// Release ExprValue elements in values array
			for (ExprValue value : values) {
				releaseExprValue(value);
			}
		}
	}

	/**
	 * This procedure will register a math function by adding it to the table of
	 * available math functions. This methods is used when regression testing
	 * the expr command.
	 */

	void registerMathFunction(String name, MathFunction mathFunc) {
		mathFuncTable.put(name, mathFunc);
	}

	/**
	 * This procedure decides whether the leading characters of a string look
	 * like an integer or something else (such as a floating-point number or
	 * string). If the whole flag is true then the entire string must look like
	 * an integer.
	 * 
	 * @return a boolean value indicating if the string looks like an integer.
	 */

	public static boolean looksLikeInt(String s, int len, int i, boolean whole) {
		char c = '\0';
		while (i < len
				&& (((c = s.charAt(i)) == ' ') || Character.isWhitespace(c))) {
			i++;
		}
		if (i >= len) {
			return false;
		}
		if ((c == '+') || (c == '-')) {
			i++;
			if (i >= len) {
				return false;
			}
			c = s.charAt(i);
		}
		if ((c >= '0' && c <= '9') || Character.isDigit(c)) {
			// This is a digit
			i++;
		} else {
			return false;
		}
		for (; i < len; i++) {
			c = s.charAt(i);
			if ((c >= '0' && c <= '9') || Character.isDigit(c)) {
				// This is a digit, keep looking at rest of string.
				// System.out.println("'" + c + "' is a digit");
			} else {
				break;
			}
		}
		if (i >= len) {
			return true;
		}

		if (!whole && (c != '.') && (c != 'E') && (c != 'e')) {
			return true;
		}
		if (c == 'e' || c == 'E') {
			// Could be a double like 1e6 or 1e-1 but
			// it could also be 1eq2. If the next
			// character is not a digit or a + or -,
			// then this must not be a double. If the
			// whole string must look like an integer
			// then we know this is not an integer.
			if (whole) {
				return false;
			} else if (i + 1 >= len) {
				return true;
			}
			c = s.charAt(i + 1);
			if (c != '+' && c != '-' && !Character.isDigit(c)) {
				// Does not look like "1e1", "1e+1", or "1e-1"
				// so strtoul would parse the text leading up
				// to the 'e' as an integer.
				return true;
			}
		}
		if (whole) {
			while (i < len
					&& (((c = s.charAt(i)) == ' ') || Character.isWhitespace(c))) {
				i++;
			}
			if (i >= len) {
				return true;
			}
		}

		return false;
	}

	static void checkIntegerRange(Interp interp, double d) throws TclException {
		if (d < 0) {
			if (d < ((double) TCL.INT_MIN)) {
				Expression.IntegerTooLarge(interp);
			}
		} else {
			if (d > ((double) TCL.INT_MAX)) {
				Expression.IntegerTooLarge(interp);
			}
		}
	}

	static void checkDoubleRange(Interp interp, double d) throws TclException {
		if (Double.isNaN(d) || Double.isInfinite(d)) {
			Expression.DoubleTooLarge(interp);
		}
	}

	// If the string starts with a boolean token, then
	// return the portion of the string that matched
	// a boolean token. Otherwise, return null.

	static String getBooleanToken(String tok) {
		int length = tok.length();
		if (length == 0) {
			return null;
		}
		char c = tok.charAt(0);
		switch (c) {
		case 'f':
			if (tok.startsWith("false")) {
				return "false";
			}
			if (tok.startsWith("fals")) {
				return "fals";
			}
			if (tok.startsWith("fal")) {
				return "fal";
			}
			if (tok.startsWith("fa")) {
				return "fa";
			}
			if (tok.startsWith("f")) {
				return "f";
			}
		case 'n':
			if (tok.startsWith("no")) {
				return "no";
			}
			if (tok.startsWith("n")) {
				return "n";
			}
		case 'o':
			if (tok.startsWith("off")) {
				return "off";
			}
			if (tok.startsWith("of")) {
				return "of";
			}
			if (tok.startsWith("on")) {
				return "on";
			}
		case 't':
			if (tok.startsWith("true")) {
				return "true";
			}
			if (tok.startsWith("tru")) {
				return "tru";
			}
			if (tok.startsWith("tr")) {
				return "tr";
			}
			if (tok.startsWith("t")) {
				return "t";
			}
		case 'y':
			if (tok.startsWith("yes")) {
				return "yes";
			}
			if (tok.startsWith("ye")) {
				return "ye";
			}
			if (tok.startsWith("y")) {
				return "y";
			}
		}
		return null;
	}

	// Get an ExprValue object out of the cache
	// of ExprValues. These values will be released
	// later on by a call to releaseExprValue. Don't
	// bother with release on syntax or other errors
	// since the exprValueCache will refill itself.

	public final ExprValue grabExprValue() {
		if (cachedExprIndex == cachedExprLength) {
			// Allocate new ExprValue if cache is empty
			return new ExprValue(0, null);
		} else {
			return cachedExprValue[cachedExprIndex++];
		}
	}

	public final void releaseExprValue(ExprValue val) {
		// Debug check for duplicate value already in cachedExprValue
		if (false) {
			if (cachedExprIndex < 0) {
				throw new TclRuntimeError("cachedExprIndex is "
						+ cachedExprIndex);
			}
			if (val == null) {
				throw new TclRuntimeError("ExprValue is null");
			}
			for (int i = cachedExprIndex; i < cachedExprLength; i++) {
				if (cachedExprValue[i] != null && val == cachedExprValue[i]) {
					throw new TclRuntimeError(
							"released ExprValue is already in cache slot " + i);
				}
			}
		}

		if (cachedExprIndex > 0) {
			// Cache is not full, return value to cache
			cachedExprValue[--cachedExprIndex] = val;
		}
	}
}

abstract class MathFunction {
	static final int INT = 0;
	static final int DOUBLE = 1;
	static final int EITHER = 2;

	int[] argTypes;

	abstract void apply(Interp interp, ExprValue[] values) throws TclException;
}

abstract class UnaryMathFunction extends MathFunction {
	UnaryMathFunction() {
		argTypes = new int[1];
		argTypes[0] = DOUBLE;
	}
}

abstract class BinaryMathFunction extends MathFunction {
	BinaryMathFunction() {
		argTypes = new int[2];
		argTypes[0] = DOUBLE;
		argTypes[1] = DOUBLE;
	}
}

abstract class NoArgMathFunction extends MathFunction {
	NoArgMathFunction() {
		argTypes = new int[0];
	}

	// A NoArgMathFunction is a special case for the
	// rand() math function. There are no arguments,
	// so pass just a result ExprValue.

	abstract void apply(Interp interp, ExprValue value) throws TclException;

	// Raise a runtime error if the wrong apply is invoked.

	void apply(Interp interp, ExprValue[] values) throws TclException {
		throw new TclRuntimeError(
				"NoArgMathFunction must be invoked via apply(interp, ExprValue)");
	}
}

class Atan2Function extends BinaryMathFunction {
	void apply(Interp interp, ExprValue[] values) throws TclException {
		double y = values[0].getDoubleValue();
		double x = values[1].getDoubleValue();
		if ((y == 0.0) && (x == 0.0)) {
			Expression.DomainError(interp);
		}
		values[0].setDoubleValue(Math.atan2(y, x));
	}
}

class AbsFunction extends MathFunction {
	AbsFunction() {
		argTypes = new int[1];
		argTypes[0] = EITHER;
	}

	void apply(Interp interp, ExprValue[] values) throws TclException {
		ExprValue value = values[0];
		if (value.isDoubleType()) {
			double d = value.getDoubleValue();
			if (d > 0) {
				value.setDoubleValue(d);
			} else {
				value.setDoubleValue(-d);
			}
		} else {
			long i = value.getIntValue();
			if (i > 0) {
				value.setIntValue(i);
			} else {
				value.setIntValue(-i);
			}
		}
	}
}

class DoubleFunction extends MathFunction {
	DoubleFunction() {
		argTypes = new int[1];
		argTypes[0] = EITHER;
	}

	void apply(Interp interp, ExprValue[] values) throws TclException {
		ExprValue value = values[0];
		if (value.isIntType()) {
			value.setDoubleValue((double) value.getIntValue());
		}
	}
}

class IntFunction extends MathFunction {
	IntFunction() {
		argTypes = new int[1];
		argTypes[0] = EITHER;
	}

	void apply(Interp interp, ExprValue[] values) throws TclException {
		ExprValue value = values[0];
		if (!value.isIntType()) {
			double d = value.getDoubleValue();
			Expression.checkIntegerRange(interp, d);
			value.setIntValue((long) d);
		}
	}
}

class WideFunction extends MathFunction {
	WideFunction() {
		argTypes = new int[1];
		argTypes[0] = EITHER;
	}

	void apply(Interp interp, ExprValue[] values) throws TclException {
		ExprValue value = values[0];
		if (!value.isIntType()) {
			double d = value.getDoubleValue();
			Expression.checkIntegerRange(interp, d);
			value.setIntValue((long) d);
		}
	}
}

class RoundFunction extends MathFunction {
	RoundFunction() {
		argTypes = new int[1];
		argTypes[0] = EITHER;
	}

	void apply(Interp interp, ExprValue[] values) throws TclException {
		ExprValue value = values[0];
		if (value.isDoubleType()) {
			double d = value.getDoubleValue();
			double i = (d < 0.0 ? Math.ceil(d) : Math.floor(d));
			double f = d - i;
			if (d < 0.0) {
				if (f <= -0.5) {
					i += -1.0;
				}
				Expression.checkIntegerRange(interp, i);
				value.setIntValue((long) i);
			} else {
				if (f >= 0.5) {
					i += 1.0;
				}
				Expression.checkIntegerRange(interp, i);
				value.setIntValue((long) i);
			}
		}
	}
}

class PowFunction extends BinaryMathFunction {
	void apply(Interp interp, ExprValue[] values) throws TclException {
		double x = values[0].getDoubleValue();
		double y = values[1].getDoubleValue();
		if (x < 0.0) {
			// If x is negative then y must be an integer

			if (((double) ((int) y)) != y) {
				Expression.DomainError(interp);
			}
		}

		double d = Math.pow(x, y);
		Expression.checkDoubleRange(interp, d);
		values[0].setDoubleValue(d);
	}
}

/*
 * The following section is generated by this script.
 * 
 * set missing {fmod} set byhand {atan2 pow}
 * 
 * 
 * foreach func {Acos Asin Atan Ceil Cos Exp Floor Log Sin Sqrt Tan} { puts "
 * class $func\Function extends UnaryMathFunction { ExprValue apply(Interp
 * interp, TclObject argv\[\]) throws TclException { return new
 * ExprValue(Math.[string tolower $func](TclDouble.get(interp, argv\[0\]))); } }
 * " }
 */

class AcosFunction extends UnaryMathFunction {
	void apply(Interp interp, ExprValue[] values) throws TclException {
		ExprValue value = values[0];
		double d = value.getDoubleValue();
		if ((d < -1.0) || (d > 1.0)) {
			Expression.DomainError(interp);
		}
		value.setDoubleValue(Math.acos(d));
	}
}

class AsinFunction extends UnaryMathFunction {
	void apply(Interp interp, ExprValue[] values) throws TclException {
		ExprValue value = values[0];
		double d = value.getDoubleValue();
		if ((d < -1.0) || (d > 1.0)) {
			Expression.DomainError(interp);
		}
		value.setDoubleValue(Math.asin(d));
	}
}

class AtanFunction extends UnaryMathFunction {
	void apply(Interp interp, ExprValue[] values) throws TclException {
		ExprValue value = values[0];
		double d = value.getDoubleValue();
		if ((d < -Math.PI / 2) || (d > Math.PI / 2)) {
			Expression.DomainError(interp);
		}
		value.setDoubleValue(Math.atan(d));
	}
}

class CeilFunction extends UnaryMathFunction {
	void apply(Interp interp, ExprValue[] values) throws TclException {
		ExprValue value = values[0];
		value.setDoubleValue(Math.ceil(value.getDoubleValue()));
	}
}

class CosFunction extends UnaryMathFunction {
	void apply(Interp interp, ExprValue[] values) throws TclException {
		ExprValue value = values[0];
		value.setDoubleValue(Math.cos(value.getDoubleValue()));
	}
}

class CoshFunction extends UnaryMathFunction {
	void apply(Interp interp, ExprValue[] values) throws TclException {
		ExprValue value = values[0];
		double x = value.getDoubleValue();
		double d1 = Math.pow(Math.E, x);
		double d2 = Math.pow(Math.E, -x);

		Expression.checkDoubleRange(interp, d1);
		Expression.checkDoubleRange(interp, d2);
		value.setDoubleValue((d1 + d2) / 2);
	}
}

class ExpFunction extends UnaryMathFunction {
	void apply(Interp interp, ExprValue[] values) throws TclException {
		ExprValue value = values[0];
		double d = Math.exp(value.getDoubleValue());
		if ((Double.isNaN(d)) || Double.isInfinite(d)) {
			Expression.DoubleTooLarge(interp);
		}
		value.setDoubleValue(d);
	}
}

class FloorFunction extends UnaryMathFunction {
	void apply(Interp interp, ExprValue[] values) throws TclException {
		ExprValue value = values[0];
		value.setDoubleValue(Math.floor(value.getDoubleValue()));
	}
}

class FmodFunction extends BinaryMathFunction {
	void apply(Interp interp, ExprValue[] values) throws TclException {
		double d1 = values[0].getDoubleValue();
		double d2 = values[1].getDoubleValue();
		if (d2 == 0.0) {
			Expression.DomainError(interp);
		}
		values[0].setDoubleValue(Math.IEEEremainder(d1, d2));
	}
}

class HypotFunction extends BinaryMathFunction {
	void apply(Interp interp, ExprValue[] values) throws TclException {
		double x = values[0].getDoubleValue();
		double y = values[1].getDoubleValue();
		values[0].setDoubleValue(Math.sqrt(((x * x) + (y * y))));
	}
}

class LogFunction extends UnaryMathFunction {
	void apply(Interp interp, ExprValue[] values) throws TclException {
		ExprValue value = values[0];
		double d = value.getDoubleValue();
		if (d < 0.0) {
			Expression.DomainError(interp);
		}
		value.setDoubleValue(Math.log(d));
	}
}

class Log10Function extends UnaryMathFunction {
	private static final double log10 = Math.log(10);

	void apply(Interp interp, ExprValue[] values) throws TclException {
		ExprValue value = values[0];
		double d = value.getDoubleValue();
		if (d < 0.0) {
			Expression.DomainError(interp);
		}
		value.setDoubleValue(Math.log(d) / log10);
	}
}

class SinFunction extends UnaryMathFunction {
	void apply(Interp interp, ExprValue[] values) throws TclException {
		ExprValue value = values[0];
		double d = value.getDoubleValue();
		value.setDoubleValue(Math.sin(d));
	}
}

class SinhFunction extends UnaryMathFunction {
	void apply(Interp interp, ExprValue[] values) throws TclException {
		ExprValue value = values[0];
		double x = value.getDoubleValue();

		double d1 = Math.pow(Math.E, x);
		double d2 = Math.pow(Math.E, -x);

		Expression.checkDoubleRange(interp, d1);
		Expression.checkDoubleRange(interp, d2);

		value.setDoubleValue((d1 - d2) / 2);
	}
}

class SqrtFunction extends UnaryMathFunction {
	void apply(Interp interp, ExprValue[] values) throws TclException {
		ExprValue value = values[0];
		double x = value.getDoubleValue();
		if (x < 0.0) {
			Expression.DomainError(interp);
		}
		value.setDoubleValue(Math.sqrt(x));
	}
}

class TanFunction extends UnaryMathFunction {
	void apply(Interp interp, ExprValue[] values) throws TclException {
		ExprValue value = values[0];
		value.setDoubleValue(Math.tan(value.getDoubleValue()));
	}
}

class TanhFunction extends UnaryMathFunction {
	void apply(Interp interp, ExprValue[] values) throws TclException {
		ExprValue value = values[0];
		double x = value.getDoubleValue();
		if (x == 0.0) {
			return;
		}

		double d1 = Math.pow(Math.E, x);
		double d2 = Math.pow(Math.E, -x);

		Expression.checkDoubleRange(interp, d1);
		Expression.checkDoubleRange(interp, d2);

		value.setDoubleValue((d1 - d2) / (d1 + d2));
	}
}

class RandFunction extends NoArgMathFunction {

	// Generate the random number using the linear congruential
	// generator defined by the following recurrence:
	// seed = ( IA * seed ) mod IM
	// where IA is 16807 and IM is (2^31) - 1. In order to avoid
	// potential problems with integer overflow, the code uses
	// additional constants IQ and IR such that
	// IM = IA*IQ + IR
	// For details on how this algorithm works, refer to the following
	// papers:
	//
	// S.K. Park & K.W. Miller, "Random number generators: good ones
	// are hard to find," Comm ACM 31(10):1192-1201, Oct 1988
	//
	// W.H. Press & S.A. Teukolsky, "Portable random number
	// generators," Computers in Physics 6(5):522-524, Sep/Oct 1992.

	private static final int randIA = 16807;
	private static final int randIM = 2147483647;
	private static final int randIQ = 127773;
	private static final int randIR = 2836;
	private static final Date date = new Date();

	/**
	 * Srand calls the main algorithm for rand after it sets the seed. To
	 * facilitate this call, the method is static and can be used w/o creating a
	 * new object. But we also need to maintain the inheritance hierarchy, thus
	 * the dynamic apply() calls the static statApply().
	 */

	void apply(Interp interp, ExprValue value) throws TclException {
		statApply(interp, value);
	}

	static void statApply(Interp interp, ExprValue value) throws TclException {

		int tmp;

		if (!(interp.randSeedInit)) {
			interp.randSeedInit = true;
			interp.randSeed = (int) date.getTime();
		}

		if (interp.randSeed == 0) {
			// Don't allow a 0 seed, since it breaks the generator. Shift
			// it to some other value.

			interp.randSeed = 123459876;
		}

		tmp = (int) (interp.randSeed / randIQ);
		interp.randSeed = ((randIA * (interp.randSeed - tmp * randIQ)) - randIR
				* tmp);

		if (interp.randSeed < 0) {
			interp.randSeed += randIM;
		}

		value.setDoubleValue(interp.randSeed * (1.0 / randIM));
	}
}

class SrandFunction extends UnaryMathFunction {

	void apply(Interp interp, ExprValue[] values) throws TclException {
		ExprValue value = values[0];

		// Reset the seed.

		interp.randSeedInit = true;
		interp.randSeed = (long) value.getDoubleValue();

		// To avoid duplicating the random number generation code we simply
		// call the static random number generator in the RandFunction
		// class.

		RandFunction.statApply(interp, value);
	}
}
