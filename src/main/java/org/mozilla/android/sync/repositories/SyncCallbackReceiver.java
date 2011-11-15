package org.mozilla.android.sync.repositories;

// Used to provide the sessionCallback and storeCallback
// mechanism to repository instances.
public interface SyncCallbackReceiver {

  public void sessionCallback(RepoStatusCode status, RepositorySession session);
  public void storeCallback(RepoStatusCode status);
}
