/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.fxaccount.activities;

import org.mozilla.gecko.R;
import org.mozilla.gecko.fxaccount.FxAccountAuthenticator;
import org.mozilla.gecko.fxaccount.FxAccountConstants;
import org.mozilla.gecko.fxaccount.FxAccountCreationException;
import org.mozilla.gecko.sync.Logger;

import android.accounts.Account;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class FxAccountSetupExistingAccountActivity extends Activity {
  private static final String LOG_TAG = FxAccountSetupExistingAccountActivity.class.getSimpleName();

  /**
   * {@inheritDoc}
   */
  @Override
  public void onCreate(Bundle icicle) {
    Logger.debug(LOG_TAG, "onCreate(" + icicle + ")");

    super.onCreate(icicle);
    setContentView(R.layout.fxaccount_setup_existing_account);

    Bundle extras = getIntent().getExtras();
    if (extras != null) {
      setDefaultValues(extras);
    }
  }

  public void onNext(View view) {
    Logger.debug(LOG_TAG, "onNext");

    EditText emailEdit = (EditText) findViewById(R.id.email);
    EditText passwordEdit = (EditText) findViewById(R.id.password);

    final String email = emailEdit.getText().toString();
    final String password = passwordEdit.getText().toString();

    try {
      Account account = FxAccountAuthenticator.createAndroidAccountForExistingFxAccount(this, email, password);

      displaySuccess(account);

      Intent result = new Intent();
      result.putExtra(FxAccountConstants.PARAM_ACCOUNT, account);

      setResult(RESULT_OK, result);
      finish();
    } catch (FxAccountCreationException e) {
      displayException(e);
    }
  }

  protected void setDefaultValues(Bundle icicle) {
    if (icicle == null) {
      return;
    }

    // Set default values if they are specified.
    String email = icicle.getString(FxAccountConstants.PARAM_EMAIL);
    if (email != null) {
      EditText emailEdit = (EditText) findViewById(R.id.email);
      if (emailEdit != null) {
        emailEdit.setText(email);
      }
    }

    String password = icicle.getString(FxAccountConstants.PARAM_PASSWORD);
    if (password != null) {
      EditText passwordEdit = (EditText) findViewById(R.id.password);
      if (passwordEdit != null) {
        passwordEdit.setText(password);
      }
    }
  }

  protected void displaySuccess(Account account) {
    Logger.info(LOG_TAG, "Created account with email address " + account.name);
  }

  protected void displayException(FxAccountCreationException e) {
    Logger.warn(LOG_TAG, "Got exception.", e);
  }
}
