/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.picl.account;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.mozilla.gecko.R;
import org.mozilla.gecko.background.common.log.Logger;
import org.mozilla.gecko.db.BrowserContract;
import org.mozilla.gecko.picl.PICLAccountConstants;
import org.mozilla.gecko.picl.account.net.PICLKeyServer0Client;
import org.mozilla.gecko.picl.account.net.PICLKeyServer0Client.KeyResponse;
import org.mozilla.gecko.picl.account.net.PICLKeyServer0Client.PICLKeyServer0ClientDelegate;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


public class PICLAccountActivity extends AccountAuthenticatorActivity implements PICLKeyServer0ClientDelegate {

  private static final String TAG = "PICLAccountAuthenticatorActivity";

  private EditText emailText;
  private Button submitButton;
  
  private PICLKeyServer0Client keyClient = new PICLKeyServer0Client(PICLAccountConstants.KEY_SERVER);
  private boolean isGetting = false;
  private Executor executor = Executors.newSingleThreadExecutor();

  @Override
  public void onCreate(Bundle savedInstanceState) {
    Logger.warn(TAG, "onCreate()");
    super.onCreate(savedInstanceState);
    setContentView(R.layout.picl_login);

    emailText = (EditText) findViewById(R.id.email);
    submitButton = (Button) findViewById(R.id.submit);

    submitButton.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        Logger.warn(TAG, "submitButton.onClick()");

        final String email = emailText.getText().toString();
        if (TextUtils.isEmpty(email)) return;

        if (isGetting) return;
        isGetting = true;

        executor.execute(new Runnable() {

          @Override
          public void run() {
            keyClient.get(email, PICLAccountActivity.this);
          }
          
        });
      }

    });
  }

  @Override
  public void handleKey(final KeyResponse res) {
    runOnUiThread(new Runnable() {
      
      @Override
      public void run() {
        Logger.debug(TAG, "onKey(res)");
        isGetting = false;
    
        Account account = PICLAccountAuthenticator.createAccount(PICLAccountActivity.this, res.email, res.kA, res.deviceId, res.version);
    
        if (account != null) {
          ContentResolver.setSyncAutomatically(account, BrowserContract.TABS_AUTHORITY, true);
    
          Bundle result = new Bundle();
          result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
          result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
          setAccountAuthenticatorResult(result);
        }
    
        finish();
      }
      
    });
  }
  
  @Override
  public void handleError(final Exception e) {
    runOnUiThread(new Runnable() {
      
      @Override
      public void run() {
        isGetting = false;
        Toast.makeText(PICLAccountActivity.this, e.toString(), Toast.LENGTH_LONG).show();
      }
      
    });
  }


}
