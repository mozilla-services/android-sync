/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.repositories.android;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.repositories.domain.BookmarkRecord;

/**
 * Queue up insertions.
 *
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
   */
  public BookmarksInsertionManager(int flushThreshold) {
    this.flushThreshold = flushThreshold;
    this.clear();
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

  /**
   * Clear state in case of redundancy (e.g., wipe).
   */
  public void clear() {
    String[] SPECIAL_GUIDS = new String[] {
      // XXX Mobile and desktop places roots have to come first.
      "places",
      "mobile",
      "toolbar",
      "menu",
      "unfiled"
    };

    writtenFolders.clear();
    for (String guid : SPECIAL_GUIDS) {
      writtenFolders.add(guid);
    }
    readyToWrite.clear();
    waitingForParent.clear();
  }

  // For debugging.
  public boolean isClear() {
    return readyToWrite.isEmpty() && waitingForParent.isEmpty();
  }

  public static String join(String delimiter, String... strings) {
    StringBuilder sb = new StringBuilder();
    int i = 0;
    for (String string : strings) {
      sb.append(string);
      i += 1;
      if (i < strings.length) {
        sb.append(delimiter);
      }
    }
    return sb.toString();
  }

  // For debugging.
  public void dumpState() {
    String[] readyGuids = new String[readyToWrite.size()];
    int i = 0;
    for (BookmarkRecord record : readyToWrite) {
      readyGuids[i++] = record.guid;
    }
    String ready = join(", ", readyGuids);

    ArrayList<String> waits = new ArrayList<String>();
    for (ArrayList<BookmarkRecord> recs : waitingForParent.values()) {
      for (BookmarkRecord rec : recs) {
        waits.add(rec.guid);
      }
    }
    String[] waitingGuids = waits.toArray(new String[waits.size()]);
    String waiting = join(", ", waitingGuids);

    String[] knownGuids = writtenFolders.toArray(new String[writtenFolders.size()]);
    String known = join(", ", knownGuids);

    Logger.debug(LOG_TAG, "Q=(" + ready + "), W = (" + waiting + "), P=(" + known + ")");
  }

  protected abstract void insertFolder(BookmarkRecord record) throws Exception;
  protected abstract void bulkInsertNonFolders(List<BookmarkRecord> records) throws Exception;
}
