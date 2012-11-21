/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.fxaccount;

import org.mozilla.gecko.fxaccount.activities.FxAccountSetupActivity;
import org.mozilla.gecko.sync.Logger;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
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

    final Intent intent = new Intent(context, FxAccountSetupActivity.class);

    intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
    intent.putExtra("accountType", FxAccountConstants.ACCOUNT_TYPE_FXACCOUNT);

    final Bundle result = new Bundle();
    result.putParcelable(AccountManager.KEY_INTENT, intent);

    return result;
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

  public static Account createAndroidAccountForExistingFxAccount(Context context, String email, String password)
      throws FxAccountCreationException {
    Logger.debug(LOG_TAG, "createAndroidAccountForExistingFxAccount");

    final Account account = new Account(email, FxAccountConstants.ACCOUNT_TYPE_FXACCOUNT);
    final AccountManager accountManager = AccountManager.get(context);

    accountManager.addAccountExplicitly(account, password, null);

    return account;
  }

  public static Account createAndroidAccountForNewFxAccount(Context context, final String email, final String password)
      throws FxAccountCreationException {
    Logger.debug(LOG_TAG, "createAndroidAccountForNewFxAccount");

    try {
      Thread.sleep(2000);
    } catch (InterruptedException e) {
      // Do nothing.
    }

    throw new FxAccountCreationException("Not yet implemented.");
  }
}
