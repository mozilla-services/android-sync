/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;

import java.util.Arrays;

import org.mozilla.android.sync.repositories.RepoStatusCode;
import org.mozilla.android.sync.repositories.RepositorySessionDelegate;

public class ExpectFetchGUIDsSinceDelegate extends DefaultRepositorySessionDelegate
    implements RepositorySessionDelegate {
  private String[] expected;

  public ExpectFetchGUIDsSinceDelegate(String[] guids) {
    expected = guids;
    Arrays.sort(expected);
  }

  public void guidsSinceCallback(RepoStatusCode status, String[] guids) {
    AssertionError err = null;
    try {
      assertEquals(status, RepoStatusCode.DONE);
      assertEquals(guids.length, this.expected.length);

      for (String string : guids) {
        assertFalse(-1 == Arrays.binarySearch(this.expected, string));
      }
    } catch (AssertionError e) {
      err = e;
    }
    testWaiter().performNotify(err);
  }
}
