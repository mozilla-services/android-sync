/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.fxa.receivers;

import org.mozilla.gecko.background.sync.AndroidSyncTestCaseWithAccounts;
import org.mozilla.gecko.db.BrowserContract;
import org.mozilla.gecko.fxa.FxAccountConstants;
import org.mozilla.gecko.fxa.authenticator.AndroidFxAccount;
import org.mozilla.gecko.fxa.login.Separated;
import org.mozilla.gecko.fxa.login.State;
import org.mozilla.gecko.fxa.receivers.FxAccountUpgradeReceiver.MaybeInitializeReadingListAuthority;

import android.accounts.Account;
import android.content.ContentResolver;

public class TestFxAccountUpgradeReceiver extends AndroidSyncTestCaseWithAccounts {
  private static final String TEST_TOKEN_SERVER_URI = "tokenServerURI";
  private static final String TEST_AUTH_SERVER_URI = "serverURI";
  private static final String TEST_PROFILE = "profile";

  private final static String TEST_ACCOUNTTYPE = FxAccountConstants.ACCOUNT_TYPE;

  // Test account names must start with TEST_USERNAME in order to be recognized
  // as test accounts and deleted in tearDown.
  public static final String TEST_USERNAME = "testFirefoxAccount@mozilla.com";

  public Account account;

  public TestFxAccountUpgradeReceiver() {
    super(TEST_ACCOUNTTYPE, TEST_USERNAME);
  }

  public AndroidFxAccount addTestAccount() throws Exception {
    final State state = new Separated(TEST_USERNAME, "uid", false); // State choice is arbitrary.
    final AndroidFxAccount account = AndroidFxAccount.addAndroidAccount(context, TEST_USERNAME,
        TEST_PROFILE, TEST_AUTH_SERVER_URI, TEST_TOKEN_SERVER_URI, state,
        AndroidSyncTestCaseWithAccounts.TEST_SYNC_AUTOMATICALLY_MAP_WITH_ALL_AUTHORITIES_DISABLED);
    assertNotNull(account);
    assertNotNull(account.getProfile());
    assertTrue(testAccountsExist()); // Sanity check.
    this.account = account.getAndroidAccount(); // To remove in tearDown() if we throw.
    return account;
  }

  protected AndroidFxAccount addTestAccountWithReadingListNotInitialized() throws Exception {
    final AndroidFxAccount fxAccount = addTestAccount();
    // Pretend we've not seen the Reading List authority.
    accountManager.setUserData(fxAccount.getAndroidAccount(),
        AndroidFxAccount.ACCOUNT_KEY_READING_LIST_AUTHORITY_INITIALIZED,
        null);
    return fxAccount;
  }

  protected void assertReadingListAuthorityInitialized(AndroidFxAccount fxAccount) throws Exception {
    final String authoritiesSeenString = accountManager.getUserData(fxAccount.getAndroidAccount(),
        AndroidFxAccount.ACCOUNT_KEY_READING_LIST_AUTHORITY_INITIALIZED);
    assertEquals("1", authoritiesSeenString);
  }

  private void upgrade(AndroidFxAccount fxAccount) {
    final MaybeInitializeReadingListAuthority runnable = new FxAccountUpgradeReceiver.MaybeInitializeReadingListAuthority(context);
    runnable.run();
  }

  public void testNewAccount() throws Exception {
    final AndroidFxAccount fxAccount = addTestAccount();
    assertReadingListAuthorityInitialized(fxAccount);
    // The test account has Firefox Sync and Reading List turned off.
    assertEquals(false, ContentResolver.getSyncAutomatically(fxAccount.getAndroidAccount(), BrowserContract.AUTHORITY));
    assertEquals(false, ContentResolver.getSyncAutomatically(fxAccount.getAndroidAccount(), BrowserContract.READING_LIST_AUTHORITY));
    // Turn on Firefox Sync.
    ContentResolver.setSyncAutomatically(fxAccount.getAndroidAccount(), BrowserContract.AUTHORITY, true);
    // The upgrade should do nothing: we should not enable syncing Reading List
    // automatically, even though Firefox Sync is enabled.
    upgrade(fxAccount);
    assertEquals(false, ContentResolver.getSyncAutomatically(fxAccount.getAndroidAccount(), BrowserContract.READING_LIST_AUTHORITY));
  }

  public void testExistingccountWithSyncEnabled() throws Exception {
    final AndroidFxAccount fxAccount = addTestAccountWithReadingListNotInitialized();
    // Turn on Firefox Sync.
    ContentResolver.setSyncAutomatically(fxAccount.getAndroidAccount(), BrowserContract.AUTHORITY, true);
    // We should upgrade and turn Reading List sync automatically on.
    upgrade(fxAccount);
    assertEquals(true, ContentResolver.getSyncAutomatically(fxAccount.getAndroidAccount(), BrowserContract.READING_LIST_AUTHORITY));
    // After upgrade, we've initialized the Reading List authority.
    assertReadingListAuthorityInitialized(fxAccount);
  }

  public void testExistingAccountWithSyncDisabled() throws Exception {
    final AndroidFxAccount fxAccount = addTestAccountWithReadingListNotInitialized();
    // Turn off Firefox Sync.
    ContentResolver.setSyncAutomatically(fxAccount.getAndroidAccount(), BrowserContract.AUTHORITY, false);
    // We should upgrade but not turn Reading List sync automatically on.
    upgrade(fxAccount);
    assertEquals(false, ContentResolver.getSyncAutomatically(fxAccount.getAndroidAccount(), BrowserContract.READING_LIST_AUTHORITY));
    // After upgrade, we've initialized the Reading List authority.
    assertReadingListAuthorityInitialized(fxAccount);
  }
}
