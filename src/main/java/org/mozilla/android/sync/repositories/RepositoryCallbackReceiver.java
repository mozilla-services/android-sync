package org.mozilla.android.sync.repositories;

import org.mozilla.android.sync.repositories.domain.Record;

public interface RepositoryCallbackReceiver {
  // This interface must be implemented by any class that needs
  // to receive callbacks from a Repository (for example a
  // callback containing error codes or fetched records)

  public void guidsSinceCallback(RepoStatusCode status, String[] guids);

  public void storeCallback(RepoStatusCode status, long rowId);

  public void fetchSinceCallback(RepoStatusCode status, Record[] records);

  public void fetchCallback(RepoStatusCode status, Record[] records);

  public void fetchAllCallback(RepoStatusCode status, Record[] records);

  public void wipeCallback(RepoStatusCode status);

  public void beginCallback(RepoStatusCode status);

  public void finishCallback(RepoStatusCode status);

}
