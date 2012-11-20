/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.fxaccount;

import org.mozilla.gecko.sync.Logger;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.os.Bundle;

public class FxAccountAuthenticator extends AbstractAccountAuthenticator {
  public static final String LOG_TAG = FxAccountAuthenticator.class.getSimpleName();

  protected final Context context;
  protected final AccountManager accountManager;

  public FxAccountAuthenticator(Context context) {
    super(context);
    this.context = context;
    this.accountManager = AccountManager.get(context);
  }

  @Override
  public Bundle addAccount(AccountAuthenticatorResponse response,
      String accountType, String authTokenType, String[] requiredFeatures,
      Bundle options)
          throws NetworkErrorException {
    Logger.debug(LOG_TAG, "addAccount");

    throw new NetworkErrorException("Not yet implemented.");
  }

  @Override
  public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options)
      throws NetworkErrorException {
    Logger.debug(LOG_TAG, "confirmCredentials");
    return null;
  }

  @Override
  public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
    Logger.debug(LOG_TAG, "editProperties");

    return null;
  }

  @Override
  public Bundle getAuthToken(final AccountAuthenticatorResponse response,
      final Account account, final String authTokenType, final Bundle options)
          throws NetworkErrorException {
    Logger.debug(LOG_TAG, "getAuthToken");

    Logger.warn(LOG_TAG, "Returning null bundle for getAuthToken.");

    return null;
  }

  @Override
  public String getAuthTokenLabel(String authTokenType) {
    Logger.debug(LOG_TAG, "getAuthTokenLabel");

    return null;
  }

  @Override
  public Bundle hasFeatures(AccountAuthenticatorResponse response,
      Account account, String[] features) throws NetworkErrorException {
    Logger.debug(LOG_TAG, "hasFeatures");

    return null;
  }

  @Override
  public Bundle updateCredentials(AccountAuthenticatorResponse response,
      Account account, String authTokenType, Bundle options)
          throws NetworkErrorException {
    Logger.debug(LOG_TAG, "updateCredentials");

    return null;
  }
}
