package tcl.lang.cmd;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TclException;
import tcl.lang.TclIO;
import tcl.lang.TclIndex;
import tcl.lang.TclInteger;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;
import tcl.lang.channel.Channel;
import tcl.lang.channel.Fcopy;

/**
 * This class implements the 'fcopy' Tcl command
 * 
 * @author Dan Bodoh
 * 
 */
public class FcopyCmd implements Command {

	static final private String[] validOpts = { "-size", "-command" };
	static final private int OPT_SIZE = 0;
	static final private int OPT_COMMAND = 1;

	public void cmdProc(Interp interp, TclObject[] objv) throws TclException {
		long size = -1;
		String callback = null;
		if (objv.length != 3 && objv.length != 5 && objv.length != 7) {
			throw new TclNumArgsException(interp, 1, objv, "input output ?-size size? ?-command callback?");
		}

		for (int argIndex = 3; argIndex < objv.length; argIndex++) {
			int option = TclIndex.get(interp, objv[argIndex], validOpts, "switch", 0);
			switch (option) {
			case OPT_COMMAND:
				callback = objv[++argIndex].toString();
				break;
			case OPT_SIZE:
				size = TclInteger.getLong(interp, objv[++argIndex]);
				break;
			}
		}
		Channel source = TclIO.getChannel(interp, objv[1].toString());
		Channel destination = TclIO.getChannel(interp, objv[2].toString());

		if (source == null) {
			throw new TclException(interp, "can not find channel named \"" + objv[1] + "\"");
		}
		if (destination == null) {
			throw new TclException(interp, "can not find channel named \"" + objv[2] + "\"");
		}

		Fcopy fcopy = new Fcopy(interp, source, destination, size, callback);

		interp.setResult(fcopy.start());
	}

}
