/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import static junit.framework.Assert.assertTrue;

import java.util.Set;

import junit.framework.AssertionFailedError;

import org.mozilla.gecko.sync.repositories.android.AndroidBrowserBookmarksRepositorySession;

public class ExpectOnlySpecialFoldersDelegate extends DefaultGuidsSinceDelegate {

  public Set<String> expected = AndroidBrowserBookmarksRepositorySession.SPECIAL_GUIDS_MAP.keySet();

  @Override
  public void onGuidsSinceSucceeded(String[] guids) {
    AssertionFailedError err = null;
    try {
      for (int i = 0; i < guids.length; i++) {
        assertTrue(expected.contains(guids[i]));
      }
    } catch (AssertionFailedError e) {
      err = e;
    }
    testWaiter().performNotify(err);
  }
}
