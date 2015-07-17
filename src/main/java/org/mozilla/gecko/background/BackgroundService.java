/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.background;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.mozilla.gecko.background.common.log.Logger;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

public abstract class BackgroundService extends IntentService {
  private static final String LOG_TAG = BackgroundService.class.getSimpleName();
  private static final long WAKE_LOCK_TIMEOUT = 1000; // milliseconds

  private static WakeLock wakeLock;

  protected BackgroundService() {
    super(LOG_TAG);
  }

  protected BackgroundService(String threadName) {
    super(threadName);
  }

  public static void runIntentInService(Context context, Intent intent, Class<? extends BackgroundService> serviceClass) {
    Intent service = new Intent(context, serviceClass);
    service.setAction(intent.getAction());
    service.putExtras(intent);
    context.startService(service);

    // We need a wake lock to ensure that the device stays awake long enough after the Intent's
    // BroadcastReceiver returns so our IntentService will actually run. Because BackgroundService
    // can be invoked without calling runIntentInService() and thus acquiring a wake lock, we
    // acquire a timed wake lock here so BackgroundService doesn't need to worry about whether a
    // wake lock needs to be released. If multiple intents are queued for this IntentService, then
    // each intent (launched from runIntentInService) will have acquired its own timed wake lock.
    WakeLock wakeLock = getWakeLock(context);
    wakeLock.acquire(WAKE_LOCK_TIMEOUT);
  }

  /**
   * Returns true if the OS will allow us to perform background
   * data operations. This logic varies by OS version.
   */
  @SuppressWarnings("deprecation")
  protected boolean backgroundDataIsEnabled() {
    ConnectivityManager connectivity = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
      return connectivity.getBackgroundDataSetting();
    }
    NetworkInfo networkInfo = connectivity.getActiveNetworkInfo();
    if (networkInfo == null) {
      return false;
    }
    return networkInfo.isAvailable();
  }

  protected AlarmManager getAlarmManager() {
    return getAlarmManager(this.getApplicationContext());
  }

  protected static AlarmManager getAlarmManager(Context context) {
    return (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
  }

  protected void scheduleAlarm(long pollInterval, PendingIntent pendingIntent) {
    Logger.info(LOG_TAG, "Setting inexact repeating alarm for interval " + pollInterval);
    if (pollInterval <= 0) {
        throw new IllegalArgumentException("pollInterval " + pollInterval + " must be positive");
    }
    final AlarmManager alarm = getAlarmManager();
    final long firstEvent = System.currentTimeMillis();
    alarm.setInexactRepeating(AlarmManager.RTC, firstEvent, pollInterval, pendingIntent);
  }

  protected void cancelAlarm(PendingIntent pendingIntent) {
    final AlarmManager alarm = getAlarmManager();
    alarm.cancel(pendingIntent);
    // For testing - allows us to see if the intent, and thus the alarm, has been cancelled.
    pendingIntent.cancel();
  }

  /**
   * To avoid tight coupling to Fennec, we use reflection to find
   * <code>GeckoPreferences</code>, invoking the same code path that
   * <code>GeckoApp</code> uses on startup to send the <i>other</i>
   * notification to which we listen.
   *
   * Invoke this to handle one of the system intents to which we listen to
   * launch our service without the browser being opened.
   *
   * All of this is neatly wrapped in <code>try…catch</code>, so this code
   * will run safely without a Firefox build installed.
   */
  protected static void reflectContextToFennec(Context context, String className, String methodName) {
    // Ask the browser to tell us the current state of the preference.
    try {
      Class<?> geckoPreferences = Class.forName(className);
      Method broadcastSnippetsPref = geckoPreferences.getMethod(methodName, Context.class);
      broadcastSnippetsPref.invoke(null, context);
      return;
    } catch (ClassNotFoundException e) {
      Logger.error(LOG_TAG, "Class " + className + " not found!");
      return;
    } catch (NoSuchMethodException e) {
      Logger.error(LOG_TAG, "Method " + className + "/" + methodName + " not found!");
      return;
    } catch (IllegalArgumentException | InvocationTargetException | IllegalAccessException e) {
      Logger.error(LOG_TAG, "Got exception invoking " + methodName + ".");
    }
  }

  protected static synchronized WakeLock getWakeLock(Context context) {
    if (BackgroundService.wakeLock == null) {
      PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
      BackgroundService.wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, LOG_TAG);
    }
    return BackgroundService.wakeLock;
  }
}
