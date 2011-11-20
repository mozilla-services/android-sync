package org.mozilla.android.sync.test.helpers;

import static org.junit.Assert.fail;

import org.mozilla.android.sync.repositories.RepositorySessionStoreDelegate;
import org.mozilla.android.sync.repositories.domain.Record;

public class DefaultStoreDelegate implements RepositorySessionStoreDelegate {
  protected WaitHelper testWaiter() {
    return WaitHelper.getTestWaiter();
  }

  public void onStoreFailed(Exception ex) {
    fail("No store.");
  }

  public void onStoreSucceeded(Record record) {
    fail("No store.");
  }   
}