/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import static junit.framework.Assert.assertEquals;

import org.mozilla.gecko.sync.repositories.android.RepoUtils;

public class ExpectNoGUIDsSinceDelegate extends DefaultGuidsSinceDelegate {
  
  @Override
  public void onGuidsSinceSucceeded(String[] guids) {
    AssertionError err = null;
    try {
      boolean bookmarks = false;
      for (int i = 0; i < guids.length; i++) {
        if(RepoUtils.SPECIAL_GUIDS_MAP.containsKey(guids[i])) {
          bookmarks = true;
        }
      }
      assertEquals(0, bookmarks ? guids.length - 5 : guids.length);
    } catch (AssertionError e) {
      err = e;
    }
    testWaiter().performNotify(err);
  }
}
