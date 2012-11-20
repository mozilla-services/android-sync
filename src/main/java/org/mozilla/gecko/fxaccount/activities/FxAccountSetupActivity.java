/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.fxaccount.activities;

import org.mozilla.gecko.R;
import org.mozilla.gecko.fxaccount.FxAccountConstants;
import org.mozilla.gecko.sync.Logger;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

/**
 * Activity which displays login screen to the user.
 */
public class FxAccountSetupActivity extends AccountAuthenticatorActivity {
  private static final String LOG_TAG = FxAccountSetupActivity.class.getSimpleName();

  public static final int NEW_ACCOUNT_REQUEST_CODE = 1;
  public static final int EXISTING_ACCOUNT_REQUEST_CODE = 2;

  /**
   * {@inheritDoc}
   */
  @Override
  public void onCreate(Bundle icicle) {
    Logger.debug(LOG_TAG, "onCreate(" + icicle + ")");

    super.onCreate(icicle);
    setContentView(R.layout.fxaccount_setup);
  }

  public void onCreateAccount(View view) {
    Logger.debug(LOG_TAG, "onCreateAccount: Asking for username/password for new account.");

    Intent intent = new Intent(this, FxAccountSetupNewAccountActivity.class);

    intent.putExtra(FxAccountConstants.PARAM_EMAIL, "test@mockmyid.com");
    intent.putExtra(FxAccountConstants.PARAM_PASSWORD, "test");

    startActivityForResult(intent, NEW_ACCOUNT_REQUEST_CODE);
  }

  public void onLogIn(View view) {
    Logger.debug(LOG_TAG, "onLogIn: Asking for username/password for existing account.");

    Intent intent = new Intent(this, FxAccountSetupExistingAccountActivity.class);

    intent.putExtra(FxAccountConstants.PARAM_EMAIL, "foo@mockmyid.com");
    intent.putExtra(FxAccountConstants.PARAM_PASSWORD, "foo");

    startActivityForResult(intent, EXISTING_ACCOUNT_REQUEST_CODE);
  }

  @Override
  public void onActivityResult(final int requestCode, int resultCode, Intent data) {
    Logger.debug(LOG_TAG, "Activity completed.");

    if (resultCode == RESULT_CANCELED) {
      Logger.debug(LOG_TAG, "Activity canceled.");
      return;
    }

    Account account = (Account) data.getParcelableExtra("account");
    if (account == null) {
      Logger.warn(LOG_TAG, "Activity returned null account.");
      return;
    }

    final Intent intent = new Intent();
    intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, account.name);
    intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, account.type);

    setAccountAuthenticatorResult(intent.getExtras());
    setResult(RESULT_OK, intent);

    finish();
  }
}
