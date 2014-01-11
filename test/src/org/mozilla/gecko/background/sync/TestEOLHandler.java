/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.background.sync;

import java.util.ArrayList;

import junit.framework.Assert;

import org.mozilla.gecko.background.testhelpers.MockSharedPreferences;
import org.mozilla.gecko.sync.EndOfLifeHandler;
import org.mozilla.gecko.sync.EndOfLifeHandler.EOLCode;
import org.mozilla.gecko.sync.EndOfLifeHandler.EOLDelegate;

public class TestEOLHandler extends MockAccountTest {
  private static final String SOFT_EOL = "{\"code\":\"soft-eol\", \"message\":\"log me\", \"url\":\"http://foo.com\"}";
  private static final String HARD_EOL = "{\"code\":\"hard-eol\", \"message\":\"log me\", \"url\":\"http://foo.com\"}";

  private class MockEOLDelegate implements EOLDelegate {
    final public ArrayList<String> hardEOLs = new ArrayList<String>();
    final public ArrayList<String> softEOLs = new ArrayList<String>();
    public boolean disableCalled = false;

    @Override
    public void displayMessageForEOLCode(EOLCode code, String url) {
      if (code == EOLCode.HARD) {
        hardEOLs.add(url);
      } else {
        softEOLs.add(url);
      }
    }

    @Override
    public void disableAccount() {
      disableCalled = true;
    }

    public void reset() {
      hardEOLs.clear();
      softEOLs.clear();
      disableCalled = false;
    }
  }

  private void doCheckResponseHandling(final String response, final boolean soft) {
    final MockEOLDelegate delegate = new MockEOLDelegate();
    final MockSharedPreferences prefs = new MockSharedPreferences();
    final EndOfLifeHandler handler = new EndOfLifeHandler(prefs, delegate);

    final long start = System.currentTimeMillis();
    handler.handleServerAlert(response);
    final long end = System.currentTimeMillis();

    // Ensure that we got the right code and URL from the EOLHandler.
    Assert.assertEquals(soft ? 0 : 1, delegate.hardEOLs.size());
    Assert.assertEquals(soft ? 1 : 0, delegate.softEOLs.size());
    Assert.assertEquals("http://foo.com", (soft ? delegate.softEOLs : delegate.hardEOLs).get(0));

    // ... and if this was a hard EOL, it asked us to disable the account.
    Assert.assertEquals(!soft, delegate.disableCalled);

    // We recorded the right earliest next message time.
    final long next = prefs.getLong(EndOfLifeHandler.PREF_EARLIEST_NEXT, 0);
    final long baseTime = next - EndOfLifeHandler.EOL_ALERT_INTERVAL_MSEC;
    Assert.assertTrue(baseTime <= end);
    Assert.assertTrue(baseTime >= start);

    Assert.assertEquals(soft ? "soft-eol" : "hard-eol", prefs.getString(EndOfLifeHandler.PREF_EOL_STATE, null));
    delegate.reset();
  }

  public void testEOL() {
    doCheckResponseHandling(SOFT_EOL, true);
    doCheckResponseHandling(HARD_EOL, false);
  }
}
