/*
 * TclEvent.java --
 *
 *	Abstract class for describing an event in the Tcl notifier
 *	API.
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: TclEvent.java,v 1.3 2003/03/11 01:45:53 mdejong Exp $
 *
 */

package tcl.lang;

/**
 * This is an abstract class that describes an event in the Jacl
 * implementation of the notifier. It contains package protected
 * fields and methods that are accessed by the Jacl notifier. Tcl Blend
 * needs a different implementation of the TclEvent base class.
 *
 * The only public methods in this class are processEvent() and
 * sync(). These methods must appear in both the Jacl and Tcl Blend versions
 * of this class.
 */

public abstract class TclEvent {

	/**
	 * The notifier in which this event is queued.
	 */

	Notifier notifier = null;

	/**
	 * This flag is true if sync() has been called on this object.
	 */

	boolean needsNotify = false;

	/**
	 * True if this event is current being processing. This flag provents an
	 * event to be processed twice when the event loop is entered recursively.
	 */

	boolean isProcessing = false;

	/**
	 * True if this event has been processed.
	 */

	boolean isProcessed = false;

	/**
	 * Links to the next event in the event queue.
	 */

	TclEvent next;

	/**
	 * Process the event. Override this method to implement new types of events.
	 * 
	 * Note: this method is called by the primary thread of the notifier.
	 * 
	 * @param flags Miscellaneous flag values: may be any combination of
	 *        TCL.DONT_WAIT,  TCL.WINDOW_EVENTS, TCL.FILE_EVENTS, 
	 *        TCL.TIMER_EVENTS, TCL.IDLE_EVENTS, or others defined by event
	 *        sources - this is the same as the flags passed to Notifier.doOneEvent()
	 *        
	 * @return 1 means the event has been processed and can be removed from the
	 *         event queue. 0 means the event should be deferred for processing
	 *         later.
	 */

	public abstract int processEvent(int flags); 

	/**
	 * Wait until this event has been processed.
	 */
	public final void sync() {
		if (notifier == null) {
			throw new TclRuntimeError(
					"TclEvent is not queued when sync() is called");
		}

		if (Thread.currentThread() == notifier.primaryThread) {
			while (!isProcessed) {
				notifier.serviceEvent(0);
			}
		} else {
			synchronized (this) {
				needsNotify = true;
				while (!isProcessed) {
					try {
						wait(0);
					} catch (InterruptedException e) {
						// Another thread has sent us an "interrupt"
						// signal. We ignore it and continue waiting until
						// the event is processed.

						continue;
					}
				}
			}
		}
	}

} // end TclEvent

