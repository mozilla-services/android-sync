/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.fxa.login;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import org.json.simple.parser.ParseException;
import org.mozilla.gecko.background.fxa.FxAccountUtils;
import org.mozilla.gecko.browserid.BrowserIDKeyPair;
import org.mozilla.gecko.browserid.JSONWebTokenUtils;
import org.mozilla.gecko.fxa.login.FxAccountLoginStateMachine.ExecuteDelegate;
import org.mozilla.gecko.fxa.login.FxAccountLoginTransition.LogMessage;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.NonObjectJSONException;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.crypto.KeyBundle;

public class Married extends TokensAndKeysState {
  protected final String certificate;

  public Married(String email, String uid, byte[] sessionToken, byte[] kA, byte[] kB, BrowserIDKeyPair keyPair, String certificate) {
    super(StateLabel.Married, email, uid, sessionToken, kA, kB, keyPair);
    Utils.throwIfNull(certificate);
    this.certificate = certificate;
  }

  @Override
  public ExtendedJSONObject toJSONObject() {
    ExtendedJSONObject o = super.toJSONObject();
    // Fields are non-null by constructor.
    o.put("certificate", certificate);
    return o;
  }

  @Override
  public void execute(final ExecuteDelegate delegate) {
    delegate.handleTransition(new LogMessage("staying married"), this);
  }

  public String generateAssertion(String audience, String issuer, long issuedAt, long durationInMilliseconds) throws NonObjectJSONException, IOException, ParseException, GeneralSecurityException {
    return JSONWebTokenUtils.createAssertion(keyPair.getPrivate(), certificate, audience, issuer, issuedAt, durationInMilliseconds);
  }

  public KeyBundle getSyncKeyBundle() throws InvalidKeyException, NoSuchAlgorithmException, UnsupportedEncodingException {
    // TODO Document this choice for deriving from kB.
    return FxAccountUtils.generateSyncKeyBundle(kB);
  }
}
