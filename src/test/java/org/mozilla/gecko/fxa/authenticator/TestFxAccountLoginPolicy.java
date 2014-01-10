/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.fxa.authenticator;

import java.security.GeneralSecurityException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mozilla.android.sync.test.integration.IntegrationTestCategory;
import org.mozilla.gecko.background.testhelpers.WaitHelper;
import org.mozilla.gecko.browserid.BrowserIDKeyPair;
import org.mozilla.gecko.browserid.RSACryptoImplementation;

@Category(IntegrationTestCategory.class)
public class TestFxAccountLoginPolicy {
  public static final String TEST_SERVERURI = "https://server.com";
  public static final String TEST_AUDIENCE = TEST_SERVERURI;

  public static class MockFxAccount implements AbstractFxAccount {
    public String serverURI = TEST_SERVERURI;
    public byte[] sessionToken = new byte[32];
    public byte[] keyFetchToken = new byte[32];
    public boolean verified = false;

    public byte[] kA = null;
    public byte[] kB = null;

    public byte[] unwrapKb = new byte[32];

    public BrowserIDKeyPair assertionKeyPair;

    public String certificate;
    public String assertion;

    @Override
    public String getServerURI() {
      return serverURI;
    }

    @Override
    public byte[] getSessionToken() {
      return sessionToken;
    }

    @Override
    public byte[] getKeyFetchToken() {
      return keyFetchToken;
    }

    @Override
    public boolean isVerified() {
      return verified;
    }

    @Override
    public void setVerified() {
      verified = true;
    }

    @Override
    public byte[] getKa() {
      return kA;
    }

    @Override
    public byte[] getKb() {
      return kB;
    }

    @Override
    public void setKa(byte[] kA) {
      this.kA = kA;
    }

    @Override
    public void setWrappedKb(byte[] kB) {
      this.kB = kB;
    }

    @Override
    public BrowserIDKeyPair getAssertionKeyPair() throws GeneralSecurityException {
      if (assertionKeyPair != null) {
        return assertionKeyPair;
      }
      assertionKeyPair = RSACryptoImplementation.generateKeyPair(512);
      return assertionKeyPair;
    }

    @Override
    public void setSessionToken(byte[] sessionToken) {
      this.sessionToken = sessionToken;
    }

    @Override
    public void setKeyFetchToken(byte[] keyFetchToken) {
      this.keyFetchToken = keyFetchToken;
    }

    @Override
    public String getCertificate() {
      return certificate;
    }

    @Override
    public void setCertificate(String certificate) {
      this.certificate = certificate;
    }

    @Override
    public String getAssertion() {
      return certificate;
    }

    @Override
    public void setAssertion(String assertion) {
      this.assertion = assertion;
    }
  }

  protected MockFxAccount fxAccount;
  protected ExecutorService executor;
  protected FxAccountLoginPolicy policy;

  @Before
  public void setUp() {
    this.executor = Executors.newSingleThreadExecutor();
    this.fxAccount = new MockFxAccount();
    this.policy = new FxAccountLoginPolicy(null, fxAccount, executor);
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
  public void testUnverified() throws Throwable {
    fxAccount.verified = false;
    login(TEST_AUDIENCE);
  }

  @Test
  public void testVerified() throws Throwable {
    fxAccount.verified = true;
    login(TEST_AUDIENCE);
  }
}
