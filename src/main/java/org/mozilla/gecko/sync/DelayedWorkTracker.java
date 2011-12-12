package org.mozilla.gecko.sync;

import android.util.Log;

/**
 * A little class to allow us to maintain a count of extant
 * things (in our case, callbacks that need to fire), and
 * some work that we want done when that count hits 0.
 *
 * @author rnewman
 *
 */
public class DelayedWorkTracker {
  private static final String LOG_TAG = "DelayedWorkTracker";
  protected Runnable workItem = null;
  protected int outstandingCount = 0;

  public int incrementOutstanding() {
    Log.d(LOG_TAG, "Incrementing outstanding.");
    synchronized(this) {
      return ++outstandingCount;
    }
  }
  public int decrementOutstanding() {
    Log.d(LOG_TAG, "Decrementing outstanding.");
    Runnable job = null;
    int count;
    synchronized(this) {
      if ((count = --outstandingCount) == 0 &&
          workItem != null) {
        job = workItem;
        workItem = null;
      } else {
        return count;
      }
    }
    job.run();
    // In case it's changed.
    return getOutstandingOperations();
  }
  public int getOutstandingOperations() {
    synchronized(this) {
      return outstandingCount;
    }
  }
  public void delayWorkItem(Runnable item) {
    Log.d(LOG_TAG, "delayWorkItem.");
    boolean runnableNow = false;
    synchronized(this) {
      Log.d(LOG_TAG, "outstandingCount: " + outstandingCount);
      if (outstandingCount == 0) {
        runnableNow = true;
      } else {
        if (workItem != null) {
          throw new IllegalStateException("Work item already set!");
        }
        workItem = item;
      }
    }
    if (runnableNow) {
      Log.d(LOG_TAG, "Running item now.");
      item.run();
    }
  }
}