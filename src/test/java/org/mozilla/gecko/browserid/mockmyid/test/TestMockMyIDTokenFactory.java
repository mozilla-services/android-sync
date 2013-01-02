/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.browserid.mockmyid.test;

import java.net.URI;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

import org.junit.Before;
import org.junit.Test;
import org.mozilla.gecko.browserid.mockmyid.MockMyIDTokenFactory;
import org.mozilla.gecko.browserid.verifier.test.BlockingBrowserIDVerifierClient;
import org.mozilla.gecko.jwcrypto.RSACryptoImplementation;


public class TestMockMyIDTokenFactory {
  public static String TEST_USERNAME = "test";
  public static String TEST_CERTIFICATE_ISSUER = "mockmyid.com";
  public static String TEST_EMAIL = TEST_USERNAME + "@" + TEST_CERTIFICATE_ISSUER;
  public static String TEST_ASSERTION_ISSUER = "127.0.0.1";
  public static String TEST_AUDIENCE = "http://localhost:8080";

  public static String VERIFIER_URL = "https://verifier.login.persona.org/verify";

  protected BlockingBrowserIDVerifierClient client;
  protected KeyPair keyPair;
  protected PublicKey publicKey;
  protected PrivateKey privateKey;

  protected MockMyIDTokenFactory mockMyIdTokenFactory;

  @Before
  public void setUp() throws Exception {
    client = new BlockingBrowserIDVerifierClient(new URI(VERIFIER_URL));

    mockMyIdTokenFactory = new MockMyIDTokenFactory();

    keyPair = new RSACryptoImplementation().generateKeypair(1024); // No need for strong keys while testing.
    publicKey = keyPair.getPublic();
    privateKey = keyPair.getPrivate();
  }

  @Test
  public void testSuccess() throws Exception {
    String assertion = mockMyIdTokenFactory.createMockMyIDAssertion(TEST_USERNAME, TEST_AUDIENCE);

    client.assertVerifySuccess(TEST_AUDIENCE, assertion);
  }

  @Test
  public void testAssertionExpired() throws Exception {
    long iat = System.currentTimeMillis();
    long dur = 1;

    String assertion = mockMyIdTokenFactory.createMockMyIDAssertion(TEST_USERNAME, TEST_AUDIENCE, iat, dur);

    client.assertVerifyFailure(TEST_AUDIENCE, assertion, "assertion has expired");
  }

  @Test
  public void testAssertionFromFuture() throws Exception {
    long iat = 2 * System.currentTimeMillis();
    long dur = 60 * 1000 * 1000;

    String assertion = mockMyIdTokenFactory.createMockMyIDAssertion(TEST_USERNAME, TEST_AUDIENCE, iat, dur);

    client.assertVerifyFailure(TEST_AUDIENCE, assertion, "assertion issued later than verification date");
  }

  @Test
  public void testCertificateExpired() throws Exception {
    long iat = System.currentTimeMillis();
    long dur = 1;

    String certificate = mockMyIdTokenFactory.createMockMyIDCertificate(publicKey, TEST_USERNAME, iat, dur);
    String assertion = mockMyIdTokenFactory.browserIdTokenFactory.createAssertion(privateKey, certificate, TEST_ASSERTION_ISSUER, TEST_AUDIENCE);

    client.assertVerifyFailure(TEST_AUDIENCE, assertion, "assertion has expired");
  }

  @Test
  public void testCertificateFromFuture() throws Exception {
    long iat = 2 * System.currentTimeMillis();
    long dur = 60 * 1000 * 1000;

    String certificate = mockMyIdTokenFactory.createMockMyIDCertificate(publicKey, TEST_USERNAME, iat, dur);
    String assertion = mockMyIdTokenFactory.browserIdTokenFactory.createAssertion(privateKey, certificate, TEST_ASSERTION_ISSUER, TEST_AUDIENCE);

    client.assertVerifyFailure(TEST_AUDIENCE, assertion, "assertion issued later than verification date");
  }
}
