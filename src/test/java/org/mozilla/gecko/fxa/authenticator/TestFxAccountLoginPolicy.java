/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.fxa.authenticator;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mozilla.android.sync.test.integration.IntegrationTestCategory;
import org.mozilla.gecko.background.testhelpers.WaitHelper;
import org.mozilla.gecko.fxa.authenticator.FxAccountLoginPolicy.AccountState;

@Category(IntegrationTestCategory.class)
public class TestFxAccountLoginPolicy {
  protected static final String TEST_AUDIENCE = "http://testAudience.com";

  protected MockFxAccount fxAccount;
  protected MockFxAccountClient client;
  protected MockFxAccountLoginPolicy policy;

  public void setUp(AccountState state) throws Exception {
    client = new MockFxAccountClient();
    fxAccount = MockFxAccount.makeAccount(state);
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

  @Test
  public void testStateInvalid() throws Throwable {
    setUp(AccountState.Invalid);
    Assert.assertEquals(AccountState.Invalid, policy.getAccountState(fxAccount));
  }

  @Test
  public void testNeedsSessionToken() throws Throwable {
    setUp(AccountState.NeedsSessionToken);
    Assert.assertEquals(AccountState.NeedsSessionToken, policy.getAccountState(fxAccount));
    String assertion = login(TEST_AUDIENCE);
    Assert.assertNotNull(assertion);
    Assert.assertNotNull(fxAccount.sessionToken);
  }

  @Test
  public void testNeedsVerification() throws Throwable {
    setUp(AccountState.NeedsVerification);
    Assert.assertEquals(AccountState.NeedsVerification, policy.getAccountState(fxAccount));
    String assertion = login(TEST_AUDIENCE);
    Assert.assertNotNull(assertion);
    Assert.assertTrue(fxAccount.verified);
  }

  @Test
  public void testNeedsKeys() throws Throwable {
    setUp(AccountState.NeedsKeys);
    Assert.assertEquals(AccountState.NeedsKeys, policy.getAccountState(fxAccount));
    String assertion = login(TEST_AUDIENCE);
    Assert.assertNotNull(assertion);
    Assert.assertNotNull(fxAccount.getKa());
    Assert.assertNotNull(fxAccount.getKb());
  }

  @Test
  public void testNeedsCertificate() throws Throwable {
    setUp(AccountState.NeedsCertificate);
    Assert.assertEquals(AccountState.NeedsCertificate, policy.getAccountState(fxAccount));
    String assertion = login(TEST_AUDIENCE);
    Assert.assertNotNull(assertion);
    Assert.assertEquals(assertion, fxAccount.getAssertion());
    Assert.assertNotNull(fxAccount.getCertificate());
  }

  @Test
  public void testNeedsAssertion() throws Throwable {
    setUp(AccountState.NeedsAssertion);
    Assert.assertEquals(AccountState.NeedsAssertion, policy.getAccountState(fxAccount));
    String assertion = login(TEST_AUDIENCE);
    Assert.assertNotNull(assertion);
    Assert.assertEquals(assertion, fxAccount.getAssertion());
  }

  @Test
  public void testValid() throws Throwable {
    setUp(AccountState.Valid);
    Assert.assertEquals(AccountState.Valid, policy.getAccountState(fxAccount));
    Assert.assertNotNull(login(TEST_AUDIENCE));
  }
}
