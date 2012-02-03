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
 *   Jason Voll <jvoll@mozilla.com>
 *   Richard Newman <rnewman@mozilla.com>
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

package org.mozilla.gecko.sync.repositories.android;

import java.util.ArrayList;
import java.util.HashMap;

import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.repositories.InactiveSessionException;
import org.mozilla.gecko.sync.repositories.InvalidRequestException;
import org.mozilla.gecko.sync.repositories.InvalidSessionTransitionException;
import org.mozilla.gecko.sync.repositories.MultipleRecordsForGuidException;
import org.mozilla.gecko.sync.repositories.NoGuidForIdException;
import org.mozilla.gecko.sync.repositories.NoStoreDelegateException;
import org.mozilla.gecko.sync.repositories.NullCursorException;
import org.mozilla.gecko.sync.repositories.ParentNotFoundException;
import org.mozilla.gecko.sync.repositories.ProfileDatabaseException;
import org.mozilla.gecko.sync.repositories.RecordFilter;
import org.mozilla.gecko.sync.repositories.Repository;
import org.mozilla.gecko.sync.repositories.StoreTrackingRepositorySession;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionBeginDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionFetchRecordsDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionGuidsSinceDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionWipeDelegate;
import org.mozilla.gecko.sync.repositories.domain.Record;

import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

/**
 * You'll notice that all delegate calls *either*:
 *
 * - request a deferred delegate with the appropriate work queue, then
 *   make the appropriate call, or
 * - create a Runnable which makes the appropriate call, and pushes it
 *   directly into the appropriate work queue.
 *
 * This is to ensure that all delegate callbacks happen off the current
 * thread. This provides lock safety (we don't enter another method that
 * might try to take a lock already taken in our caller), and ensures
 * that operations take place off the main thread.
 *
 * Don't do both -- the two approaches are equivalent -- and certainly
 * don't do neither unless you know what you're doing!
 *
 * Similarly, all store calls go through the appropriate store queue. This
 * ensures that store() and storeDone() consequences occur before-after.
 *
 * @author rnewman
 *
 */
public abstract class AndroidBrowserRepositorySession extends StoreTrackingRepositorySession {

  protected AndroidBrowserRepositoryDataAccessor dbHelper;
  public static final String LOG_TAG = "AndroidBrowserRepositorySession";
  private HashMap<String, String> recordToGuid;

  public AndroidBrowserRepositorySession(Repository repository) {
    super(repository);
  }

  /**
   * Override this.
   * Return null if this record should not be processed.
   *
   * @param cur
   * @return
   * @throws NoGuidForIdException
   * @throws NullCursorException
   * @throws ParentNotFoundException
   */
  protected abstract Record recordFromMirrorCursor(Cursor cur) throws NoGuidForIdException, NullCursorException, ParentNotFoundException;

  // Must be overriden by AndroidBookmarkRepositorySession.
  protected boolean checkRecordType(Record record) {
    return true;
  }

  /**
   * Override in subclass to implement record extension.
   * Return null if this record should not be processed.
   *
   * @param record
   *        The record to transform. Can be null.
   * @return The transformed record. Can be null.
   * @throws NullCursorException
   */
  protected Record transformRecord(Record record) throws NullCursorException {
    return record;
  }

  @Override
  public void begin(RepositorySessionBeginDelegate delegate) {
    RepositorySessionBeginDelegate deferredDelegate = delegate.deferredBeginDelegate(delegateQueue);
    try {
      super.sharedBegin();
    } catch (InvalidSessionTransitionException e) {
      deferredDelegate.onBeginFailed(e);
      return;
    }

    try {
      // We do this check here even though it results in one extra call to the DB
      // because if we didn't, we have to do a check on every other call since there
      // is no way of knowing which call would be hit first.
      checkDatabase();
    } catch (ProfileDatabaseException e) {
      Log.e(LOG_TAG, "ProfileDatabaseException from begin. Fennec must be launched once until this error is fixed");
      deferredDelegate.onBeginFailed(e);
      return;
    } catch (NullCursorException e) {
      deferredDelegate.onBeginFailed(e);
      return;
    } catch (Exception e) {
      deferredDelegate.onBeginFailed(e);
      return;
    }
    storeTracker = createStoreTracker();
    deferredDelegate.onBeginSucceeded(this);
  }

  protected abstract String buildRecordString(Record record);

  protected void checkDatabase() throws ProfileDatabaseException, NullCursorException {
    Utils.info(LOG_TAG, "BEGIN: checking database.");
    try {
      dbHelper.fetch(new String[] { "none" }).close();
      Utils.info(LOG_TAG, "END: checking database.");
    } catch (NullPointerException e) {
      throw new ProfileDatabaseException(e);
    }
  }

  @Override
  public void guidsSince(long timestamp, RepositorySessionGuidsSinceDelegate delegate) {
    GuidsSinceRunnable command = new GuidsSinceRunnable(timestamp, delegate);
    delegateQueue.execute(command);
  }

  class GuidsSinceRunnable implements Runnable {

    private RepositorySessionGuidsSinceDelegate delegate;
    private long                                timestamp;

    public GuidsSinceRunnable(long timestamp,
                              RepositorySessionGuidsSinceDelegate delegate) {
      this.timestamp = timestamp;
      this.delegate = delegate;
    }

    @Override
    public void run() {
      if (!isActive()) {
        delegate.onGuidsSinceFailed(new InactiveSessionException(null));
        return;
      }

      Cursor cur;
      try {
        cur = dbHelper.getGUIDsSince(timestamp);
      } catch (NullCursorException e) {
        delegate.onGuidsSinceFailed(e);
        return;
      } catch (Exception e) {
        delegate.onGuidsSinceFailed(e);
        return;
      }

      ArrayList<String> guids;
      try {
        if (!cur.moveToFirst()) {
          delegate.onGuidsSinceSucceeded(new String[] {});
          return;
        }
        guids = new ArrayList<String>();
        while (!cur.isAfterLast()) {
          guids.add(RepoUtils.getStringFromCursor(cur, "guid"));
          cur.moveToNext();
        }
      } finally {
        Log.d(LOG_TAG, "Closing cursor after guidsSince.");
        cur.close();
      }

      String guidsArray[] = new String[guids.size()];
      guids.toArray(guidsArray);
      delegate.onGuidsSinceSucceeded(guidsArray);
    }
  }

  @Override
  public void fetch(String[] guids,
                    RepositorySessionFetchRecordsDelegate delegate) {
    FetchRunnable command = new FetchRunnable(guids, now(), null, delegate);
    delegateQueue.execute(command);
  }

  abstract class FetchingRunnable implements Runnable {
    protected RepositorySessionFetchRecordsDelegate delegate;

    public FetchingRunnable(RepositorySessionFetchRecordsDelegate delegate) {
      this.delegate = delegate;
    }

    protected void fetchFromCursor(Cursor cursor, RecordFilter filter, long end) {
      Log.d(LOG_TAG, "Fetch from cursor:");
      try {
        try {
          if (!cursor.moveToFirst()) {
            delegate.onFetchCompleted(end);
            return;
          }
          while (!cursor.isAfterLast()) {
            Log.d(LOG_TAG, "... one more record.");
            Record r = recordFromMirrorCursor(cursor);
            if (r != null) {
              if (filter == null || !filter.excludeRecord(r)) {
                delegate.onFetchedRecord(transformRecord(r));
              } else {
                Log.d(LOG_TAG, "Filter says to skip record.");
              }
            }
            cursor.moveToNext();
          }
          delegate.onFetchCompleted(end);
        } catch (NoGuidForIdException e) {
          Log.w(LOG_TAG, "No GUID for ID.", e);
          delegate.onFetchFailed(e, null);
        } catch (Exception e) {
          Log.w(LOG_TAG, "Exception in fetchFromCursor.", e);
          delegate.onFetchFailed(e, null);
          return;
        }
      } finally {
        Log.d(LOG_TAG, "Closing cursor after fetch.");
        cursor.close();
      }
    }
  }

  class FetchRunnable extends FetchingRunnable {
    private String[] guids;
    private long     end;
    private RecordFilter filter;

    public FetchRunnable(String[] guids,
                         long end,
                         RecordFilter filter,
                         RepositorySessionFetchRecordsDelegate delegate) {
      super(delegate);
      this.guids  = guids;
      this.end    = end;
      this.filter = filter;
    }

    @Override
    public void run() {
      if (!isActive()) {
        delegate.onFetchFailed(new InactiveSessionException(null), null);
        return;
      }

      if (guids == null || guids.length < 1) {
        Log.e(LOG_TAG, "No guids sent to fetch");
        delegate.onFetchFailed(new InvalidRequestException(null), null);
        return;
      }

      try {
        Cursor cursor = dbHelper.fetch(guids);
        this.fetchFromCursor(cursor, filter, end);
      } catch (NullCursorException e) {
        delegate.onFetchFailed(e, null);
      }
    }
  }

  @Override
  public void fetchSince(long timestamp,
                         RepositorySessionFetchRecordsDelegate delegate) {
    if (this.storeTracker == null) {
      throw new IllegalStateException("Store tracker not yet initialized!");
    }

    Log.i(LOG_TAG, "Running fetchSince(" + timestamp + ").");
    FetchSinceRunnable command = new FetchSinceRunnable(timestamp, now(), this.storeTracker.getFilter(), delegate);
    delegateQueue.execute(command);
  }

  class FetchSinceRunnable extends FetchingRunnable {
    private long since;
    private long end;
    private RecordFilter filter;

    public FetchSinceRunnable(long since,
                              long end,
                              RecordFilter filter,
                              RepositorySessionFetchRecordsDelegate delegate) {
      super(delegate);
      this.since  = since;
      this.end    = end;
      this.filter = filter;
    }

    @Override
    public void run() {
      if (!isActive()) {
        delegate.onFetchFailed(new InactiveSessionException(null), null);
        return;
      }

      try {
        Cursor cursor = dbHelper.fetchSince(since);
        this.fetchFromCursor(cursor, filter, end);
      } catch (NullCursorException e) {
        delegate.onFetchFailed(e, null);
        return;
      }
    }
  }

  @Override
  public void fetchAll(RepositorySessionFetchRecordsDelegate delegate) {
    this.fetchSince(0, delegate);
  }

  @Override
  public void store(final Record record) throws NoStoreDelegateException {
    if (delegate == null) {
      throw new NoStoreDelegateException();
    }
    if (record == null) {
      Log.e(LOG_TAG, "Record sent to store was null");
      throw new IllegalArgumentException("Null record passed to AndroidBrowserRepositorySession.store().");
    }

    // Store Runnables *must* complete synchronously. It's OK, they
    // run on a background thread.
    Runnable command = new Runnable() {

      @Override
      public void run() {
        if (!isActive()) {
          delegate.onRecordStoreFailed(new InactiveSessionException(null));
          return;
        }

        // Check that the record is a valid type.
        // Fennec only supports bookmarks and folders. All other types of records,
        // including livemarks and queries, are simply ignored.
        // See Bug 708149. This might be resolved by Fennec changing its database
        // schema, or by Sync storing non-applied records in its own private database.
        if (!checkRecordType(record)) {
          Log.d(LOG_TAG, "Ignoring record " + record.guid + " due to unknown record type.");

          // Don't throw: we don't want to abort the entire sync when we get a livemark!
          // delegate.onRecordStoreFailed(new InvalidBookmarkTypeException(null));
          return;
        }


        // TODO: lift these into the session.
        // Temporary: this matches prior syncing semantics, in which only
        // the relationship between the local and remote record is considered.
        // In the future we'll track these two timestamps and use them to
        // determine which records have changed, and thus process incoming
        // records more efficiently.
        long lastLocalRetrieval  = 0;      // lastSyncTimestamp?
        long lastRemoteRetrieval = 0;      // TODO: adjust for clock skew.
        boolean remotelyModified = record.lastModified > lastRemoteRetrieval;

        Record existingRecord;
        try {
          // GUID matching only: deleted records don't have a payload with which to search.
          existingRecord = recordForGUID(record.guid);
          if (record.deleted) {
            if (existingRecord == null) {
              // We're done. Don't bother with a callback. That can change later
              // if we want it to.
              trace("Incoming record " + record.guid + " is deleted, and no local version. Bye!");
              return;
            }

            if (existingRecord.deleted) {
              trace("Local record already deleted. Bye!");
              return;
            }

            // Which one wins?
            if (!remotelyModified) {
              trace("Ignoring deleted record from the past.");
              return;
            }

            boolean locallyModified = existingRecord.lastModified > lastLocalRetrieval;
            if (!locallyModified) {
              trace("Remote modified, local not. Deleting.");
              storeRecordDeletion(record);
              return;
            }

            trace("Both local and remote records have been modified.");
            if (record.lastModified > existingRecord.lastModified) {
              trace("Remote is newer, and deleted. Deleting local.");
              storeRecordDeletion(record);
              return;
            }

            trace("Remote is older, local is not deleted. Ignoring.");
            if (!locallyModified) {
              Log.w(LOG_TAG, "Inconsistency: old remote record is deleted, but local record not modified!");
              // Ensure that this is tracked for upload.
            }
            return;
          }
          // End deletion logic.

          // Now we're processing a non-deleted incoming record.
          if (existingRecord == null) {
            trace("Looking up match for record " + record.guid);
            existingRecord = findExistingRecord(record);
          }

          if (existingRecord == null) {
            // The record is new.
            trace("No match. Inserting.");
            Record inserted = insert(record);
            trackRecord(inserted);
            delegate.onRecordStoreSucceeded(inserted);
            return;
          }

          // We found a local dupe.
          trace("Incoming record " + record.guid + " dupes to local record " + existingRecord.guid);

          // Populate more expensive fields prior to reconciling.
          existingRecord = transformRecord(existingRecord);
          Record toStore = reconcileRecords(record, existingRecord, lastRemoteRetrieval, lastLocalRetrieval);

          if (toStore == null) {
            Log.d(LOG_TAG, "Reconciling returned null. Not inserting a record.");
            return;
          }

          // TODO: pass in timestamps?
          Log.d(LOG_TAG, "Replacing " + existingRecord.guid + " with record " + toStore.guid);
          Record replaced = replace(toStore, existingRecord);

          // Note that we don't track records here; deciding that is the job
          // of reconcileRecords.
          Log.d(LOG_TAG, "Calling delegate callback with guid " + replaced.guid +
                         "(" + replaced.androidID + ")");
          delegate.onRecordStoreSucceeded(replaced);
          return;

        } catch (MultipleRecordsForGuidException e) {
          Log.e(LOG_TAG, "Multiple records returned for given guid: " + record.guid);
          delegate.onRecordStoreFailed(e);
          return;
        } catch (NoGuidForIdException e) {
          Log.e(LOG_TAG, "Store failed for " + record.guid, e);
          delegate.onRecordStoreFailed(e);
          return;
        } catch (NullCursorException e) {
          Log.e(LOG_TAG, "Store failed for " + record.guid, e);
          delegate.onRecordStoreFailed(e);
          return;
        } catch (Exception e) {
          Log.e(LOG_TAG, "Store failed for " + record.guid, e);
          delegate.onRecordStoreFailed(e);
          return;
        }
      }
    };
    storeWorkQueue.execute(command);
  }

  protected void storeRecordDeletion(final Record record) {
    // TODO: we ought to mark the record as deleted rather than deleting it,
    // in order to support syncing to multiple destinations. Bug 722607.
    dbHelper.delete(record);      // TODO: mm?
    delegate.onRecordStoreSucceeded(record);
  }

  protected Record insert(Record record) throws NoGuidForIdException, NullCursorException, ParentNotFoundException {
    Record toStore = prepareRecord(record);
    Uri recordURI = dbHelper.insert(toStore);
    long id = RepoUtils.getAndroidIdFromUri(recordURI);
    Log.d(LOG_TAG, "Inserted as " + id);

    toStore.androidID = id;
    updateBookkeeping(toStore);
    Log.d(LOG_TAG, "insert() returning record " + toStore.guid);
    return toStore;
  }

  protected Record replace(Record newRecord, Record existingRecord) throws NoGuidForIdException, NullCursorException, ParentNotFoundException {
    Record toStore = prepareRecord(newRecord);

    // newRecord should already have suitable androidID and guid.
    dbHelper.update(existingRecord.guid, toStore);
    updateBookkeeping(toStore);
    Log.d(LOG_TAG, "replace() returning record " + toStore.guid);
    return toStore;
  }

  protected Record recordForGUID(String guid) throws
                                             NoGuidForIdException,
                                             NullCursorException,
                                             ParentNotFoundException,
                                             MultipleRecordsForGuidException {
    Cursor cursor = dbHelper.fetch(new String[] { guid });
    try {
      if (!cursor.moveToFirst()) {
        return null;
      }

      Record r = recordFromMirrorCursor(cursor);

      cursor.moveToNext();
      if (cursor.isAfterLast()) {
        // Got one record!
        return r; // Not transformed.
      }

      // More than one. Oh dear.
      throw (new MultipleRecordsForGuidException(null));
    } finally {
      cursor.close();
    }
  }

  /**
   * Attempt to find an equivalent record through some means other than GUID.
   *
   * @param record
   *        The record for which to search.
   * @return
   *        An equivalent Record object, or null if none is found.
   *
   * @throws MultipleRecordsForGuidException
   * @throws NoGuidForIdException
   * @throws NullCursorException
   * @throws ParentNotFoundException
   */
  protected Record findExistingRecord(Record record) throws MultipleRecordsForGuidException,
    NoGuidForIdException, NullCursorException, ParentNotFoundException {

    Log.d(LOG_TAG, "Finding existing record for incoming record with GUID " + record.guid);
    String recordString = buildRecordString(record);
    Log.d(LOG_TAG, "Searching with record string " + recordString);
    String guid = getRecordToGuidMap().get(recordString);
    if (guid != null) {
      Log.d(LOG_TAG, "Found one. Returning computed record.");
      return recordForGUID(guid);
    }
    Log.d(LOG_TAG, "findExistingRecord failed to find one for " + record.guid);
    return null;
  }

  public HashMap<String, String> getRecordToGuidMap() throws NoGuidForIdException, NullCursorException, ParentNotFoundException {
    if (recordToGuid == null) {
      createRecordToGuidMap();
    }
    return recordToGuid;
  }

  private void createRecordToGuidMap() throws NoGuidForIdException, NullCursorException, ParentNotFoundException {
    Utils.info(LOG_TAG, "BEGIN: creating record -> GUID map.");
    recordToGuid = new HashMap<String, String>();
    Cursor cur = dbHelper.fetchAll();
    try {
      if (!cur.moveToFirst()) {
        return;
      }
      while (!cur.isAfterLast()) {
        Record record = recordFromMirrorCursor(cur);
        if (record != null) {
          recordToGuid.put(buildRecordString(record), record.guid);
        }
        cur.moveToNext();
      }
    } finally {
      cur.close();
    }
    Utils.info(LOG_TAG, "END: creating record -> GUID map.");
  }

  public void putRecordToGuidMap(String recordString, String guid) throws NoGuidForIdException, NullCursorException, ParentNotFoundException {
    if (recordToGuid == null) {
      createRecordToGuidMap();
    }
    recordToGuid.put(recordString, guid);
  }

  protected abstract Record prepareRecord(Record record);
  protected void updateBookkeeping(Record record) throws NoGuidForIdException,
                                                 NullCursorException,
                                                 ParentNotFoundException {
    putRecordToGuidMap(buildRecordString(record), record.guid);
  }

  // Wipe method and thread.
  @Override
  public void wipe(RepositorySessionWipeDelegate delegate) {
    Runnable command = new WipeRunnable(delegate);
    storeWorkQueue.execute(command);
  }

  class WipeRunnable implements Runnable {
    private RepositorySessionWipeDelegate delegate;

    public WipeRunnable(RepositorySessionWipeDelegate delegate) {
      this.delegate = delegate;
    }

    public void run() {
      if (!isActive()) {
        delegate.onWipeFailed(new InactiveSessionException(null));
        return;
      }
      dbHelper.wipe();
      delegate.onWipeSucceeded();
    }
  }
}
