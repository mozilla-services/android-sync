package org.mozilla.android.sync.synchronizer;

import org.mozilla.android.sync.repositories.domain.Record;

/**
 * Consume records from a queue inside a RecordsChannel, storing them serially.
 * @author rnewman
 *
 */
class RecordConsumer implements Runnable {
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
    synchronized (monitor) {
      this.stopEventually = true;
      this.stopImmediately = immediately;
      monitor.notify();
    }
  }

  private Object storeSerializer = new Object();
  public void stored() {
    synchronized (storeSerializer) {
      storeSerializer.notify();
    }
  }
  private void storeSerially(Record record) {
    synchronized (storeSerializer) {
      this.delegate.store(record);
      try {
        storeSerializer.wait();
      } catch (InterruptedException e) {
        // TODO
      }
    }
  }

  @Override
  public void run() {
    while (true) {
      synchronized (monitor) {
        if (stopImmediately) {
          delegate.getQueue().clear();
          delegate.consumerIsDone();
          return;
        }
      }
      // The queue is concurrent-safe.
      while (!delegate.getQueue().isEmpty()) {
        Record record = delegate.getQueue().remove();
        // Block here, allowing us to process records
        // serially.
        this.storeSerially(record);
      }
      synchronized (monitor) {
        if (stopEventually) {
          delegate.consumerIsDone();
        }
        try {
          monitor.wait(10000);
        } catch (InterruptedException e) {
          // TODO
        }
      }
    }
  }
}