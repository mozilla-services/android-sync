package org.mozilla.android.sync.synchronizer;

import java.util.concurrent.ConcurrentLinkedQueue;

import org.mozilla.android.sync.repositories.RepositorySession;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionBeginDelegate;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionFetchRecordsDelegate;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionStoreDelegate;
import org.mozilla.android.sync.repositories.domain.Record;

/**
 * Pulls records from `source`, applying them to `sink`.
 * Notifies its delegate of errors and completion.
 *
 * @author rnewman
 *
 */
class RecordsChannel implements RepositorySessionFetchRecordsDelegate, RepositorySessionStoreDelegate, RecordsConsumerDelegate, RepositorySessionBeginDelegate {
  public RepositorySession source;
  public RepositorySession sink;
  private RecordsChannelDelegate delegate;
  private long timestamp;
  private boolean sourceBegun = false;
  private boolean sinkBegun   = false;

  public RecordsChannel(RepositorySession source, RepositorySession sink, RecordsChannelDelegate delegate, long timestamp) {
    this.source = source;
    this.sink   = sink;
    this.delegate = delegate;
    this.timestamp = timestamp;
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
    if (waitingForQueueDone) {
      delegate.onFlowCompleted(this);
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
    this.consumer.stop(true);
  }

  @Override
  public void onFetchedRecord(Record record) {
    this.enqueue(record);
    this.consumer.doNotify();
  }

  @Override
  public void onFetchCompleted() {
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
  public void onFetchSucceeded(Record[] records) {
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
      source.fetchSince(timestamp, this);
      return;
    }

    // TODO: error!
  }
}