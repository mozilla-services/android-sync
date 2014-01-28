/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.fxa.activities;

import org.mozilla.gecko.R;
import org.mozilla.gecko.background.common.log.Logger;

import android.os.Bundle;
import android.widget.TextView;

/**
 * Activity which displays "Account verified" success screen.
 */
public class FxAccountVerifiedAccountActivity extends FxAccountAbstractActivity {
  private static final String LOG_TAG = FxAccountVerifiedAccountActivity.class.getSimpleName();

  protected TextView emailText;

  public FxAccountVerifiedAccountActivity() {
    super(CANNOT_RESUME_WHEN_NO_ACCOUNTS_EXIST);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onCreate(Bundle icicle) {
    Logger.debug(LOG_TAG, "onCreate(" + icicle + ")");

    super.onCreate(icicle);
    setContentView(R.layout.fxaccount_account_verified);

    emailText = (TextView) ensureFindViewById(null, R.id.email, "email text");
    if (getIntent() != null && getIntent().getExtras() != null) {
      emailText.setText(getIntent().getStringExtra("email"));
    }
  }
}
