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

package org.mozilla.gecko.sync.setup.activities;

import org.mozilla.gecko.R;
import org.mozilla.gecko.sync.repositories.android.Authorities;
import org.mozilla.gecko.sync.setup.Constants;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Browser;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

public class AccountActivity extends AccountAuthenticatorActivity {
  private final static String LOG_TAG = "AccountActivity";
  private AccountManager mAccountManager;
  private Context mContext;
  private String username;
  private String password;
  private String key;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.sync_account);
    mContext = getApplicationContext();
    mAccountManager = AccountManager.get(getApplicationContext());
  }

  @Override
  public void onStart() {
    super.onStart();
    // Start with an empty form
    setContentView(R.layout.sync_account);
  }

  public void cancelClickHandler(View target) {
    moveTaskToBack(true);
  }

  /*
   * Get credentials on "Connect" and write to AccountManager, where it can be
   * accessed by Fennec and Sync Service.
   */
  public void connectClickHandler(View target) {
    Log.d(LOG_TAG, "connectClickHandler for view " + target);
    username = ((EditText) findViewById(R.id.username)).getText().toString();
    password = ((EditText) findViewById(R.id.password)).getText().toString();
    key = ((EditText) findViewById(R.id.key)).getText().toString();

    // TODO : Authenticate with Sync Service, once implemented, with
    // onAuthSuccess as callback

    authCallback();
  }

  /*
   * Callback that handles auth based on success/failure
   */
  private void authCallback() {
    // Create and add account to AccountManager
    // TODO: only allow one account to be added?
    final Account account = new Account(username, Constants.ACCOUNTTYPE_SYNC);
    final Bundle userbundle = new Bundle();
    // Add sync key
    userbundle.putString(Constants.OPTION_SYNCKEY, key);
    mAccountManager.addAccountExplicitly(account, password, userbundle);

    Log.d(LOG_TAG, "account: " + account.toString());
    // Set components to sync (default: all).
    ContentResolver.setSyncAutomatically(account, Authorities.BROWSER_AUTHORITY, true);
    // TODO: add other ContentProviders as needed (e.g. passwords)
    // TODO: for each, also add to res/xml to make visible in account settings
    ContentResolver.setMasterSyncAutomatically(true);
    Log.d(LOG_TAG, "finished setting syncables");

    final Intent intent = new Intent();
    intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, username);
    intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, Constants.ACCOUNTTYPE_SYNC);
    intent.putExtra(AccountManager.KEY_AUTHTOKEN, Constants.ACCOUNTTYPE_SYNC);
    setAccountAuthenticatorResult(intent.getExtras());

    // Testing out the authFailure case
    //authFailure();

    // TODO: Currently, we do not actually authenticate username/pass against Moz sync server.

    // Successful authentication result
    setResult(RESULT_OK, intent);
    authSuccess();
  }

  private void authFailure() {
    Intent intent = new Intent(mContext, SetupFailureActivity.class);
    startActivity(intent);
  }

  private void authSuccess() {
    Intent intent = new Intent(mContext, SetupSuccessActivity.class);
    startActivity(intent);
    finish();
  }
}
