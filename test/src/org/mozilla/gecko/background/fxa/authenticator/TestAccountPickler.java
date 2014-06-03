/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.background.fxa.authenticator;

import org.mozilla.gecko.background.sync.AndroidSyncTestCaseWithAccounts;
import org.mozilla.gecko.fxa.FxAccountConstants;
import org.mozilla.gecko.fxa.authenticator.AccountPickler;
import org.mozilla.gecko.fxa.authenticator.AndroidFxAccount;
import org.mozilla.gecko.fxa.login.Separated;
import org.mozilla.gecko.fxa.login.State;
import org.mozilla.gecko.sync.Utils;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.test.RenamingDelegatingContext;

public class TestAccountPickler extends AndroidSyncTestCaseWithAccounts {
  private final static String FILENAME_PREFIX = "TestAccountPickler-";
  private final static String PICKLE_FILENAME = "pickle";

  private final static String TEST_ACCOUNTTYPE = FxAccountConstants.ACCOUNT_TYPE;

  // Test account names must start with TEST_USERNAME in order to be recognized
  // as test accounts and deleted in tearDown.
  public static final String TEST_USERNAME   = "testFirefoxAccount@mozilla.com";

  public Account account;
  public RenamingDelegatingContext context;

  public TestAccountPickler() {
    super(TEST_ACCOUNTTYPE, TEST_USERNAME);
  }

  @Override
  public void setUp() {
    super.setUp();
    this.account = null;
    // Randomize the filename prefix in case we don't clean up correctly.
    this.context = new RenamingDelegatingContext(getApplicationContext(), FILENAME_PREFIX +
        Math.random() * 1000001 + "-");
    this.accountManager = AccountManager.get(context);
  }

  @Override
  public void tearDown() {
    super.tearDown();
    this.context.deleteFile(PICKLE_FILENAME);
  }

  public AndroidFxAccount addTestAccount() throws Exception {
    final State state = new Separated(TEST_USERNAME, "uid", false); // State choice is arbitrary.
    final AndroidFxAccount account = AndroidFxAccount.addAndroidAccount(context, TEST_USERNAME,
        "profile", "serverURI", "tokenServerURI", state);
    assertNotNull(account);
    assertNotNull(account.getProfile());
    assertTrue(testAccountsExist()); // Sanity check.
    this.account = account.getAndroidAccount(); // To remove in tearDown() if we throw.
    return account;
  }

  public void testPickleAndUnpickle() throws Exception {
    final AndroidFxAccount inputAccount = addTestAccount();
    // Sync is enabled by default so we do a more thorough test by disabling it.
    inputAccount.disableSyncing();

    AccountPickler.pickle(inputAccount, PICKLE_FILENAME);

    // unpickle adds an account to the AccountManager so delete it first.
    deleteTestAccounts();
    assertFalse(testAccountsExist());

    final AndroidFxAccount unpickledAccount =
        AccountPickler.unpickle(context, PICKLE_FILENAME);
    assertNotNull(unpickledAccount);
    this.account = unpickledAccount.getAndroidAccount(); // To remove in tearDown().
    assertAccountsEquals(inputAccount, unpickledAccount);
  }

  public void testDeletePickle() throws Exception {
    final AndroidFxAccount account = addTestAccount();
    AccountPickler.pickle(account, PICKLE_FILENAME);

    final String s = Utils.readFile(context, PICKLE_FILENAME);
    assertNotNull(s);
    assertTrue(s.length() > 0);

    AccountPickler.deletePickle(context, PICKLE_FILENAME);
    assertFileNotPresent(context, PICKLE_FILENAME);
  }

  private void assertAccountsEquals(final AndroidFxAccount expected,
      final AndroidFxAccount actual) throws Exception {
    // TODO: Write and use AndroidFxAccount.equals
    // TODO: protected.
    //assertEquals(expected.getAccountVersion(), actual.getAccountVersion());
    assertEquals(expected.getProfile(), actual.getProfile());
    assertEquals(expected.getAccountServerURI(), actual.getAccountServerURI());
    assertEquals(expected.getAudience(), actual.getAudience());
    assertEquals(expected.getTokenServerURI(), actual.getTokenServerURI());
    assertEquals(expected.getSyncPrefsPath(), actual.getSyncPrefsPath());
    assertEquals(expected.isSyncing(), actual.isSyncing());
    assertEquals(expected.getEmail(), actual.getEmail());
    assertStateEquals(expected.getState(), actual.getState());
  }

  private void assertStateEquals(final State expected, final State actual) throws Exception {
    // TODO: Write and use State.equals. Thus, this is only thorough for the State base class.
    assertEquals(expected.getStateLabel(), actual.getStateLabel());
    assertEquals(expected.email, actual.email);
    assertEquals(expected.uid, actual.uid);
    assertEquals(expected.verified, actual.verified);
  }
}
