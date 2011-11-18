package org.mozilla.android.sync.repositories;

import android.content.Context;

public class BookmarksRepository extends Repository {

  // protected constructor to force use of Repository static factory method makeRepository
  protected BookmarksRepository() { }

  // TODO it is annoying to have to pass the context around to get access to the DB...is there anywhere
  // else I can get this from rather than passing it around?

  // TODO this needs to happen in a thread :S
  public void createSession(Context context, SyncCallbackReceiver callbackMechanism,long lastSyncTimestamp) {
    CreateSessionThread thread = new CreateSessionThread(context, callbackMechanism, lastSyncTimestamp);
    thread.start();
  }

  class CreateSessionThread extends Thread {

    private Context context;
    private SyncCallbackReceiver callbackMechanism;
    private long lastSyncTimestamp;

    public CreateSessionThread(Context context, SyncCallbackReceiver callbackMechanism,
        long lastSyncTimestamp) {
      this.context = context;
      this.callbackMechanism = callbackMechanism;
      this.lastSyncTimestamp = lastSyncTimestamp;
    }

    public void run() {
      if (context == null) {
        callbackMechanism.sessionCallback(RepoStatusCode.NULL_CONTEXT, null);
        return;
      }
      BookmarksRepositorySession session = new BookmarksRepositorySession(BookmarksRepository.this, callbackMechanism, context, lastSyncTimestamp);
      callbackMechanism.sessionCallback(RepoStatusCode.DONE, session);
    }
  }

}
