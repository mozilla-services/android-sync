/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.repositories.android;

import java.util.ArrayList;
import java.util.List;

import org.mozilla.gecko.db.BrowserContract;
import org.mozilla.gecko.db.BrowserContract.DeletedColumns;
import org.mozilla.gecko.db.BrowserContract.DeletedPasswords;
import org.mozilla.gecko.db.BrowserContract.Passwords;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.repositories.InactiveSessionException;
import org.mozilla.gecko.sync.repositories.InvalidRequestException;
import org.mozilla.gecko.sync.repositories.NoStoreDelegateException;
import org.mozilla.gecko.sync.repositories.NullCursorException;
import org.mozilla.gecko.sync.repositories.RecordFilter;
import org.mozilla.gecko.sync.repositories.Repository;
import org.mozilla.gecko.sync.repositories.StoreTrackingRepositorySession;
import org.mozilla.gecko.sync.repositories.android.RepoUtils.QueryHelper;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionCreationDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionFetchRecordsDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionFinishDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionGuidsSinceDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionWipeDelegate;
import org.mozilla.gecko.sync.repositories.domain.PasswordRecord;
import org.mozilla.gecko.sync.repositories.domain.Record;

import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;

public class PasswordsRepositorySession extends
    StoreTrackingRepositorySession {

  public static class PasswordsRepository extends Repository {
    @Override
    public void createSession(RepositorySessionCreationDelegate delegate,
        Context context) {
      PasswordsRepositorySession session = new PasswordsRepositorySession(PasswordsRepository.this, context);
      final RepositorySessionCreationDelegate deferredCreationDelegate = delegate.deferredCreationDelegate();
      deferredCreationDelegate.onSessionCreated(session);
    }
  }

  private static final String LOG_TAG = "PasswordsRepoSession";
  private static String COLLECTION = "passwords";
  private RepoUtils.QueryHelper passwordsHelper;
  private RepoUtils.QueryHelper deletedPasswordsHelper;
  private ContentProviderClient passwordsProvider;

  private final Context context;

  public PasswordsRepositorySession(Repository repository, Context context) {
    super(repository);
    this.context = context;
    passwordsProvider = context.getContentResolver().acquireContentProviderClient(BrowserContract.PASSWORDS_AUTHORITY_URI);
    passwordsHelper = new QueryHelper(context, getDataUri(), LOG_TAG);
    deletedPasswordsHelper = new QueryHelper(context, getDeletedUri(), LOG_TAG);
    dumpDbs();
  }

  @Override
  public void guidsSince(final long timestamp,
      final RepositorySessionGuidsSinceDelegate delegate) {
     Runnable guidsSinceRunnable = new Runnable() {
     @Override
     public void run() {
        if (!isActive()) {
          delegate.onGuidsSinceFailed(new InactiveSessionException(null));
          return;
        }

        // Checks succeeded, now get guids.
        List<String> guids = new ArrayList<String>();
        Cursor cursor = null;
        try {
          // Fetch guids from data table.
          Logger.debug(LOG_TAG, "Fetching guidsSince from data table.");

          String[] guidCols = new String[] { Passwords.GUID };
          cursor = passwordsHelper.safeQuery(passwordsProvider, ".getGUIDsSince", guidCols, dateModifiedWhere(timestamp), null, null);
          if (cursor != null && cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
              guids.add(RepoUtils.getStringFromCursor(cursor, Passwords.GUID));
              cursor.moveToNext();
            }
            cursor.close();
          }

          // Fetch guids from deleted table.
          Logger.debug(LOG_TAG, "Fetching guidsSince from deleted table.");

          String[] deletedGuidCols = new String[] {DeletedColumns.GUID };
          cursor = deletedPasswordsHelper.safeQuery(passwordsProvider, ".getGUIDsSince", deletedGuidCols, dateModifiedWhereDeleted(timestamp), null, null);
          if (cursor != null && cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
              guids.add(RepoUtils.getStringFromCursor(cursor, DeletedColumns.GUID));
              cursor.moveToNext();
            }
          }
        } catch (NullCursorException e) {
          Logger.error(LOG_TAG, "Null cursor in fetch.");
          delegate.onGuidsSinceFailed(e);
        } catch (Exception e) {
          Logger.error(LOG_TAG, "Exception in fetch.");
          delegate.onGuidsSinceFailed(e);
        } finally {
          Logger.info(LOG_TAG, "Closing cursor after fetch.");
          if (cursor != null) {
            cursor.close();
          }
        }
        String[] guidStrings = new String[guids.size()];
        delegate.onGuidsSinceSucceeded(guids.toArray(guidStrings));
      }
    };
    storeWorkQueue.execute(guidsSinceRunnable);
  }

  @Override
  public void fetchSince(final long timestamp,
      final RepositorySessionFetchRecordsDelegate delegate) {

    if (!isActive()) {
      delegate.onFetchFailed(new InactiveSessionException(null), null);
      return;
    }

    // Checks succeeded, now fetch.
    final RecordFilter filter = this.storeTracker.getFilter();
    Runnable fetchSinceRunnable = new Runnable() {
      @Override
      public void run() {
        Cursor cursor = null;
        long end = now();
        try {
          // Fetch from data table.
          cursor = passwordsHelper.safeQuery(passwordsProvider, ".fetchSince",
              getAllColumns(), dateModifiedWhere(timestamp), null, null);
          fetchFromCursorDeleted(cursor, false, filter, delegate);
          cursor.close();

          // Fetch from deleted table.
          cursor = deletedPasswordsHelper.safeQuery(passwordsProvider, ".fetchSince",
              getAllDeletedColumns(), dateModifiedWhereDeleted(timestamp), null, null);
          fetchFromCursorDeleted(cursor, true, filter, delegate);
          delegate.onFetchCompleted(end);

        } catch (NullCursorException e) {
          Logger.error(LOG_TAG, "Null cursor in fetch.");
          delegate.onFetchFailed(e, null);
        } catch (Exception e) {
          Logger.error(LOG_TAG, "Exception in fetch.");
          delegate.onFetchFailed(e, null);
        } finally {
          Logger.info(LOG_TAG, "Closing cursor after fetch.");
          if (cursor != null) {
            cursor.close();
          }
        }
      }
    };
    storeWorkQueue.execute(fetchSinceRunnable);
  }

  @Override
  public void fetch(final String[] guids,
      final RepositorySessionFetchRecordsDelegate delegate) {

    if (!isActive()) {
      delegate.onFetchFailed(new InactiveSessionException(null), null);
      return;
    }
    if (guids == null || guids.length < 1) {
      Logger.error(LOG_TAG, "No guids to be fetched.");
      delegate.onFetchFailed(new InvalidRequestException(), null);
      return;
    }

    // Checks succeeded, now fetch.
    final RecordFilter filter = this.storeTracker.getFilter();
    Runnable fetchRunnable = new Runnable() {
      @Override
      public void run() {
        Cursor cursor = null;
        long end = now();
        String where = computeSQLInClause(guids.length, "guid");
        Logger.debug(LOG_TAG, "Fetch guids where: " + where);
        try {
          // Fetch records from data table.
          cursor = passwordsHelper.safeQuery(passwordsProvider, ".fetch",
              getAllColumns(), where, guids, null);
          fetchFromCursorDeleted(cursor, false, filter, delegate);
          cursor.close();

          // Fetch records from deleted table.
          cursor = deletedPasswordsHelper.safeQuery(passwordsProvider, ".fetch",
              getAllDeletedColumns(), where, guids, null);
          fetchFromCursorDeleted(cursor, true, filter, delegate);
          delegate.onFetchCompleted(end);

        } catch (NullCursorException e) {
          Logger.error(LOG_TAG, "Null cursor in fetch.");
          delegate.onFetchFailed(e, null);
        } catch (Exception e) {
          Logger.error(LOG_TAG, "Exception in fetch.");
          delegate.onFetchFailed(e, null);
        } finally {
          Logger.info(LOG_TAG, "Closing cursor after fetch.");
          if (cursor != null) {
            cursor.close();
          }
        }
      }
    };
    storeWorkQueue.execute(fetchRunnable);
  }


  @Override
  public void fetchAll(RepositorySessionFetchRecordsDelegate delegate) {
    fetchSince(0, delegate);
  }

  // TODO: lift out of AndroidBrowserRepoSession. Used pretty much wholesale.
  @Override
  public void store(final Record record) throws NoStoreDelegateException {
    if (delegate == null) {
      Logger.error(LOG_TAG, "No store delegate.");
      throw new NoStoreDelegateException();
    }
    if (record == null) {
      Logger.error(LOG_TAG, "Record sent to store was null.");
      throw new IllegalArgumentException("Null record passed to PasswordsRepositorySession.store().");
    }
    if (!(record instanceof PasswordRecord)) {
      Logger.error(LOG_TAG, "Can't store anything but a PasswordRecord.");
      throw new IllegalArgumentException("Non-PasswordRecord passed to PasswordsRepositorySession.store().");
    }

    final PasswordRecord remoteRecord = (PasswordRecord) record;

    Runnable storeRunnable = new Runnable() {
      @Override
      public void run() {
        if (!isActive()) {
          Logger.warn(LOG_TAG, "RepositorySession is inactive. Store failing.");
          delegate.onRecordStoreFailed(new InactiveSessionException(null));
          return;
        }

        String guid = remoteRecord.guid;
        if (guid == null) {
          delegate.onRecordStoreFailed(new RuntimeException("Can't store record with null GUID."));
          return;
        }

        PasswordRecord existingRecord = retrieveByGuid(guid);
        long lastLocalRetrieval  = 0;      // lastSyncTimestamp?
        long lastRemoteRetrieval = 0;      // TODO: adjust for clock skew.
        boolean remotelyModified = remoteRecord.lastModified > lastRemoteRetrieval;

        // Check deleted state first.
        if (remoteRecord.deleted) {
          if (existingRecord == null) {
            // Do nothing, record does not exist anyways.
            Logger.info(LOG_TAG, "Incoming record " + remoteRecord.guid + " is deleted, and no local version.");
            return;
          }

          if (existingRecord.deleted) {
            // Record is already tracked as deleted. Delete from local.
            storeRecordDeletion(existingRecord); // different from ABRepoSess.
            Logger.info(LOG_TAG, "Incoming record " + remoteRecord.guid + " and local are both deleted.");
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
            storeRecordDeletion(remoteRecord);
            return;
          }

          trace("Both local and remote records have been modified.");
          if (remoteRecord.lastModified > existingRecord.lastModified) {
            trace("Remote is newer, and deleted. Deleting local.");
            storeRecordDeletion(remoteRecord);
            return;
          }

          trace("Remote is older, local is not deleted. Ignoring.");
          if (!locallyModified) {
            Logger.warn(LOG_TAG, "Inconsistency: old remote record is deleted, but local record not modified!");
            // Ensure that this is tracked for upload.
          }
          return;
        }
        // End deletion logic.

        // Now we're processing a non-deleted incoming record.
        if (existingRecord == null) {
          trace("Looking up match for record " + remoteRecord.guid);
          existingRecord = findExistingRecord(remoteRecord);
        }

        if (existingRecord == null) {
          // The record is new.
          trace("No match. Inserting.");
          Logger.debug(LOG_TAG, "Didn't find matching record. Inserting.");
          Record inserted = null;
          try {
            inserted = insert(remoteRecord);
          } catch (RemoteException e) {
            Logger.debug(LOG_TAG, "Record insert caused a RemoteException.");
            delegate.onRecordStoreFailed(e);
            return;
          }
          trackRecord(inserted);
          delegate.onRecordStoreSucceeded(inserted);
          return;
        }

        // We found a local dupe.
        trace("Incoming record " + remoteRecord.guid + " dupes to local record " + existingRecord.guid);
        Logger.debug(LOG_TAG, "remote " + remoteRecord + " dupes to " + existingRecord);
        Record toStore = reconcileRecords(remoteRecord, existingRecord, lastRemoteRetrieval, lastLocalRetrieval);

        if (toStore == null) {
          Logger.debug(LOG_TAG, "Reconciling returned null. Not inserting a record.");
          return;
        }

        // TODO: pass in timestamps?
        Logger.debug(LOG_TAG, "Replacing " + existingRecord.guid + " with record " + toStore.guid);
        Logger.debug(LOG_TAG, "existing: " + existingRecord);
        Logger.debug(LOG_TAG, "toStore: " + toStore);
        Record replaced = null;
        try {
          replaced = replace(existingRecord, toStore);
        } catch (RemoteException e) {
          Logger.debug(LOG_TAG, "Record replace caused a RemoteException.");
          delegate.onRecordStoreFailed(e);
          return;
        }

        // Note that we don't track records here; deciding that is the job
        // of reconcileRecords.
        Logger.debug(LOG_TAG, "Calling delegate callback with guid " + replaced.guid +
                              "(" + replaced.androidID + ")");
        delegate.onRecordStoreSucceeded(replaced);
        return;
      }
    };
    storeWorkQueue.execute(storeRunnable);
  }

  @Override
  public void wipe(final RepositorySessionWipeDelegate delegate) {
    Logger.info(LOG_TAG, "Wiping " + getDataUri() + ", " + getDeletedUri());

    Runnable wipeRunnable = new Runnable() {
      @Override
      public void run() {
        if (!isActive()) {
          delegate.onWipeFailed(new InactiveSessionException(null));
        } else {
          // Wipe both data and deleted.
          context.getContentResolver().delete(getDataUri(), null, null);
          context.getContentResolver().delete(getDeletedUri(), null, null);
          delegate.onWipeSucceeded();
        }
      }
    };
    storeWorkQueue.execute(wipeRunnable);
  }

  @Override
  public void abort() {
    passwordsProvider.release();
    super.abort();
  }

  @Override
  public void finish(final RepositorySessionFinishDelegate delegate) throws InactiveSessionException {
    passwordsProvider.release();
    super.finish(delegate);
  }

  public void deleteGuid(String guid) {
    String wherePasswords  = Passwords.GUID + " = ?";
    String whereDeleted = DeletedPasswords.GUID + " = ?";
    String[] args = new String[] { guid };

    try {
      int deleted = passwordsProvider.delete(BrowserContractHelpers.PASSWORDS_CONTENT_URI, wherePasswords, args);
      deleted += passwordsProvider.delete(BrowserContractHelpers.DELETED_PASSWORDS_CONTENT_URI, whereDeleted, args);
      if (deleted == 1) {
        return;
      }
      Logger.warn(LOG_TAG, "Unexpectedly deleted " + deleted + " rows for guid " + guid);
    } catch (RemoteException e) {
      Logger.error(LOG_TAG, "Remote Exception in password delete.");
      delegate.onRecordStoreFailed(e);
    }
  }
  /**
   * Insert record and return the record with its updated androidId set.
   * @param record
   * @return
   * @throws RemoteException
   */
  public PasswordRecord insert(PasswordRecord record) throws RemoteException {
    record.timePasswordChanged = now();
    // TODO: are these necessary for Fennec autocomplete?
    // record.timesUsed = 1;
    // record.timeLastUsed = now();
    ContentValues cv = getContentValues(record);
    Uri insertedUri = passwordsProvider.insert(getDataUri(), cv);
    record.androidID = RepoUtils.getAndroidIdFromUri(insertedUri);
    return record;
  }

  public Record replace(Record origRecord, Record newRecord) throws RemoteException {
    PasswordRecord newPasswordRecord = (PasswordRecord) newRecord;
    PasswordRecord origPasswordRecord = (PasswordRecord) origRecord;
    String where  = Passwords.GUID + " = ?";
    String[] args = new String[] { origRecord.guid };

    propagateTimes(newPasswordRecord, origPasswordRecord);
    ContentValues cv = getContentValues(newPasswordRecord);
    int updated = context.getContentResolver().update(getDataUri(), cv, where, args);
    if (updated != 1) {
      Logger.warn(LOG_TAG, "Unexpectedly updated " + updated + " rows for guid " + origPasswordRecord.guid);
    }
    return newRecord;
  }

  // When replacing a record, propagate the times.
  private void propagateTimes(PasswordRecord toRecord, PasswordRecord fromRecord) {
    toRecord.timePasswordChanged = now();
    toRecord.timeCreated = fromRecord.timeCreated;
    toRecord.timeLastUsed = fromRecord.timeLastUsed;
    toRecord.timesUsed = fromRecord.timesUsed;
  }

  // Helper Functions.
  private Uri getDataUri() {
    return BrowserContractHelpers.PASSWORDS_CONTENT_URI;
  }

  private Uri getDeletedUri() {
    return BrowserContractHelpers.DELETED_PASSWORDS_CONTENT_URI;
  }

  private String[] getAllColumns() {
    return BrowserContractHelpers.PasswordColumns;
  }

  private String[] getAllDeletedColumns() {
    return BrowserContractHelpers.DeletedColumns;
  }

  /**
   * Constructs the DB query string for entry age for deleted records.
   *
   * @param timestamp
   * @return String DB query string for dates to fetch.
   */
  private String dateModifiedWhereDeleted(long timestamp) {
    return DeletedColumns.TIME_DELETED + " >= " + Long.toString(timestamp);
  }

  /**
   * Constructs the DB query string for entry age for (undeleted) records.
   *
   * @param timestamp
   * @return String DB query string for dates to fetch.
   */
  private String dateModifiedWhere(long timestamp) {
    return Passwords.TIME_PASSWORD_CHANGED + " >= " + Long.toString(timestamp);
  }

  /**
   * Helper function to process records from a fetch* cursor.
   * @param cursor
            fetch* cursor.
   * @param deleted
   *        true if using deleted table, false when using data table.
   * @param delegate
   *        FetchRecordsDelegate to process records.
   */
  private void fetchFromCursorDeleted(Cursor cursor, boolean deleted, RecordFilter filter, RepositorySessionFetchRecordsDelegate delegate) {

    if (cursor == null || !cursor.moveToFirst()) {
      // Empty cursor.
      return;
    }
    int cursorLen = cursor.getCount();
    // Hack: cursor sometimes gets in an infinite loop when using
    // Cursor.moveToNext() and Cursor.isAfterLast() to iterate through.
    for (int i = 0; i <= cursorLen; i++) {
      if (!cursor.moveToPosition(i)) {
        return;
      }
      Record r = deleted ? deletedPasswordRecordFromCursor(cursor) : passwordRecordFromCursor(cursor);
      if (r != null) {
        if (filter == null || !filter.excludeRecord(r)) {
          Logger.debug(LOG_TAG, "Fetched record " + r);
          delegate.onFetchedRecord(r);
        } else {
          Logger.debug(LOG_TAG, "Skipping filtered record " + r.guid);
        }
      }
    }
  }

  // TODO: move to RepoUtils?
  protected String computeSQLInClause(int items, String field) {
    StringBuilder builder = new StringBuilder(field);
    builder.append(" IN ( ?");
    for (int i = 0; i < items - 1; ++i) {
      builder.append(", ?");
    }
    builder.append(" )");
    return builder.toString();
  }

  private PasswordRecord retrieveByGuid(String guid) {
    PasswordRecord record = null;
    String[] guidArg = new String[] { guid };

    Cursor cursor = null;
    try {
      // Check data table.
      String where = Passwords.GUID + " = ?";
      cursor = passwordsHelper.safeQuery(passwordsProvider, ".store", BrowserContractHelpers.PasswordColumns, where, guidArg, null);
      if (cursor != null && cursor.moveToFirst()) {
          record = passwordRecordFromCursor(cursor);
          // Cursor will be closed in finally.
          return record;
      }

      // Check deleted table.
      where = DeletedPasswords.GUID + " = ?";
      cursor = deletedPasswordsHelper.safeQuery(passwordsProvider, ".retrieveByGuid", BrowserContractHelpers.DeletedColumns, where, guidArg, null);
      if (cursor != null && cursor.moveToFirst()) {
        record = deletedPasswordRecordFromCursor(cursor);
      }

    } catch (RemoteException e) {
      Logger.error(LOG_TAG, "RemoteException on deleted check for store.");
      delegate.onRecordStoreFailed(e);
    } catch (NullCursorException e) {
      Logger.error(LOG_TAG, "Null cursor on deleted check for store.");
      delegate.onRecordStoreFailed(e);
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
    return record;
  }

  private PasswordRecord findExistingRecord(PasswordRecord record) {
    PasswordRecord foundRecord = null;
    Cursor cursor = null;
    // Only check the data table.
    String dataWhere = Passwords.HOSTNAME + " = ? AND " +
                       Passwords.FORM_SUBMIT_URL + " = ? AND " +
                       // TODO: Bug 738347 - SQLiteBridge does not check for nulls in ContentValues.
                       // Passwords.HTTP_REALM + " = ? AND " +
                       Passwords.USERNAME_FIELD + " = ? AND " +
                       Passwords.PASSWORD_FIELD + " = ?";
    Logger.debug(LOG_TAG, "where: " + dataWhere);
    // We can't encrypt username directly for query, so run a more general query and then filter.
    String[] whereArgs = new String[] {
          record.hostname,
          record.formSubmitURL,
          // TODO: Bug 738347 - SQLiteBridge does not check for nulls in ContentValues.
          // record.httpRealm,
          record.usernameField,
          record.passwordField
      };
    try {
      cursor = passwordsHelper.safeQuery(passwordsProvider, ".findRecord", getAllColumns(), dataWhere, whereArgs, null);
      if (!cursor.moveToFirst()) {
        // Empty cursor.
        return null;
      }
      int cursorLen = cursor.getCount();
      // Hack: cursor sometimes gets in an infinite loop when using
      // Cursor.moveToNext() and Cursor.isAfterLast() to iterate through.
      for (int i = 0; i <= cursorLen; i++) {
        if (!cursor.moveToPosition(i)) {
          return null;
        }
        Logger.debug(LOG_TAG, "find cursor at pos " + i);
        foundRecord = passwordRecordFromCursor(cursor);
        // NOTE: We don't directly query for username because the
        // username/password values are encrypted in the db.
        // We don't have the keys for encrypting our query, so we run a more
        // general query and then filter the
        // the returned records for a matching username.
        Logger.debug(LOG_TAG, "Checking incoming [" + record.encryptedUsername + "] to [" + foundRecord.encryptedUsername + "]");
        if (record.encryptedUsername.equals(foundRecord.encryptedUsername)) {
          Logger.debug(LOG_TAG, "Found matching record: " + foundRecord);
          return foundRecord;
        }
      }
    } catch (RemoteException e) {
      Logger.error(LOG_TAG, "Remote exception in findExistingRecord.");
      delegate.onRecordStoreFailed(e);
    } catch (NullCursorException e) {
      Logger.error(LOG_TAG, "Null cursor in findExistingRecord.");
      delegate.onRecordStoreFailed(e);
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
    Logger.debug(LOG_TAG, "No matching records, returning null.");
    return null;
  }

  private void storeRecordDeletion(Record record) {
    deleteGuid(record.guid);
    delegate.onRecordStoreSucceeded(record);
  }
  /**
   * Make a PasswordRecord from a Cursor.
   * @param cur
   *        Cursor from query.
   * @param deleted
   *        true if creating a deleted Record, false if otherwise.
   * @return
   *        PasswordRecord populated from Cursor.
   */
  private PasswordRecord passwordRecordFromCursor(Cursor cur) {
    if (cur == null || !cur.moveToFirst()) {
      return null;
    }
    String guid = RepoUtils.getStringFromCursor(cur, BrowserContract.Passwords.GUID);
    long lastModified = RepoUtils.getLongFromCursor(cur, BrowserContract.Passwords.TIME_PASSWORD_CHANGED);

    PasswordRecord rec = new PasswordRecord(guid, COLLECTION, lastModified, false);
    rec.id = RepoUtils.getStringFromCursor(cur, BrowserContract.Passwords.ID);
    rec.hostname = RepoUtils.getStringFromCursor(cur, BrowserContract.Passwords.HOSTNAME);
    rec.httpRealm = RepoUtils.getStringFromCursor(cur, BrowserContract.Passwords.HTTP_REALM);
    rec.formSubmitURL = RepoUtils.getStringFromCursor(cur, BrowserContract.Passwords.FORM_SUBMIT_URL);
    rec.usernameField = RepoUtils.getStringFromCursor(cur, BrowserContract.Passwords.USERNAME_FIELD);
    rec.passwordField = RepoUtils.getStringFromCursor(cur, BrowserContract.Passwords.PASSWORD_FIELD);
    rec.encType = RepoUtils.getStringFromCursor(cur, BrowserContract.Passwords.ENC_TYPE);

    // TODO decryption of username/password here (Bug 711636)
    rec.encryptedUsername = RepoUtils.getStringFromCursor(cur, BrowserContract.Passwords.ENCRYPTED_USERNAME);
    rec.encryptedPassword = RepoUtils.getStringFromCursor(cur, BrowserContract.Passwords.ENCRYPTED_PASSWORD);

    rec.timeCreated = RepoUtils.getLongFromCursor(cur, BrowserContract.Passwords.TIME_CREATED);
    rec.timeLastUsed = RepoUtils.getLongFromCursor(cur, BrowserContract.Passwords.TIME_LAST_USED);
    rec.timePasswordChanged = RepoUtils.getLongFromCursor(cur, BrowserContract.Passwords.TIME_PASSWORD_CHANGED);
    rec.timesUsed = RepoUtils.getLongFromCursor(cur, BrowserContract.Passwords.TIMES_USED);
    return rec;
  }

  private PasswordRecord deletedPasswordRecordFromCursor(Cursor cur) {
    if (cur == null || !cur.moveToFirst()) {
      return null;
    }
    String guid = RepoUtils.getStringFromCursor(cur, DeletedColumns.GUID);
    long lastModified = RepoUtils.getLongFromCursor(cur, DeletedColumns.TIME_DELETED);
    PasswordRecord rec = new PasswordRecord(guid, COLLECTION, lastModified, true);
    rec.androidID = RepoUtils.getLongFromCursor(cur, DeletedColumns.ID);
    return rec;
  }

  private ContentValues getContentValues(Record record) {
    PasswordRecord rec = (PasswordRecord) record;

    ContentValues cv = new ContentValues();
    cv.put(BrowserContract.Passwords.GUID,            rec.guid);
    cv.put(BrowserContract.Passwords.HOSTNAME,        rec.hostname);
    // TODO: Bug 738347 - SQLiteBridge does not check for nulls in ContentValues.
    // For now, don't set httpRealm, because it can be null and Fennec SQLite doesn't handle null CV.
    // cv.put(BrowserContract.Passwords.HTTP_REALM,      rec.httpRealm);
    cv.put(BrowserContract.Passwords.FORM_SUBMIT_URL, rec.formSubmitURL);
    cv.put(BrowserContract.Passwords.USERNAME_FIELD,  rec.usernameField);
    cv.put(BrowserContract.Passwords.PASSWORD_FIELD,  rec.passwordField);

    // TODO Do encryption of username/password here. Bug 711636
    // TODO: Bug 738347 - SQLiteBridge does not check for nulls in ContentValues.
    // For now, don't set encType. (same as httpRealm)
    // cv.put(BrowserContract.Passwords.ENC_TYPE,           rec.encType);
    cv.put(BrowserContract.Passwords.ENCRYPTED_USERNAME, rec.encryptedUsername);
    cv.put(BrowserContract.Passwords.ENCRYPTED_PASSWORD, rec.encryptedPassword);

    cv.put(BrowserContract.Passwords.TIME_CREATED,          rec.timeCreated);
    cv.put(BrowserContract.Passwords.TIME_LAST_USED,        rec.timeLastUsed);
    cv.put(BrowserContract.Passwords.TIME_PASSWORD_CHANGED, rec.timePasswordChanged);
    cv.put(BrowserContract.Passwords.TIMES_USED,            rec.timesUsed);
    return cv;
  }

  private void dumpDbs() {
    Cursor cursor = null;
    try {
      // Dump passwords.
      Logger.debug(LOG_TAG, "passwordsProvider: ");
      cursor = passwordsHelper.safeQuery(passwordsProvider, ".dumpDBs", getAllColumns(), dateModifiedWhere(0), null, null);
      RepoUtils.dumpCursor(cursor);
      cursor.close();

      // Dump deleted passwords.
      Logger.debug(LOG_TAG, "deletedPasswordsProvider: ");
      cursor = deletedPasswordsHelper.safeQuery(passwordsProvider, ".dumpDBs", getAllDeletedColumns(), dateModifiedWhereDeleted(0), null, null);
      RepoUtils.dumpCursor(cursor);

    } catch (NullCursorException e) {
      Logger.debug(LOG_TAG, "NullCursor in dumping DBs.");
    } catch (RemoteException e) {
      Logger.debug(LOG_TAG, "RemoteException in dumping DBs.");
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }

  }
}
