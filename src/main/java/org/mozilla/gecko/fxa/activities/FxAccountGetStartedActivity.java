/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.fxa.activities;

import org.mozilla.gecko.R;
import org.mozilla.gecko.background.common.log.Logger;

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

/**
 * Activity which displays sign up/sign in screen to the user.
 */
public class FxAccountGetStartedActivity extends FxAccountAbstractActivity {
  protected static final String LOG_TAG = FxAccountGetStartedActivity.class.getSimpleName();

  /**
   * {@inheritDoc}
   */
  @Override
  public void onCreate(Bundle icicle) {
    Logger.debug(LOG_TAG, "onCreate(" + icicle + ")");

    super.onCreate(icicle);
    setContentView(R.layout.fxaccount_get_started);

    linkifyTextViews(null, new int[] { R.id.old_firefox });

    Button getStartedButton = (Button) ensureFindViewById(null, R.id.get_started_button, "get started button");
    getStartedButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        redirectToActivity(FxAccountCreateAccountActivity.class);
      }
    });
  }
}
