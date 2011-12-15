/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Android Sync Client.
 *
 * The Initial Developer of the Original Code is
 * the Mozilla Foundation.
 * Portions created by the Initial Developer are Copyright (C) 2011
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *  Chenxia Liu <liuche@mozilla.com>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

package org.mozilla.gecko.sync.setup;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import org.mozilla.android.sync.crypto.KeyBundle;
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
import android.util.Log;

public class SyncAuthenticatorService extends Service {
  private static final String LOG_TAG = "SyncAuthenticatorService";
  private SyncAccountAuthenticator sAccountAuthenticator = null;

  @Override
  public void onCreate() {
    Log.d(LOG_TAG, "onCreate");
    sAccountAuthenticator = getAuthenticator();
  }

  @Override
  public IBinder onBind(Intent intent) {
    IBinder ret = null;
    if (intent.getAction().equals(
        android.accounts.AccountManager.ACTION_AUTHENTICATOR_INTENT))
      ret = getAuthenticator().getIBinder();
    return ret;
  }

  private SyncAccountAuthenticator getAuthenticator() {
    if (sAccountAuthenticator == null) {
      sAccountAuthenticator = new SyncAccountAuthenticator(this);
    }
    return sAccountAuthenticator;
  }

  private static class SyncAccountAuthenticator extends
      AbstractAccountAuthenticator {
    private Context mContext;
    public SyncAccountAuthenticator(Context context) {
      super(context);
      mContext = context;
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response,
        String accountType, String authTokenType, String[] requiredFeatures,
        Bundle options) throws NetworkErrorException {
      Log.d(LOG_TAG, "addAccount()");
      final Intent intent = new Intent(mContext, SetupSyncActivity.class);
      intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE,
          response);
      intent.putExtra("accountType", Constants.ACCOUNTTYPE_SYNC);

      final Bundle result = new Bundle();
      result.putParcelable(AccountManager.KEY_INTENT, intent);

      return result;
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response,
        Account account, Bundle options) throws NetworkErrorException {
      Log.d(LOG_TAG, "confirmCredentials()");
      return null;
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response,
        String accountType) {
      Log.d(LOG_TAG, "editProperties");
      return null;
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response,
        Account account, String authTokenType, Bundle options)
        throws NetworkErrorException {
      Log.d(LOG_TAG, "getAuthToken()");
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

        // Full username, before hashing.
        result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);

        // Username after hashing.
        try {
          String username = KeyBundle.usernameFromAccount(account.name);
          Log.i("rnewman", "Account " + account.name + " hashes to " + username);
          result.putString(Constants.OPTION_USERNAME, username);
        } catch (NoSuchAlgorithmException e) {
          // Do nothing. Calling code must check for missing value.
        } catch (UnsupportedEncodingException e) {
          // Do nothing. Calling code must check for missing value.
        }

        // Sync key.
        final String syncKey = am.getUserData(account, Constants.OPTION_SYNCKEY);
        Log.i("rnewman", "Setting Sync Key to " + syncKey);
        result.putString(Constants.OPTION_SYNCKEY, syncKey);

        // Password.
        result.putString(AccountManager.KEY_AUTHTOKEN, password);
        return result;
      }
      return null;
    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {
      Log.d(LOG_TAG, "getAuthTokenLabel()");
      return null;
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response,
        Account account, String[] features) throws NetworkErrorException {
      Log.d(LOG_TAG, "hasFeatures()");
      return null;
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response,
        Account account, String authTokenType, Bundle options)
        throws NetworkErrorException {
      Log.d(LOG_TAG, "updateCredentials()");
      return null;
    }
  }
}
