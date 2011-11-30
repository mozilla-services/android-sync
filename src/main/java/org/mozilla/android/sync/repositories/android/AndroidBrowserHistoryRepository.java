package org.mozilla.android.sync.repositories.android;

import org.mozilla.android.sync.repositories.HistoryRepository;

import android.content.Context;

public class AndroidBrowserHistoryRepository extends AndroidBrowserRepository implements HistoryRepository{

  @Override
  protected void sessionCreator(Context context, long lastSyncTimestamp) {
    AndroidBrowserHistoryRepositorySession session = new AndroidBrowserHistoryRepositorySession(AndroidBrowserHistoryRepository.this, context, lastSyncTimestamp);
    delegate.onSessionCreated(session);
  }

}
