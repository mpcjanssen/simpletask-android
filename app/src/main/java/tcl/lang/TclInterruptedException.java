/*
 * TclInterruptedException.java
 *
 * Copyright (c) 2006 Moses DeJong
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: TclInterruptedException.java,v 1.1 2006/04/27 02:16:13 mdejong Exp $
 *
 */

package tcl.lang;

/**
 * Signals that an interp has been interrupted via the Interp.setInterrupted()
 * API. This exception is used to unwind the Tcl stack and remove pending events
 * from the Tcl event queue.
 */

public final class TclInterruptedException extends RuntimeException {
	Interp interp;

	public TclInterruptedException(Interp interp) {
		this.interp = interp;
	}

	// This method should be invoked after the Tcl
	// stack has been fully unwound to cleanup
	// Interp state, remove any pending events,
	// and dispose of the Interp object.

	public void disposeInterruptedInterp() {
		interp.disposeInterrupted();
	}
}

// This class implements an event that will raise
// a TclInterruptedException in the interp. This
// event is queued from a thread other the one
// the interp is executing in, so be careful
// not to interact with the interp since it
// would not be thread safe. This event will
// wake up a Jacl thread waiting in a vwait
// or in the main processing loop. This class is
// used only in Jacl's Interp implementaton.

class TclInterruptedExceptionEvent extends TclEvent implements EventDeleter {
	Interp interp;
	boolean wasProcessed;
	boolean exceptionRaised;

	TclInterruptedExceptionEvent(Interp interp) {
		this.interp = interp;
		this.wasProcessed = false;
		this.exceptionRaised = false;
	}

	// processEvent() is invoked in the interp thread,
	// so this code can interact with the interp.

	public int processEvent(int flags) {
		wasProcessed = true;
		interp.checkInterrupted();

		// Should never reach here since
		// an earlier call to setInterrupted()
		// would cause checkInterrupted() to
		// raise a TclInterruptedException.
		// return code just makes the compiler happy.

		return 1;
	}

	// Implement EventDeleter so that this event can
	// delete itself from the pending event queue.
	// This method returns 1 when an event should
	// be deleted from the queue.

	public int deleteEvent(TclEvent evt) {
		if (evt == this) {
			return 1;
		}
		return 0;
	}

}
