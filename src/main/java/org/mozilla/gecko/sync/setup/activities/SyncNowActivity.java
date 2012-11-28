/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.setup.activities;

import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.syncadapter.SyncAdapter;

import android.os.Bundle;
import android.widget.Toast;

public class SyncNowActivity extends SyncAccountActivity {
  public static final String LOG_TAG = SyncNowActivity.class.getSimpleName();

  @Override
  public void onCreate(Bundle savedInstanceState) {
    Logger.debug(LOG_TAG, "onCreate");
    super.onCreate(savedInstanceState);
  }

  @Override
  public void onResume() {
    Logger.debug(LOG_TAG, "onResume");
    super.onResume();

    if (this.localAccount == null) {
      Logger.warn(LOG_TAG, "No local Sync account.");
      return;
    }

    String[] stagesToSync = getStagesToSync();

    if (stagesToSync == null) {
      Logger.info(LOG_TAG, "Requesting immediate sync.");
      SyncAdapter.requestImmediateSync(localAccount, null);
    } else if (stagesToSync.length > 0) {
      Logger.info(LOG_TAG, "Requesting immediate sync of stages " + stagesToSync + ".");
      SyncAdapter.requestImmediateSync(localAccount, stagesToSync);
    }

    Toast.makeText(this, "Requested immediate sync of " + localAccount.name, Toast.LENGTH_LONG).show();

    setResult(RESULT_OK);
    finish();
  }

  protected String[] getStagesToSync() {
    return null;
  }
}
