/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import static org.junit.Assert.fail;

import org.mozilla.android.sync.repositories.RepoStatusCode;
import org.mozilla.android.sync.repositories.RepositorySessionDelegate;
import org.mozilla.android.sync.repositories.domain.Record;

public class DefaultRepositorySessionDelegate implements RepositorySessionDelegate {
  protected WaitHelper testWaiter() {
    return WaitHelper.getTestWaiter();
  }
  private void sharedFail() {
    try {
      fail("Should not be called.");
    } catch (AssertionError e) {
      testWaiter().performNotify(e);
    }
  }
  public void guidsSinceCallback(RepoStatusCode status, String[] guids) {
    sharedFail();
  }
  public void storeCallback(RepoStatusCode status, long rowId) {
    sharedFail();
  }
  public void fetchSinceCallback(RepoStatusCode status, Record[] records) {
    sharedFail();
  }
  public void fetchCallback(RepoStatusCode status, Record[] records) {
    sharedFail();
  }
  public void fetchAllCallback(RepoStatusCode status, Record[] records) {
    sharedFail();
  }
  public void wipeCallback(RepoStatusCode status) {
    sharedFail();
  }
  public void beginCallback(RepoStatusCode status) {
    sharedFail();
  }
  public void finishCallback(RepoStatusCode status) {
    sharedFail();
  }
}
