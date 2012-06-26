/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.setup;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.setup.activities.SetupSyncActivity;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

/**
 * We generate JSON-encoded auth tokens.
 * <p>
 * The JSON-encoded object always has the key
 * <code>Constants.JSON_KEY_VERSION</code> with an integer version number.
 * <p>
 * Version 1 has the keys:
 * <ul>
 * <li><code>Constants.JSON_KEY_ACCOUNT</code>: the Sync account's hashed
 * username;</li>
 *
 * <li><code>Constants.JSON_KEY_PASSWORD</code>: the Sync account's password;</li>
 *
 * <li><code>Constants.JSON_KEY_SERVER</code>: the Sync account's server;</li>
 *
 * <li><code>Constants.JSON_KEY_SYNCKEY</code>: the Sync account's sync key.</li>
 * </ul>
 */
public class SyncAuthenticatorService extends Service {
  private static final String LOG_TAG = "SyncAuthService";
  private SyncAccountAuthenticator sAccountAuthenticator = null;

  public static final int JSON_VERSION = 1;

  @Override
  public void onCreate() {
    Logger.debug(LOG_TAG, "onCreate");
    sAccountAuthenticator = getAuthenticator();
  }

  @Override
  public IBinder onBind(Intent intent) {
    if (intent.getAction().equals(android.accounts.AccountManager.ACTION_AUTHENTICATOR_INTENT)) {
      return getAuthenticator().getIBinder();
    }
    return null;
  }

  private SyncAccountAuthenticator getAuthenticator() {
    if (sAccountAuthenticator == null) {
      sAccountAuthenticator = new SyncAccountAuthenticator(this);
    }
    return sAccountAuthenticator;
  }

  /**
   * Generate a "plain" auth token.
   * <p>
   * Android caches only the value of the key
   * <code>AccountManager.KEY_AUTHTOKEN</code>, so if a caller needs the other
   * keys in this bundle, it needs to invalidate the token (so that the bundle
   * is re-generated).
   *
   * @param response
   * @param account
   * @param options
   * @return a <code>Bundle</code> instance containing a subset of the keys:
   *         <ul>
   *         <li><code>AccountManager.KEY_ACCOUNT_TYPE</code>: the Android
   *         Account's type</li>
   *
   *         <li><code>AccountManager.KEY_ACCOUNT_NAME</code>: the Android
   *         Account's name</li>
   *
   *         <li><code>AccountManager.KEY_AUTHTOKEN</code>: the Sync account's
   *         password </li>
   *
   *         <li><code> Constants.OPTION_USERNAME</code>: the Sync account's
   *         hashed username</li>
   *
   *         <li><code>Constants.OPTION_SERVER</code>: the Sync account's
   *         server</li>
   *
   *         <li><code> Constants.OPTION_SYNCKEY</code>: the Sync account's
   *         sync key</li>
   *
   *         </ul>
   * @throws NetworkErrorException
   */
  public static Bundle getPlainAuthToken(final Context context, final Account account)
      throws NetworkErrorException {
    // Extract the username and password from the Account Manager, and ask
    // the server for an appropriate AuthToken.
    final AccountManager am = AccountManager.get(context);
    final String password = am.getPassword(account);
    if (password == null) {
      Logger.warn(LOG_TAG, "Returning null bundle for getPlainAuthToken since Account password is null.");
      return null;
    }

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
  }

  /**
   * Generate a "JSON version 1" auth token.
   *
   * @param response
   * @param account
   * @param options
   * @return a <code>Bundle</code> instance containing a subset of the keys:
   *         <ul>
   *         <li><code>AccountManager.KEY_ACCOUNT_TYPE</code>: the Android
   *         Account's type</li>
   *
   *         <li><code>AccountManager.KEY_ACCOUNT_NAME</code>: the Android
   *         Account's name</li>
   *
   *         <li><code>AccountManager.KEY_AUTHTOKEN</code>: a String
   *         JSON-encoding an object with the format given in the class comment.</li>
   *         </ul>
   * @throws NetworkErrorException
   */
  public static Bundle getJSONV1AuthToken(final Context context, final Account account)
      throws NetworkErrorException {
    final AccountManager am = AccountManager.get(context);
    final String password = am.getPassword(account);
    if (password == null) {
      Logger.warn(LOG_TAG, "Returning null bundle for getJSONAuthToken since Account password is null.");
      return null;
    }

    String username = null;
    try {
      username = Utils.usernameFromAccount(account.name); // Username after hashing.
    } catch (Exception e) {
      Logger.warn(LOG_TAG, "Returning null bundle for getJSONAuthToken because of exception.", e);
      return null;
    }

    final String syncKey = am.getUserData(account, Constants.OPTION_SYNCKEY);
    final String serverURL = am.getUserData(account, Constants.OPTION_SERVER);

    ExtendedJSONObject o = new ExtendedJSONObject();
    o.put(Constants.JSON_KEY_VERSION, JSON_VERSION);
    o.put(Constants.JSON_KEY_ACCOUNT, username);
    o.put(Constants.JSON_KEY_PASSWORD, password);
    o.put(Constants.JSON_KEY_SYNCKEY, syncKey);
    o.put(Constants.JSON_KEY_SERVER, serverURL);

    final Bundle result = new Bundle();
    result.putString(AccountManager.KEY_ACCOUNT_TYPE, Constants.ACCOUNTTYPE_SYNC);
    result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name); // Username before hashing.
    result.putString(AccountManager.KEY_AUTHTOKEN, o.toJSONString());
    return result;
  }

  private static class SyncAccountAuthenticator extends AbstractAccountAuthenticator {
    private Context mContext;
    public SyncAccountAuthenticator(Context context) {
      super(context);
      mContext = context;
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response,
        String accountType, String authTokenType, String[] requiredFeatures,
        Bundle options) throws NetworkErrorException {
      Logger.debug(LOG_TAG, "addAccount()");
      final Intent intent = new Intent(mContext, SetupSyncActivity.class);
      intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE,
                      response);
      intent.putExtra("accountType", Constants.ACCOUNTTYPE_SYNC);
      intent.putExtra(Constants.INTENT_EXTRA_IS_SETUP, true);

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
    public Bundle getAuthToken(AccountAuthenticatorResponse response,
        Account account, String authTokenType, Bundle options)
        throws NetworkErrorException {
      Logger.debug(LOG_TAG, "getAuthToken()");

      if (Constants.AUTHTOKEN_TYPE_PLAIN.equals(authTokenType)) {
        return getPlainAuthToken(mContext, account);
      }

      final Bundle result = new Bundle();
      result.putString(AccountManager.KEY_ERROR_MESSAGE, "invalid authTokenType");

      if (authTokenType.startsWith(Constants.AUTHTOKEN_TYPE_JSON_PREFIX)) {
        try {
          String versionString = authTokenType.substring(Constants.AUTHTOKEN_TYPE_JSON_PREFIX.length());
          int version = Integer.parseInt(versionString);
          if (version >= 1) {
            // We only know about JSON version 1.
            Logger.debug(LOG_TAG, "Returning JSON auth token version 1 for requested version " + version + ".");
            return getJSONV1AuthToken(mContext, account);
          }

          result.putString(AccountManager.KEY_ERROR_MESSAGE, "Can't return JSON auth token for requested version " + version + ".");
          // Fall through to return default.
        } catch (NumberFormatException e) {
          return null;
        }
      }

      return result;
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
}
