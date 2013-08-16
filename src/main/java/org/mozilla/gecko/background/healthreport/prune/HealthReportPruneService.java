/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.background.healthreport.prune;

import org.mozilla.gecko.background.BackgroundService;
import org.mozilla.gecko.background.common.log.Logger;
import org.mozilla.gecko.background.healthreport.HealthReportConstants;

import android.content.Intent;
import android.os.IBinder;

/**
 * A <code>Service</code> to prune unnecessary or excessive health report data.
 *
 * We extend <code>IntentService</code>, rather than just <code>Service</code>,
 * because this gives us a worker thread to avoid excessive main-thread disk access.
 */
public class HealthReportPruneService extends BackgroundService {
  public static final String LOG_TAG = HealthReportPruneService.class.getSimpleName();
  public static final String WORKER_THREAD_NAME = LOG_TAG + "Worker";

  public HealthReportPruneService() {
    super(WORKER_THREAD_NAME);
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  @Override
  public void onHandleIntent(Intent intent) {
    Logger.setThreadLogTag(HealthReportConstants.GLOBAL_LOG_TAG);
    Logger.debug(LOG_TAG, "Handling prune intent.");

    if (!isIntentValid(intent)) {
      Logger.warn(LOG_TAG, "Intent not valid - returning.");
      return;
    }
  }

  protected boolean isIntentValid(Intent intent) {
    boolean isValid = true;

    final String profileName = intent.getStringExtra("profileName");
    if (profileName == null) {
      Logger.warn(LOG_TAG, "Got intent without profileName.");
      isValid = false;
    }

    final String profilePath = intent.getStringExtra("profilePath");
    if (profilePath == null) {
      Logger.warn(LOG_TAG, "Got intent without profilePath.");
      isValid = false;
    }

    return isValid;
  }
}
