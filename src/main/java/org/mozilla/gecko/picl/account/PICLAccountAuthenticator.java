/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.picl.account;

import org.mozilla.gecko.R;
import org.mozilla.gecko.background.common.log.Logger;
import org.mozilla.gecko.picl.PICLAccountConstants;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class PICLAccountAuthenticator extends AbstractAccountAuthenticator {
  public static final String LOG_TAG = PICLAccountAuthenticator.class.getSimpleName();

  protected final Context context;
  protected final AccountManager accountManager;

  public PICLAccountAuthenticator(Context context) {
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

    Bundle reply = new Bundle();

    Intent i = new Intent(this.context, PICLAccountActivity.class);
    i.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
    reply.putParcelable(AccountManager.KEY_INTENT, i);

    return reply;
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


    Bundle result = new Bundle();

    if (PICLAccountConstants.AUTH_TOKEN_TYPE.equals(authTokenType)) {
      String kA = accountManager.getUserData(account, "kA");

      result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
      result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
      result.putString(AccountManager.KEY_AUTHTOKEN, kA);

      Logger.warn(LOG_TAG, "Return authToken for type: " + authTokenType);
    } else {
      Logger.warn(LOG_TAG, "Unrecognized authTokenType: " + authTokenType);
      result.putInt(AccountManager.KEY_ERROR_CODE, 400);
      result.putString(AccountManager.KEY_ERROR_MESSAGE, "Unrecognized authTokenType: " + authTokenType);
    }


    return result;
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

  public static Account createAccount(Context context, String email, String password, String kA, String deviceId, String version) {
    String accountType = context.getString(R.string.picl_account_type);
    Account account = new Account(email, accountType);
    Bundle options = new Bundle();

    options.putString("kA", kA);
    options.putString("deviceId", deviceId);
    options.putString("version", version);

    AccountManager am = AccountManager.get(context);
    am.addAccountExplicitly(account, password, options); //TODO: oh gawd! the password is stored in plain text!

    return account;
  }
}
