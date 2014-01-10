/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.fxa.authenticator;

import org.junit.Assert;
import org.junit.Test;
import org.mozilla.gecko.background.testhelpers.WaitHelper;
import org.mozilla.gecko.browserid.BrowserIDKeyPair;
import org.mozilla.gecko.fxa.authenticator.FxAccountLoginPolicy.AccountState;
import org.mozilla.gecko.sync.Utils;

public class TestFxAccountLoginPolicy {
  protected static final String TEST_AUDIENCE = "http://testAudience.com";

  protected MockFxAccount fxAccount;
  protected MockFxAccountClient client;
  protected MockFxAccountLoginPolicy policy;

  public void setUp(AccountState state) throws Exception {
    client = new MockFxAccountClient();
    fxAccount = MockFxAccount.makeAccount(state);
    client.addUser(fxAccount);
    policy = new MockFxAccountLoginPolicy(fxAccount, client);
  }

  protected String login(final String audience) throws Throwable {
    try {
      final String assertions[] = new String[1];
      WaitHelper.getTestWaiter().performWait(new Runnable() {
        @Override
        public void run() {
          policy.login(audience, new FxAccountLoginDelegate() {
            @Override
            public void handleError(FxAccountLoginException e) {
              WaitHelper.getTestWaiter().performNotify(e);
            }

            @Override
            public void handleSuccess(String assertion) {
              assertions[0] = assertion;
              WaitHelper.getTestWaiter().performNotify();
            }
          });
        }
      });
      return assertions[0];
    } catch (WaitHelper.InnerError e) {
      throw e.innerError;
    }
  }

  protected String assertLogin() throws Throwable {
    String assertion = login(TEST_AUDIENCE);
    Assert.assertNotNull(assertion);
    return assertion;
  }

  protected void assertLoginFails(AccountState finalState) throws Throwable {
    try {
      login(TEST_AUDIENCE);
      Assert.fail("Expected login to fail and leave account in state " + finalState);
    } catch (FxAccountLoginException e) {
      Assert.assertEquals(finalState, policy.getAccountState(fxAccount));
    }
  }

  @Test
  public void testStateInvalid() throws Throwable {
    setUp(AccountState.Invalid);
    Assert.assertEquals(AccountState.Invalid, policy.getAccountState(fxAccount));

    assertLoginFails(AccountState.Invalid);
  }

  @Test
  public void testNeedsSessionToken() throws Throwable {
    setUp(AccountState.NeedsSessionToken);
    Assert.assertEquals(AccountState.NeedsSessionToken, policy.getAccountState(fxAccount));

    // Unverified locally, unverified remotely. Login should get the session
    // token but fail due to verification state.
    assertLoginFails(AccountState.NeedsVerification);
    Assert.assertNotNull(fxAccount.sessionToken);

    setUp(AccountState.NeedsSessionToken);
    Assert.assertEquals(AccountState.NeedsSessionToken, policy.getAccountState(fxAccount));

    // Unverified locally, verified remotely. Login should get the session
    // token, mark the account verified, and succeed.
    client.verifyUser(fxAccount);
    assertLogin();
    Assert.assertNotNull(fxAccount.sessionToken);

    // Now suppose we invalidate our session token. We should be able to get a
    // new one, since we're validated.
    fxAccount.sessionToken = null;
    assertLogin();
    Assert.assertNotNull(fxAccount.sessionToken);
  }

  @Test
  public void testNeedsSessionTokenPW() throws Throwable {
    setUp(AccountState.Valid);
    Assert.assertEquals(AccountState.Valid, policy.getAccountState(fxAccount));

    assertLogin();
    fxAccount.sessionToken = null;
    fxAccount.quickStretchedPW = Utils.generateRandomBytes(32);

    assertLoginFails(AccountState.Invalid);
  }

  @Test
  public void testNeedsVerification() throws Throwable {
    setUp(AccountState.NeedsVerification);
    Assert.assertEquals(AccountState.NeedsVerification, policy.getAccountState(fxAccount));

    // Unverified locally, unverified remotely.  Login should fail and not advance state.
    assertLoginFails(AccountState.NeedsVerification);

    // Now verify remotely.
    client.verifyUser(fxAccount);

    // Unverified locally, verified remotely.  Login should succeed and advance state.
    assertLogin();

    Assert.assertTrue(fxAccount.verified);
  }

  @Test
  public void testNeedsKeys() throws Throwable {
    setUp(AccountState.NeedsKeys);
    Assert.assertEquals(AccountState.NeedsKeys, policy.getAccountState(fxAccount));
    assertLogin();
    Assert.assertNotNull(fxAccount.getKa());
    Assert.assertNotNull(fxAccount.getKb());
  }

  @Test
  public void testNeedsKeysFails() throws Throwable {
    setUp(AccountState.NeedsKeys);
    Assert.assertEquals(AccountState.NeedsKeys, policy.getAccountState(fxAccount));

    // If we submit a garbage keyFetchToken, it should be invalidated and we
    // remain in needs keys.
    fxAccount.keyFetchToken = Utils.generateRandomBytes(8);
    assertLoginFails(AccountState.NeedsKeys);
    Assert.assertEquals(null, fxAccount.keyFetchToken);

    // If we try again, we'll fetch a valid keyFetchToken and succeed.
    assertLogin();
  }

  @Test
  public void testNeedsKeysFailsPW() throws Throwable {
    setUp(AccountState.NeedsKeys);
    Assert.assertEquals(AccountState.NeedsKeys, policy.getAccountState(fxAccount));

    // Suppose we don't have a keyFetchToken. If we don't have a valid
    // credentials, we'll be unable to fetch a keyFetchToken. We should revert
    // to invalid, requiring user intervention.
    fxAccount.keyFetchToken = null;
    fxAccount.quickStretchedPW = Utils.generateRandomBytes(32);
    assertLoginFails(AccountState.Invalid);
  }

  @Test
  public void testNeedsCertificate() throws Throwable {
    setUp(AccountState.NeedsCertificate);
    Assert.assertEquals(AccountState.NeedsCertificate, policy.getAccountState(fxAccount));
    String assertion = assertLogin();
    Assert.assertEquals(assertion, fxAccount.getAssertion());
    Assert.assertNotNull(fxAccount.getCertificate());
  }

  @Test
  public void testNeedsCertificateFails() throws Throwable {
    setUp(AccountState.NeedsCertificate);
    Assert.assertEquals(AccountState.NeedsCertificate, policy.getAccountState(fxAccount));
    client.clearAllUserTokens();
    assertLoginFails(AccountState.NeedsSessionToken);
  }

  @Test
  public void testNeedsAssertion() throws Throwable {
    setUp(AccountState.NeedsAssertion);
    Assert.assertEquals(AccountState.NeedsAssertion, policy.getAccountState(fxAccount));
    String assertion = assertLogin();
    Assert.assertEquals(assertion, fxAccount.getAssertion());
  }

  @Test
  public void testNeedsAssertionFails() throws Throwable {
    setUp(AccountState.NeedsAssertion);
    Assert.assertEquals(AccountState.NeedsAssertion, policy.getAccountState(fxAccount));
    fxAccount.assertionKeyPair = new BrowserIDKeyPair(null, null); // This will fail to generate a new assertion.
    // If we fail to generate new assertion, we mark the account permanently invalid.
    assertLoginFails(AccountState.Invalid);
  }

  @Test
  public void testValid() throws Throwable {
    setUp(AccountState.Valid);
    Assert.assertEquals(AccountState.Valid, policy.getAccountState(fxAccount));
    String assertion = assertLogin();
    Assert.assertNotNull(assertion);
  }
}
