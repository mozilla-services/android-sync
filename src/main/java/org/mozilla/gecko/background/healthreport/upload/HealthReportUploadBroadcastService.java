/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.background.healthreport.upload;

import org.mozilla.gecko.background.BackgroundService;
import org.mozilla.gecko.background.common.GlobalConstants;
import org.mozilla.gecko.background.common.log.Logger;
import org.mozilla.gecko.background.healthreport.HealthReportConstants;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

/**
 * A service which listens to broadcast intents from the system and from the
 * browser, registering or unregistering the main
 * {@link HealthReportUploadStartReceiver} with the {@link AlarmManager}.
 */
public class HealthReportUploadBroadcastService extends BackgroundService {
  public static final String LOG_TAG = HealthReportUploadBroadcastService.class.getSimpleName();
  public static final String WORKER_THREAD_NAME = LOG_TAG + "Worker";

  public HealthReportUploadBroadcastService() {
    super(WORKER_THREAD_NAME);
  }

  private void toggleAlarm(final Context context, boolean enabled) {
    Logger.info(LOG_TAG, (enabled ? "R" : "Unr") + "egistering health report start broadcast receiver...");

    final PendingIntent pending = createPendingIntent(context, HealthReportUploadStartReceiver.class);

    if (!enabled) {
      cancelAlarm(pending);
      return;
    }

    final long pollInterval = getPollInterval(context);
    scheduleAlarm(pollInterval, pending);
  }

  public static long getPollInterval(final Context context) {
    SharedPreferences preferences = context.getSharedPreferences(HealthReportConstants.PREFS_BRANCH, GlobalConstants.SHARED_PREFERENCES_MODE);
    return preferences.getLong(HealthReportConstants.PREF_HEALTHREPORT_UPLOAD_INTERVAL_MSEC, HealthReportConstants.DEFAULT_HEALTHREPORT_UPLOAD_INTERVAL_MSEC);
  }

  public static void setPollInterval(final Context context, long interval) {
    SharedPreferences preferences = context.getSharedPreferences(HealthReportConstants.PREFS_BRANCH, GlobalConstants.SHARED_PREFERENCES_MODE);
    preferences.edit().putLong(HealthReportConstants.PREF_HEALTHREPORT_UPLOAD_INTERVAL_MSEC, interval).commit();
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    Logger.setThreadLogTag(HealthReportConstants.GLOBAL_LOG_TAG);
    final String action = intent.getAction();
    Logger.debug(LOG_TAG, "Broadcast onReceive. Intent is " + action);

    if (HealthReportConstants.ACTION_HEALTHREPORT_UPLOAD_PREF.equals(action)) {
      handlePrefIntent(intent);
      return;
    }

    if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
        Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE.equals(action)) {
      BackgroundService.reflectContextToFennec(this,
          GlobalConstants.GECKO_PREFERENCES_CLASS,
          GlobalConstants.GECKO_BROADCAST_HEALTHREPORT_UPLOAD_PREF_METHOD);
      return;
    }

    // Failure case.
    Logger.warn(LOG_TAG, "Unknown intent " + action);
  }

  /**
   * Handle the intent sent by the browser when it wishes to notify us
   * of the value of the user preference. Look at the value and toggle the
   * alarm service accordingly.
   */
  protected void handlePrefIntent(Intent intent) {
    if (!intent.hasExtra("enabled")) {
      Logger.warn(LOG_TAG, "Got " + HealthReportConstants.ACTION_HEALTHREPORT_UPLOAD_PREF + " intent without enabled. Ignoring.");
      return;
    }

    final boolean enabled = intent.getBooleanExtra("enabled", true);
    Logger.debug(LOG_TAG, intent.getStringExtra("branch") + "/" +
                          intent.getStringExtra("pref")   + " = " +
                          (intent.hasExtra("enabled") ? enabled : ""));

    toggleAlarm(this, enabled);
//
//    // Primarily intended for debugging and testing, but this doesn't do any harm.
//    if (!enabled) {
//      Logger.info(LOG_TAG, "!enabled: clearing last fetch.");
//      final SharedPreferences sharedPreferences = this.getSharedPreferences(AnnouncementsConstants.PREFS_BRANCH,
//                                                                            GlobalConstants.SHARED_PREFERENCES_MODE);
//      final Editor editor = sharedPreferences.edit();
//      editor.remove(AnnouncementsConstants.PREF_LAST_FETCH_LOCAL_TIME);
//      editor.remove(AnnouncementsConstants.PREF_EARLIEST_NEXT_ANNOUNCE_FETCH);
//      editor.commit();
//    }
  }
}
