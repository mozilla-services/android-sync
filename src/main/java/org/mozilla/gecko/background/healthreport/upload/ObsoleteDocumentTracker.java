/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.background.healthreport.upload;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.mozilla.gecko.background.common.log.Logger;
import org.mozilla.gecko.background.healthreport.HealthReportConstants;
import org.mozilla.gecko.sync.ExtendedJSONObject;

import android.content.SharedPreferences;

public class ObsoleteDocumentTracker {
  public static final String LOG_TAG = ObsoleteDocumentTracker.class.getSimpleName();

  protected final SharedPreferences sharedPrefs;

  public ObsoleteDocumentTracker(SharedPreferences sharedPrefs) {
    this.sharedPrefs = sharedPrefs;
  }

  protected ExtendedJSONObject getObsoleteIds() {
    String s = sharedPrefs.getString(HealthReportConstants.PREF_OBSOLETE_DOCUMENT_IDS_TO_DELETION_ATTEMPTS_REMAINING, null);
    if (s == null) {
      return new ExtendedJSONObject();
    }
    try {
      return ExtendedJSONObject.parseJSONObject(s);
    } catch (Exception e) {
      Logger.warn(LOG_TAG, "Got exception getting obsolete ids.", e);
      return new ExtendedJSONObject();
    }
  }

  /**
   * Write obsolete ids to disk.
   *
   * @param ids to write.
   */
  protected void setObsoleteIds(ExtendedJSONObject ids) {
    sharedPrefs
      .edit()
      .putString(HealthReportConstants.PREF_OBSOLETE_DOCUMENT_IDS_TO_DELETION_ATTEMPTS_REMAINING, ids.toString())
      .commit();
  }

  /**
   * Remove id from set of obsolete document ids tracked for deletion.
   *
   * Public for testing.
   *
   * @param id to stop tracking.
   */
  public void removeObsoleteId(String id) {
    ExtendedJSONObject ids = getObsoleteIds();
    ids.remove(id);
    setObsoleteIds(ids);
  }

  protected void decrementObsoleteId(ExtendedJSONObject ids, String id) {
    if (!ids.containsKey(id)) {
      return;
    }
    try {
      Long attempts = ids.getLong(id);
      if (attempts == null || --attempts < 1) {
        ids.remove(id);
      } else {
        ids.put(id, attempts);
      }
    } catch (ClassCastException e) {
      ids.remove(id);
      Logger.info(LOG_TAG, "Got exception decrementing obsolete ids counter.", e);
    }
  }

  /**
   * Decrement attempts remaining for id in set of obsolete document ids tracked
   * for deletion.
   *
   * Public for testing.
   *
   * @param id to decrement attempts.
   */
  public void decrementObsoleteIdAttempts(String id) {
    ExtendedJSONObject ids = getObsoleteIds();
    decrementObsoleteId(ids, id);
    setObsoleteIds(ids);
  }

  public void purgeObsoleteIds(Collection<String> oldIds) {
    ExtendedJSONObject ids = getObsoleteIds();
    for (String oldId : oldIds) {
      ids.remove(oldId);
    }
    setObsoleteIds(ids);
  }

  public void decrementObsoleteIdAttempts(Collection<String> oldIds) {
    ExtendedJSONObject ids = getObsoleteIds();
    for (String oldId : oldIds) {
      decrementObsoleteId(ids, oldId);
    }
    setObsoleteIds(ids);
  }

  /**
   * Return a batch of obsolete document IDs that should be deleted next.
   *
   * Document IDs are long and sending too many in a single request might
   * increase the likelihood of POST failures, so we delete a (deterministic)
   * subset here.
   *
   * @return a non-null collection.
   */
  public Collection<String> getBatchOfObsoleteIds() {
    ExtendedJSONObject ids = getObsoleteIds();
    List<String> batch = new ArrayList<String>(ids.keySet());
    Collections.sort(batch);
    if (batch.size() < HealthReportConstants.MAXIMUM_DELETIONS_PER_POST) {
      return batch;
    }
    // subList returns a view to the backing collection, which could be large.
    return new ArrayList<String>(batch.subList(0, HealthReportConstants.MAXIMUM_DELETIONS_PER_POST));
  }


  public long getDeletionAttemptsPerObsoleteDocumentId() {
    return sharedPrefs.getLong(HealthReportConstants.PREF_DELETION_ATTEMPTS_PER_OBSOLETE_DOCUMENT_ID, HealthReportConstants.DEFAULT_DELETION_ATTEMPTS_PER_OBSOLETE_DOCUMENT_ID);
  }

  public void addObsoleteId(String id) {
    ExtendedJSONObject ids = getObsoleteIds();
    ids.put(id, HealthReportConstants.DEFAULT_DELETION_ATTEMPTS_PER_OBSOLETE_DOCUMENT_ID);
    setObsoleteIds(ids);
  }

  public boolean hasObsoleteIds() {
    return getObsoleteIds().size() > 0;
  }

  public int numberOfObsoleteIds() {
    return getObsoleteIds().size();
  }

  public String getNextObsoleteId() {
    ExtendedJSONObject ids = getObsoleteIds();
    if (ids.size() < 1) {
      return null;
    }
    try {
      // We don't care what the order is, but let's make testing easier by
      // being deterministic. Deleting in random order might avoid failing too
      // many times in succession, but we expect only a single pending delete
      // in practice.
      return Collections.min(ids.keySet());
    } catch (Exception e) {
      Logger.warn(LOG_TAG, "Got exception picking obsolete id to delete.", e);
      return null;
    }
  }
}
