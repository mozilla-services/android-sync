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
 *   Chenxia Liu <liuche@mozilla.com>
 *   Richard Newman <rnewman@mozilla.com>
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
import org.mozilla.gecko.sync.setup.AccountCreator;
import org.mozilla.gecko.sync.setup.Constants;
import org.mozilla.gecko.sync.setup.auth.AccountAuthenticator;

import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;

public class AccountActivity extends AccountAuthenticatorActivity {
  private final static String LOG_TAG        = "AccountActivity";

  private AccountManager      mAccountManager;
  private Context             mContext;
  private String              username;
  private String              password;
  private String              key;
  private String              server = Constants.AUTH_NODE_DEFAULT;

  // UI elements.
  private EditText            serverInput;
  private EditText            usernameInput;
  private EditText            passwordInput;
  private EditText            synckeyInput;
  private CheckBox            serverCheckbox;
  private Button              connectButton;
  private Button              cancelButton;

  private AccountAuthenticator accountAuthenticator;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    setTheme(R.style.SyncTheme);
    super.onCreate(savedInstanceState);
    setContentView(R.layout.sync_account);
    mContext = getApplicationContext();
    Log.d(LOG_TAG, "AccountManager.get(" + mContext + ")");
    mAccountManager = AccountManager.get(mContext);

    // Find UI elements.
    usernameInput = (EditText) findViewById(R.id.usernameInput);
    passwordInput = (EditText) findViewById(R.id.passwordInput);
    synckeyInput = (EditText) findViewById(R.id.keyInput);
    serverInput = (EditText) findViewById(R.id.serverInput);

    TextWatcher inputValidator = makeInputValidator();

    usernameInput.addTextChangedListener(inputValidator);
    passwordInput.addTextChangedListener(inputValidator);
    synckeyInput.addTextChangedListener(inputValidator);
    serverInput.addTextChangedListener(inputValidator);

    connectButton = (Button) findViewById(R.id.accountConnectButton);
    cancelButton = (Button) findViewById(R.id.accountCancelButton);
    serverCheckbox = (CheckBox) findViewById(R.id.checkbox_server);

    serverCheckbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Log.i(LOG_TAG, "Toggling checkbox: " + isChecked);
        // Hack for pre-3.0 Android: can enter text into disabled EditText.
        if (!isChecked) { // Clear server input.
          serverInput.setVisibility(View.GONE);
          serverInput.setText("");
        } else {
          serverInput.setVisibility(View.VISIBLE);
        }
        // Activate connectButton if necessary.
        activateView(connectButton, validateInputs());
      }
    });
  }

  @Override
  public void onResume() {
    super.onResume();
    clearCredentials();
    usernameInput.requestFocus();
    enableCredEntry(true);
    cancelButton.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {
        cancelClickHandler(v);
      }

    });
  }
  public void cancelClickHandler(View target) {
    finish();
  }

  public void cancelConnectHandler(View target) {
    if (accountAuthenticator != null) {
      accountAuthenticator.isCanceled = true;
      accountAuthenticator = null;
    }
    enableCredEntry(true);
    activateView(connectButton, true);
    clearCredentials();
    usernameInput.requestFocus();
  }

  private void clearCredentials() {
    // Start with an empty form
    usernameInput.setText("");
    passwordInput.setText("");
    passwordInput.setText("");
    // Don't clear sync key until exiting.
  }

  /*
   * Get credentials on "Connect" and write to AccountManager, where it can be
   * accessed by Fennec and Sync Service.
   */
  public void connectClickHandler(View target) {
    Log.d(LOG_TAG, "connectClickHandler for view " + target);
    username = usernameInput.getText().toString();
    password = passwordInput.getText().toString();
    key = synckeyInput.getText().toString();
    if (serverCheckbox.isChecked()) {
      server = serverInput.getText().toString();
    }
    enableCredEntry(false);
    activateView(connectButton, false);
    cancelButton.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {
        cancelConnectHandler(v);
      }
    });

    accountAuthenticator = new AccountAuthenticator(this);
    accountAuthenticator.authenticate(server, username, password);
  }

  /* Helper UI functions */
  private void enableCredEntry(boolean toEnable) {
    usernameInput.setEnabled(toEnable);
    passwordInput.setEnabled(toEnable);
    synckeyInput.setEnabled(toEnable);
    serverCheckbox.setEnabled(toEnable);
    if (!toEnable) {
      serverInput.setEnabled(toEnable);
    } else {
      serverInput.setEnabled(serverCheckbox.isChecked());
    }
  }

  private TextWatcher makeInputValidator() {
    return new TextWatcher() {

      @Override
      public void afterTextChanged(Editable s) {
        activateView(connectButton, validateInputs());
      }

      @Override
      public void beforeTextChanged(CharSequence s, int start, int count,
          int after) {
      }

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
      }
    };
  }

  private boolean validateInputs() {
    if (usernameInput.length() == 0 || passwordInput.length() == 0
        || synckeyInput.length() == 0
        || (serverCheckbox.isChecked() && serverInput.length() == 0)) {
      return false;
    }
    return true;
  }

  /*
   * Callback that handles auth based on success/failure
   */
  public void authCallback(boolean isSuccess) {
    if (!isSuccess) {
      Log.d(LOG_TAG, "not successful");
      runOnUiThread(new Runnable() {
        @Override
        public void run() {
          authFailure();
        }
      });
      return;
    }

    // Successful authentication. Create and add account to AccountManager.
    // Note: Sync key may incorrect!
    Log.d(LOG_TAG, "Using account manager " + mAccountManager);
    final Intent intent = AccountCreator.createAccount(mContext, mAccountManager,
                                        username,
                                        key, password, server);
    setAccountAuthenticatorResult(intent.getExtras());
    setResult(RESULT_OK, intent);

    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        authSuccess();
      }
    });
  }

  /**
   * Feedback to user of account setup failure.
   */
  public void authFailure() {
    Intent intent = new Intent(mContext, SetupFailureActivity.class);
    intent.setFlags(Constants.FLAG_ACTIVITY_REORDER_TO_FRONT_NO_ANIMATION);
    startActivity(intent);
  }

  /**
   * Feedback to user of account setup success.
   */
  public void authSuccess() {
    // Display feedback of successful account setup.
    Intent intent = new Intent(mContext, SetupSuccessActivity.class);
    intent.setFlags(Constants.FLAG_ACTIVITY_REORDER_TO_FRONT_NO_ANIMATION);
    startActivity(intent);
    finish();
  }

  private void activateView(View view, boolean toActivate) {
    view.setEnabled(toActivate);
    view.setClickable(toActivate);
  }
}
