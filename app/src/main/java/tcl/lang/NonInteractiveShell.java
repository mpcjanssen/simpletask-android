package tcl.lang;

/**
 * This class creates a Shell that has tcl_interactive set to 0.  This shell
 * will not print the prompt or the command results to standard out.  It's main
 * purpose is for testing JTCL.
 * 
 * The TCL test suite execs a shell with redirected stdin  to test several command, and 
 * inspects the results from stdout (which shouldn't include the '%' prompt).  
 * C TCL automatically creates a non-interactive shell when stdin is not from a terminal. 
 * Since the Java VM can't detect where stdin comes from, this NonInteractiveShell class can
 * be used instead.  
 *
 */
public class NonInteractiveShell {
	public static void main(String args[]) {
		Shell.forceNonInteractive = true;
		Shell.main(args);
	}

}
