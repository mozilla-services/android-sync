package org.mozilla.gecko.sync.synchronizer;

import java.util.concurrent.ConcurrentLinkedQueue;

import org.mozilla.gecko.sync.repositories.domain.Record;

interface RecordsConsumerDelegate {
  public abstract ConcurrentLinkedQueue<Record> getQueue();
  public abstract void consumerIsDone();
  public abstract void store(Record record);
}