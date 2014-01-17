/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.fxa.authenticator;

import java.security.GeneralSecurityException;

import org.mozilla.gecko.background.fxa.FxAccountUtils;
import org.mozilla.gecko.browserid.BrowserIDKeyPair;
import org.mozilla.gecko.browserid.JSONWebTokenUtils;
import org.mozilla.gecko.browserid.MockMyIDTokenFactory;
import org.mozilla.gecko.browserid.RSACryptoImplementation;
import org.mozilla.gecko.fxa.FxAccountConstants;
import org.mozilla.gecko.fxa.authenticator.FxAccountLoginPolicy.AccountState;
import org.mozilla.gecko.sync.Utils;

public class MockFxAccount implements AbstractFxAccount {
  public byte[] emailUTF8 = null;
  public byte[] quickStretchedPW = null;

  public String serverURI = null;
  public boolean valid = true;

  public byte[] sessionToken;
  public byte[] keyFetchToken;
  public boolean verified = false;

  public byte[] kA = null;
  public byte[] kB = null;

  public byte[] unwrapKb = new byte[32];

  public BrowserIDKeyPair assertionKeyPair;

  public String certificate;
  public String assertion;

  @Override
  public byte[] getEmailUTF8() {
    return emailUTF8;
  }

  @Override
  public byte[] getQuickStretchedPW() {
    return quickStretchedPW;
  }

  @Override
  public void setQuickStretchedPW(byte[] quickStretchedPW) {
    this.quickStretchedPW = quickStretchedPW;
  }
  
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
    return assertion;
  }

  @Override
  public void setAssertion(String assertion) {
    this.assertion = assertion;
  }

  public static MockFxAccount makeAccount(AccountState state) throws Exception {
    MockFxAccount fxAccount = new MockFxAccount();
    fxAccount.emailUTF8 = "testEmail@test.com".getBytes("UTF-8");
    fxAccount.quickStretchedPW = FxAccountUtils.generateQuickStretchedPW(fxAccount.emailUTF8, "testPassword".getBytes("UTF-8"));
    fxAccount.serverURI = FxAccountConstants.DEFAULT_IDP_ENDPOINT;
    if (state == AccountState.Invalid) {
      fxAccount.setInvalid();
      return fxAccount;
    }
    if (state == AccountState.NeedsSessionToken) {
      return fxAccount;
    }
    fxAccount.sessionToken = Utils.generateRandomBytes(32);
    if (state == AccountState.NeedsVerification) {
      return fxAccount;
    }
    fxAccount.verified = true;
    if (state == AccountState.NeedsKeys) {
      return fxAccount;
    }
    fxAccount.kA = Utils.generateRandomBytes(32);
    fxAccount.kB = Utils.generateRandomBytes(32);
    if (state == AccountState.NeedsCertificate) {
      return fxAccount;
    }
    MockMyIDTokenFactory mockMyIDTokenFactory = new MockMyIDTokenFactory();
    BrowserIDKeyPair keyPair = fxAccount.getAssertionKeyPair();
    fxAccount.certificate = mockMyIDTokenFactory.createMockMyIDCertificate(keyPair.getPublic(), "testUsername");
    if (state == AccountState.NeedsAssertion) {
      return fxAccount;
    }
    fxAccount.assertion = JSONWebTokenUtils.createAssertion(keyPair.getPrivate(), fxAccount.certificate, "http://testAudience.com");
    return fxAccount;
  }

  @Override
  public boolean isValid() {
    return valid;
  }

  @Override
  public void setInvalid() {
    valid = false;
  }
}
