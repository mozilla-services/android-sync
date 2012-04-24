/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.repositories.android;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.repositories.domain.BookmarkRecord;

/**
 * Queue up insertions:
 * <ul>
 * <li>Folder inserts where the parent is known. Do these immediately, because
 * they allow other records to be inserted. Requires bookkeeping updates. On
 * insert, flush the next set.</li>
 * <li>Regular inserts where the parent is known. These can happen whenever.
 * Batch for speed.</li>
 * <li>Records where the parent is not known. These can be flushed out when the
 * parent is known, or entered as orphans. This can be a queue earlier in the
 * process, so they don't get assigned to Unsorted. Feed into the main batch
 * when the parent arrives.</li>
 * </ul>
 * <p>
 * Deletions are always done at the end so that orphaning is minimized, and
 * that's why we are batching folders and non-folders separately.
 * <p>
 * Updates are always applied as they arrive.
 * <p>
 * Note that this class is not thread safe. This should be fine: call it only
 * from within a store runnable.
 */
public abstract class BookmarksInsertionManager {
  public static final String LOG_TAG = "BookmarkInsert";
  public static boolean DEBUG = false;

  private final int flushThreshold;

  private final HashSet<String> writtenFolders = new HashSet<String>();
  private final ArrayList<BookmarkRecord> readyToWrite = new ArrayList<BookmarkRecord>();
  private final HashMap<String, ArrayList<BookmarkRecord>> waitingForParent = new HashMap<String, ArrayList<BookmarkRecord>>();

  /**
   * Create an instance to be used for tracking insertions in a bookmarks
   * repository session.
   *
   * @param flushThreshold
   *        When this many non-folder records have been stored for insertion,
   *        an incremental flush occurs.
   * @param writtenFolders
   *        The GUIDs of all the folders already written to the database.
   */
  public BookmarksInsertionManager(int flushThreshold, Set<String> writtenFolders) {
    this.flushThreshold = flushThreshold;
    this.writtenFolders.addAll(writtenFolders);
  }

  protected void addRecordWithUnwrittenParent(BookmarkRecord record) {
    if (!waitingForParent.containsKey(record.parentID)) {
      waitingForParent.put(record.parentID, new ArrayList<BookmarkRecord>());
    }
    waitingForParent.get(record.parentID).add(record);
  }

  protected void recursivelyAddRecordAndChildren(BookmarkRecord record) {
    Logger.debug(LOG_TAG, "Record has known parent with guid " + record.parentID + "; adding to insertion queue.");
    readyToWrite.add(record);

    if (record.isFolder()) {
      writtenFolders.add(record.guid);
    }

    ArrayList<BookmarkRecord> children = waitingForParent.get(record.guid);
    waitingForParent.remove(record.guid);
    if (children == null) {
      return;
    }
    for (BookmarkRecord child : children) {
      recursivelyAddRecordAndChildren(child);
    }
  }

  protected void enqueueFolder(BookmarkRecord record) {
    Logger.debug(LOG_TAG, "Inserting folder with guid " + record.guid);
    if (!writtenFolders.contains(record.parentID)) {
      Logger.debug(LOG_TAG, "Folder has unknown parent with guid " + record.parentID + "; keeping until we see the parent.");
      addRecordWithUnwrittenParent(record);
      return;
    }

    // Parent is known; add as much of the tree as this roots.
    recursivelyAddRecordAndChildren(record);
    incrementalFlush();
  }

  public void enqueueRecord(BookmarkRecord record) {
    try {
      if (record.isFolder()) {
        enqueueFolder(record);
        return;
      }
      Logger.debug(LOG_TAG, "Inserting bookmark with guid " + record.guid);

      if (!writtenFolders.contains(record.parentID)) {
        Logger.debug(LOG_TAG, "Bookmark has unknown parent with guid " + record.parentID + "; keeping until we see the parent.");
        addRecordWithUnwrittenParent(record);
        return;
      }

      Logger.debug(LOG_TAG, "Bookmark has known parent with guid " + record.parentID + "; adding to insertion queue.");
      readyToWrite.add(record);
      incrementalFlush();
    } finally {
      if (DEBUG) {
        dumpState();
      }
    }
  }

  /**
   * Flush insertions that can be easily taken care of right now.
   */
  protected void incrementalFlush() {
    int num = readyToWrite.size();
    if (num < flushThreshold) {
      Logger.debug(LOG_TAG, "Incremental flush called with " + num + " < " + flushThreshold + " records; not flushing.");
      return;
    }
    Logger.debug(LOG_TAG, "Incremental flush called with " + num + " records; flushing.");
    flushReadyToWrite();
  }

  protected void flushReadyToWrite() {
    Logger.debug(LOG_TAG, "Flush ready to write called with " + readyToWrite.size() + " records; flushing all.");

    if (readyToWrite.isEmpty()) {
      return;
    }

    // Write folders first.
    ArrayList<BookmarkRecord> nonFolders = new ArrayList<BookmarkRecord>();
    for (BookmarkRecord record : readyToWrite) {
      if (!record.isFolder()) {
        nonFolders.add(record);
        continue;
      }
      // Folders are inserted one at a time.
      try {
        insertFolder(record);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    int num = readyToWrite.size() - nonFolders.size();
    Logger.debug(LOG_TAG, "Flush ready to write wrote " + num +
          " folders and will bulkInsert " + nonFolders.size() + " non-folders.");
    readyToWrite.clear();

    if (nonFolders.isEmpty()) {
      return;
    }

    // bulkInsert non folders in batches.
    int start = 0;
    try {
      while (start < nonFolders.size()) {
        int end = Math.min(start + flushThreshold, nonFolders.size());
        List<BookmarkRecord> batch = nonFolders.subList(start, end);
        bulkInsertNonFolders(batch);
        start = start + flushThreshold;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void flushAll(long timestamp) {
    int num = 0;
    for (ArrayList<BookmarkRecord> records : waitingForParent.values()) {
      readyToWrite.addAll(records);
      num += records.size();
    }
    Logger.debug(LOG_TAG, "Flush all called with " + readyToWrite.size() + " ready records " +
        "and " + num + " records without known parents; flushing all.");
    flushReadyToWrite();
    if (DEBUG) {
      dumpState();
    }
  }

  // For debugging.
  public boolean isClear() {
    return readyToWrite.isEmpty() && waitingForParent.isEmpty();
  }

  // For debugging.
  public void dumpState() {
    ArrayList<String> readies = new ArrayList<String>();
    for (BookmarkRecord record : readyToWrite) {
      readies.add(record.guid);
    }
    String ready = Utils.toCommaSeparatedString(new ArrayList<String>(readies));

    ArrayList<String> waits = new ArrayList<String>();
    for (ArrayList<BookmarkRecord> recs : waitingForParent.values()) {
      for (BookmarkRecord rec : recs) {
        waits.add(rec.guid);
      }
    }
    String waiting = Utils.toCommaSeparatedString(waits);
    String known = Utils.toCommaSeparatedString(writtenFolders);

    Logger.debug(LOG_TAG, "Q=(" + ready + "), W = (" + waiting + "), P=(" + known + ")");
  }

  protected abstract void insertFolder(BookmarkRecord record) throws Exception;
  protected abstract void bulkInsertNonFolders(List<BookmarkRecord> records) throws Exception;
}
