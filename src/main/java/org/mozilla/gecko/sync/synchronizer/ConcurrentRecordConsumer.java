/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.synchronizer;

import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.repositories.domain.Record;

import android.util.Log;

/**
 * Consume records from a queue inside a RecordsChannel, as fast as we can.
 * TODO: rewrite this in terms of an ExecutorService and a CompletionService.
 * See Bug 713483.
 *
 * @author rnewman
 *
 */
class ConcurrentRecordConsumer extends RecordConsumer {
  private static final String LOG_TAG = "CRecordConsumer";

  /**
   * When this is true and all records have been processed, the consumer
   * will notify its delegate.
   */
  protected boolean allRecordsQueued = false;
  private long counter = 0;

  public ConcurrentRecordConsumer(RecordsConsumerDelegate delegate) {
    this.delegate = delegate;
  }

  private static void info(String message) {
    Logger.info(LOG_TAG, message);
  }

  private static void debug(String message) {
    Logger.debug(LOG_TAG, message);
  }

  private static void trace(String message) {
    Logger.trace(LOG_TAG, message);
  }

  private Object monitor = new Object();
  @Override
  public void doNotify() {
    synchronized (monitor) {
      monitor.notify();
    }
  }

  @Override
  public void queueFilled() {
    debug("Queue filled.");
    synchronized (monitor) {
      this.allRecordsQueued = true;
      monitor.notify();
    }
  }

  @Override
  public void halt() {
    synchronized (monitor) {
      this.stopImmediately = true;
      monitor.notify();
    }
  }

  private Object countMonitor = new Object();
  @Override
  public void stored() {
    trace("Record stored. Notifying.");
    synchronized (countMonitor) {
      counter++;
    }
  }

  private void consumerIsDone() {
    info("Consumer is done. Processed " + counter + ((counter == 1) ? " record." : " records."));
    delegate.consumerIsDone(!allRecordsQueued);
  }

  @Override
  public void run() {
    while (true) {
      synchronized (monitor) {
        trace("run() took monitor.");
        if (stopImmediately) {
          debug("Stopping immediately. Clearing queue.");
          delegate.getQueue().clear();
          debug("Notifying consumer.");
          consumerIsDone();
          return;
        }
        debug("run() dropped monitor.");
      }
      // The queue is concurrent-safe.
      while (!delegate.getQueue().isEmpty()) {
        trace("Grabbing record...");
        Record record = delegate.getQueue().remove();
        trace("Storing record... " + delegate);
        try {
          delegate.store(record);
        } catch (Exception e) {
          // TODO: Bug 709371: track records that failed to apply.
          Log.e(LOG_TAG, "Caught error in store.", e);
        }
        trace("Done with record.");
      }
      synchronized (monitor) {
        trace("run() took monitor.");

        if (allRecordsQueued) {
          debug("Done with records and no more to come. Notifying consumerIsDone.");
          consumerIsDone();
          return;
        }
        if (stopImmediately) {
          debug("Done with records and told to stop immediately. Notifying consumerIsDone.");
          consumerIsDone();
          return;
        }
        try {
          debug("Not told to stop but no records. Waiting.");
          monitor.wait(10000);
        } catch (InterruptedException e) {
          // TODO
        }
        trace("run() dropped monitor.");
      }
    }
  }
}
