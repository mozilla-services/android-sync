/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.fxaccount.activities;

import java.net.URI;

import org.mozilla.gecko.R;
import org.mozilla.gecko.sync.Logger;

import android.accounts.AccountAuthenticatorActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

/**
 * Activity which displays login screen to the user.
 */
public class FxAccountSetupActivity extends AccountAuthenticatorActivity {
  private static final String LOG_TAG = FxAccountSetupActivity.class.getSimpleName();

  public static final URI AVATAR_SERVICE_URI = URI.create("https://stage-token.services.mozilla.com");

  public static final int NEW_ACCOUNT_REQUEST_CODE = 1;
  public static final int EXISTING_ACCOUNT_REQUEST_CODE = 2;

  public static final String PARAM_EMAIL = "email";
  public static final String PARAM_PASSWORD = "password";
  public static final String PARAM_RESULT = "result";

  /**
   * {@inheritDoc}
   */
  @Override
  public void onCreate(Bundle icicle) {
    Log.i(LOG_TAG, "onCreate(" + icicle + ")");
    super.onCreate(icicle);
    setContentView(R.layout.fxa_setup);
  }

  public void onCreateAccount(View view) {
    Logger.debug(LOG_TAG, "Asking for certificate from new account.");

    Intent intent = new Intent(this, FxAccountSetupNewAccountActivity.class);
    startActivityForResult(intent, NEW_ACCOUNT_REQUEST_CODE);
  }

  public void onLogIn(View view) {
    Logger.debug(LOG_TAG, "Asking for certificate for existing account.");

    Intent intent = new Intent(this, FxAccountSetupExistingAccountActivity.class);
    startActivityForResult(intent, EXISTING_ACCOUNT_REQUEST_CODE);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (resultCode == RESULT_CANCELED) {
      Logger.debug(LOG_TAG, "Activity canceled.");
      return;
    }

    String email = data.getStringExtra(PARAM_EMAIL);
    String password = data.getStringExtra(PARAM_PASSWORD);
    String result = data.getStringExtra(PARAM_RESULT);

    Logger.debug(LOG_TAG, "Got certificate from activity!");
    Logger.debug(LOG_TAG, result);

    Logger.debug(LOG_TAG, "Asking for update for account.");
    Intent intent = new Intent(this, FxAccountAvatarUpdateActivity.class);
    intent.putExtra(PARAM_RESULT, result);
    startActivity(intent);

/*
    if (email == null || password == null) {
      return;
    }

    final Account account = new Account(email, FxAccountAuthenticator.ACCOUNT_TYPE_FXACCOUNT);
    final AccountManager accountManager = AccountManager.get(this);

    accountManager.addAccountExplicitly(account, password, null);

    final Intent intent = new Intent();
    intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, email);
    intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, FxAccountAuthenticator.ACCOUNT_TYPE_FXACCOUNT);

    setAccountAuthenticatorResult(intent.getExtras());
    setResult(RESULT_OK, intent);

    finish();
*/
  }

  //  Logger.info(LOG_TAG, "Userbundle " + userBundle);
  //  accountManager.addAccountExplicitly(account, null, userBundle);
  //
  //  final Intent intent = new Intent();
  //  // mAuthtoken = "XXX";
  //  intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, email);
  //  intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, FxAccountAuthenticator.ACCOUNT_TYPE_FXACCOUNT);
  //  //  if (mAuthtokenType != null
  //  //      && mAuthtokenType.equals(Constants.AUTHTOKEN_TYPE)) {
  //  //      intent.putExtra(AccountManager.KEY_AUTHTOKEN, mAuthtoken);
  //  //  }
  //  setAccountAuthenticatorResult(intent.getExtras());
  //  setResult(RESULT_OK, intent);
  //  finish();
  //  /*
  //    final Intent intent = new Intent();
  //    intent.putExtra(AccountManager.KEY_BOOLEAN_RESULT, result);
  //    setAccountAuthenticatorResult(intent.getExtras());
  //    setResult(RESULT_OK, intent);
  //    finish();
  //   */

  /*
        mAccountManager = AccountManager.get(this);
        Log.i(TAG, "loading data from Intent");
        final Intent intent = getIntent();
        mUsername = intent.getStringExtra(PARAM_USERNAME);
        mAuthtokenType = intent.getStringExtra(PARAM_AUTHTOKEN_TYPE);
        mRequestNewAccount = mUsername == null;
        mConfirmCredentials =
            intent.getBooleanExtra(PARAM_CONFIRMCREDENTIALS, false);
   */
  //        Log.i(TAG, "    request new: " + mRequestNewAccount);
  //        requestWindowFeature(Window.FEATURE_LEFT_ICON);
  //        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON,
  //            android.R.drawable.ic_dialog_alert);
  /*
        mMessage = (TextView) findViewById(R.id.message);
        mUsernameEdit = (EditText) findViewById(R.id.username_edit);
        mPasswordEdit = (EditText) findViewById(R.id.password_edit);

        mUsernameEdit.setText(mUsername);
        mMessage.setText(getMessage());
   */
}


//
// /**
//  * Called when response is received from the server for confirm credentials
//  * request. See onAuthenticationResult(). Sets the
//  * AccountAuthenticatorResult which is sent back to the caller.
//  *
//  * @param the confirmCredentials result.
//  */
// protected void finishConfirmCredentials(boolean result) {
//     Log.i(TAG, "finishConfirmCredentials()");
//     final Account account = new Account(mUsername, Constants.ACCOUNT_TYPE);
//     mAccountManager.setPassword(account, mPassword);
// }
//    /*
//     * {@inheritDoc}
//     */
//    @Override
//    protected Dialog onCreateDialog(int id) {
//        final ProgressDialog dialog = new ProgressDialog(this);
//        dialog.setMessage(getText(R.string.ui_activity_authenticating));
//        dialog.setIndeterminate(true);
//        dialog.setCancelable(true);
//        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
//            public void onCancel(DialogInterface dialog) {
//                Log.i(TAG, "dialog cancel has been invoked");
//                if (mAuthThread != null) {
//                    mAuthThread.interrupt();
//                    finish();
//                }
//            }
//        });
//        return dialog;
//    }
//

//
//    /**
//     *
//     * Called when response is received from the server for authentication
//     * request. See onAuthenticationResult(). Sets the
//     * AccountAuthenticatorResult which is sent back to the caller. Also sets
//     * the authToken in AccountManager for this account.
//     *
//     * @param the confirmCredentials result.
//     */
//
//    protected void finishLogin() {
//        Log.i(TAG, "finishLogin()");
//        final Account account = new Account(mUsername, Constants.ACCOUNT_TYPE);
//
//        if (mRequestNewAccount) {
//            mAccountManager.addAccountExplicitly(account, mPassword, null);
//            // Set contacts sync for this account.
//            ContentResolver.setSyncAutomatically(account,
//                ContactsContract.AUTHORITY, true);
//        } else {
//            mAccountManager.setPassword(account, mPassword);
//        }
//        final Intent intent = new Intent();
//        mAuthtoken = mPassword;
//        intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, mUsername);
//        intent
//            .putExtra(AccountManager.KEY_ACCOUNT_TYPE, Constants.ACCOUNT_TYPE);
//        if (mAuthtokenType != null
//            && mAuthtokenType.equals(Constants.AUTHTOKEN_TYPE)) {
//            intent.putExtra(AccountManager.KEY_AUTHTOKEN, mAuthtoken);
//        }
//        setAccountAuthenticatorResult(intent.getExtras());
//        setResult(RESULT_OK, intent);
//        finish();
//    }
//
//    /**
//     * Hides the progress UI for a lengthy operation.
//     */
//    protected void hideProgress() {
//        dismissDialog(0);
//    }
//
//    /**
//     * Called when the authentication process completes (see attemptLogin()).
//     */
//    public void onAuthenticationResult(boolean result) {
//        Log.i(TAG, "onAuthenticationResult(" + result + ")");
//        // Hide the progress dialog
//        hideProgress();
//        if (result) {
//            if (!mConfirmCredentials) {
//                finishLogin();
//            } else {
//                finishConfirmCredentials(true);
//            }
//        } else {
//            Log.e(TAG, "onAuthenticationResult: failed to authenticate");
//            if (mRequestNewAccount) {
//                // "Please enter a valid username/password.
//                mMessage
//                    .setText(getText(R.string.login_activity_loginfail_text_both));
//            } else {
//                // "Please enter a valid password." (Used when the
//                // account is already in the database but the password
//                // doesn't work.)
//                mMessage
//                    .setText(getText(R.string.login_activity_loginfail_text_pwonly));
//            }
//        }
//    }

//    /**
//     * Returns the message to be displayed at the top of the login dialog box.
//     */
//    private CharSequence getMessage() {
//        getString(R.string.persona_account_label);
//        if (TextUtils.isEmpty(mUsername)) {
//            // If no username, then we ask the user to log in using an
//            // appropriate service.
//            final CharSequence msg =
//                getText(R.string.login_activity_newaccount_text);
//            return msg;
//        }
//        if (TextUtils.isEmpty(mPassword)) {
//            // We have an account but no password
//            return getText(R.string.login_activity_loginfail_text_pwmissing);
//        }
//        return null;
//    }

//    /**
//     * Shows the progress UI for a lengthy operation.
//     */
//    protected void showProgress() {
//        showDialog(0);
//    }
