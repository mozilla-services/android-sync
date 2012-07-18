package org.mozilla.persona.test;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.KeySpec;
import java.security.spec.RSAPrivateKeySpec;

import org.mozilla.apache.commons.codec.binary.Base64;
import org.mozilla.gecko.sync.ExtendedJSONObject;

/**
 * Implement JSON Web Crypto via RSA.
 */
public class RSAJWCrypto {
  protected static String base64urlEncode(final String s) throws UnsupportedEncodingException {
    return Base64.encodeBase64URLSafeString(s.getBytes("UTF-8"));
  }

  protected static String base64urlDecode(final String s) throws UnsupportedEncodingException {
    return new String(Base64.decodeBase64(s), "UTF-8");
  }

  public static String signature(
      final ExtendedJSONObject payloadToSign,
      final String issuer,
      final long issuedAt,
      final String audience,
      final long expiresAt,
      final ExtendedJSONObject secretKey) throws Exception {

    final ExtendedJSONObject payload = new ExtendedJSONObject(payloadToSign.toJSONString());
    payload.put("iss", issuer);
    payload.put("iat", issuedAt);
    if (audience != null) {
      payload.put("aud", audience);
    }
    payload.put("exp", expiresAt);
    final String sPayload = payload.toJSONString();

    final ExtendedJSONObject header = new ExtendedJSONObject();
    header.put("alg", "RS256");
    final String sHeader = header.toJSONString();

    final String string = base64urlEncode(sHeader) + "." + base64urlEncode(sPayload);
    final byte[] bytes = string.getBytes("UTF-8");

    final BigInteger n = new BigInteger(secretKey.getString("n"));
    final BigInteger d = new BigInteger(secretKey.getString("d"));

    final KeyFactory keyFactory = KeyFactory.getInstance("RSA");
    final KeySpec keySpec = new RSAPrivateKeySpec(n, d);
    final PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

    final Signature signer = Signature.getInstance("SHA256withRSA");
    signer.initSign(privateKey);
    signer.update(bytes);
    final String signature = Base64.encodeBase64URLSafeString(signer.sign()); // Change to base64urlEncode for badness.

    return string + "." + signature;
  }

  public static String certificate(
      final ExtendedJSONObject publicKeyToSign,
      final String email,
      final String issuer,
      final long issuedAt,
      final long expiresAt,
      final ExtendedJSONObject secretKey) throws Exception {

    final ExtendedJSONObject principal = new ExtendedJSONObject();
    principal.put("email", email);

    final ExtendedJSONObject payload = new ExtendedJSONObject();
    payload.put("principal", principal);
    payload.put("public-key", publicKeyToSign);

    return signature(payload, issuer, issuedAt, null, expiresAt, secretKey);
  }

  public static ExtendedJSONObject generateKeypair(final int keysize) throws NoSuchAlgorithmException {
    final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
    keyPairGenerator.initialize(keysize);
    final KeyPair keyPair = keyPairGenerator.generateKeyPair();
    final RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
    final RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();

    BigInteger n = publicKey.getModulus();
    BigInteger e = publicKey.getPublicExponent();
    BigInteger d = privateKey.getPrivateExponent();

    final ExtendedJSONObject publicKeyToSign = new ExtendedJSONObject();
    publicKeyToSign.put("algorithm", "RS");
    publicKeyToSign.put("n", n.toString(10));
    publicKeyToSign.put("e", e.toString(10));

    final ExtendedJSONObject privateKeyToSignWith = new ExtendedJSONObject();
    privateKeyToSignWith.put("algorithm", "RS");
    privateKeyToSignWith.put("n", n.toString(10));
    privateKeyToSignWith.put("d", d.toString(10));

    final ExtendedJSONObject pair = new ExtendedJSONObject();
    pair.put("publicKey", publicKeyToSign);
    pair.put("privateKey", privateKeyToSignWith);

    return pair;
  }

  public static String assertion(
      final ExtendedJSONObject privateKeyToSignWith,
      final String certificate,
      final String issuer,
      final String audience) throws Exception {

    final long issuedAt = System.currentTimeMillis();
    final long expiresAt = issuedAt + 60 * 60 * 1000;

    String assertion;
    try {
      assertion = signature(new ExtendedJSONObject(), issuer, issuedAt, audience, expiresAt, privateKeyToSignWith);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }

    return certificate + "~" + assertion;
  }
}
