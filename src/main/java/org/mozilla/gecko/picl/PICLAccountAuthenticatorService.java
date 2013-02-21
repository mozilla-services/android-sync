/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.picl;

import org.mozilla.gecko.sync.Logger;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class PICLAccountAuthenticatorService extends Service {
  public static final String LOG_TAG = PICLAccountAuthenticatorService.class.getSimpleName();

  // Lazily initialized by <code>getAuthenticator</code>.
  protected PICLAccountAuthenticator accountAuthenticator = null;

  protected PICLAccountAuthenticator getAuthenticator() {
    if (accountAuthenticator == null) {
      accountAuthenticator = new PICLAccountAuthenticator(this);
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
    Logger.debug(LOG_TAG, "onBind");

    if (intent.getAction().equals(android.accounts.AccountManager.ACTION_AUTHENTICATOR_INTENT)) {
      return getAuthenticator().getIBinder();
    }

    return null;
  }
}
