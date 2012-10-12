/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.background;

import org.mozilla.gecko.sync.GlobalConstants;

import android.app.AlarmManager;

@SuppressWarnings("unused")    // Unused until test code switched back to constant.
public class BackgroundServiceConstants {
  public static final String ACTION_ANNOUNCEMENTS_PREF = "org.mozilla.gecko.ANNOUNCEMENTS_PREF";

  static final String PREFS_BRANCH = "background";
  static final String PREF_LAST_FETCH  = "last_fetch";
  static final String PREF_LAST_LAUNCH = "last_firefox_launch";
  static final String PREF_ANNOUNCE_SERVER_URL  = "announce_server_url";
  static final String PREF_EARLIEST_NEXT_ANNOUNCE_FETCH = "earliest_next_announce_fetch";
  static final String PREF_ANNOUNCE_FETCH_INTERVAL_MSEC = "announce_fetch_interval_msec";

  public static final int SHARED_PREFERENCES_MODE = 0;

  // TODO
  static final String DEFAULT_ANNOUNCE_SERVER_URL = "http://people.mozilla.com/~rnewman/announce/";

  public static final long DEFAULT_ANNOUNCE_FETCH_INTERVAL_MSEC = AlarmManager.INTERVAL_HALF_DAY;
  public static final long DEFAULT_BACKOFF = 2 * 24 * 60 * 60 * 1000;   // Two days. Used if no Retry-After header.

  public static final String BACKGROUND_USER_AGENT = "Firefox Background " + GlobalConstants.MOZ_APP_VERSION;
  public static final String ANNOUNCE_CHANNEL = GlobalConstants.MOZ_UPDATE_CHANNEL.replace("default", GlobalConstants.MOZ_OFFICIAL_BRANDING ? "release" : "dev");

  // These are used to ask Fennec (via reflection) to send
  // us a pref notification. This avoids us having to guess
  // Fennec's prefs branch and pref name.
  // Eventually Fennec might listen to startup notifications and
  // do this automatically, but this will do for now. See Bug 800244.
  public static String GECKO_PREFERENCES_CLASS = "org.mozilla.gecko.GeckoPreferences";
  public static String GECKO_BROADCAST_METHOD  = "broadcastAnnouncementsPref";
}
