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

package org.mozilla.android.sync.synchronizer;

import java.util.concurrent.ConcurrentLinkedQueue;

import org.mozilla.android.sync.repositories.RepositorySession;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionBeginDelegate;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionFetchRecordsDelegate;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionStoreDelegate;
import org.mozilla.android.sync.repositories.domain.Record;

import android.util.Log;

/**
 * Pulls records from `source`, applying them to `sink`.
 * Notifies its delegate of errors and completion.
 *
 * @author rnewman
 *
 */
class RecordsChannel implements RepositorySessionFetchRecordsDelegate, RepositorySessionStoreDelegate, RecordsConsumerDelegate, RepositorySessionBeginDelegate {
  private static final String LOG_TAG = "RecordsChannel";
  public RepositorySession source;
  public RepositorySession sink;
  private RecordsChannelDelegate delegate;
  private long timestamp;
  private long end = -1;                     // Oo er, missus.

  private boolean sourceBegun = false;
  private boolean sinkBegun   = false;

  public RecordsChannel(RepositorySession source, RepositorySession sink, RecordsChannelDelegate delegate) {
    this.source = source;
    this.sink   = sink;
    this.delegate = delegate;
    this.timestamp = source.lastSyncTimestamp;
  }

  /*
   * We push fetched records into a queue.
   * A separate thread is waiting for us to notify it of work to do.
   * When we tell it to stop, it'll stop.
   * When it stops, we notify our delegate of completion.
   */
  private boolean waitingForQueueDone = false;
  ConcurrentLinkedQueue<Record> toProcess = new ConcurrentLinkedQueue<Record>();
  private RecordConsumer consumer;

  private void enqueue(Record record) {
    toProcess.add(record);
  }

  @Override
  public ConcurrentLinkedQueue<Record> getQueue() {
    return toProcess;
  }

  @Override
  public void consumerIsDone() {
    Log.d(LOG_TAG, "Consumer is done. Are we waiting for it? " + waitingForQueueDone);
    if (waitingForQueueDone) {
      waitingForQueueDone = false;
      delegate.onFlowCompleted(this, end);
    }
  }


  /**
   * Attempt to abort an outstanding fetch. Finish both sessions.
   */
  public void abort() {
    if (sourceBegun) {
      source.abort();
    }
    if (sinkBegun) {
      sink.abort();
    }
  }

  public void flow(RecordsChannelDelegate delegate) {
    source.begin(this);
  }


  @Override
  public void store(Record record) {
    sink.store(record, this);
  }

  @Override
  public void onFetchFailed(Exception ex, Record record) {
    Log.w(LOG_TAG, "onFetchFailed. Calling for immediate stop.", ex);
    this.consumer.stop(true);
  }

  @Override
  public void onFetchedRecord(Record record) {
    this.enqueue(record);
    this.consumer.doNotify();
  }

  @Override
  public void onFetchCompleted(long end) {
    Log.i(LOG_TAG, "onFetchCompleted. Stopping consumer once stores are done.");
    this.end = end;
    this.consumer.stop(false);
  }

  @Override
  public void onStoreFailed(Exception ex) {
    this.consumer.stored();
    delegate.onFlowStoreFailed(this, ex);
    // TODO: abort?
  }

  @Override
  public void onStoreSucceeded(Record record) {
    this.consumer.stored();
  }

  @Override
  public void onFetchSucceeded(Record[] records, long end) {
    for (Record record : records) {
      this.toProcess.add(record);
    }
    this.consumer.doNotify();
  }

  @Override
  public void onBeginFailed(Exception ex) {
    delegate.onFlowBeginFailed(this, ex);
  }

  @Override
  public void onBeginSucceeded(RepositorySession session) {
    if (session == source) {
      if (sourceBegun) {
        // TODO: inconsistency!
        return;
      }
      sourceBegun = true;
      sink.begin(this);
      return;
    }
    if (session == sink) {
      if (sinkBegun) {
        // TODO: inconsistency!
        return;
      }
      sinkBegun = true;
      // Start a consumer thread.
      this.consumer = new RecordConsumer(this);
      new Thread(this.consumer).start();
      waitingForQueueDone = true;
      source.fetchSince(timestamp, this);
      return;
    }

    // TODO: error!
  }
}