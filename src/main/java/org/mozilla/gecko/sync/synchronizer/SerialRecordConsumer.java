/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Android Sync Client.
 *
 * The Initial Developer of the Original Code is
 * the Mozilla Foundation.
 * Portions created by the Initial Developer are Copyright (C) 2011
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *   Richard Newman <rnewman@mozilla.com>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

package org.mozilla.gecko.sync.synchronizer;

import org.mozilla.gecko.sync.repositories.domain.Record;

import android.util.Log;

/**
 * Consume records from a queue inside a RecordsChannel, storing them serially.
 * @author rnewman
 *
 */
class SerialRecordConsumer extends RecordConsumer {
  private static final String LOG_TAG = "SerialRecordConsumer";
  protected boolean stopEventually = false;
  private volatile long counter = 0;

  public SerialRecordConsumer(RecordsConsumerDelegate delegate) {
    this.delegate = delegate;
  }

  private static void info(String message) {
    System.out.println("INFO: " + message);
    Log.i(LOG_TAG, message);
  }

  private static void warn(String message, Exception ex) {
    System.out.println("WARN: " + message);
    Log.w(LOG_TAG, message, ex);
  }

  private static void debug(String message) {
    System.out.println("DEBUG: " + message);
    Log.d(LOG_TAG, message);
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
      this.stopEventually = true;
      monitor.notify();
    }
  }

  @Override
  public void halt() {
    debug("Halting.");
    synchronized (monitor) {
      this.stopEventually = true;
      this.stopImmediately = true;
      monitor.notify();
    }
  }

  private Object storeSerializer = new Object();
  @Override
  public void stored() {
    debug("Record stored. Notifying.");
    synchronized (storeSerializer) {
      debug("stored() took storeSerializer.");
      counter++;
      storeSerializer.notify();
      debug("stored() dropped storeSerializer.");
    }
  }
  private void storeSerially(Record record) {
    debug("New record to store.");
    synchronized (storeSerializer) {
      debug("storeSerially() took storeSerializer.");
      debug("Storing...");
      try {
        this.delegate.store(record);
      } catch (Exception e) {
        warn("Got exception in store. Not waiting.", e);
        return;      // So we don't block for a stored() that never comes.
      }
      try {
        debug("Waiting...");
        storeSerializer.wait();
      } catch (InterruptedException e) {
        // TODO
      }
      debug("storeSerially() dropped storeSerializer.");
    }
  }

  private void consumerIsDone() {
    long counterNow = this.counter;
    info("Consumer is done. Processed " + counterNow + ((counterNow == 1) ? " record." : " records."));
    delegate.consumerIsDone(stopImmediately);
  }

  @Override
  public void run() {
    while (true) {
      synchronized (monitor) {
        debug("run() took monitor.");
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
        debug("Grabbing record...");
        Record record = delegate.getQueue().remove();
        // Block here, allowing us to process records
        // serially.
        debug("Invoking storeSerially...");
        this.storeSerially(record);
        debug("Done with record.");
      }
      synchronized (monitor) {
        debug("run() took monitor.");

        if (stopEventually) {
          debug("Done with records and told to stop. Notifying consumer.");
          consumerIsDone();
          return;
        }
        try {
          debug("Not told to stop but no records. Waiting.");
          monitor.wait(10000);
        } catch (InterruptedException e) {
          // TODO
        }
        debug("run() dropped monitor.");
      }
    }
  }
}