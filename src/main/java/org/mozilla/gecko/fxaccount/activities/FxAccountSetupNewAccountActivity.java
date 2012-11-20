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

    String email = emailEdit.getText().toString();
    String password = passwordEdit.getText().toString();
    String password2 = password2Edit.getText().toString();

    try {
      ensureEmailAndPasswordsAreValid(email, password, password2);

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

  /**
   * Helper to check that email and password are non-null and contain characters.
   *
   * @param email to check.
   * @param password to check.
   * @throws FxAccountCreationException if either email or password is invalid.
   */
  protected void ensureEmailAndPasswordsAreValid(String email, String password, String password2)
      throws FxAccountCreationException {
    if (email == null || email.trim().length() == 0) {
      throw new FxAccountCreationException("Email address must be specified.");
    }

    if (password == null || password.trim().length() == 0 ||
        password2 == null || password2.trim().length() == 0) {
      throw new FxAccountCreationException("Passwords must be specified.");
    }

    if (!password.equals(password2)) {
      throw new FxAccountCreationException("Passwords must match.");
    }
  }
}
