package org.mozilla.android.sync.repositories;

// Used to provide the sessionCallback and storeCallback
// mechanism to repository instances.
public interface SyncCallbackReceiver {

  public void sessionCallback(RepoStatusCode error, RepositorySession session);
  public void storeCallback(RepoStatusCode error);
}
