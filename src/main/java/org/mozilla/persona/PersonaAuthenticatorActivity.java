/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.persona;

import java.security.NoSuchAlgorithmException;

import org.mozilla.gecko.R;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.NonObjectJSONException;
import org.mozilla.persona.crypto.RSAJWCrypto;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

/**
 * Activity which displays login screen to the user.
 */
public class PersonaAuthenticatorActivity extends AccountAuthenticatorActivity {
  private static final String LOG_TAG = "PersonaAuthenticatorActivity";

  public static final String PARAM_CONFIRMCREDENTIALS = "confirmCredentials";
  public static final String PARAM_PASSWORD = "password";
  public static final String PARAM_USERNAME = "username";
  public static final String PARAM_AUTHTOKEN_TYPE = "authtokenType";

  /** Was the original caller asking for an entirely new account? */
  protected boolean mRequestNewAccount = false;
  //
  //    private String mUsername;
  //    private EditText mUsernameEdit;

  /**
   * {@inheritDoc}
   */
  @Override
  public void onCreate(Bundle icicle) {
    Log.i(LOG_TAG, "onCreate(" + icicle + ")");
    super.onCreate(icicle);
    setContentView(R.layout.persona_authenticator_activity);

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

  public void handleNext(View view) {
    final String email = ((EditText) findViewById(R.id.email)).getText().toString();

    final Account account = new Account(email, PersonaAccountAuthenticator.ACCOUNT_TYPE_PERSONA);
    final AccountManager accountManager = AccountManager.get(this);

    // Keys!
    final Bundle userBundle = new Bundle();
    try {
      final ExtendedJSONObject pair = RSAJWCrypto.generateKeypair(2048);
      final ExtendedJSONObject publicKeyToSign = pair.getObject("publicKey");
      final ExtendedJSONObject privateKeyToSignWith = pair.getObject("privateKey");

      userBundle.putString("publicKey", publicKeyToSign.toJSONString());
      userBundle.putString("privateKey", privateKeyToSignWith.toJSONString());
    } catch (NoSuchAlgorithmException e) {
      Logger.warn(LOG_TAG, "Got exception generating keys.", e);
    } catch (NonObjectJSONException e) {
      Logger.warn(LOG_TAG, "Got exception generating keys.", e);
    }

    Logger.info(LOG_TAG, "Userbundle " + userBundle);
    accountManager.addAccountExplicitly(account, null, userBundle);

    final Intent intent = new Intent();
    // mAuthtoken = "XXX";
    intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, email);
    intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, PersonaAccountAuthenticator.ACCOUNT_TYPE_PERSONA);
    //  if (mAuthtokenType != null
    //      && mAuthtokenType.equals(Constants.AUTHTOKEN_TYPE)) {
    //      intent.putExtra(AccountManager.KEY_AUTHTOKEN, mAuthtoken);
    //  }
    setAccountAuthenticatorResult(intent.getExtras());
    setResult(RESULT_OK, intent);
    finish();
    /*
      final Intent intent = new Intent();
      intent.putExtra(AccountManager.KEY_BOOLEAN_RESULT, result);
      setAccountAuthenticatorResult(intent.getExtras());
      setResult(RESULT_OK, intent);
      finish();
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
}
