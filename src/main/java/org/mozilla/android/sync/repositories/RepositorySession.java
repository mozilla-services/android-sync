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
 * Richard Newman <rnewman@mozilla.com>
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

package org.mozilla.android.sync.repositories;

import java.util.ArrayList;

import org.mozilla.android.sync.repositories.android.AndroidBrowserRepositoryDatabaseHelper;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionBeginDelegate;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionCreationDelegate;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionFetchRecordsDelegate;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionFinishDelegate;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionGuidsSinceDelegate;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionStoreDelegate;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionWipeDelegate;
import org.mozilla.android.sync.repositories.domain.Record;

import android.database.Cursor;
import android.util.Log;

public abstract class RepositorySession {
  
  public enum SessionStatus {
    UNSTARTED,
    ACTIVE,
    DONE
  }

  private static final String tag = "RepositorySession";
  protected SessionStatus status = SessionStatus.UNSTARTED;
  protected Repository repository;
  protected RepositorySessionCreationDelegate callbackReceiver;
  protected AndroidBrowserRepositoryDatabaseHelper dbHelper;

  // The time that the last sync on this collection completed, in milliseconds.
  protected long lastSyncTimestamp;
  protected long syncBeginTimestamp;

  public RepositorySession(Repository repository, RepositorySessionCreationDelegate delegate, long lastSyncTimestamp) {
    this.repository = repository;
    this.callbackReceiver = delegate;
    this.lastSyncTimestamp = lastSyncTimestamp;
  }

  // guids since method and thread
  public void guidsSince(long timestamp,
      RepositorySessionGuidsSinceDelegate delegate) {
    GuidsSinceThread thread = new GuidsSinceThread(timestamp, delegate,
        dbHelper);
    thread.start();
  }

  class GuidsSinceThread extends Thread {

    private long                                timestamp;
    private RepositorySessionGuidsSinceDelegate delegate;
    private AndroidBrowserRepositoryDatabaseHelper            dbHelper;

    public GuidsSinceThread(long timestamp,
        RepositorySessionGuidsSinceDelegate delegate,
        AndroidBrowserRepositoryDatabaseHelper dbHelper) {
      this.timestamp = timestamp;
      this.delegate = delegate;
      this.dbHelper = dbHelper;
    }

    public void run() {
      if (!confirmSessionActive()) {
        delegate.onGuidsSinceFailed(new InactiveSessionException(null));
        return;
      }

      Cursor cur = dbHelper.getGUIDSSince(timestamp);
      int index = cur.getColumnIndex(AndroidBrowserRepositoryDatabaseHelper.COL_GUID);

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

  protected abstract Record[] compileIntoRecordsArray(Cursor cur);
  
  // Fetch since method and thread
  public void fetchSince(long timestamp,
      RepositorySessionFetchRecordsDelegate delegate) {
    FetchSinceThread thread = new FetchSinceThread(timestamp, delegate);
    thread.start();
  }

  class FetchSinceThread extends Thread {

    private long                                  timestamp;
    private RepositorySessionFetchRecordsDelegate delegate;

    public FetchSinceThread(long timestamp,
        RepositorySessionFetchRecordsDelegate delegate) {
      this.timestamp = timestamp;
      this.delegate = delegate;
    }

    public void run() {
      if (!confirmSessionActive()) {
        delegate.onFetchFailed(new InactiveSessionException(null));
        return;
      }

      Cursor cur = dbHelper.fetchSince(timestamp);
      delegate.onFetchSucceeded(compileIntoRecordsArray(cur));
    }
  }

  // Fetch method and thread
  public void fetch(String[] guids,
      RepositorySessionFetchRecordsDelegate delegate) {
    FetchThread thread = new FetchThread(guids, delegate);
    thread.start();
  }

  class FetchThread extends Thread {
    private String[]                              guids;
    private RepositorySessionFetchRecordsDelegate delegate;

    public FetchThread(String[] guids,
        RepositorySessionFetchRecordsDelegate delegate) {
      this.guids = guids;
      this.delegate = delegate;
    }

    public void run() {
      if (!confirmSessionActive()) {
        delegate.onFetchFailed(new InactiveSessionException(null));
        return;
      }

      if (guids == null || guids.length < 1) {
        Log.e(tag, "No guids sent to fetch");
        delegate.onFetchFailed(new InvalidRequestException(null));
      } else {
        delegate.onFetchSucceeded(doFetch(guids));
      }
    }
  }
  
  private Record[] doFetch(String[]guids) {
    Cursor cur = dbHelper.fetch(guids);
    return compileIntoRecordsArray(cur);
  }
  
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
        delegate.onFetchFailed(new InactiveSessionException(null));
        return;
      }

      Cursor cur = dbHelper.fetchAllOrderByAndroidId();
      delegate.onFetchSucceeded(compileIntoRecordsArray(cur));
    }
  }

  // Store method and thread
  public void store(Record record, RepositorySessionStoreDelegate delegate) {
    StoreThread thread = new StoreThread(record, delegate);
    thread.start();
  }

  class StoreThread extends Thread {
    private Record                         record;
    private RepositorySessionStoreDelegate delegate;

    public StoreThread(Record record, RepositorySessionStoreDelegate delegate) {
      if (record == null) {
        Log.e(tag, "Record sent to store was null");
        throw new IllegalArgumentException("record is null.");
      }
      this.record = record;
      this.delegate = delegate;
    }

    public void run() {
      if (!confirmSessionActive()) {
        delegate.onStoreFailed(new InactiveSessionException(null));
        return;
      }

      Record existingRecord;
      try {
        existingRecord = findExistingRecord();
      } catch (MultipleRecordsForGuidException e) {
        Log.e(tag, "Multiple records returned for given guid: " + record.guid);
        delegate.onStoreFailed(e);
        return;
      }

      // If the record is new and not deleted, store it
      if (existingRecord == null && !record.deleted) {
        dbHelper.insert(record);
      } else if (existingRecord != null) {
        // Record exists already, need to figure out what to store
        Record store = reconcileRecords(existingRecord, record);
        dbHelper.delete(existingRecord);
        dbHelper.insert(store);
      }

      // Invoke callback with result.
      delegate.onStoreSucceeded(record);
    }
    
    // Check if record already exists locally
    private Record findExistingRecord()
        throws MultipleRecordsForGuidException {
      Record[] records = doFetch(new String[] { record.guid });
      if (records.length == 1) {
        return records[0];
      } else if (records.length > 1) {
        throw (new MultipleRecordsForGuidException(null));
      }
      return null;
    }

  }
  
  protected abstract Record reconcileRecords(Record local, Record remote);

  // Wipe method and thread.
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
        delegate.onWipeFailed(new InactiveSessionException(null));
        return;
      }
      dbHelper.wipe();
      delegate.onWipeSucceeded();
    }
  }
  
  public void begin(RepositorySessionBeginDelegate delegate) {
     if (this.status == SessionStatus.UNSTARTED) {
      this.status = SessionStatus.ACTIVE;
      this.syncBeginTimestamp = System.currentTimeMillis();
      delegate.onBeginSucceeded();
    } else {
      Log.e(tag, "Tried to begin() an already active or finished session");
      delegate.onBeginFailed(new InvalidSessionTransitionException(null));
    }
  }

  public void finish(RepositorySessionFinishDelegate delegate) {
    if (this.status == SessionStatus.ACTIVE) {
      this.status = SessionStatus.DONE;
      delegate.onFinishSucceeded();
    } else {
      Log.e(tag, "Tried to finish() an unstarted or already finished session");
      delegate.onFinishFailed(new InvalidSessionTransitionException(null));
    }
  }
 
  protected boolean confirmSessionActive() {
    return status == SessionStatus.ACTIVE ? true : false;
  }
}
