/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.fxa.activities;

import org.mozilla.gecko.R;
import org.mozilla.gecko.background.common.log.Logger;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTabHost;
import android.widget.TabHost;

/**
 * Activity which displays login screen to the user.
 */
public class FxAccountSetupActivity
    extends FragmentActivity {
  protected static final String LOG_TAG = FxAccountSetupActivity.class.getSimpleName();

  /**
   * {@inheritDoc}
   */
  @Override
  public void onCreate(Bundle icicle) {
    Logger.debug(LOG_TAG, "onCreate(" + icicle + ")");

    super.onCreate(icicle);
    setContentView(R.layout.fxaccount_setup);

    FragmentTabHost tabHost = (FragmentTabHost) findViewById(android.R.id.tabhost);
    tabHost.setup(this, getSupportFragmentManager(), R.id.tabcontent);

    TabHost.TabSpec createAccountTab = tabHost.newTabSpec("create_account");
    createAccountTab.setIndicator(getResources().getString(R.string.fxaccount_create_account_tab_label));
    tabHost.addTab(createAccountTab, FxAccountCreateAccountFragment.class, null);

    TabHost.TabSpec signInTab = tabHost.newTabSpec("sign_in");
    signInTab.setIndicator(getResources().getString(R.string.fxaccount_sign_in_tab_label));
    tabHost.addTab(signInTab, FxAccountSignInFragment.class, null);
  }
}
