/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.setup.activities;

import org.mozilla.gecko.R;
import org.mozilla.gecko.sync.setup.Constants;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;

public class SetupSuccessActivity extends Activity {
  @SuppressWarnings("unused")
  private final static String LOG_TAG = "SetupSuccessActivity";
  private TextView setupSubtitle;
  private Context mContext;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    setTheme(R.style.SyncTheme);
    super.onCreate(savedInstanceState);
    mContext = getApplicationContext();
    Bundle extras = this.getIntent().getExtras();
    setContentView(R.layout.sync_setup_success);
    setupSubtitle = ((TextView) findViewById(R.id.setup_success_subtitle));
    if (extras != null) {
      boolean isSetup = extras.getBoolean(Constants.INTENT_EXTRA_IS_SETUP);
      if (!isSetup) {
        setupSubtitle.setText(getString(R.string.sync_subtitle_manage));
      }
    }
  }

  /* Click Handlers */
  public void settingsClickHandler(View target) {
    Intent intent = new Intent(Settings.ACTION_SYNC_SETTINGS);
    intent.setFlags(Constants.FLAG_ACTIVITY_REORDER_TO_FRONT_NO_ANIMATION);
    startActivity(intent);
    finish();
  }

  public void pairClickHandler(View target) {
    Intent intent = new Intent(mContext, SetupSyncActivity.class);
    intent.setFlags(Constants.FLAG_ACTIVITY_REORDER_TO_FRONT_NO_ANIMATION);
    intent.putExtra(Constants.INTENT_EXTRA_IS_SETUP, false);
    startActivity(intent);
  }
}
