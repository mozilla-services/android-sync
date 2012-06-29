/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import static junit.framework.Assert.assertEquals;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import junit.framework.AssertionFailedError;

public class ExpectNoGUIDsSinceDelegate extends DefaultGuidsSinceDelegate {
  public Set<String> ignore = new HashSet<String>();

  @Override
  public void onGuidsSinceSucceeded(Collection<String> guids) {
    AssertionFailedError err = null;
    try {
      int nonIgnored = 0;
      for (String guid : guids) {
        if (!ignore.contains(guid)) {
          nonIgnored++;
        }
      }
      assertEquals(0, nonIgnored);
    } catch (AssertionFailedError e) {
      err = e;
    }
    performNotify(err);
  }
}
