package org.mozilla.android.sync.test.helpers;

import static org.junit.Assert.assertEquals;

import org.mozilla.android.sync.repositories.RepoStatusCode;
import org.mozilla.android.sync.test.TestAndroidBookmarksRepo;

public class ExpectNoGUIDsSinceDelegate extends DefaultRepositorySessionDelegate {
  public void guidsSinceCallback(RepoStatusCode status, String[] guids) {
    AssertionError err = null;
    try {
      assertEquals(0, guids.length);
      assertEquals(status, RepoStatusCode.DONE);
    } catch (AssertionError e) {
      err = e;
    }
    TestAndroidBookmarksRepo.testWaiter.performNotify(err);
  }
}