/*
 * Notifier.java --
 *
 *	Implements the Jacl version of the Notifier class.
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: Notifier.java,v 1.11 2006/04/27 02:16:13 mdejong Exp $
 *
 */

package tcl.lang;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Implements the Jacl version of the Notifier class. The Notifier is the
 * lowest-level part of the event system. It is used by higher-level event
 * sources such as file, JavaBean and timer events. The Notifier manages an
 * event queue that holds TclEvent objects.
 * 
 * The Jacl notifier is designed to run in a multi-threaded environment. Each
 * notifier instance is associated with a primary thread. Any thread can queue
 * (or dequeue) events using the queueEvent (or deleteEvents) call. However,
 * only the primary thread may process events in the queue using the
 * doOneEvent() call. Attepmts to call doOneEvent from a non-primary thread will
 * cause a TclRuntimeError.
 * 
 * This class does not have a public constructor and thus cannot be
 * instantiated. The only way to for a Tcl extension to get an Notifier is to
 * call Interp.getNotifier() (or Notifier.getNotifierForThread() ), which
 * returns the Notifier for that interpreter (thread).
 */
public class Notifier implements EventDeleter {
	
	/**
	 * timeout for doOneEvent wait()
	 */
	private static final long PAUSE_WAIT = 100L;

	/**
	 *  First pending event, or null if none.
	 */

	private TclEvent firstEvent;

	/**
	 *  Last pending event, or null if none.
	 */

	private TclEvent lastEvent;

	/**
	 *  Last high-priority event in queue, or null if none.
	 */

	private TclEvent markerEvent;

	/**
	 *  Event that was just processed by serviceEvent
	 */

	private TclEvent servicedEvent = null;

	/**
	 * The primary thread of this notifier. Only this thread should process
	 * 	events from the event queue.
	 */

	Thread primaryThread;

	/**
	 *  Stores the Notifier for each thread.
	 */

	private static HashMap<Thread, Notifier> notifierTable = new HashMap<>();

	/**
	 *  List of registered timer handlers.
	 */

	ArrayList<TimerHandler> timerList;

	/**
	 *  Used to distinguish older timer handlers from recently-created ones.
	 */

	int timerGeneration;

	/** True if there is a pending timer event in the event queue, false
	 * otherwise.
	 */
	boolean timerPending;

	/**
	 *  List of registered idle handlers.
	 */
	ArrayList<IdleHandler> idleList;

	/**
	 *  Used to distinguish older idle handlers from recently-created ones.
	 */

	int idleGeneration;

	/**
	 *  Reference count of the notifier. It's used to tell when a notifier is no longer needed
	 */

	int refCount;

	/**
	 * Creates a Notifier instance.
	 * 
	 * @param primaryThread the primary thread for this notifier
	 */

	private Notifier(Thread primaryThread) // The primary thread for this
											// Notifier.
	{
		if (primaryThread == null) {
			throw new NullPointerException("primaryThread");
		}
		this.primaryThread = primaryThread;
		firstEvent = null;
		lastEvent = null;
		markerEvent = null;

		timerList = new ArrayList<>();
		timerGeneration = 0;
		idleList = new ArrayList<>();
		idleGeneration = 0;
		timerPending = false;
		refCount = 0;
	}

	/**
	 * Get the notifier for this thread, creating the Notifier, when necessary.
	 * 
	 * @param thread the thread that owns this notifier
	 * @return The Notifier for this thread.
	 */

	public static synchronized Notifier getNotifierForThread(Thread thread) 
	{
		Notifier notifier = (Notifier) notifierTable.get(thread);
		if (notifier == null) {
			notifier = new Notifier(thread);
			notifierTable.put(thread, notifier);
		}

		return notifier;
	}

	/**
	 * Increment the reference count of the notifier. The notifier will be kept
	 * in the notifierTable (and alive) as long as its reference count is
	 * greater than zero.
	 */
	public synchronized void preserve() {
		if (refCount < 0) {
			throw new TclRuntimeError("Attempting to preserve a freed Notifier");
		}
		++refCount;
	}

	/**
	 * Decrement the reference count of the notifier. The notifier will be freed
	 * when its refCount goes from one to zero.
	 * Side effects: The notifier may be removed from the notifierTable when its
	 * refCount reaches zero.
	 */

	public synchronized void release() {
		if ((refCount == 0) && (primaryThread != null)) {
			throw new TclRuntimeError(
					"Attempting to release a Notifier before it's preserved");
		}
		if (refCount <= 0) {
			throw new TclRuntimeError("Attempting to release a freed Notifier");
		}
		--refCount;
		if (refCount == 0) {
			notifierTable.remove(primaryThread);
			primaryThread = null;
		}
	}

	/**
	 * Insert an event into the event queue at one of three positions: the head,
	 * the tail, or before a floating marker. Events inserted before the marker
	 * will be processed in first-in-first-out order, but before any events
	 * inserted at the tail of the queue. Events inserted at the head of the
	 * queue will be processed in last-in-first-out order.
	 * 
	 * Results: None.
	 * 
	 * Side effects: If this method is invoked by a non-primary thread, the
	 * primaryThread of this Notifier will be notified about the new event.
	 * 
	 * @param evt the event to be put on the queue
	 * @param position one of  TCL.QUEUE_TAIL, TCL.QUEUE_HEAD or TCL.QUEUE_MARK.
	 */

	public synchronized void queueEvent(TclEvent evt, int position) 
	{
		if (primaryThread == null) {
			// queueEvent() invoked after the Notifier has been
			// released. This could happen if this method was
			// invoked after all the Interp objects in this
			// thread have been disposed.

			throw new TclRuntimeError("Notifier.queueEvent() with "
					+ "no Interp() objects in the current thread");
		}

		evt.notifier = this;

		if (position == TCL.QUEUE_TAIL) {
			// Append the event on the end of the queue.

			evt.next = null;

			if (firstEvent == null) {
				firstEvent = evt;
			} else {
				lastEvent.next = evt;
			}
			lastEvent = evt;
		} else if (position == TCL.QUEUE_HEAD) {
			// Push the event on the head of the queue.

			evt.next = firstEvent;
			if (firstEvent == null) {
				lastEvent = evt;
			}
			firstEvent = evt;
		} else if (position == TCL.QUEUE_MARK) {
			// Insert the event after the current marker event and advance
			// the marker to the new event.

			if (markerEvent == null) {
				evt.next = firstEvent;
				firstEvent = evt;
			} else {
				evt.next = markerEvent.next;
				markerEvent.next = evt;
			}
			markerEvent = evt;
			if (evt.next == null) {
				lastEvent = evt;
			}
		} else {
			// Wrong flag.

			throw new TclRuntimeError(
					"wrong position \""
							+ position
							+ "\", must be TCL.QUEUE_HEAD, TCL.QUEUE_TAIL or TCL.QUEUE_MARK");
		}

		if (Thread.currentThread() != primaryThread) {
			notifyAll();
		}
	}

	/**
	 * Calls an EventDeleter for each event in the queue and deletes those for
	 * which deleter.deleteEvent() returns 1. Events for which the deleter
	 * returns 0 are left in the queue. This method includes code to handle the
	 * special case of the Notifier wanting to delete a single event after is
	 * has been serviced. This method is concurrent safe.
	 * 
	 * Results: None.
	 * 
	 * Side effects: Potentially removes one or more events from the event
	 * queue.
	 * 
	 * @param deleter the deleter that checks whether an event should be removed
	 */
	public synchronized void deleteEvents(EventDeleter deleter) 
	{
		TclEvent evt, prev;
		TclEvent servicedEvent = null;

		// Handle the special case of deletion of a single event that was just
		// processed by the serviceEvent() method.

		if (deleter == this) {
			servicedEvent = this.servicedEvent;
			if (servicedEvent == null)
				throw new TclRuntimeError(
						"servicedEvent was not set by serviceEvent()");
			this.servicedEvent = null;
		}

		for (prev = null, evt = firstEvent; evt != null; evt = evt.next) {
			if (((servicedEvent == null) && (deleter.deleteEvent(evt) == 1))
					|| (evt == servicedEvent)) {
				if (evt == firstEvent) {
					firstEvent = evt.next;
				} else {
					prev.next = evt.next;
				}
				if (evt.next == null) {
					lastEvent = prev;
				}
				if (evt == markerEvent) {
					markerEvent = prev;
				}
				if (evt == servicedEvent) {
					servicedEvent = null;
					break; // Just service this one event in the special case
				}
			} else {
				prev = evt;
			}
		}
		if (servicedEvent != null) {
			throw new TclRuntimeError(
					"servicedEvent was not removed from the queue");
		}
	}

	/**
	 * This method is required to implement the EventDeleter interface It is not
	 * actually used though, see deleteEvents method for special casing of the
	 * deletion of a specific event.
	 */
	@Deprecated
	public int deleteEvent(TclEvent evt) {
		throw new TclRuntimeError(
				"The Notifier.deleteEvent() method should not be called");
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * serviceEvent --
	 * 
	 * Process one event from the event queue.
	 * 
	 * Results: The return value is 1 if the procedure actually found an event
	 * to process. If no processing occurred, then 0 is returned.
	 * 
	 * Side effects: Invokes all of the event handlers for the highest priority
	 * event in the event queue. May collapse some events into a single event or
	 * discard stale events.
	 * 
	 * ----------------------------------------------------------------------
	 */

	int serviceEvent(int flags) // Indicates what events should be processed.
	// May be any combination of TCL.WINDOW_EVENTS
	// TCL.FILE_EVENTS, TCL.TIMER_EVENTS, or other
	// flags defined elsewhere. Events not
	// matching this will be skipped for processing
	// later.
	{
		TclEvent evt;

		// No event flags is equivalent to TCL_ALL_EVENTS.

		if ((flags & TCL.ALL_EVENTS) == 0) {
			flags |= TCL.ALL_EVENTS;
		}

		// Loop through all the events in the queue until we find one
		// that can actually be handled.

		evt = null;
		while ((evt = getAvailableEvent(evt)) != null) {
			// Call the handler for the event. If it actually handles the
			// event then free the storage for the event. There are two
			// tricky things here, both stemming from the fact that the event
			// code may be re-entered while servicing the event:
			//
			// 1. Set the "isProcessing" field to true. This is a signal to
			// ourselves that we shouldn't reexecute the handler if the
			// event loop is re-entered.
			// 2. When freeing the event, must search the queue again from the
			// front to find it. This is because the event queue could
			// change almost arbitrarily while handling the event, so we
			// can't depend on pointers found now still being valid when
			// the handler returns.

			evt.isProcessing = true;

			if (evt.processEvent(flags) != 0) {
				evt.isProcessed = true;
				// Don't allocate/grab the monitor for the event unless sync()
				// has been called in another thread. This is thread safe
				// since sync() checks the isProcessed flag before calling wait.
				if (evt.needsNotify) {
					synchronized (evt) {
						evt.notifyAll();
					}
				}
				// Remove this specific event from the queue
				servicedEvent = evt;
				deleteEvents(this);
				return 1;
			} else {
				// The event wasn't actually handled, so we have to
				// restore the isProcessing field to allow the event to be
				// attempted again.

				evt.isProcessing = false;
			}

			// The handler for this event asked to defer it. Just go on to
			// the next event.

			continue;
		}
		return 0;
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * getAvailableEvent --
	 * 
	 * Search through the internal event list to find the first event that is
	 * has not being processed AND the event is not equal to the given
	 * 'skipEvent'. This method is concurrent safe.
	 * 
	 * Results: The return value is a pointer to the first found event that can
	 * be processed. If no event is found, this method returns null.
	 * 
	 * Side effects: This method synchronizes on the 'notifierMutex', which will
	 * block any other thread from adding or removing events from the event
	 * queue.
	 * 
	 * ----------------------------------------------------------------------
	 */

	private synchronized TclEvent getAvailableEvent(TclEvent skipEvent) // Indicates
																		// that
																		// the
																		// given
																		// event
																		// should
																		// not
	// be returned. This argument can be null.
	{
		TclEvent evt;

		for (evt = firstEvent; evt != null; evt = evt.next) {
			if ((evt.isProcessing == false) && (evt.isProcessed == false)
					&& (evt != skipEvent)) {
				return evt;
			}
		}
		return null;
	}

	/**
	 * Process a single event of some sort. If there's no work to do, wait for
	 * an event to occur, then process it. May delay execution of process while
	 * waiting for an event, unless TCL.DONT_WAIT is set in the flags argument.
	 * 
	 * Side effects: May delay execution of process while waiting for an event,
	 * unless TCL.DONT_WAIT is set in the flags argument. Event sources are
	 * invoked to check for and queue events. Event handlers may produce
	 * arbitrary side effects.
	 * 
	 * @param flags
	 *            Miscellaneous flag values: may be any combination of
	 *            TCL.DONT_WAIT, TCL.WINDOW_EVENTS, TCL.FILE_EVENTS, 
	 *            TCL.TIMER_EVENTS, TCL.IDLE_EVENTS, or others defined by
	 *            event sources.
	 * @return 1 if the procedure actually found an event to process. If no
	 *         processing occurred, then 0 is returned (this can happen if the
	 *         TCL.DONT_WAIT flag is set or if there are no event handlers to
	 *         wait for in the set specified by flags).
	 */

	public int doOneEvent(int flags)
	{
		final boolean debug = false;

		int result = 0;

		if (primaryThread == null) {
			// queueEvent() invoked after the Notifier has been
			// released. This could happen if this method was
			// invoked after all the Interp objects in this
			// thread have been disposed.

			throw new TclRuntimeError("Notifier.doOneEvent() with "
					+ "no Interp() objects in the current thread");
		}

		// No event flags is equivalent to TCL_ALL_EVENTS.

		if ((flags & TCL.ALL_EVENTS) == 0) {
			flags |= TCL.ALL_EVENTS;
		}

		// The core of this procedure is an infinite loop, even though
		// we only service one event. The reason for this is that we
		// may be processing events that don't do anything inside of Tcl.

		while (true) {
			// If idle events are the only things to service, skip the
			// main part of the loop and go directly to handle idle
			// events (i.e. don't wait even if TCL_DONT_WAIT isn't set).

			if ((flags & TCL.ALL_EVENTS) == TCL.IDLE_EVENTS) {
				return serviceIdle();
			}

			long sysTime = System.currentTimeMillis();

			// If some timers have been expired, queue them into the
			// event queue. We can't process expired times right away,
			// because there may already be other events on the queue.

			if (!timerPending && (timerList.size() > 0)) {
				TimerHandler h = (TimerHandler) timerList.get(0);

				if (h.atTime <= sysTime) {
					TimerEvent event = new TimerEvent();
					event.notifier = this;
					queueEvent(event, TCL.QUEUE_TAIL);
					timerPending = true;
				}
			}

			// Service a queued event, if there are any.

			if (serviceEvent(flags) != 0) {
				result = 1;
				// file event handling thread can keep putting events in the queue, thus
				// starving idle event handling.  service idle event here, if any,
				if ((flags & TCL.IDLE_EVENTS) != 0) {
					serviceIdle();
				}
				break;
			}

			// There is no event on the queue. Check for idle events.

			if ((flags & TCL.IDLE_EVENTS) != 0) {
				if (serviceIdle() != 0) {
					result = 1;
					break;
				}
			}

			if ((flags & TCL.DONT_WAIT) != 0) {
				break;
			}

			// We don't have any event to service. We'll wait if
			// TCL.DONT_WAIT. When the following wait() call returns,
			// one of the following things may happen:
			//
			// (1) waitTime milliseconds has elasped (if waitTime != 0);
			//
			// (2) The primary notifier has been notify()'ed by other threads:
			// (a) an event is queued by queueEvent().
			// (b) a timer handler was created by new TimerHandler();
			// (c) an idle handler was created by new IdleHandler();
			// (3) We receive an InterruptedException.
			//

			try {
				// Don't acquire the monitor until we are about to wait
				// for notification from another thread. It is critical
				// that this entire method not be synchronized since
				// a call to processEvent via serviceEvent could take
				// a very long time. We don't want the monitor held
				// during that time since that would force calls to
				// queueEvent in other threads to wait.

				synchronized (this) {
					if (timerList.size() > 0) {
						TimerHandler h = (TimerHandler) timerList.get(0);
						long waitTime = h.atTime - sysTime;
						if (waitTime > 0) {
							wait(waitTime<PAUSE_WAIT ? waitTime: PAUSE_WAIT);
						}
					} else {
						wait(PAUSE_WAIT);
					}
				} // synchronized (this)
			} catch (InterruptedException e) {
				// We ignore any InterruptedException and loop continuously
				// until we receive an event.
			}
		}

		return result;
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * serviceIdle --
	 * 
	 * Service all idle handlers that have been registered in the notifier.
	 * 
	 * Results: 1 if any idle handlers have been processed. 0 otherwise.
	 * 
	 * Side effects: The idle handlers may have arbitrary side effects.
	 * 
	 * ----------------------------------------------------------------------
	 */

	private int serviceIdle() {
		int result = 0;
		int gen = idleGeneration;
		idleGeneration++;

		// The code below is trickier than it may look, for the following
		// reasons:
		//
		// 1. New handlers can get added to the list while the current
		// one is being processed. If new ones get added, we don't
		// want to process them during this pass through the list (want
		// to check for other work to do first). This is implemented
		// using the generation number in the handler: new handlers
		// will have a different generation than any of the ones currently
		// on the list.
		// 2. The handler can call doOneEvent, so we have to remove
		// the handler from the list before calling it. Otherwise an
		// infinite loop could result.

		while (idleList.size() > 0) {
			IdleHandler h = (IdleHandler) idleList.get(0);
			if (h.generation > gen) {
				break;
			}
			idleList.remove(0);
			if (h.invoke() != 0) {
				result = 1;
			}
		}

		return result;
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * hasActiveInterps --
	 * 
	 * Return true if this Notifier is processing events for 1 or more active
	 * Interp objects. When an Interp is disposed() of it decrements the
	 * refCount of the Notifier object. When the last active Interp in a thread
	 * is disposed of, the Notifier is disposed of. This method will return true
	 * for a Notifier object that has active Interp object and false when the
	 * Notifier has been disposed of because the last active interp was disposed
	 * of.
	 * 
	 * Results: None.
	 * 
	 * Side effects: None.
	 * 
	 * ----------------------------------------------------------------------
	 */

	public synchronized boolean hasActiveInterps() {
		if (primaryThread == null) {
			return false;
		} else {
			return true;
		}
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * processTclEvents --
	 * 
	 * This util method is provided for use in a thread dedicated to processing
	 * events from the Tcl event queue. This method must only be used as the
	 * outermost event processing loop. It is not legal to use this method from
	 * any code that could be invoked by Tcl. This method supports interps that
	 * will make use of the setInterrupted() API.
	 * 
	 * If only one Interp exists in the thread and the interp is interrupted,
	 * then this method will return. If more than one interp exists in the
	 * thread and one interp is interrupted, then events for the other interps
	 * will continue to be processed. This method will return when all the
	 * interps in the current thread have been disposed of. This is a convience
	 * method only, there is no reason this logic could not appear in user code.
	 * 
	 * ----------------------------------------------------------------------
	 */

	public static void processTclEvents(Notifier notifier) {
		while (notifier.hasActiveInterps()) {
			try {
				notifier.doOneEvent(TCL.ALL_EVENTS);
			} catch (TclInterruptedException tie) {
				tie.disposeInterruptedInterp();
			}
		}

		// The while loop will exit when the last
		// interp is disposed of. If this was
		// called by Thread.run() then the thread
		// will die when that method terminates.
		// If "exit" is called the process will
		// terminate without unwinding the stack.

		return;
	}

} // end Notifier

// This class is used to service timer events. When one or more timers
// have expired but not processed, one TimerEvent will be generated
// and put into the event queue. When the TimerEvent is pulled off the
// queue, it will process all expired timers in a bunch.

class TimerEvent extends TclEvent {

	// The notifier that owns this TimerEvent.

	Notifier notifier;

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * TimerHandlerEventProc -> processEvent
	 * 
	 * This function is called by Tcl_ServiceEvent when a timer event reaches
	 * the front of the event queue. This function handles the event by invoking
	 * the callbacks for all timers that are ready.
	 * 
	 * Results: Returns 1 if the event was handled, meaning it should be removed
	 * from the queue. Returns 0 if the event was not handled, meaning it should
	 * stay on the queue. The only time the event isn't handled is if the
	 * TCL.TIMER_EVENTS flag bit isn't set.
	 * 
	 * Side effects: The TimerHandler may have arbitrary side effects while
	 * processing the event.
	 * 
	 * ----------------------------------------------------------------------
	 */

	public int processEvent(int flags) // Same as flags passed to
										// Notifier.doOneEvent.
	{
		// Do nothing if timers aren't enabled. This leaves the event on the
		// queue, so we will get to it as soon as ServiceEvents() is called with
		// timers enabled.

		if ((flags & TCL.TIMER_EVENTS) == 0) {
			return 0;
		}

		long sysTime = System.currentTimeMillis();
		int gen = notifier.timerGeneration;
		notifier.timerGeneration++;

		// The code below is trickier than it may look, for the following
		// reasons:
		//
		// 1. New handlers can get added to the list while the current
		// one is being processed. If new ones get added, we don't
		// want to process them during this pass through the list to
		// avoid starving other event sources. This is implemented
		// using the timer generation number: new handlers will have
		// a newer generation number than any of the ones currently on
		// the list.
		// 2. The handler can call doOneEvent, so we have to remove
		// the handler from the list before calling it. Otherwise an
		// infinite loop could result.
		// 3. Tcl_DeleteTimerHandler can be called to remove an element from the
		// list while a handler is executing, so the list could change
		// structure during the call.
		// 4. Because we only fetch the current time before entering the loop,
		// the only way a new timer will even be considered runnable is if
		// its expiration time is within the same millisecond as the
		// current time. This is fairly likely on Windows, since it has
		// a course granularity clock. Since timers are placed
		// on the queue in time order with the most recently created
		// handler appearing after earlier ones with the same expiration
		// time, we don't have to worry about newer generation timers
		// appearing before later ones.

		notifier.timerPending = false;

		while (notifier.timerList.size() > 0) {
			TimerHandler h = (TimerHandler) notifier.timerList.get(0);
			if (h.generation > gen) {
				break;
			}
			if (h.atTime > sysTime) {
				break;
			}
			notifier.timerList.remove(0);
			h.invoke();
		}

		return 1;
	}

} // end TimerEvent

