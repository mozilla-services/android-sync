/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.fxaccount.activities;

import org.mozilla.gecko.R;
import org.mozilla.gecko.fxaccount.FxAccountConstants;
import org.mozilla.gecko.fxaccount.FxAccountCreationException;
import org.mozilla.gecko.fxaccount.FxAccountIntentService;
import org.mozilla.gecko.sync.Logger;

import android.content.Intent;
import android.view.View;

public class FxAccountSetupExistingAccountActivity extends FxAccountAbstractSetupAccountActivity {
  public static final String LOG_TAG = FxAccountSetupExistingAccountActivity.class.getSimpleName();

  public FxAccountSetupExistingAccountActivity() {
    super(R.layout.fxaccount_setup_existing_account);
  }

  /**
   * Helper to check that email and password are non-null and contain characters.
   *
   * @param email to check.
   * @param password to check.
   * @throws FxAccountCreationException if either email or password is invalid.
   */
  protected void ensureEmailAndPasswordAreValid(String email, String password)
      throws FxAccountCreationException {
    if (email == null || email.trim().length() == 0) {
      throw new FxAccountCreationException("Email address must be specified.");
    }

    if (password == null || password.trim().length() == 0) {
      throw new FxAccountCreationException("Password must be specified.");
    }
  }

  public void onNext(View view) {
    Logger.debug(LOG_TAG, "onNext");

    String email = emailEdit.getText().toString();
    String password = passwordEdit.getText().toString();

    try {
      ensureEmailAndPasswordAreValid(email, password);
    } catch (FxAccountCreationException e) {
      displayException(e);
      return;
    }

    setState(State.WORKING, null);

    Intent serviceIntent = new Intent(this, FxAccountIntentService.class);
    serviceIntent.setAction(FxAccountConstants.FXACCOUNT_CREATE_ANDROID_ACCOUNT_FOR_EXISTING_FX_ACCOUNT_ACTION);

    serviceIntent.putExtra(FxAccountConstants.PARAM_RECEIVER, createResultReceiver());
    serviceIntent.putExtra(FxAccountConstants.PARAM_EMAIL, email);
    serviceIntent.putExtra(FxAccountConstants.PARAM_PASSWORD, password);

    startService(serviceIntent);
  }
}
