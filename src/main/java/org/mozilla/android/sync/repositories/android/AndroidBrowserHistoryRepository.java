package org.mozilla.android.sync.repositories.android;

import org.mozilla.android.sync.repositories.HistoryRepository;
import org.mozilla.android.sync.repositories.Repository;

import android.content.Context;

public class AndroidBrowserHistoryRepository extends Repository implements HistoryRepository{

  @Override
  protected void sessionCreator(Context context, long lastSyncTimestamp) {
    AndroidBrowserHistoryRepositorySession session = new AndroidBrowserHistoryRepositorySession(AndroidBrowserHistoryRepository.this, delegate, context, lastSyncTimestamp);
    delegate.onSessionCreated(session);
  }

}
