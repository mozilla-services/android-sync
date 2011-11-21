/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import static junit.framework.Assert.assertEquals;

import org.mozilla.android.sync.repositories.RepoStatusCode;

public class ExpectNoGUIDsSinceDelegate extends DefaultRepositorySessionDelegate {
  public void guidsSinceCallback(RepoStatusCode status, String[] guids) {
    AssertionError err = null;
    try {
      assertEquals(0, guids.length);
      assertEquals(status, RepoStatusCode.DONE);
    } catch (AssertionError e) {
      err = e;
    }
    testWaiter().performNotify(err);
  }
}
