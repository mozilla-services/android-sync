/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.picl.sync;

import org.mozilla.android.sync.test.AndroidSyncTestCase;
import org.mozilla.gecko.picl.account.PICLAccountAuthenticator;

import android.accounts.Account;

public class TestPICLTabsGlobalSession extends AndroidSyncTestCase {
  public static final String TEST_EMAIL = "test@test.com";
  public static final String TEST_KA = "testkA";
  public static final String TEST_DEVICEID = "testDeviceId";
  public static final String TEST_VERSION = "testVersion";

  protected Account account;

  public void setUp() {
    account = PICLAccountAuthenticator.createAccount(getApplicationContext(), TEST_EMAIL, TEST_KA, TEST_DEVICEID, TEST_VERSION);
  }

  // Not really a test -- just an easy way to create a PICL account for testing.
  public void test() {
    assertNotNull(account);
  }
}
