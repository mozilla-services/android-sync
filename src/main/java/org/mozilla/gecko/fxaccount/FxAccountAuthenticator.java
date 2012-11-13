/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.fxaccount;

import org.mozilla.gecko.browserid.crypto.JWCrypto;
import org.mozilla.gecko.browserid.crypto.RSAJWCrypto;
import org.mozilla.gecko.fxaccount.activities.FxAccountSetupActivity;
import org.mozilla.gecko.sync.ExtendedJSONObject;
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

  public static final String ACCOUNT_TYPE_FXACCOUNT = "org.mozilla.fxaccount";

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
      Bundle options) throws NetworkErrorException {
    Logger.debug(LOG_TAG, "addAccount()");
    final Intent intent = new Intent(context, FxAccountSetupActivity.class);

    intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
    intent.putExtra("accountType", ACCOUNT_TYPE_FXACCOUNT);

    final Bundle result = new Bundle();
    result.putParcelable(AccountManager.KEY_INTENT, intent);

    return result;
  }

  @Override
  public Bundle confirmCredentials(AccountAuthenticatorResponse response,
                                   Account account,
                                   Bundle options) throws NetworkErrorException {
    Logger.debug(LOG_TAG, "confirmCredentials()");
    return null;
  }

  @Override
  public Bundle editProperties(AccountAuthenticatorResponse response,
                               String accountType) {
    Logger.debug(LOG_TAG, "editProperties");
    return null;
  }

  @Override
  public Bundle getAuthToken(final AccountAuthenticatorResponse response,
      final Account account, final String authTokenType, final Bundle options)
      throws NetworkErrorException {

    final String MOCKMYID_SUFFIX = "@mockmyid.com";
    if (!account.name.endsWith(MOCKMYID_SUFFIX)) {
      Logger.warn(LOG_TAG, "Can't get auth token's for non-mockmyid.com accounts yet!");
      return null;
    }
    final String username = account.name.substring(0, account.name.lastIndexOf(MOCKMYID_SUFFIX));

    ExtendedJSONObject publicKey;
    try {
      publicKey = ExtendedJSONObject.parseJSONObject(accountManager.getUserData(account, "publicKey"));
    } catch (Exception e) {
      Logger.warn(LOG_TAG, "Got exception extracting account's public key.", e);
      return null;
    }

    ExtendedJSONObject privateKey;
    try {
      privateKey = ExtendedJSONObject.parseJSONObject(accountManager.getUserData(account, "privateKey"));
    } catch (Exception e) {
      Logger.warn(LOG_TAG, "Got exception extracting account's private key.", e);
      return null;
    }

    String certificate;
    try {
      certificate = JWCrypto.createMockMyIdCertificate(publicKey, username);
    } catch (Exception e) {
      Logger.warn(LOG_TAG, "Got exception creating mockmyid.com certificate.", e);
      return null;
    }

    final String issuer = "127.0.0.1";
    final String audience = authTokenType;
    String assertion;
    try {
      assertion = RSAJWCrypto.assertion(privateKey, certificate, issuer, audience);
    } catch (Exception e) {
      Logger.warn(LOG_TAG, "Got exception creating assertion.", e);
      return null;
    }

    final Bundle result = new Bundle();
    result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
    result.putString(AccountManager.KEY_ACCOUNT_TYPE, FxAccountAuthenticator.ACCOUNT_TYPE_FXACCOUNT);
    result.putString(AccountManager.KEY_AUTHTOKEN, assertion);

    Logger.info(LOG_TAG, "Returning assertion " + assertion + ".");

    return result;
/*    Logger.debug(LOG_TAG, "getAuthToken()");
    if (!authTokenType.equals(Constants.AUTHTOKEN_TYPE_PLAIN)) {
      final Bundle result = new Bundle();
      result.putString(AccountManager.KEY_ERROR_MESSAGE,
          "invalid authTokenType");
      return result;
    }

    // Extract the username and password from the Account Manager, and ask
    // the server for an appropriate AuthToken.
    final AccountManager am = AccountManager.get(mContext);
    final String password = am.getPassword(account);
    if (password != null) {
      final Bundle result = new Bundle();

      // This is a Sync account.
      result.putString(AccountManager.KEY_ACCOUNT_TYPE, Constants.ACCOUNTTYPE_SYNC);

      // Server.
      String serverURL = am.getUserData(account, Constants.OPTION_SERVER);
      result.putString(Constants.OPTION_SERVER, serverURL);

      // Full username, before hashing.
      result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);

      // Username after hashing.
      try {
        String username = Utils.usernameFromAccount(account.name);
        Logger.pii(LOG_TAG, "Account " + account.name + " hashes to " + username + ".");
        Logger.debug(LOG_TAG, "Setting username. Null? " + (username == null));
        result.putString(Constants.OPTION_USERNAME, username);
      } catch (NoSuchAlgorithmException e) {
        // Do nothing. Calling code must check for missing value.
      } catch (UnsupportedEncodingException e) {
        // Do nothing. Calling code must check for missing value.
      }

      // Sync key.
      final String syncKey = am.getUserData(account, Constants.OPTION_SYNCKEY);
      Logger.debug(LOG_TAG, "Setting sync key. Null? " + (syncKey == null));
      result.putString(Constants.OPTION_SYNCKEY, syncKey);

      // Password.
      result.putString(AccountManager.KEY_AUTHTOKEN, password);
      return result;
    }*/
//    Logger.warn(LOG_TAG, "Returning null bundle for getAuthToken.");
//    return null;
  }

  @Override
  public String getAuthTokenLabel(String authTokenType) {
    Logger.debug(LOG_TAG, "getAuthTokenLabel()");
    return null;
  }

  @Override
  public Bundle hasFeatures(AccountAuthenticatorResponse response,
      Account account, String[] features) throws NetworkErrorException {
    Logger.debug(LOG_TAG, "hasFeatures()");
    return null;
  }

  @Override
  public Bundle updateCredentials(AccountAuthenticatorResponse response,
      Account account, String authTokenType, Bundle options)
      throws NetworkErrorException {
    Logger.debug(LOG_TAG, "updateCredentials()");
    return null;
  }
}
