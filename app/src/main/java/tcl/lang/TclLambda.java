/*
 * TclLambda.java --
 *
 * Copyright (C) 2010 Neil Madden &lt;nem@cs.nott.ac.uk&gt.
 *
 * See the file "license.terms" for information on usage and redistribution of
 * this file, and for a DISCLAIMER OF ALL WARRANTIES.
 *
 * RCS: @(#) $Id$
 */

package tcl.lang;

/**
 * Lambda internal rep.
 *
 * @author  Neil Madden &lt;nem@cs.nott.ac.uk&gt;
 * @version $Revision$
 */
public class TclLambda implements InternalRep {
	private final Procedure procedure;
	private final String namespaceName;

	private TclLambda(Procedure procedure, String ns) {
		this.procedure = procedure;
		this.namespaceName = ns;
	}

	public void dispose() {
		// TODO: figure out how to cleanup correctly (ref count?)
		//this.procedure.disposeCmd();
	}

	public InternalRep duplicate() {
		return new TclLambda(procedure, namespaceName);
	}

	public static void apply(Interp interp, TclObject lambdaExpr, TclObject[] args)
			throws TclException
	{
		if (!(lambdaExpr.getInternalRep() instanceof TclLambda)) {
			setLambdaFromAny(interp, lambdaExpr);
		}

		TclLambda lambda = (TclLambda) lambdaExpr.getInternalRep();
		Procedure proc = lambda.procedure;

		// Lookup the namespace afresh on each call to avoid bugs when the
		// namespace is changed or deleted between calls. Note: [apply] treats
		// all namespace names as relative to the global namespace, regardless
		// of whether they start with "::" or not.
		Namespace ns = Namespace.findNamespace(interp, lambda.namespaceName,
				/* contextNs: */null, TCL.GLOBAL_ONLY | TCL.LEAVE_ERR_MSG);
		if (ns == null) {
			throw new TclException(interp, interp.getResult().toString());
		}
		// Now attach the namespace to the procedure.
		proc.wcmd.ns = ns;

		proc.cmdProc(interp, args);
	}

	private static void setLambdaFromAny(Interp interp, TclObject lambdaExpr)
			throws TclException
	{
		if (lambdaExpr.getInternalRep() instanceof TclLambda) return;

		TclLambda lambda = parseLambda(interp, lambdaExpr);
		lambdaExpr.setInternalRep(lambda);
	}

	private static TclLambda parseLambda(Interp interp, TclObject lambdaExpr)
			throws TclException
	{
		// A lambda expression is a list of either two or three elements:
		// {arglist body ?namespace?}
		TclObject[] elems = TclList.getElements(interp, lambdaExpr);
		if (elems.length < 2 || elems.length > 3) {
			throw new TclException(interp, "can't interpret \""
					+ lambdaExpr.toString() + "\" as a lambda expression");
		}
		TclObject args = elems[0];
		TclObject body = elems[1];
		String ns = elems.length == 3 ? elems[2].toString() : "::";

		// Ensure the namespace is fully-qualified relative to the global namespace.
		// The test-suite tests for this! (apply-3.3)
		if (!ns.startsWith("::")) {
			ns = "::" + ns;
		}

		// Use the string rep of the lambda as the proc name -- this also ensures
		// that the lambda has a string rep, so we never have to regenerate it.
		String name = lambdaExpr.toString();

		Procedure proc = null;
		try {
			proc = new Procedure(interp, null /* ns */, name, args,
				body, interp.getScriptFile(), interp.getArgLineNumber(1));
		} catch (TclException tex) {
			interp.addErrorInfo("\n    (parsing lambda expression \""+ lambdaExpr.toString() + "\")");
			throw tex;
		}

		// Initialise the WrappedCommand element of the procedure
		proc.wcmd = new WrappedCommand();
		proc.wcmd.cmd = proc;
		proc.wcmd.cmdEpoch = 1;
		proc.wcmd.deleted = false;
		proc.wcmd.hashKey = null;
		proc.wcmd.ns = null; // will be filled in during [apply]

		TclLambda lambda = new TclLambda(proc, ns);

		return lambda;
	}

	/**
	 * The string representation of a lambda is generated (if necessary) when
	 * the lambda is first created, and should then never need to be regenerated.
	 * @throws TclRuntimeError always.
	 */
	@Override
	public String toString() {
		throw new TclRuntimeError("string rep of lambda expression is inexplicably null!");
	}

}