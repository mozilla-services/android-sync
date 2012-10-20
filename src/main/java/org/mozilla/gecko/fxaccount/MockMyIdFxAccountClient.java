package org.mozilla.gecko.fxaccount;

import java.security.NoSuchAlgorithmException;

import org.mozilla.apache.commons.codec.binary.Base64;
import org.mozilla.apache.commons.codec.digest.DigestUtils;
import org.mozilla.gecko.browserid.crypto.JWCrypto;
import org.mozilla.gecko.browserid.crypto.RSAJWCrypto;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.NonObjectJSONException;

public class MockMyIdFxAccountClient implements FxAccountClient {

  @Override
  public void logIn(String email, String password, FxAccountClientDelegate delegate) {
    String mockmyidUsername = "fxa-" + Base64.encodeBase64URLSafeString(DigestUtils.sha(email)).substring(0, 5);

    ExtendedJSONObject keyPair;
    ExtendedJSONObject publicKeyToSign;
    try {
      keyPair = RSAJWCrypto.generateKeypair(2048);
      publicKeyToSign = keyPair.getObject("publicKey");
    } catch (NoSuchAlgorithmException e) {
      delegate.onError(e);
      return;
    } catch (NonObjectJSONException e) {
      delegate.onError(e);
      return;
    }

    String certificate;
    try {
      certificate = JWCrypto.createMockMyIdCertificate(publicKeyToSign, mockmyidUsername);
    } catch (Exception e) {
      delegate.onFailure(e);
      return;
    }

    ExtendedJSONObject result = new ExtendedJSONObject();
    result.put("mockmyidUsername", mockmyidUsername);
    result.put("certificate", certificate);
    result.put("keyPair", keyPair);

    delegate.onSuccess(result);
  }

  @Override
  public void createAccount(String email, String password, FxAccountClientDelegate delegate) {
    logIn(email, password, delegate);
  }

  @Override
  public String getAssertion(ExtendedJSONObject result, String audience) throws Exception {
    // String mockmyidUsername = result.getString("mockmyidUsername");
    String certificate = result.getString("certificate");
    String issuer = "127.0.0.1";

    String assertion = null;

    ExtendedJSONObject keyPair = result.getObject("keyPair");
    ExtendedJSONObject privateKeyToSignWith = keyPair.getObject("privateKey");

    assertion = RSAJWCrypto.assertion(privateKeyToSignWith, certificate, issuer, audience);

    return assertion;
  }
}
