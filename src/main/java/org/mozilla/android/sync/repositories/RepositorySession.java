package org.mozilla.android.sync.repositories;

import org.mozilla.android.sync.repositories.domain.Record;

public abstract class RepositorySession {

  protected Repository repository;
  protected SyncCallbackReceiver callbackReceiver;
  // The time that the last sync on this collection completed
  protected long lastSyncTimestamp;
  protected long syncBeginTimestamp;
  // TODO logger and logger level here

  public RepositorySession(Repository repository, SyncCallbackReceiver callbackReceiver, long lastSyncTimestamp) {
    this.repository = repository;
    this.callbackReceiver = callbackReceiver;
    this.lastSyncTimestamp = lastSyncTimestamp;
  }

  public abstract void guidsSince(long timestamp, RepositoryCallbackReceiver receiver);
  public abstract void fetchSince(long timestamp, RepositoryCallbackReceiver receiver);
  public abstract void fetch(String[] guids, RepositoryCallbackReceiver receiver);

  // Test function only
  public abstract void fetchAll(RepositoryCallbackReceiver receiver);

  public abstract void store(Record record, RepositoryCallbackReceiver receiver);
  public abstract void wipe(RepositoryCallbackReceiver receiver);

  public void begin(RepositoryCallbackReceiver receiver) {
    this.syncBeginTimestamp = Utils.currentEpoch();
  }

  public abstract void finish(RepositoryCallbackReceiver receiver);

}
