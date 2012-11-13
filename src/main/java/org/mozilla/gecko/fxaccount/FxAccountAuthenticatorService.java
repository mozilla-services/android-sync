/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.fxaccount;

import org.mozilla.gecko.sync.Logger;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class FxAccountAuthenticatorService extends Service {
  public static final String LOG_TAG = "FxAccountAuthService";

  protected FxAccountAuthenticator accountAuthenticator = null;

  protected FxAccountAuthenticator getAuthenticator() {
    if (accountAuthenticator == null) {
      accountAuthenticator = new FxAccountAuthenticator(this);
    }
    return accountAuthenticator;
  }

  @Override
  public void onCreate() {
    Logger.debug(LOG_TAG, "onCreate");
    accountAuthenticator = getAuthenticator();
  }

  @Override
  public IBinder onBind(Intent intent) {
    if (intent.getAction().equals(android.accounts.AccountManager.ACTION_AUTHENTICATOR_INTENT)) {
      return getAuthenticator().getIBinder();
    }
    return null;
  }
}
