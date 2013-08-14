/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.background.healthreport.test;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import java.util.concurrent.BrokenBarrierException;

import org.mozilla.gecko.background.common.GlobalConstants;
import org.mozilla.gecko.background.healthreport.HealthReportConstants;
import org.mozilla.gecko.background.healthreport.HealthReportBroadcastService;
import org.mozilla.gecko.background.healthreport.upload.HealthReportUploadService;
import org.mozilla.gecko.background.test.helpers.BackgroundServiceTestCase;

public class TestHealthReportBroadcastService
    extends BackgroundServiceTestCase<TestHealthReportBroadcastService.MockHealthReportBroadcastService> {
  public static class MockHealthReportBroadcastService extends HealthReportBroadcastService {
    @Override
    protected SharedPreferences getSharedPreferences() {
      return this.getSharedPreferences(SHARED_PREFS_NAME, GlobalConstants.SHARED_PREFERENCES_MODE);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
      super.onHandleIntent(intent);
      try {
        barrier.await();
      } catch (InterruptedException e) {
        fail("Awaiting Service thread should not be interrupted.");
      } catch (BrokenBarrierException e) {
        // This will happen on timeout - do nothing.
      }
    }
  }

  public TestHealthReportBroadcastService() {
    super(MockHealthReportBroadcastService.class);
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    // We can't mock AlarmManager since it has a package-private constructor, so instead we reset
    // the alarm by hand.
    cancelAlarm(getUploadIntent());
  }

  @Override
  public void tearDown() throws Exception {
    cancelAlarm(getUploadIntent());
    super.tearDown();
  }

  protected void cancelAlarm(Intent intent) {
    final AlarmManager am = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
    final PendingIntent pi = PendingIntent.getService(getContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    am.cancel(pi);
    pi.cancel();
  }

  protected boolean isServiceAlarmSet(Intent intent) {
    return PendingIntent.getService(getContext(), 0, intent, PendingIntent.FLAG_NO_CREATE) != null;
  }

  protected Intent getUploadIntent() {
    final Intent intent = new Intent(getContext(), HealthReportUploadService.class);
    intent.setAction("upload");
    return intent;
  }

  public void testIgnoredUploadPrefIntents() throws Exception {
    // Intent without "upload" extra is ignored.
    intent.setAction(HealthReportConstants.ACTION_HEALTHREPORT_UPLOAD_PREF)
        .putExtra("profileName", "profileName")
        .putExtra("profilePath", "profilePath");
    startService(intent);
    awaitOrFail();

    assertFalse(isServiceAlarmSet(getUploadIntent()));
    barrier.reset();

    // No "profileName" extra.
    intent.putExtra("enabled", true)
        .removeExtra("profileName");
    startService(intent);
    awaitOrFail();

    assertFalse(isServiceAlarmSet(getUploadIntent()));
    barrier.reset();

    // No "profilePath" extra.
    intent.putExtra("profileName", "profileName")
        .removeExtra("profilePath");
    startService(intent);
    awaitOrFail();

    assertFalse(isServiceAlarmSet(getUploadIntent()));
  }

  public void testUploadPrefIntentDisabled() throws Exception {
    intent.setAction(HealthReportConstants.ACTION_HEALTHREPORT_UPLOAD_PREF)
        .putExtra("enabled", false)
        .putExtra("profileName", "profileName")
        .putExtra("profilePath", "profilePath");
    startService(intent);
    awaitOrFail();

    assertFalse(isServiceAlarmSet(getUploadIntent()));
  }

  public void testUploadPrefIntentEnabled() throws Exception {
    intent.setAction(HealthReportConstants.ACTION_HEALTHREPORT_UPLOAD_PREF)
        .putExtra("enabled", true)
        .putExtra("profileName", "profileName")
        .putExtra("profilePath", "profilePath");
    startService(intent);
    awaitOrFail();

    assertTrue(isServiceAlarmSet(getUploadIntent()));
  }

  public void testUploadServiceCancelled() throws Exception {
    intent.setAction(HealthReportConstants.ACTION_HEALTHREPORT_UPLOAD_PREF)
        .putExtra("enabled", true)
        .putExtra("profileName", "profileName")
        .putExtra("profilePath", "profilePath");
    startService(intent);
    awaitOrFail();

    assertTrue(isServiceAlarmSet(getUploadIntent()));
    barrier.reset();

    intent.putExtra("enabled", false);
    startService(intent);
    awaitOrFail();

    assertFalse(isServiceAlarmSet(getUploadIntent()));
  }
}
