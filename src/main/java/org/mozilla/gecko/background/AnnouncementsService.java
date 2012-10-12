/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.background;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.net.URI;
import java.util.List;
import java.util.Locale;

import org.mozilla.gecko.sync.Logger;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.IBinder;

/**
 * A Service to periodically check for new published announcements,
 * presenting them to the user if local conditions permit.
 *
 * We extend IntentService, rather than just Service, because this gives us
 * a worker thread to avoid main-thread networking.
 *
 * Yes, even though we're in an alarm-triggered service, it still counts
 * as main-thread.
 *
 * The operation of this service is as follows:
 *
 * 0. Decide if a request should be made.
 * 1. Compute the arguments to the request. This includes enough
 *    pertinent details to allow the server to pre-filter a message
 *    set, recording enough tracking details to compute statistics.
 * 2. Issue the request. If this succeeds with a 200 or 204, great;
 *    track that timestamp for the next run through Step 0.
 * 3. Process any received messages.
 *
 * Message processing is as follows:
 *
 * 0. Decide if message display should occur. This might involve
 *    user preference or other kinds of environmental factors.
 * 1. Use the AnnouncementPresenter to open the announcement.
 *
 * Future:
 * * Persisting of multiple announcements.
 * * Prioritization.
 */
public class AnnouncementsService extends IntentService implements AnnouncementsFetchDelegate {

  private static final String LOG_TAG = "GeckoAnnounce";

  private static final long MINIMUM_FETCH_INTERVAL_MSEC = 60 * 60 * 1000;   // 1 hour.

  public AnnouncementsService() {
    super("AnnouncementsServiceWorker");
    Logger.debug(LOG_TAG, "Creating AnnouncementsService.");
  }

  public boolean shouldFetchAnnouncements() {
    final long now = System.currentTimeMillis();

    if (!backgroundDataIsEnabled()) {
      Logger.debug(LOG_TAG, "Background data not possible. Skipping.");
      return false;
    }

    // Don't fetch if we were told to back off.
    if (getEarliestNextFetch() > now) {
      return false;
    }

    // Don't do anything if we haven't waited long enough.
    final long lastFetch = getLastFetch();

    // Just in case the alarm manager schedules us more frequently, or something
    // goes awry with relaunches.
    if ((now - lastFetch) < MINIMUM_FETCH_INTERVAL_MSEC) {
      Logger.debug(LOG_TAG, "Returning: minimum fetch interval of " + MINIMUM_FETCH_INTERVAL_MSEC + "ms not met.");
      return false;
    }

    return true;
  }

  protected void processAnnouncements(List<Announcement> announcements) {
    for (Announcement an : announcements) {
      AnnouncementPresenter.displayAnnouncement(this, an);
    }
  }

  /**
   * If it's time to do a fetch -- we've waited long enough,
   * we're allowed to use background data, etc. -- then issue
   * a fetch. The subsequent background check is handled implicitly
   * by the AlarmManager.
   */
  @Override
  public void onHandleIntent(Intent intent) {
    Logger.debug(LOG_TAG, "Running AnnouncementsService.");

    if (!shouldFetchAnnouncements()) {
      Logger.debug(LOG_TAG, "Not fetching.");
      return;
    }

    // Otherwise, grab our snippets URL and process the contents.
    AnnouncementsFetcher.fetchAndProcessAnnouncements(getLastLaunch(), this);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

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

  protected long getLastLaunch() {
    return getSharedPreferences().getLong(BackgroundServiceConstants.PREF_LAST_LAUNCH, 0);
  }

  private SharedPreferences getSharedPreferences() {
    return this.getSharedPreferences(BackgroundServiceConstants.PREFS_BRANCH, BackgroundServiceConstants.SHARED_PREFERENCES_MODE);
  }

  @Override
  protected void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
    super.dump(fd, writer, args);

    final long lastFetch = getLastFetch();
    final long lastLaunch = getLastLaunch();
    writer.write("AnnouncementsService: last fetch " + lastFetch +
                 ", last Firefox activity: " + lastLaunch + "\n");
  }

  protected void setEarliestNextFetch(final long earliest) {
    this.getSharedPreferences().edit().putLong(BackgroundServiceConstants.PREF_EARLIEST_NEXT_ANNOUNCE_FETCH, earliest).commit();
  }

  protected long getEarliestNextFetch() {
    return this.getSharedPreferences().getLong(BackgroundServiceConstants.PREF_EARLIEST_NEXT_ANNOUNCE_FETCH, 0L);
  }

  protected void setLastFetch(final long fetch) {
    this.getSharedPreferences().edit().putLong(BackgroundServiceConstants.PREF_LAST_FETCH, fetch).commit();
  }

  @Override
  public long getLastFetch() {
    return getSharedPreferences().getLong(BackgroundServiceConstants.PREF_LAST_FETCH, 0L);
  }

  /**
   * Use this to write the persisted server URL, overriding
   * the default value.
   * @param url a URI identifying the full request path, e.g.,
   *            "http://foo.com:1234/snippets/"
   */
  public void setSnippetsServerURL(final URI url) {
    if (url == null) {
      throw new IllegalArgumentException("url cannot be null.");
    }
    final String scheme = url.getScheme();
    if (scheme == null) {
      throw new IllegalArgumentException("url must have a scheme.");
    }
    if (!scheme.startsWith("http")) {
      throw new IllegalArgumentException("url must be http or https.");
    }
    SharedPreferences p = this.getSharedPreferences();
    p.edit().putString(BackgroundServiceConstants.PREF_ANNOUNCE_SERVER_URL, url.toASCIIString()).commit();
  }

  @Override
  public String getServerURL() {
    SharedPreferences p = this.getSharedPreferences();
    return p.getString(BackgroundServiceConstants.PREF_ANNOUNCE_SERVER_URL, BackgroundServiceConstants.DEFAULT_ANNOUNCE_SERVER_URL);
  }

  @Override
  public Locale getLocale() {
    return Locale.getDefault();
  }

  @Override
  public String getUserAgent() {
    return BackgroundServiceConstants.BACKGROUND_USER_AGENT;
  }

  @Override
  public void onNoNewAnnouncements(long fetched) {
    Logger.info(LOG_TAG, "No new announcements to display.");
    setLastFetch(fetched);
  }

  @Override
  public void onNewAnnouncements(List<Announcement> announcements, long fetched) {
    Logger.info(LOG_TAG, "Processing announcements: " + announcements.size());
    setLastFetch(fetched);
    processAnnouncements(announcements);
  }

  @Override
  public void onRemoteFailure(int status) {
    // Bump our fetch timestamp.
    Logger.warn(LOG_TAG, "Got remote fetch status " + status + "; bumping fetch time.");
    setLastFetch(System.currentTimeMillis());
  }

  @Override
  public void onRemoteError(Exception e) {
    // Bump our fetch timestamp.
    Logger.warn(LOG_TAG, "Error processing response.", e);
    setLastFetch(System.currentTimeMillis());
  }

  @Override
  public void onLocalError(Exception e) {
    Logger.error(LOG_TAG, "Got exception in fetch.", e);
    // Do nothing yet, so we'll retry.
  }

  @Override
  public void onBackoff(int retryAfterInSeconds) {
    Logger.info(LOG_TAG, "Got retry after: " + retryAfterInSeconds);
    final long delay = (Math.max(retryAfterInSeconds, BackgroundServiceConstants.DEFAULT_BACKOFF) * 1000);
    setEarliestNextFetch(delay + System.currentTimeMillis());
  }
}
