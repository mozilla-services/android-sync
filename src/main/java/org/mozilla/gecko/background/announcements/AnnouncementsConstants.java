/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.background.announcements;

import org.mozilla.gecko.sync.GlobalConstants;

import android.app.AlarmManager;

public class AnnouncementsConstants {
  public static final String GLOBAL_LOG_TAG = "GeckoAnnounce";
  public static final String ACTION_ANNOUNCEMENTS_PREF = "org.mozilla.gecko.ANNOUNCEMENTS_PREF";

  static final String PREFS_BRANCH = "background";
  static final String PREF_LAST_FETCH  = "last_fetch";
  static final String PREF_LAST_LAUNCH = "last_firefox_launch";
  static final String PREF_ANNOUNCE_SERVER_URL  = "announce_server_url";
  static final String PREF_EARLIEST_NEXT_ANNOUNCE_FETCH = "earliest_next_announce_fetch";
  static final String PREF_ANNOUNCE_FETCH_INTERVAL_MSEC = "announce_fetch_interval_msec";

  static final String DEFAULT_ANNOUNCE_SERVER_URL = "https://announce.services.mozilla.com/announce/";

  public static final long DEFAULT_ANNOUNCE_FETCH_INTERVAL_MSEC = AlarmManager.INTERVAL_HALF_DAY;
  public static final long DEFAULT_BACKOFF_MSEC = 2 * 24 * 60 * 60 * 1000;   // Two days. Used if no Retry-After header.

  public static final String ANNOUNCE_USER_AGENT = "Firefox Announcements " + GlobalConstants.MOZ_APP_VERSION;
  public static final String ANNOUNCE_CHANNEL = GlobalConstants.MOZ_UPDATE_CHANNEL.replace("default", GlobalConstants.MOZ_OFFICIAL_BRANDING ? "release" : "dev");
}
