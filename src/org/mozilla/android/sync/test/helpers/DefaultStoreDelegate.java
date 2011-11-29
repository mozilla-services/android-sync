/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import static junit.framework.Assert.fail;

import org.mozilla.android.sync.repositories.delegates.RepositorySessionStoreDelegate;
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
