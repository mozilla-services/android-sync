package org.mozilla.android.sync.repositories;

import android.content.Context;

public class BookmarksRepository extends Repository {

  // protected constructor to force use of Repository static factory method makeRepository
  protected BookmarksRepository() { }

  // TODO it is annoying to have to pass the context around to get access to the DB...is there anywhere
  // else I can get this from rather than passing it around?

  // TODO this needs to happen in a thread :S
  public void createSession(Context context, SyncCallbackReceiver callbackMechanism) {
    if (context == null) {
      callbackMechanism.sessionCallback(RepoStatusCode.NULL_CONTEXT, null);
      return;
    }
    BookmarksRepositorySession session = new BookmarksRepositorySession(this, callbackMechanism, context);
    callbackMechanism.sessionCallback(RepoStatusCode.DONE, session);
  }

}
