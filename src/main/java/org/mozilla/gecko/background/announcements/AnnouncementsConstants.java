/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.background.announcements;

import org.mozilla.gecko.background.common.GlobalConstants;

import android.app.AlarmManager;

public class AnnouncementsConstants {
  // Not `final` so we have the option to turn this on at runtime with a magic addon.
  public static boolean DISABLED = false;

  public static final long MILLISECONDS_PER_DAY = 24 * 60 * 60 * 1000;

  public static final String GLOBAL_LOG_TAG = "GeckoAnnounce";
  public static final String ACTION_ANNOUNCEMENTS_PREF = "org.mozilla.fennec.ANNOUNCEMENTS_PREF";

  static final String PREFS_BRANCH = "background";
  static final String PREF_LAST_FETCH_LOCAL_TIME  = "last_fetch";
  static final String PREF_LAST_FETCH_SERVER_DATE = "last_announce_date";
  static final String PREF_LAST_LAUNCH = "last_firefox_launch";
  static final String PREF_ANNOUNCE_SERVER_BASE_URL  = "announce_server_base_url";
  static final String PREF_EARLIEST_NEXT_ANNOUNCE_FETCH = "earliest_next_announce_fetch";
  static final String PREF_ANNOUNCE_FETCH_INTERVAL_MSEC = "announce_fetch_interval_msec";

  public static String DEFAULT_ANNOUNCE_SERVER_BASE_URL = "https://campaigns.services.mozilla.com/announce/";

  public static final String ANNOUNCE_PROTOCOL_VERSION = "1";
  public static final String ANNOUNCE_APPLICATION = "android";
  public static String ANNOUNCE_PATH_SUFFIX = AnnouncementsConstants.ANNOUNCE_PROTOCOL_VERSION + "/" +
                                              AnnouncementsConstants.ANNOUNCE_APPLICATION + "/";

  public static long DEFAULT_ANNOUNCE_FETCH_INTERVAL_MSEC = AlarmManager.INTERVAL_HALF_DAY;
  public static long DEFAULT_BACKOFF_MSEC = 2 * 24 * 60 * 60 * 1000;   // Two days. Used if no Retry-After header.
  public static long MINIMUM_FETCH_INTERVAL_MSEC = 60 * 60 * 1000;     // 1 hour.

  // Stop reporting idle counts once they hit one year.
  public static long MAX_SANE_IDLE_DAYS = 365;

  // Don't track last launch if the timestamp is ridiculously out of range:
  // four years after build.
  public static long LATEST_ACCEPTED_LAUNCH_TIMESTAMP_MSEC = GlobalConstants.BUILD_TIMESTAMP_MSEC +
                                                             4 * 365 * MILLISECONDS_PER_DAY;

  public static String USER_AGENT = "Firefox Announcements " + GlobalConstants.MOZ_APP_VERSION;
  public static String ANNOUNCE_CHANNEL = GlobalConstants.MOZ_UPDATE_CHANNEL.replace("default", GlobalConstants.MOZ_OFFICIAL_BRANDING ? "release" : "dev");
}
