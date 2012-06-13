/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.synchronizer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.repositories.InactiveSessionException;
import org.mozilla.gecko.sync.repositories.RepositorySession;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionGuidsSinceDelegate;

/**
 * A <code>RecordsChannel</code> that updates a collection of GUIDs using
 * <code>source.guidsSince</code> before downloading the freshest GUIDs using
 * <code>source.fetch</code>. GUIDs are only removed from the collection when
 * they are successfully stored.
 */
public class FillingRecordsChannel extends RecordsChannel {
  public static final String LOG_TAG = "FillingRecsChan";

  protected final FillingGuidsManager guidsManager;

  /**
   * GUIDs of records that were successfully stored this session and do not need
   * to be fetched later.
   */
  protected final ArrayList<String> fetchedGuids = new ArrayList<String>();

  /**
   * GUIDs of records that were not successfully fetched and stored this
   * session, and may need to be fetched again later.
   */
  protected final ArrayList<String> failedGuids = new ArrayList<String>();

  /**
   * GUIDs of records to fetch right now.
   */
  protected Collection<String> nextGuids;

  public FillingRecordsChannel(final RepositorySession source, final RepositorySession sink,
      final RecordsChannelDelegate delegate, final FillingGuidsManager guidsManager) {
    super(source, sink, delegate);
    this.guidsManager = guidsManager;
  }

  @Override
  protected void fetch() {
    final RecordsChannel self = this;
    final RepositorySessionGuidsSinceDelegate delegate = new RepositorySessionGuidsSinceDelegate() {
      @Override
      public void onGuidsSinceSucceeded(final Collection<String> guids) {
        Logger.debug(LOG_TAG, "guidsSince returned " + guids.size() + " fresh GUIDs.");

        nextGuids = null;
        try {
          guidsManager.addFreshGuids(guids);
          nextGuids = guidsManager.nextGuids();
        } catch (Exception e) {
          Logger.warn(LOG_TAG, "Got exception prepending or getting GUIDs. Ignoring and trying to fetch retrieved GUIDs.", e);
        }
        if (nextGuids == null) {
          nextGuids = guids;
        }
        // Early abort if possible.
        if (nextGuids == null || nextGuids.isEmpty()) {
          Logger.debug(LOG_TAG, "No GUIDs to fetch. Succeeding fetch early.");
          onFetchCompleted(timestamp);
          return;
        }

        try {
          String[] nextGuidsArray = nextGuids.toArray(new String[nextGuids.size()]);
          Logger.debug(LOG_TAG, "Fetching " + nextGuidsArray.length + " records by GUID.");
          source.fetch(nextGuidsArray, self);
        } catch (InactiveSessionException e) {
          onFetchFailed(e, null);
        }
      }

      @Override
      public void onGuidsSinceFailed(final Exception ex) {
        // A white lie.
        onFetchFailed(ex, null);
      }
    };

    fetchedGuids.clear();
    source.guidsSince(timestamp, delegate);
  }

  @Override
  public void onStoreCompleted(final long storeEnd) {
    if (nextGuids.isEmpty()) {
      Logger.debug(LOG_TAG, "No GUIDs to fetch. Completing store early.");
      super.onStoreCompleted(storeEnd);
      return;
    }

    Logger.debug(LOG_TAG, "Removing " + fetchedGuids.size() + " GUIDs and retrying " + failedGuids.size() + " GUIDs.");
    try {
      guidsManager.removeGuids(fetchedGuids);
    } catch (Exception e) {
      Logger.warn(LOG_TAG, "Got exception removing fetched GUIDs. Ignoring. We might try to re-fetch records!", e);
    }
    try {
      guidsManager.retryGuids(failedGuids);
    } catch (Exception e) {
      Logger.warn(LOG_TAG, "Got exception re-adding failed GUIDs. Ignoring. We might try to re-fetch records!", e);
    }

    Set<String> orphaned = new HashSet<String>(nextGuids);
    orphaned.removeAll(fetchedGuids);
    orphaned.removeAll(failedGuids);
    fetchedGuids.clear(); // GC as soon as possible.
    failedGuids.clear();
    if (!orphaned.isEmpty()) {
      // Hmm... we asked for a record and didn't get anything back.
      Logger.warn(LOG_TAG, "No record returned for GUIDs " + Utils.toCommaSeparatedString(orphaned) + ".");
      Logger.warn(LOG_TAG, "Removing orphaned GUIDs and continuing.");
      try {
        guidsManager.removeGuids(orphaned);
      } catch (Exception e) {
        Logger.warn(LOG_TAG, "Got exception removing orphaned GUIDs. Ignoring. We might re-fetch records for ever!", e);
      }
      orphaned = null; // GC as soon as possible.
    }

    Logger.debug(LOG_TAG, "" + guidsManager.numGuidsRemaining() + " GUIDs still need to be fetched. Completing store.");
    super.onStoreCompleted(storeEnd);
  }


  @Override
  public void onRecordStoreSucceeded(final String guid) {
    super.onRecordStoreSucceeded(guid);
    fetchedGuids.add(guid);
  }

  @Override
  public void onRecordStoreFailed(final Exception ex, final String guid) {
    failedGuids.add(guid);
    super.onRecordStoreSucceeded(guid);
  }
}
