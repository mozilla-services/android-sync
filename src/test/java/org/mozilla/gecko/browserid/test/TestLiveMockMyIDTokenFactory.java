/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.browserid.test;

import java.net.URI;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mozilla.android.sync.test.helpers.WaitHelper;
import org.mozilla.android.sync.test.integration.IntegrationTestCategory;
import org.mozilla.gecko.browserid.DSACryptoImplementation;
import org.mozilla.gecko.browserid.JSONWebTokenUtils;
import org.mozilla.gecko.browserid.KeyPair;
import org.mozilla.gecko.browserid.MockMyIDTokenFactory;
import org.mozilla.gecko.browserid.PrivateKey;
import org.mozilla.gecko.browserid.PublicKey;
import org.mozilla.gecko.browserid.RSACryptoImplementation;
import org.mozilla.gecko.browserid.verifier.BrowserIDRemoteVerifierClient;
import org.mozilla.gecko.browserid.verifier.BrowserIDVerifierDelegate;
import org.mozilla.gecko.sync.ExtendedJSONObject;

@Category(IntegrationTestCategory.class)
public class TestLiveMockMyIDTokenFactory {
  public static String TEST_USERNAME = "test";
  public static String TEST_CERTIFICATE_ISSUER = "mockmyid.com";
  public static String TEST_EMAIL = TEST_USERNAME + "@" + TEST_CERTIFICATE_ISSUER;
  public static String TEST_AUDIENCE = "http://localhost:8080";

  public static String VERIFIER_URL = "https://verifier.login.persona.org/verify";
  // public static String VERIFIER_URL = "http://127.0.0.1:10002/verify";

  protected BrowserIDRemoteVerifierClient client;
  protected KeyPair keyPair;
  protected PublicKey publicKey;
  protected PrivateKey privateKey;

  protected MockMyIDTokenFactory mockMyIdTokenFactory;

  @Before
  public void setUp() throws Exception {
    client = new BrowserIDRemoteVerifierClient(new URI(VERIFIER_URL));

    mockMyIdTokenFactory = new MockMyIDTokenFactory();

    keyPair = RSACryptoImplementation.generateKeypair(1024); // No need for strong keys while testing.
    publicKey = keyPair.getPublic();
    privateKey = keyPair.getPrivate();
  }

  protected void assertVerifySuccess(final String audience, final String assertion) {
    WaitHelper.getTestWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        client.verify(audience, assertion, new BrowserIDVerifierDelegate() {
          @Override
          public void handleSuccess(ExtendedJSONObject response) {
            WaitHelper.getTestWaiter().performNotify();
          }

          @Override
          public void handleFailure(String reason) {
            WaitHelper.getTestWaiter().performNotify(new RuntimeException(reason));
          }

          @Override
          public void handleError(Exception e) {
            WaitHelper.getTestWaiter().performNotify(e);
          }
        });
      }
    });
  }

  protected void assertVerifyFailure(final String audience, final String assertion, final String expectedReason) {
    try {
      assertVerifySuccess(audience, assertion);
      Assert.fail("Expected verification failure but got success.");
    } catch (WaitHelper.InnerError e) {
      if (!(e.innerError instanceof RuntimeException)) {
        throw e;
      }
      Assert.assertEquals(expectedReason, e.innerError.getMessage());
    }
  }

  @Test
  public void testRSASuccess() throws Exception {
    String assertion = mockMyIdTokenFactory.createMockMyIDAssertion(keyPair, TEST_USERNAME, TEST_AUDIENCE);
    assertVerifySuccess(TEST_AUDIENCE, assertion);
  }

  @Test
  public void testDSASuccess() throws Exception {
    KeyPair keyPair = DSACryptoImplementation.generateKeypair(1024);
    String assertion = mockMyIdTokenFactory.createMockMyIDAssertion(keyPair, TEST_USERNAME, TEST_AUDIENCE);
    assertVerifySuccess(TEST_AUDIENCE, assertion);
  }

  @Test
  public void testAssertionExpired() throws Exception {
    long iat = System.currentTimeMillis();
    long dur = 1;
    String assertion = mockMyIdTokenFactory.createMockMyIDAssertion(keyPair, TEST_USERNAME, TEST_AUDIENCE,
        iat, dur,
        System.currentTimeMillis(), JSONWebTokenUtils.DEFAULT_ASSERTION_DURATION_IN_MILLISECONDS);
    assertVerifyFailure(TEST_AUDIENCE, assertion, "assertion has expired");
  }

  @Test
  public void testAssertionFromFuture() throws Exception {
    long iat = 2 * System.currentTimeMillis();
    long dur = 60 * 1000 * 1000;
    String assertion = mockMyIdTokenFactory.createMockMyIDAssertion(keyPair, TEST_USERNAME, TEST_AUDIENCE,
        iat, dur,
        System.currentTimeMillis(), JSONWebTokenUtils.DEFAULT_ASSERTION_DURATION_IN_MILLISECONDS);
    assertVerifyFailure(TEST_AUDIENCE, assertion, "assertion issued later than verification date");
  }

  @Test
  public void testCertificateExpired() throws Exception {
    long iat = System.currentTimeMillis();
    long dur = 1;
    String certificate = mockMyIdTokenFactory.createMockMyIDCertificate(publicKey, TEST_USERNAME, iat, dur);
    String assertion = JSONWebTokenUtils.createAssertion(privateKey, certificate, TEST_AUDIENCE);
    assertVerifyFailure(TEST_AUDIENCE, assertion, "assertion has expired");
  }

  @Test
  public void testCertificateFromFuture() throws Exception {
    long iat = 2 * System.currentTimeMillis();
    long dur = 60 * 1000 * 1000;
    String certificate = mockMyIdTokenFactory.createMockMyIDCertificate(publicKey, TEST_USERNAME, iat, dur);
    String assertion = JSONWebTokenUtils.createAssertion(privateKey, certificate, TEST_AUDIENCE);
    assertVerifyFailure(TEST_AUDIENCE, assertion, "assertion issued later than verification date");
  }
}
