/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Android Sync Client.
 *
 * The Initial Developer of the Original Code is
 * the Mozilla Foundation.
 * Portions created by the Initial Developer are Copyright (C) 2011
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * Jason Voll <jvoll@mozilla.com>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

package org.mozilla.android.sync.repositories.bookmarks;

import java.util.ArrayList;

import org.mozilla.android.sync.repositories.InvalidRequestException;
import org.mozilla.android.sync.repositories.MultipleRecordsForGuidException;
import org.mozilla.android.sync.repositories.Repository;
import org.mozilla.android.sync.repositories.RepositorySession;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionBeginDelegate;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionFinishDelegate;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionCreationDelegate;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionFetchRecordsDelegate;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionGuidsSinceDelegate;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionStoreDelegate;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionWipeDelegate;
import org.mozilla.android.sync.repositories.domain.BookmarkRecord;
import org.mozilla.android.sync.repositories.domain.Record;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

public class BookmarksRepositorySession extends RepositorySession {

  BookmarksDatabaseHelper dbHelper;
  private static String tag = "BookmarksRepositorySession";

  public BookmarksRepositorySession(Repository repository,
      RepositorySessionCreationDelegate callbackReciever, Context context, long lastSyncTimestamp) {
    super(repository, callbackReciever, lastSyncTimestamp);
    dbHelper = new BookmarksDatabaseHelper(context);
  }

  @Override
  public void begin(RepositorySessionBeginDelegate receiver) {
    receiver.onBeginSucceeded();
  }

  @Override
  public void finish(RepositorySessionFinishDelegate receiver) {
    receiver.onFinishSucceeded();
  }

  // guids since method and thread
  @Override
  public void guidsSince(long timestamp, RepositorySessionGuidsSinceDelegate delegate) {
    GuidsSinceThread thread = new GuidsSinceThread(timestamp, delegate, dbHelper);
    thread.start();
  }

  class GuidsSinceThread extends Thread {

    private long timestamp;
    private RepositorySessionGuidsSinceDelegate delegate;
    private BookmarksDatabaseHelper dbHelper;

    public GuidsSinceThread(long timestamp, RepositorySessionGuidsSinceDelegate delegate, BookmarksDatabaseHelper dbHelper) {
      this.timestamp = timestamp;
      this.delegate = delegate;
      this.dbHelper = dbHelper;
    }

    public void run() {
      if (!confirmSessionActive()) {
        callbackReceiver.handleException(new InactiveSessionException(null));
        return;
      }

      Cursor cur = dbHelper.getGUIDSSince(timestamp);
      int index = cur.getColumnIndex(BookmarksDatabaseHelper.COL_GUID);

      ArrayList<String> guids = new ArrayList<String>();
      cur.moveToFirst();
      while (!cur.isAfterLast()) {
        guids.add(cur.getString(index));
        cur.moveToNext();
      }
      cur.close();

      String guidsArray[] = new String[guids.size()];
      guids.toArray(guidsArray);
      delegate.onGuidsSinceSucceeded(guidsArray);

    }
  }

  @Override
  // Fetch since method and thread
  public void fetchSince(long timestamp, RepositorySessionFetchRecordsDelegate delegate) {
    FetchSinceThread thread = new FetchSinceThread(timestamp, delegate);
    thread.start();
  }

  class FetchSinceThread extends Thread {

    private long timestamp;
    private RepositorySessionFetchRecordsDelegate delegate;

    public FetchSinceThread(long timestamp, RepositorySessionFetchRecordsDelegate delegate) {
      this.timestamp = timestamp;
      this.delegate = delegate;
    }

    public void run() {
      if (!confirmSessionActive()) {
        callbackReceiver.handleException(new InactiveSessionException(null));
        return;
      }

      Cursor cur = dbHelper.fetchSince(timestamp);
      ArrayList<BookmarkRecord> records = new ArrayList<BookmarkRecord>();
      cur.moveToFirst();
      while (!cur.isAfterLast()) {
        records.add(DBUtils.bookmarkFromMozCursor(cur));
        cur.moveToNext();
      }
      cur.close();

      Record[] recordArray = new Record[records.size()];
      records.toArray(recordArray);
      delegate.onFetchSucceeded(recordArray);
    }
  }

  @Override
  // Fetch method and thread
  public void fetch(String[] guids, RepositorySessionFetchRecordsDelegate delegate) {
    FetchThread thread = new FetchThread(guids, delegate);
    thread.start();
  }

  class FetchThread extends Thread {
    private String[] guids;
    private RepositorySessionFetchRecordsDelegate delegate;

    public FetchThread(String[] guids, RepositorySessionFetchRecordsDelegate delegate) {
      this.guids = guids;
      this.delegate = delegate;
    }

    public void run() {
      if (!confirmSessionActive()) {
        callbackReceiver.handleException(new InactiveSessionException(null));
        return;
      }

      if (guids == null || guids.length < 1) {
        Log.e(tag, "No guids sent to fetch");
        delegate.onFetchFailed(new InvalidRequestException(null));
      } else {
        delegate.onFetchSucceeded(fetchRecordsForGuids(guids));
      }
    }
  }

  private Record[] fetchRecordsForGuids(String[] guids) {
    Cursor cur = dbHelper.fetch(guids);
    ArrayList<BookmarkRecord> records = new ArrayList<BookmarkRecord>();
    cur.moveToFirst();
    while (!cur.isAfterLast()) {
      records.add(DBUtils.bookmarkFromMozCursor(cur));
      cur.moveToNext();
    }
    cur.close();

    Record[] recordArray = new Record[records.size()];
    records.toArray(recordArray);
    return recordArray;
  }

  @Override
  // Fetch all method and thread
  public void fetchAll(RepositorySessionFetchRecordsDelegate delegate) {
    FetchAllThread thread = new FetchAllThread(delegate);
    thread.start();
  }

  class FetchAllThread extends Thread {
    private RepositorySessionFetchRecordsDelegate delegate;

    public FetchAllThread(RepositorySessionFetchRecordsDelegate delegate) {
      this.delegate = delegate;
    }

    public void run() {
      if (!confirmSessionActive()) {
        callbackReceiver.handleException(new InactiveSessionException(null));
        return;
      }

      Cursor cur = dbHelper.fetchAllBookmarksOrderByAndroidId();
      ArrayList<BookmarkRecord> records = new ArrayList<BookmarkRecord>();
      cur.moveToFirst();
      while (!cur.isAfterLast()) {
        records.add(DBUtils.bookmarkFromMozCursor(cur));
        cur.moveToNext();
      }
      cur.close();

      Record[] recordArray = new Record[records.size()];
      records.toArray(recordArray);
      delegate.onFetchSucceeded(recordArray);
    }
  }

  // Store method and thread
  @Override
  public void store(Record record, RepositorySessionStoreDelegate receiver) {
    StoreThread thread = new StoreThread(record, receiver);
    thread.start();
  }

  class StoreThread extends Thread {
    private BookmarkRecord record;
    private RepositorySessionStoreDelegate callbackReceiver;

    public StoreThread(Record record, RepositorySessionStoreDelegate callbackReceiver) {
      if (record == null) {
        Log.e(tag, "Record sent to store was null");
        throw new IllegalArgumentException("record is null.");
      }
      this.record = (BookmarkRecord) record;
      this.callbackReceiver = callbackReceiver;
    }

    public void run() {
      if (!confirmSessionActive()) {
        callbackReceiver.onStoreFailed(new InactiveSessionException(null));
        return;
      }

      BookmarkRecord existingRecord;
      try {
        existingRecord = findExistingRecord();
      } catch (MultipleRecordsForGuidException e) {
        Log.e(tag, "Multiple records returned for given guid: " + record.guid);
        callbackReceiver.onStoreFailed(e);
        return;
      }
      
      // If the record is new and not deleted, store it
      if (existingRecord == null && !record.deleted) {
        dbHelper.insertBookmark((BookmarkRecord) record);
      } else if (existingRecord != null) {
        // Record exists already, need to figure out what to store

        if (existingRecord.lastModified > lastSyncTimestamp) {
          // Remote and local record have both been modified since since last sync
          BookmarkRecord store = reconcileBookmarks(existingRecord, record);
          dbHelper.deleteBookmark(existingRecord);
          dbHelper.insertBookmark(store);
        } else {
          // Only remote record modified, so take that one
          // (except for androidId which we obviously want to keep)
          record.androidID = existingRecord.androidID;

          // To keep things simple, we don't update, we delete then re-insert
          dbHelper.deleteBookmark(existingRecord);
          dbHelper.insertBookmark(record);
        }
      }

      // Invoke callback with result.
      callbackReceiver.onStoreSucceeded(record);
    }

    // Check if record already exists locally
    private BookmarkRecord findExistingRecord() throws MultipleRecordsForGuidException {
      Record[] records = fetchRecordsForGuids(new String[] { record.guid });
      if (records.length == 1) {
        return (BookmarkRecord) records[0];
      }
      else if (records.length > 1) {
        throw (new MultipleRecordsForGuidException(null));
      }
      return null;
    }

  }

  // Wipe method and thread.
  @Override
  public void wipe(RepositorySessionWipeDelegate delegate) {
    WipeThread thread = new WipeThread(delegate);
    thread.start();
  }

  class WipeThread extends Thread {
    private RepositorySessionWipeDelegate delegate;

    public WipeThread(RepositorySessionWipeDelegate delegate) {
      this.delegate = delegate;
    }

    public void run() {
      if (!confirmSessionActive()) {
        callbackReceiver.handleException(new InactiveSessionException(null));
        return;
      }
      dbHelper.wipe();
      delegate.onWipeSucceeded();
    }
  }

  private BookmarkRecord reconcileBookmarks(BookmarkRecord local, BookmarkRecord remote) {
    // Do modifications on local since we always want to keep guid and androidId from local

    // Determine which record is newer since this is the one we will take in case of conflict
    BookmarkRecord newer;
    if (local.lastModified > remote.lastModified) {
      newer = local;
    } else {
      newer = remote;
    }

    // Do dumb resolution for now and just return the newer one with the android id added if it wasn't the local one
    // Need to track changes (not implemented yet) in order to merge two changed bookmarks nicely
    newer.androidID = local.androidID;

    /*
    // Title
    if (!local.title.equals(remote.title)) {
      local.title = newer.title;
    }

    // URI
    if (!local.bookmarkURI.equals(remote.bookmarkURI)) {
      local.bookmarkURI = newer.bookmarkURI;
    }

    // Description
    if (!local.description.equals(remote.description)) {
      local.description = newer.description;
    }

    // Load in sidebar.
    if (local.loadInSidebar != remote.loadInSidebar) {
    }
    */

    return newer;
  }
}
