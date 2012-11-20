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
import android.content.Intent;
import android.view.View;

public class FxAccountSetupNewAccountActivity extends FxAccountAbstractSetupAccountActivity {
  private static final String LOG_TAG = FxAccountSetupNewAccountActivity.class.getSimpleName();

  public FxAccountSetupNewAccountActivity() {
    super(R.layout.fxaccount_setup_new_account);
  }

  public void onNext(View view) {
    Logger.debug(LOG_TAG, "onNext");

    final String email = emailEdit.getText().toString();
    final String password = passwordEdit.getText().toString();
    String password2 = password2Edit.getText().toString();

    if (email == null || password == null || !password.equals(password2)) {
      Logger.warn(LOG_TAG, "No email, no password, or non-matching passwords.");
      return;
    }

    try {
      Account account = FxAccountAuthenticator.createAndroidAccountForNewFxAccount(this, email, password);

      displaySuccess(account);

      Intent result = new Intent();
      result.putExtra(FxAccountConstants.PARAM_ACCOUNT, account);

      setResult(RESULT_OK, result);
      finish();
    } catch (FxAccountCreationException e) {
      displayException(e);
    }
  }
}
