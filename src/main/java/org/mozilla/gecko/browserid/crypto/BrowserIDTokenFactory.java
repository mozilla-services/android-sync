package org.mozilla.gecko.browserid.crypto;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;

import org.json.simple.parser.ParseException;
import org.mozilla.gecko.jwcrypto.JWCryptoImplementation;
import org.mozilla.gecko.jwcrypto.JWTokenFactory;
import org.mozilla.gecko.jwcrypto.PublicKeySerializer;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.NonObjectJSONException;

public class BrowserIDTokenFactory {
  protected final JWTokenFactory jwTokenFactory;
  protected final PublicKeySerializer publicKeySerializer;

  public BrowserIDTokenFactory(String algorithm, JWCryptoImplementation cryptoImplementation, PublicKeySerializer publicKeySerializer) {
    this.jwTokenFactory = new JWTokenFactory(algorithm, cryptoImplementation);
    this.publicKeySerializer = publicKeySerializer;
  }

  public String getAlgorithm() {
    return jwTokenFactory.getAlgorithm();
  }

  public JWTokenFactory getJWTokenFactory() {
    return jwTokenFactory;
  }

  protected String getPayloadString(
      String payloadString,
      String issuer,
      long issuedAt,
      String audience,
      long expiresAt) throws NonObjectJSONException, IOException, ParseException {

    ExtendedJSONObject payload;
    if (payloadString != null) {
      payload = new ExtendedJSONObject(payloadString);
    } else {
      payload = new ExtendedJSONObject();
    }

    payload.put("iss", issuer);
    payload.put("iat", issuedAt);
    if (audience != null) {
      payload.put("aud", audience);
    }
    payload.put("exp", expiresAt);

    return payload.toJSONString();
  }

  // protected abstract String getHeaderString();

//  public static class JWSignature {
//    public final String header;
//    public final String payload;
//
//    public final byte[] signatureBytes;
//
//    public JWSignature(String header, String payload, byte[] signatureBytes) throws Exception {
//      this.header = header;
//      this.payload = payload;
//      this.signatureBytes = signatureBytes;
//    }
//
//    public String serialize() throws Exception {
//      String string = Base64.encodeBase64URLSafeString(header.getBytes("UTF-8")) + "." + base64urlEncode(payload);
//      String signature = Base64.encodeBase64URLSafeString(signatureBytes); // Change to base64urlEncode for badness.
//
//      return string + "." + signature;
//    }
//
//    public JWSignature(String serialized) throws Exception {
//      int i = serialized.indexOf(".");
//      if (i == -1) {
//        throw new Exception();
//      }
//      int j = serialized.indexOf(".", i + 1);
//      if (j == -1) {
//        throw new Exception();
//      }
//
//      String header = serialized.substring(0, i);
//      String payload = serialized.substring(i, j);
//      String signature = serialized.substring(j);
//
//      this.header = base64urlDecode(header);
//      this.payload = base64urlDecode(payload);
//      this.signatureBytes = Base64.decodeBase64(signature);
//    }
//  }

  public String sign(
      String payloadString,
      String issuer,
      long issuedAt,
      String audience,
      long expiresAt,
      PrivateKey privateKey) throws Exception {
    String payload = getPayloadString(payloadString, issuer, issuedAt, audience, expiresAt);

    //    String header = getHeaderString();
//
//    String string = Base64.encodeBase64URLSafeString(header.getBytes("UTF-8")) +
//        "." +
//        Base64.encodeBase64URLSafeString(payload.getBytes("UTF-8"));
//
//    byte[] signatureBytes = jwTokenFactory.encode(payload, privateKey, null);
//
//    return new JWSignature(header, payload, signatureBytes);

    return jwTokenFactory.encode(payload, privateKey);
  }

  public final String getCertificatePayloadString(
      PublicKey publicKeyToSign,
      String email) throws Exception {
    ExtendedJSONObject principal = new ExtendedJSONObject();
    principal.put("email", email);

    ExtendedJSONObject payload = new ExtendedJSONObject();
    payload.put("principal", principal);
    payload.put("public-key", new ExtendedJSONObject(publicKeySerializer.serializePublicKey(publicKeyToSign)));

    return payload.toJSONString();
  }

  public String createCertificate(
      PublicKey publicKeyToSign,
      String email,
      String issuer,
      long issuedAt,
      long expiresAt,
      PrivateKey privateKey) throws Exception {
    String certificatePayloadString = getCertificatePayloadString(publicKeyToSign, email);

    String payloadString = getPayloadString(certificatePayloadString, issuer, issuedAt, null, expiresAt);

    return jwTokenFactory.encode(payloadString, privateKey);
  }

  public String createAssertion(
      PrivateKey privateKeyToSignWith,
      String certificate,
      String issuer,
      String audience,
      long issuedAt,
      long durationInMilliseconds) throws Exception {
    long expiresAt = issuedAt + durationInMilliseconds;

    // String signature = sign(new ExtendedJSONObject().toJSONString(), issuer, issuedAt, audience, expiresAt, privateKeyToSignWith).serialize();
    String emptyAssertionPayloadString = "{}";
    String payloadString = getPayloadString(emptyAssertionPayloadString, issuer, issuedAt, audience, expiresAt);

    //    String header = getHeaderString();
//
//    String string = Base64.encodeBase64URLSafeString(header.getBytes("UTF-8")) +
//        "." +
//        Base64.encodeBase64URLSafeString(payload.getBytes("UTF-8"));
//
//    byte[] signatureBytes = jwTokenFactory.encode(payload, privateKey, null);
//
//    return new JWSignature(header, payload, signatureBytes);

    String signature = jwTokenFactory.encode(payloadString, privateKeyToSignWith);

    return certificate + "~" + signature;
  }

  public String createAssertion(
      PrivateKey privateKeyToSignWith,
      String certificate,
      String issuer,
      String audience) throws Exception {
    long issuedAt = System.currentTimeMillis();
    long durationInMilliseconds = 60 * 60 * 1000;

    return createAssertion(privateKeyToSignWith, certificate, issuer, audience, issuedAt, durationInMilliseconds);
  }
}
