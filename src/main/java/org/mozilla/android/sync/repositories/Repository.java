package org.mozilla.android.sync.repositories;

import android.content.Context;

import org.mozilla.android.sync.repositories.CollectionType;

// TODO: For now we are only concerned with android repos...eventually I'll need
// to implement this for both a server repo and an android repo
public abstract class Repository {

  public abstract void createSession(Context context, SyncCallbackReceiver callbackMechanism, long lastSyncTimestamp);

  public static Repository makeRepository(CollectionType collection) {
    switch (collection) {
    case Bookmarks:
      return new BookmarksRepository();
    default:
      return null;
    }
  }
}
