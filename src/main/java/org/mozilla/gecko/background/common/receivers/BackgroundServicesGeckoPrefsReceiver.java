/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.background.common.receivers;

import org.mozilla.gecko.background.common.GlobalConstants;
import org.mozilla.gecko.background.common.log.Logger;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.ThreadPool;
import org.mozilla.gecko.sync.setup.activities.ActivityUtils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

/**
 * Listen for broadcasts sending Gecko Prefs to Background Services, and write
 * them to SharedPrefs.
 */
public class BackgroundServicesGeckoPrefsReceiver extends BroadcastReceiver {
  private static final String LOG_TAG = BackgroundServicesGeckoPrefsReceiver.class.getSimpleName();

  // This is the same as Fennec's per-profile prefs for the default profile; but
  // it's not important that they overlap.
  public static final String DEFAULT_PROFILE_SHARED_PREFS = "GeckoProfile-default";

  public static final String PREF_AUTH_SERVER_ENDPOINT = "identity.fxaccounts.auth.uri";
  public static final String PREF_TOKEN_SERVER_ENDPOINT = "services.sync.tokenServerURI";

  protected class PersistRunnable implements Runnable {
    protected final ExtendedJSONObject token;
    protected Context context = null;

    public PersistRunnable(Context context, ExtendedJSONObject token) {
      this.context = context;
      this.token = token;
    }

    @Override
    public void run() {
      final String type = token.getString("type");
      final String name = token.getString("name");
      final String value = token.getString("value");
      final Boolean isUserSet = token.getBoolean("isUserSet");
      if (!"char".equals(type)) {
        return;
      }
      final SharedPreferences sharedPrefs = context.getSharedPreferences(DEFAULT_PROFILE_SHARED_PREFS, GlobalConstants.SHARED_PREFERENCES_MODE);
      if (isUserSet == null || !isUserSet) {
        sharedPrefs.edit().remove(name).commit();
        return;
      }
      sharedPrefs.edit().putString(name, value).commit();
    }
  }

  @Override
  public void onReceive(Context context, Intent intent) {
    ActivityUtils.prepareLogging();

    final String token = intent.getStringExtra("token");
    if (token == null) {
      return;
    }

    try {
      ThreadPool.run(new PersistRunnable(context, new ExtendedJSONObject(token)));
    } catch (Exception e) {
      Logger.error(LOG_TAG, "Got exception persisting token; ignoring broadcast intent.", e);
      return;
    }
  }
}
