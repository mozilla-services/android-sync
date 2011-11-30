package org.mozilla.android.sync.repositories.android;

import org.mozilla.android.sync.repositories.Repository;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionCreationDelegate;

import android.content.Context;

public abstract class AndroidBrowserRepository extends Repository {

  public void createSession(Context context, RepositorySessionCreationDelegate delegate, long lastSyncTimestamp) {
    super.createSession(delegate);
    CreateSessionThread thread = new CreateSessionThread(context, lastSyncTimestamp);
    thread.start();
  }
  
  protected abstract void sessionCreator(Context context, long lastSyncTimestamp); 
  
  class CreateSessionThread extends Thread {

    private Context context;
    private long lastSyncTimestamp;

    public CreateSessionThread(Context context, long lastSyncTimestamp) {
      if (context == null) {
        throw new IllegalArgumentException("context is null.");
      }
      this.context = context;
      this.lastSyncTimestamp = lastSyncTimestamp;
    }

    public void run() {
      sessionCreator(context, lastSyncTimestamp);
    }
  }

}
