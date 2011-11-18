package org.mozilla.android.sync.repositories;

import android.content.Context;

// TODO: For now we are only concerned with android repos...eventually I'll need
// to implement this for both a server repo and an android repo
public abstract class Repository {

  private CollectionType collection;

  public Repository(CollectionType collection) {
    this.setCollection(collection);
  }
  public abstract void createSession(Context context, SyncCallbackReceiver callbackMechanism);

  public CollectionType getCollection() {
    return collection;
  }
  public void setCollection(CollectionType collection) {
    this.collection = collection;
  }
}
