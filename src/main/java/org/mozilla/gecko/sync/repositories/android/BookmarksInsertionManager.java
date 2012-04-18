/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.repositories.android;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionStoreDelegate;
import org.mozilla.gecko.sync.repositories.domain.BookmarkRecord;

/**
 * Queue up insertions.
 *
 * Note that this class is not thread safe. This should be fine: call it only
 * from within a store runnable.
 */
public abstract class BookmarksInsertionManager {
  private static final String LOG_TAG = "BookmarkInsert";

  // private RepositorySessionStoreDelegate delegate;

  private final int flushThreshold;

  private final HashSet<String> knownFolders = new HashSet<String>();
  private final ArrayList<BookmarkRecord> readyToWrite = new ArrayList<BookmarkRecord>();
  private final HashMap<String, ArrayList<BookmarkRecord>> waitingForParent = new HashMap<String, ArrayList<BookmarkRecord>>();

  /**
   * Create an instance to be used for tracking insertions in a bookmarks
   * repository session.
   *
   * @param dataAccessor
   *        Used to effect database changes.
   *        XXX TODO
   *
   * @param flushThreshold
   *        When this many non-folder records have been stored for insertion,
   *        an incremental flush occurs.
   */
  public BookmarksInsertionManager(int flushThreshold) {
    this.flushThreshold = flushThreshold;
    this.clear();
  }

  /**
   * Set the delegate to use for callbacks.
   * If not invoked, no callbacks will be submitted.
   *
   * @param delegate a delegate, which should already be a delayed delegate.
   */
  public void setDelegate(RepositorySessionStoreDelegate delegate) {
    // XXX this.delegate = delegate;
  }


  protected void addRecordWithUnknownParent(BookmarkRecord record) {
    if (!waitingForParent.containsKey(record.parentID)) {
      waitingForParent.put(record.parentID, new ArrayList<BookmarkRecord>());
    }
    waitingForParent.get(record.parentID).add(record);
  }

  protected void insertFolder(BookmarkRecord record) {
    Logger.debug(LOG_TAG, "Inserting folder with guid " + record.guid);
    if (!knownFolders.contains(record.parentID)) {
      Logger.debug(LOG_TAG, "Folder has unknown parent with guid " + record.parentID + "; keeping until we see the parent.");
      addRecordWithUnknownParent(record);
      return;
    }

    Logger.debug(LOG_TAG, "Folder has known parent with guid " + record.parentID + "; adding to insertion queue.");
    readyToWrite.add(record);

    knownFolders.add(record.guid);
    ArrayList<BookmarkRecord> children = waitingForParent.get(record.guid);
    if (children == null) {
      return;
    }
    waitingForParent.remove(record.guid);
    readyToWrite.addAll(children);
    incrementalFlush();
  }

  public void insertRecord(BookmarkRecord record) {
    if (record.isFolder()) {
      insertFolder(record);
      return;
    }
    Logger.debug(LOG_TAG, "Inserting bookmark with guid " + record.guid);

    if (!knownFolders.contains(record.parentID)) {
      Logger.debug(LOG_TAG, "Bookmark has unknown parent with guid " + record.parentID + "; keeping until we see the parent.");
      addRecordWithUnknownParent(record);
      return;
    }

    Logger.debug(LOG_TAG, "Bookmark has known parent with guid " + record.parentID + "; adding to insertion queue.");
    readyToWrite.add(record);
    incrementalFlush();
  }

  /**
   * Flush insertions that can be easily taken care of right now.
   */
  public void incrementalFlush() {
    int num = readyToWrite.size();
    if (num < flushThreshold) {
      Logger.debug(LOG_TAG, "Incremental flush called with " + num + " < " + flushThreshold + " records; not flushing.");
      return;
    }
    Logger.debug(LOG_TAG, "Incremental flush called with " + num + " records; flushing.");
    flushAll(System.currentTimeMillis());
  }

  public void flushAll(long timestamp) {
    Logger.debug(LOG_TAG, "Full flush called with " + readyToWrite.size() + " records; flushing all.");
    for (BookmarkRecord record : readyToWrite) {
      // XXX folders first?
      try {
        insert(record);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    readyToWrite.clear();
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

    knownFolders.clear();
    for (String guid : SPECIAL_GUIDS) {
      knownFolders.add(guid);
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

    String[] knownGuids = knownFolders.toArray(new String[knownFolders.size()]);
    String known = join(", ", knownGuids);

    System.out.println("Q=(" + ready + "), W = (" + waiting + "), P=(" + known + ")");
  }

  protected abstract void insert(BookmarkRecord record) throws Exception;
}
