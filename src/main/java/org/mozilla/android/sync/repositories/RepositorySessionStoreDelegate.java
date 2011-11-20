package org.mozilla.android.sync.repositories;

import org.mozilla.android.sync.repositories.domain.Record;

public interface RepositorySessionStoreDelegate {
  public void onStoreFailed(Exception ex);
  public void onStoreSucceeded(Record record);
}
