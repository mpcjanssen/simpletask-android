/*
 * EventDeleter.java --
 *
 *	Interface for deleting events in the notifier's event queue.
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: EventDeleter.java,v 1.1.1.1 1998/10/14 21:09:14 cvsadmin Exp $
 *
 */

package tcl.lang;

/**
 * This is the interface for deleting events in the notifier's event
 * queue. It's used together with the Notifier.deleteEvents() method.
 *
 */

public interface EventDeleter {

	/**
	 * This method is called once for each event in the event queue. It returns
	 * 1 for all events that should be deleted and 0 for events that should
	 * remain in the queue.
	 * 
	 * If this method determines that an event should be removed, it should
	 * perform appropriate clean up on the event object.
	 *
	 * @param evt Event to test whether it should be removed
	 * @return 1 means evt should be removed from the event queue. 0 otherwise.
	 */
	public int deleteEvent(TclEvent evt);

} // end EventDeleter

