/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.browserid.test;

import java.net.URI;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mozilla.android.sync.test.integration.IntegrationTestCategory;
import org.mozilla.gecko.background.testhelpers.WaitHelper;
import org.mozilla.gecko.browserid.BrowserIDKeyPair;
import org.mozilla.gecko.browserid.DSACryptoImplementation;
import org.mozilla.gecko.browserid.JSONWebTokenUtils;
import org.mozilla.gecko.browserid.MockMyIDTokenFactory;
import org.mozilla.gecko.browserid.RSACryptoImplementation;
import org.mozilla.gecko.browserid.SigningPrivateKey;
import org.mozilla.gecko.browserid.VerifyingPublicKey;
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
  protected BrowserIDKeyPair keyPair;
  protected VerifyingPublicKey publicKey;
  protected SigningPrivateKey privateKey;

  protected MockMyIDTokenFactory mockMyIdTokenFactory;

  @Before
  public void setUp() throws Exception {
    client = new BrowserIDRemoteVerifierClient(new URI(VERIFIER_URL));

    mockMyIdTokenFactory = new MockMyIDTokenFactory();

    keyPair = RSACryptoImplementation.generateKeyPair(1024); // No need for strong keys while testing.
    publicKey = keyPair.getPublic();
    privateKey = keyPair.getPrivate();
  }

  protected void assertVerifySuccess(final String audience, final String assertion) {
    assertVerifySuccess(client, audience, assertion);
  }

  public static void assertVerifySuccess(final BrowserIDVerifierClient client, final String audience, final String assertion) {
    WaitHelper.getTestWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        client.verify(audience, assertion, new BrowserIDVerifierDelegate() {
          @Override
          public void handleSuccess(ExtendedJSONObject response) {
            WaitHelper.getTestWaiter().performNotify();
          }

          @Override
          public void handleFailure(ExtendedJSONObject response) {
            WaitHelper.getTestWaiter().performNotify(new RuntimeException(response.toJSONString()));
          }

          @Override
          public void handleError(Exception e) {
            WaitHelper.getTestWaiter().performNotify(e);
          }
        });
      }
    });
  }

  protected void assertVerifyFailure(final String audience, final String assertion, final String expectedReason) throws Exception {
    try {
      assertVerifySuccess(audience, assertion);
      Assert.fail("Expected verification failure but got success.");
    } catch (WaitHelper.InnerError e) {
      if (!(e.innerError instanceof RuntimeException)) {
        throw e;
      }
      ExtendedJSONObject o = ExtendedJSONObject.parseJSONObject(e.innerError.getMessage());
      Assert.assertEquals(expectedReason, o.getString("reason"));
    }
  }

  @Test
  public void testRSASuccess() throws Exception {
    String assertion = mockMyIdTokenFactory.createMockMyIDAssertion(keyPair, TEST_USERNAME, TEST_AUDIENCE);
    assertVerifySuccess(TEST_AUDIENCE, assertion);
  }

  @Test
  public void testDSASuccess() throws Exception {
    BrowserIDKeyPair keyPair = DSACryptoImplementation.generateKeyPair(1024);
    String assertion = mockMyIdTokenFactory.createMockMyIDAssertion(keyPair, TEST_USERNAME, TEST_AUDIENCE);
    assertVerifySuccess(TEST_AUDIENCE, assertion);
  }

  @Test
  public void testAssertionWithoutIssuedAt() throws Exception {
    long ciat = System.currentTimeMillis();
    long cexp = ciat + JSONWebTokenUtils.DEFAULT_CERTIFICATE_DURATION_IN_MILLISECONDS;
    String assertion = mockMyIdTokenFactory.createMockMyIDAssertion(keyPair, TEST_USERNAME, TEST_AUDIENCE,
        ciat, cexp, null, JSONWebTokenUtils.DEFAULT_FUTURE_EXPIRES_AT_IN_MILLISECONDS);
    assertVerifySuccess(TEST_AUDIENCE, assertion);
  }

  @Test
  public void testAssertionExpired() throws Exception {
    long ciat = System.currentTimeMillis();
    long cexp = ciat + 1;
    long aiat = cexp + 1;
    long aexp = aiat + JSONWebTokenUtils.DEFAULT_ASSERTION_DURATION_IN_MILLISECONDS;
    String assertion = mockMyIdTokenFactory.createMockMyIDAssertion(keyPair, TEST_USERNAME, TEST_AUDIENCE,
        ciat, cexp, aiat, aexp);
    assertVerifyFailure(TEST_AUDIENCE, assertion, "assertion has expired");
  }

  @Test
  public void testAssertionFromFuture() throws Exception {
    long ciat = 2 * System.currentTimeMillis();
    long cexp = ciat + 60 * 1000 * 1000;
    long aiat = System.currentTimeMillis();
    long aexp = aiat + JSONWebTokenUtils.DEFAULT_ASSERTION_DURATION_IN_MILLISECONDS;
    String assertion = mockMyIdTokenFactory.createMockMyIDAssertion(keyPair, TEST_USERNAME, TEST_AUDIENCE,
        ciat, cexp, aiat, aexp);
    assertVerifyFailure(TEST_AUDIENCE, assertion, "assertion issued later than verification date");
  }

  @Test
  public void testCertificateExpired() throws Exception {
    long ciat = System.currentTimeMillis() - 2;
    long cexp = ciat + 1;
    long aiat = System.currentTimeMillis();
    long aexp = aiat + JSONWebTokenUtils.DEFAULT_ASSERTION_DURATION_IN_MILLISECONDS;
    String certificate = mockMyIdTokenFactory.createMockMyIDCertificate(publicKey, TEST_USERNAME, ciat, cexp);
    String assertion = JSONWebTokenUtils.createAssertion(privateKey, certificate, TEST_AUDIENCE, JSONWebTokenUtils.DEFAULT_ASSERTION_ISSUER, aiat, aexp);
    assertVerifyFailure(TEST_AUDIENCE, assertion, "assertion has expired");
  }

  @Test
  public void testCertificateFromFuture() throws Exception {
    long ciat = 2 * System.currentTimeMillis();
    long cexp = ciat + JSONWebTokenUtils.DEFAULT_CERTIFICATE_DURATION_IN_MILLISECONDS;
    long aiat = System.currentTimeMillis();
    long aexp = aiat + JSONWebTokenUtils.DEFAULT_ASSERTION_DURATION_IN_MILLISECONDS;
    String certificate = mockMyIdTokenFactory.createMockMyIDCertificate(publicKey, TEST_USERNAME, ciat, cexp);
    String assertion = JSONWebTokenUtils.createAssertion(privateKey, certificate, TEST_AUDIENCE, JSONWebTokenUtils.DEFAULT_ASSERTION_ISSUER, aiat, aexp);
    assertVerifyFailure(TEST_AUDIENCE, assertion, "assertion issued later than verification date");
  }
}
