package org.mozilla.gecko.sync.synchronizer;

import org.mozilla.gecko.sync.repositories.domain.Record;

import android.util.Log;

/**
 * Consume records from a queue inside a RecordsChannel, storing them serially.
 * @author rnewman
 *
 */
class RecordConsumer implements Runnable {
  private static final String LOG_TAG = "RecordConsumer";
  private boolean stopEventually = false;
  private boolean stopImmediately = false;
  private RecordsConsumerDelegate delegate;

  public RecordConsumer(RecordsConsumerDelegate delegate) {
    this.delegate = delegate;
  }

  private Object monitor = new Object();
  public void doNotify() {
    synchronized (monitor) {
      monitor.notify();
    }
  }

  public void stop(boolean immediately) {
    Log.d(LOG_TAG, "Called stop(" + immediately + ").");
    synchronized (monitor) {
      Log.d(LOG_TAG, "stop() took monitor.");
      this.stopEventually = true;
      this.stopImmediately = immediately;
      monitor.notify();
      Log.d(LOG_TAG, "stop() dropped monitor.");
    }
  }

  private Object storeSerializer = new Object();
  public void stored() {
    Log.d(LOG_TAG, "Record stored. Notifying.");
    synchronized (storeSerializer) {
      Log.d(LOG_TAG, "stored() took storeSerializer.");
      storeSerializer.notify();
      Log.d(LOG_TAG, "stored() dropped storeSerializer.");
    }
  }
  private void storeSerially(Record record) {
    Log.d(LOG_TAG, "New record to store.");
    synchronized (storeSerializer) {
      Log.d(LOG_TAG, "storeSerially() took storeSerializer.");
      Log.d(LOG_TAG, "Storing...");
      try {
        this.delegate.store(record);
      } catch (Exception e) {
        Log.w(LOG_TAG, "Got exception in store. Not waiting.", e);
        return;      // So we don't block for a stored() that never comes.
      }
      try {
        storeSerializer.wait();
      } catch (InterruptedException e) {
        // TODO
      }
      Log.d(LOG_TAG, "storeSerially() dropped storeSerializer.");
    }
  }

  @Override
  public void run() {
    while (true) {
      synchronized (monitor) {
        Log.d(LOG_TAG, "run() took monitor.");
        if (stopImmediately) {
          Log.d(LOG_TAG, "Stopping immediately. Clearing queue.");
          delegate.getQueue().clear();
          Log.d(LOG_TAG, "Notifying consumer.");
          delegate.consumerIsDone();
          return;
        }
        Log.d(LOG_TAG, "run() dropped monitor.");
      }
      // The queue is concurrent-safe.
      while (!delegate.getQueue().isEmpty()) {
        Log.d(LOG_TAG, "Grabbing record...");
        Record record = delegate.getQueue().remove();
        // Block here, allowing us to process records
        // serially.
        Log.d(LOG_TAG, "Invoking storeSerially...");
        this.storeSerially(record);
        Log.d(LOG_TAG, "Done with record.");
      }
      synchronized (monitor) {
        Log.d(LOG_TAG, "run() took monitor.");

        if (stopEventually) {
          Log.d(LOG_TAG, "Done with records and told to stop. Notifying consumer.");
          delegate.consumerIsDone();
          return;
        }
        try {
          Log.d(LOG_TAG, "Not told to stop but no records. Waiting.");
          monitor.wait(10000);
        } catch (InterruptedException e) {
          // TODO
        }
        Log.d(LOG_TAG, "run() dropped monitor.");
      }
    }
  }
}