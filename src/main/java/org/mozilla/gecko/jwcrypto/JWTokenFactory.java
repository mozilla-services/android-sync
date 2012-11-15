package org.mozilla.gecko.jwcrypto;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

import org.mozilla.apache.commons.codec.binary.Base64;
import org.mozilla.apache.commons.codec.binary.StringUtils;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.Utils;

public class JWTokenFactory {
  protected final JWCryptoImplementation cryptoImplementation;
  protected final String algorithm;

  public JWTokenFactory(String algorithm, JWCryptoImplementation cryptoImplementation) {
    this.algorithm = algorithm;
    this.cryptoImplementation = cryptoImplementation;
  }

  public String getAlgorithm() {
    return this.algorithm;
  }

  public String encode(String payload, PrivateKey privateKey) throws Exception {
    return encode(payload, privateKey, null);
  }

  protected String encode(String payload, PrivateKey privateKey, Map<String, Object> headerFields) throws Exception {
    ExtendedJSONObject header = new ExtendedJSONObject();
    if (headerFields != null) {
      for (Entry<String, Object> entry : headerFields.entrySet()) {
        header.put(entry.getKey(), entry.getValue());
      }
    }
    header.put("typ", "JWT");
    header.put("alg", getAlgorithm());

    String encodedHeader  = Base64.encodeBase64URLSafeString(header.toJSONString().getBytes("UTF-8"));
    String encodedPayload = Base64.encodeBase64URLSafeString(payload.getBytes("UTF-8"));

    ArrayList<String> segments = new ArrayList<String>();

    segments.add(encodedHeader);
    segments.add(encodedPayload);

    byte[] message = Utils.toDelimitedString(".", segments).getBytes("UTF-8");

    segments.add(Base64.encodeBase64URLSafeString(cryptoImplementation.signMessage(message, privateKey)));

    return Utils.toDelimitedString(".", segments);
  }

  public String decode(String token, PublicKey publicKey) throws GeneralSecurityException, UnsupportedEncodingException {
    String[] segments = token.split("\\.");
    if (segments == null || segments.length != 3) {
      throw new GeneralSecurityException();
    }

    byte[] message = (segments[0] + "." + segments[1]).getBytes("UTF-8");
    byte[] signature = Base64.decodeBase64(segments[2]);

    boolean verifies = cryptoImplementation.verifyMessage(message, signature, publicKey);

    if (!verifies) {
      throw new GeneralSecurityException();
    }

    String payload = StringUtils.newStringUtf8(Base64.decodeBase64(segments[1]));
    return payload;
  }
}
