/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.background.healthreport.prune;

import org.mozilla.gecko.background.common.log.Logger;
import org.mozilla.gecko.background.healthreport.EnvironmentBuilder;
import org.mozilla.gecko.background.healthreport.HealthReportConstants;
import org.mozilla.gecko.background.healthreport.HealthReportDatabaseStorage;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.ContentProviderClient;

/**
 * Manages scheduling of the pruning of old Firefox Health Report data.
 *
 * There are three main actions that take place:
 *   1) Excessive storage pruning: The recorded data is taking up an unreasonable amount of space.
 *   2) Expired data pruning: Data that is kept around longer than is useful.
 *   3) Periodic vacuuming: To deal with database bloat and fragmentation.
 *
 * (1) and (2) are performed periodically on their own schedules. (3) will occur via a timed
 * schedule (like (1) and (2)), or additionally when excessive database fragmentation occurs.
 *
 * Due to (3), auto_vacuum does not need to be enabled. However, by default it is enabled. Since
 * turning this feature off requires an expensive vacuum, we wait until the users' first vacuum
 * (which must be entirely time based - see {@link attemptVacuum}) to disable auto_vacuum.
 */
public class PrunePolicy {
  public static final String LOG_TAG = PrunePolicy.class.getSimpleName();

  protected final Context context;
  protected final SharedPreferences sharedPreferences;
  protected final String profilePath;

  protected ContentProviderClient client;
  protected HealthReportDatabaseStorage storage;

  public PrunePolicy(final Context context, final SharedPreferences sharedPrefs, final String profilePath) {
    this.context = context;
    this.sharedPreferences = sharedPrefs;
    this.profilePath = profilePath;
  }

  protected SharedPreferences getSharedPreferences() {
    return this.sharedPreferences;
  }

  public void tick(final long time) {
    try {
      attemptPruneBySize(time);
      attemptPruneByDuration(time);
      attemptVacuum(time);
    } catch (Exception e) {
      // While catching Exception is ordinarily bad form, this Service runs in the same process as
      // Fennec so if we crash, it crashes. Additionally, this Service runs regularly so these
      // crashes could be regular. Thus, we choose to quietly fail instead.
      Logger.warn(LOG_TAG, "Got exception pruning document.", e);
    } finally {
      releaseClient();
    }
  }

  protected boolean attemptPruneBySize(final long time) {
    return false;
  }

  protected boolean attemptPruneByDuration(final long time) {
    return false;
  }

  protected boolean attemptVacuum(final long time) {
    return false;
  }

  /**
   * Retrieves the {@link HealthReportDatabaseStorage} associated with the profile of the policy.
   * For efficiency, the underlying {@link ContentProviderClient} and
   * {@link HealthReportDatabaseStorage} are cached for later invocations. However, this means a
   * call to this method MUST be accompanied by a call to {@link releaseClient}. Throws
   * {@link IllegalStateException} if the storage instance could not be retrieved - note that the
   * {@link ContentProviderClient} instance will not be closed in this case and
   * {@link releaseClient} should still be called.
   */
  protected HealthReportDatabaseStorage getStorage() {
    if (storage != null) {
      return storage;
    }

    client = EnvironmentBuilder.getContentProviderClient(context);
    if (client == null) {
      // TODO: Record prune failures and submit as part of FHR upload.
      Logger.warn(LOG_TAG, "Unable to get ContentProviderClient - throwing.");
      throw new IllegalStateException("Unable to get ContentProviderClient.");
    }

    try {
      storage = EnvironmentBuilder.getStorage(client, profilePath);
      if (storage == null) {
        // TODO: Record prune failures and submit as part of FHR upload.
        Logger.warn(LOG_TAG,"Unable to get HealthReportDatabaseStorage for " + profilePath +
            " - throwing.");
        throw new IllegalStateException("Unable to get HealthReportDatabaseStorage for " +
            profilePath + " (== null).");
      }
    } catch (ClassCastException ex) {
      // TODO: Record prune failures and submit as part of FHR upload.
      Logger.warn(LOG_TAG,"Unable to get HealthReportDatabaseStorage for " + profilePath +
          profilePath + " (ClassCastException).");
      throw new IllegalStateException("Unable to get HealthReportDatabaseStorage for " +
          profilePath + ".", ex);
    }

    return storage;
  }

  /**
   * Closes the underlying {@link ContentProviderClient} instance owned by this policy. MUST be
   * called before this policy is garbage collected.
   */
  protected void releaseClient() {
    if (client != null) {
      client.release();
      client = null;
    }
  }
}
