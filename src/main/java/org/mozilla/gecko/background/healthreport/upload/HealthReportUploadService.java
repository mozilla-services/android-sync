/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.background.healthreport.upload;

import org.mozilla.gecko.background.BackgroundService;
import org.mozilla.gecko.background.common.log.Logger;
import org.mozilla.gecko.background.healthreport.HealthReportConstants;

import android.content.Intent;
import android.os.IBinder;

/**
 * A <code>Service</code> to manage and upload health report data.
 *
 * We extend <code>IntentService</code>, rather than just <code>Service</code>,
 * because this gives us a worker thread to avoid main-thread networking.
 *
 * Yes, even though we're in an alarm-triggered service, it still counts as
 * main-thread.
 *
 * The operation of this service is as follows:
 *
 * XXX
 */
public class HealthReportUploadService extends BackgroundService {
  public static final String LOG_TAG = HealthReportUploadService.class.getSimpleName();
  public static final String WORKER_THREAD_NAME = LOG_TAG + "Worker";

  public HealthReportUploadService() {
    super(WORKER_THREAD_NAME);
    Logger.setThreadLogTag(HealthReportConstants.GLOBAL_LOG_TAG);
    Logger.debug(LOG_TAG, "Creating.");
  }

  @Override
  public void onHandleIntent(Intent intent) {
    Logger.setThreadLogTag(HealthReportConstants.GLOBAL_LOG_TAG);
    Logger.debug(LOG_TAG, "Running.");
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }
}
