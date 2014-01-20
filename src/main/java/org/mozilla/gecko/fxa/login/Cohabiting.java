/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.fxa.login;

import org.mozilla.gecko.browserid.BrowserIDKeyPair;
import org.mozilla.gecko.fxa.login.FxAccountLoginStateMachine.ExecuteDelegate;
import org.mozilla.gecko.fxa.login.FxAccountLoginTransition.LogMessage;

public class Cohabiting extends TokensAndKeysState {
  public Cohabiting(String email, String uid, byte[] sessionToken, byte[] kA, byte[] kB, BrowserIDKeyPair keyPair) {
    super(StateLabel.Cohabiting, email, uid, sessionToken, kA, kB, keyPair);
  }

  @Override
  public void execute(final ExecuteDelegate delegate) {
    delegate.getClient().sign(sessionToken, keyPair.getPublic().toJSONObject(), delegate.getCertificateDurationInMilliseconds(),
        new BaseRequestDelegate<String>(this, delegate) {
      @Override
      public void handleSuccess(String result) {
        delegate.handleTransition(new LogMessage("sign succeeded"), new Married(email, uid, sessionToken, kA, kB, keyPair, result));
      }
    });
  }
}
