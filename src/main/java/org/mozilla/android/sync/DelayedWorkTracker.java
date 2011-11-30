package org.mozilla.android.sync;

/**
 * A little class to allow us to maintain a count of extant 
 * things (in our case, callbacks that need to fire), and 
 * some work that we want done when that count hits 0.
 *
 * @author rnewman
 *
 */
public class DelayedWorkTracker {
  protected Runnable workItem = null; 
  protected int outstandingCount = 0;

  public int incrementOutstanding() {
    synchronized(this) {
      return ++outstandingCount;
    }
  }
  public int decrementOutstanding() {
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
    boolean runnableNow = false;
    synchronized(this) {
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
      item.run();
    }
  }
}