package org.mozilla.android.sync.test.helpers;

import static org.junit.Assert.fail;

import org.mozilla.android.sync.repositories.RepositorySessionStoreDelegate;
import org.mozilla.android.sync.repositories.domain.Record;

public class DefaultStoreDelegate implements RepositorySessionStoreDelegate {
  protected WaitHelper testWaiter() {
    return WaitHelper.getTestWaiter();
  }
  private void sharedFail() {
    try {
      fail("No store.");
    } catch (AssertionError e) {
      testWaiter().performNotify(e);
    }
  }
  public void onStoreFailed(Exception ex) {
    sharedFail();
  }

  public void onStoreSucceeded(Record record) {
    sharedFail();
  }   
}
