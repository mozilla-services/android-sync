package org.mozilla.gecko.browserid.crypto.test;

import org.junit.Before;
import org.junit.Test;
import org.mozilla.gecko.browserid.crypto.JWCrypto;
import org.mozilla.gecko.browserid.crypto.RSAJWCrypto;
import org.mozilla.gecko.sync.ExtendedJSONObject;

public class TestJWCrypto {
  public static final String TEST_USERNAME = "test";
  public static final String TEST_CERTIFICATE_ISSUER = "mockmyid.com";
  public static final String TEST_EMAIL = TEST_USERNAME + "@" + TEST_CERTIFICATE_ISSUER;
  public static final String TEST_ASSERTION_ISSUER = "127.0.0.1";
  public static final String TEST_AUDIENCE = "http://localhost:8080";

  protected BlockingBrowserIDVerifierClient client;

  @Before
  public void setUp() throws Exception {
    client = new BlockingBrowserIDVerifierClient();
  }

  @Test
  public void testSuccess() throws Exception {
    final ExtendedJSONObject pair = RSAJWCrypto.generateKeypair(2048);
    final ExtendedJSONObject publicKeyToSign = pair.getObject("publicKey");
    final ExtendedJSONObject privateKeyToSignWith = pair.getObject("privateKey");

    final String certificate = JWCrypto.createMockMyIdCertificate(publicKeyToSign, TEST_USERNAME);
    final String assertion = RSAJWCrypto.assertion(privateKeyToSignWith, certificate, TEST_ASSERTION_ISSUER, TEST_AUDIENCE);

    client.assertVerifySuccess(TEST_AUDIENCE, assertion);
  }

  @Test
  public void testCertificateExpired() throws Exception {
    final ExtendedJSONObject pair = RSAJWCrypto.generateKeypair(2048);
    final ExtendedJSONObject publicKeyToSign = pair.getObject("publicKey");
    final ExtendedJSONObject privateKeyToSignWith = pair.getObject("privateKey");

    final long iat = System.currentTimeMillis();
    final long dur = 1;

    final String certificate = JWCrypto.createMockMyIdCertificate(publicKeyToSign, TEST_USERNAME, iat, dur);
    final String assertion = RSAJWCrypto.assertion(privateKeyToSignWith, certificate, TEST_ASSERTION_ISSUER, TEST_AUDIENCE);

    client.assertVerifyFailure(TEST_AUDIENCE, assertion, "assertion has expired");
  }

  @Test
  public void testAssertionExpired() throws Exception {
    final ExtendedJSONObject pair = RSAJWCrypto.generateKeypair(2048);
    final ExtendedJSONObject publicKeyToSign = pair.getObject("publicKey");
    final ExtendedJSONObject privateKeyToSignWith = pair.getObject("privateKey");

    final String certificate = JWCrypto.createMockMyIdCertificate(publicKeyToSign, TEST_USERNAME);

    final long iat = System.currentTimeMillis();
    final long dur = 1;

    final String assertion = RSAJWCrypto.assertion(privateKeyToSignWith, certificate, TEST_ASSERTION_ISSUER, TEST_AUDIENCE, iat, dur);

    client.assertVerifyFailure(TEST_AUDIENCE, assertion, "assertion has expired");
  }

  @Test
  public void testCertificateFromFuture() throws Exception {
    final ExtendedJSONObject pair = RSAJWCrypto.generateKeypair(2048);
    final ExtendedJSONObject publicKeyToSign = pair.getObject("publicKey");
    final ExtendedJSONObject privateKeyToSignWith = pair.getObject("privateKey");

    final long iat = 2 * System.currentTimeMillis();
    final long dur = 60 * 1000 * 1000;

    final String certificate = JWCrypto.createMockMyIdCertificate(publicKeyToSign, TEST_USERNAME, iat, dur);
    final String assertion = RSAJWCrypto.assertion(privateKeyToSignWith, certificate, TEST_ASSERTION_ISSUER, TEST_AUDIENCE);

    client.assertVerifyFailure(TEST_AUDIENCE, assertion, "assertion issued later than verification date");
  }

  @Test
  public void testAssertionFromFuture() throws Exception {
    final ExtendedJSONObject pair = RSAJWCrypto.generateKeypair(2048);
    final ExtendedJSONObject publicKeyToSign = pair.getObject("publicKey");
    final ExtendedJSONObject privateKeyToSignWith = pair.getObject("privateKey");

    final String certificate = JWCrypto.createMockMyIdCertificate(publicKeyToSign, TEST_USERNAME);

    final long iat = 2 * System.currentTimeMillis();
    final long dur = 60 * 1000 * 1000;

    final String assertion = RSAJWCrypto.assertion(privateKeyToSignWith, certificate, TEST_ASSERTION_ISSUER, TEST_AUDIENCE, iat, dur);

    client.assertVerifyFailure(TEST_AUDIENCE, assertion, "assertion issued later than verification date");
  }
}
