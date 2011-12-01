package org.mozilla.android.sync.repositories.android;

import org.mozilla.android.sync.repositories.Repository;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionCreationDelegate;

import android.content.Context;

public abstract class AndroidBrowserRepository extends Repository {

  @Override
  public void createSession(RepositorySessionCreationDelegate delegate, Context context, long lastSyncTimestamp) {
    new CreateSessionThread(delegate, context, lastSyncTimestamp).start();
  }
  
  protected abstract void sessionCreator(RepositorySessionCreationDelegate delegate, Context context, long lastSyncTimestamp);
  
  class CreateSessionThread extends Thread {
    private RepositorySessionCreationDelegate delegate;
    private Context context;
    private long lastSyncTimestamp;

    public CreateSessionThread(RepositorySessionCreationDelegate delegate, Context context, long lastSyncTimestamp) {
      if (context == null) {
        throw new IllegalArgumentException("context is null.");
      }
      this.delegate = delegate;
      this.context = context;
      this.lastSyncTimestamp = lastSyncTimestamp;
    }

    public void run() {
      sessionCreator(delegate, context, lastSyncTimestamp);
    }
  }

}
