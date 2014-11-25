/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.fxa.activities;

import org.mozilla.gecko.R;
import org.mozilla.gecko.background.common.log.Logger;
import org.mozilla.gecko.fxa.login.State;
import org.mozilla.gecko.fxa.login.State.StateLabel;

/**
 * Activity which displays a screen for inputting the password and finishing
 * migrating to Firefox Accounts / Sync 1.5.
 */
public class FxAccountFinishMigratingActivity extends FxAccountAbstractUpdateCredentialsActivity {
  protected static final String LOG_TAG = FxAccountFinishMigratingActivity.class.getSimpleName();

  public FxAccountFinishMigratingActivity() {
    super(R.layout.fxaccount_finish_migrating);
  }

  @Override
  public void onResume() {
    super.onResume();
    this.fxAccount = getAndroidFxAccount();
    if (fxAccount == null) {
      Logger.warn(LOG_TAG, "Could not get Firefox Account.");
      setResult(RESULT_CANCELED);
      finish();
      return;
    }
    final State state = fxAccount.getState();
    if (state.getStateLabel() != StateLabel.MigratedFromSync11) {
      Logger.warn(LOG_TAG, "Cannot finish migrating from Firefox Account in state: " + state.getStateLabel());
      setResult(RESULT_CANCELED);
      finish();
      return;
    }
    emailEdit.setText(fxAccount.getEmail());
  }
}
