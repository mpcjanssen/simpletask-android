/*
 * TimerHandler.java --
 *
 *	The API for defining timer event handler.
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: TimerHandler.java,v 1.3 2006/04/27 02:16:13 mdejong Exp $
 *
 */

package tcl.lang;

/**
 * This abstract class is used to define timer handlers.
 */
abstract public class TimerHandler {

	/**
	 * Back pointer to the notifier that will fire this timer.
	 */
	Notifier notifier;

	/**
	 * System time at (of after) which the timer should be fired.
	 */
	long atTime;

	/**
	 * True if the cancel() method has been called.
	 */
	boolean isCancelled;

	/**
	 * Used to distinguish older idle handlers from recently-created ones.
	 */
	int generation;

	/**
	 * Create a timer handler to be fired after the given time lapse.
	 * Side effects: The timer is registered in the list of timers in the given
	 * notifier. After milliseconds have elapsed, the processTimerEvent() method
	 * will be invoked exactly once inside the primary thread of the notifier.
	 * 
	 * @param n The notifier to fire the event
	 * @param milliseconds number of milliseconds to wait before invoking processTimerEvent()
	 */
	public TimerHandler(Notifier n, 
			long milliseconds) 
	{
		int i;

		atTime = System.currentTimeMillis() + milliseconds;
		notifier = n;
		isCancelled = false;

		/*
		 * Add timer event to the pending timer list in the correct position
		 * (ordered by event firing time).
		 * 
		 * NOTE: it's very important that if two timer handlers have the same
		 * atTime, the newer timer handler always goes after the older handler
		 * in the list. See comments in Notifier.TimerEvent.processEvent() for
		 * details.
		 */

		synchronized (notifier) {
			generation = notifier.timerGeneration;

			for (i = 0; i < notifier.timerList.size(); i++) {
				TimerHandler q = (TimerHandler) notifier.timerList.get(i);
				if (atTime < q.atTime) {
					break;
				}
			}
			notifier.timerList.add(i, this);

			if (Thread.currentThread() != notifier.primaryThread) {
				notifier.notifyAll();
			}
		}
	}

	/**
	 * Mark this timer handler as cancelled so that it won't be invoked.
	 * Side effects: The timer handler is marked as cancelled so that its
	 * processTimerEvent() method will not be called. If the timer has already
	 * fired, then nothing this call has no effect.
	 */
	public synchronized void cancel() {
		if (isCancelled) {
			return;
		}

		isCancelled = true;

		synchronized (notifier) {
			for (int i = 0; i < notifier.timerList.size(); i++) {
				if (notifier.timerList.get(i) == this) {
					notifier.timerList.remove(i);

					/*
					 * We can return now because the same timer can be
					 * registered only once in the list of timers.
					 */

					return;
				}
			}
		}
	}

	/**
	 * Execute the timer handler if it has not been cancelled. This method
	 * should be called by the notifier only.
	 * 
	 * Because the timer handler may be being cancelled by another thread, both
	 * this method and cancel() must be synchronized to ensure correctness.
	 * 
	 * @return 0 if the handler was not executed because it was already
	 * cancelled, 1 otherwise
	 */
	synchronized final int invoke() {
		/*
		 * The timer may be cancelled after it was put on the event queue. Check
		 * its isCancelled field to make sure it's not cancelled.
		 */

		if (!isCancelled) {
			processTimerEvent();
			return 1;
		} else {
			return 0;
		}
	}

	/**
	 * This method is called when the timer is expired. Override This method to
	 * implement your own timer handlers.
	 */
	abstract public void processTimerEvent();

} // end TimerHandler

