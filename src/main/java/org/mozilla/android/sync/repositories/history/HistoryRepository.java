package org.mozilla.android.sync.repositories.history;

import org.mozilla.android.sync.repositories.Repository;

import android.content.Context;

public class HistoryRepository extends Repository {

  @Override
  protected void sessionCreator(Context context, long lastSyncTimestamp) {
    HistoryRepositorySession session = new HistoryRepositorySession(HistoryRepository.this, delegate, context, lastSyncTimestamp);
    delegate.onSessionCreated(session);
  }

}
