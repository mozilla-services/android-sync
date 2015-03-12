/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.reading;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.json.simple.parser.ParseException;
import org.mozilla.gecko.background.common.PrefsBranch;
import org.mozilla.gecko.background.common.log.Logger;
import org.mozilla.gecko.db.BrowserContract.ReadingListItems;
import org.mozilla.gecko.reading.ReadingListRecord.ServerMetadata;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.NonObjectJSONException;
import org.mozilla.gecko.sync.net.MozResponse;

import android.database.Cursor;
import android.text.TextUtils;

/**
 * This class implements the multi-phase synchronizing approach described
 * at <https://github.com/mozilla-services/readinglist/wiki/Client-phases>.
 *
 * This is also where delegate-based control flow comes to die.
 */
public class ReadingListSynchronizer {
  public static final String LOG_TAG = ReadingListSynchronizer.class.getSimpleName();

  public static final String PREF_LAST_MODIFIED = "download.serverlastmodified";

  private final PrefsBranch prefs;
  private final ReadingListClient remote;
  private final ReadingListStorage local;
  private final Executor executor;

  private interface StageDelegate {
    void next();
    void fail();
    void fail(Exception e);
  }

  private abstract static class NextDelegate implements StageDelegate {
    private final Executor executor;
    NextDelegate(final Executor executor) {
      this.executor = executor;
    }

    abstract void doNext();
    abstract void doFail(Exception e);

    @Override
    public void next() {
      executor.execute(new Runnable() {
        @Override
        public void run() {
          doNext();
        }
      });
    }

    @Override
    public void fail() {
      fail(null);
    }

    @Override
    public void fail(final Exception e) {
      executor.execute(new Runnable() {
        @Override
        public void run() {
          doFail(e);
        }
      });
    }
  }

  public ReadingListSynchronizer(final PrefsBranch prefs, final ReadingListClient remote, final ReadingListStorage local) {
    this(prefs, remote, local, Executors.newSingleThreadExecutor());
  }

  public ReadingListSynchronizer(final PrefsBranch prefs, final ReadingListClient remote, final ReadingListStorage local, Executor executor) {
    this.prefs = prefs;
    this.remote = remote;
    this.local = local;
    this.executor = executor;
  }

  private static final class NewItemUploadDelegate implements ReadingListRecordUploadDelegate {
    public volatile int failures = 0;
    private final ReadingListChangeAccumulator acc;
    private final StageDelegate next;

    NewItemUploadDelegate(ReadingListChangeAccumulator acc, StageDelegate next) {
      this.acc = acc;
      this.next = next;
    }

    @Override
    public void onSuccess(ClientReadingListRecord up,
                          ReadingListRecordResponse response,
                          ServerReadingListRecord down) {
      // Apply the resulting record. The server will have populated some fields.
      acc.addChangedRecord(up.givenServerRecord(down));
    }

    @Override
    public void onConflict(ClientReadingListRecord up, ReadingListResponse response) {
      ExtendedJSONObject body;
      try {
        body = response.jsonObjectBody();
        String conflicting = body.getString("id");
        Logger.warn(LOG_TAG, "Conflict detected: remote ID is " + conflicting);

        // TODO: When an operation implies that a server record is a replacement
        // of what we uploaded, we should ensure that we have a local copy of
        // that server record!
      } catch (IllegalStateException | NonObjectJSONException | IOException |
               ParseException e) {
        // Oops.
        // But our workaround is the same either way.
      }

      // Either the record exists locally, in which case we need to merge,
      // or it doesn't, and we'll download it shortly.
      // The simplest thing to do in both cases is to simply delete the local
      // record we tried to upload. Yes, we might lose some annotations, but
      // we can leave doing better to a follow-up.
      // Issues here are so unlikely that we don't do anything sophisticated
      // (like moving the record to a holding area) -- just delete it ASAP.
      acc.addDeletion(up);
    }

    @Override
    public void onInvalidUpload(ClientReadingListRecord up, ReadingListResponse response) {
      recordFailed(up);
    }

    @Override
    public void onFailure(ClientReadingListRecord up, MozResponse response) {
      recordFailed(up);
    }

    @Override
    public void onFailure(ClientReadingListRecord up, Exception ex) {
      recordFailed(up);
    }

    @Override
    public void onBadRequest(ClientReadingListRecord up, MozResponse response) {
      recordFailed(up);
    }

    private void recordFailed(ClientReadingListRecord up) {
      ++failures;
    }

    @Override
    public void onBatchDone() {
      // We mark uploaded records as synced when we apply the server record with the
      // GUID -- we don't know the GUID yet!
      if (failures == 0) {
        try {
          next.next();
        } catch (Exception e) {
          next.fail(e);
        }
        return;
      }
      next.fail();
    }
  }

  private static class StatusUploadDelegate implements ReadingListRecordUploadDelegate {
    private final ReadingListChangeAccumulator acc;

    public volatile int failures = 0;
    private final StageDelegate next;

    StatusUploadDelegate(ReadingListChangeAccumulator acc, StageDelegate next) {
      this.acc = acc;
      this.next = next;
    }

    @Override
    public void onInvalidUpload(ClientReadingListRecord up,
                                ReadingListResponse response) {
      recordFailed(up);
    }

    @Override
    public void onConflict(ClientReadingListRecord up,
                           ReadingListResponse response) {
      // This should never happen for a status-only change.
      // TODO: mark this record as requiring a full upload or download.
      failures++;
    }

    @Override
    public void onSuccess(ClientReadingListRecord up,
                          ReadingListRecordResponse response,
                          ServerReadingListRecord down) {
      if (!TextUtils.equals(up.getGUID(), down.getGUID())) {
        // Uh oh!
        // This should never occur. We should get an onConflict instead,
        // so this would imply a server bug, or something like a truncated
        // over-long GUID string.
        //
        // Should we wish to recover from this case, probably the right approach
        // is to ensure that the GUID is overwritten locally (given that we know
        // the numeric ID).
      }

      acc.addChangedRecord(up.givenServerRecord(down));
    }

    @Override
    public void onBadRequest(ClientReadingListRecord up, MozResponse response) {
      recordFailed(up);
    }

    @Override
    public void onFailure(ClientReadingListRecord up, Exception ex) {
      recordFailed(up);
    }

    @Override
    public void onFailure(ClientReadingListRecord up, MozResponse response) {
      recordFailed(up);
    }

    private void recordFailed(ClientReadingListRecord up) {
      ++failures;
    }

    @Override
    public void onBatchDone() {
      try {
        acc.finish();
      } catch (Exception e) {
        next.fail(e);
        return;
      }

      if (failures == 0) {
        try {
          next.next();
        } catch (Exception e) {
        }
      }
      next.fail();
    }
  }

  private Queue<ClientReadingListRecord> collectStatusChangesFromCursor(final Cursor cursor) {
    try {
      final Queue<ClientReadingListRecord> toUpload = new LinkedList<>();

      // The columns should come in this order, FWIW.
      final int columnGUID = cursor.getColumnIndexOrThrow(ReadingListItems.GUID);
      final int columnIsUnread = cursor.getColumnIndexOrThrow(ReadingListItems.IS_UNREAD);
      final int columnIsFavorite = cursor.getColumnIndexOrThrow(ReadingListItems.IS_FAVORITE);
      final int columnMarkedReadBy = cursor.getColumnIndexOrThrow(ReadingListItems.MARKED_READ_BY);
      final int columnMarkedReadOn = cursor.getColumnIndexOrThrow(ReadingListItems.MARKED_READ_ON);
      final int columnChangeFlags = cursor.getColumnIndexOrThrow(ReadingListItems.SYNC_CHANGE_FLAGS);

      while (cursor.moveToNext()) {
        final String guid = cursor.getString(columnGUID);
        if (guid == null) {
          // Nothing we can do here.
          continue;
        }

        final ExtendedJSONObject o = new ExtendedJSONObject();
        o.put("id", guid);

        final int changeFlags = cursor.getInt(columnChangeFlags);
        if ((changeFlags & ReadingListItems.SYNC_CHANGE_FAVORITE_CHANGED) > 0) {
          o.put("favorite", cursor.getInt(columnIsFavorite) == 1);
        }

        if ((changeFlags & ReadingListItems.SYNC_CHANGE_UNREAD_CHANGED) > 0) {
          final boolean isUnread = cursor.getInt(columnIsUnread) == 1;
          o.put("unread", isUnread);
          if (!isUnread) {
            o.put("marked_read_by", cursor.getString(columnMarkedReadBy));
            o.put("marked_read_on", cursor.getLong(columnMarkedReadOn));
          }
        }

        final ClientMetadata cm = null;
        final ServerMetadata sm = new ServerMetadata(guid, -1L);
        final ClientReadingListRecord record = new ClientReadingListRecord(sm, cm, o);
        toUpload.add(record);
      }

      return toUpload;
    } finally {
      cursor.close();
    }
  }

  private Queue<ClientReadingListRecord> accumulateNewItems(Cursor cursor) {
    try {
      final Queue<ClientReadingListRecord> toUpload = new LinkedList<>();
      final ReadingListClientRecordFactory factory = new ReadingListClientRecordFactory(cursor);

      ClientReadingListRecord record;
      while ((record = factory.getNext()) != null) {
        toUpload.add(record);
      }
      return toUpload;
    } finally {
      cursor.close();
    }
  }

  // N.B., status changes for items that haven't been uploaded yet are dealt with in
  // uploadNewItems.
  protected void uploadUnreadChanges(final StageDelegate delegate) {
    try {
      final Cursor cursor = local.getStatusChanges();

      if (cursor == null) {
        delegate.fail(new RuntimeException("Unable to get unread item cursor."));
        return;
      }

      final Queue<ClientReadingListRecord> toUpload = collectStatusChangesFromCursor(cursor);

      // Nothing to do.
      if (toUpload.isEmpty()) {
        delegate.next();
        return;
      }

      // Upload each record. This looks like batching, but it's really chained serial requests.
      final ReadingListChangeAccumulator acc = this.local.getChangeAccumulator();
      final StatusUploadDelegate uploadDelegate = new StatusUploadDelegate(acc, delegate);

      // Don't send I-U-S; in the case of favorites we're
      // happy to overwrite the server value, and in the case of unread status
      // the server will reconcile for us.
      this.remote.patch(toUpload, executor, uploadDelegate);
    } catch (Exception e) {
      delegate.fail(e);
    }
  }

  protected void uploadNewItems(final StageDelegate delegate) {
    try {
      final Cursor cursor = this.local.getNew();

      if (cursor == null) {
        delegate.fail(new RuntimeException("Unable to get new item cursor."));
        return;
      }

      Queue<ClientReadingListRecord> toUpload = accumulateNewItems(cursor);

      // Nothing to do.
      if (toUpload.isEmpty()) {
        Logger.debug(LOG_TAG, "No new items to upload. Skipping.");
        delegate.next();
        return;
      }

      final ReadingListChangeAccumulator acc = this.local.getChangeAccumulator();
      final NewItemUploadDelegate uploadDelegate = new NewItemUploadDelegate(acc, new StageDelegate() {
        private boolean tryFlushChanges() {
          Logger.debug(LOG_TAG, "Flushing post-upload changes.");
          try {
            acc.finish();
            return true;
          } catch (Exception e) {
            Logger.warn(LOG_TAG, "Flushing changes failed! This sync went wrong.", e);
            delegate.fail(e);
            return false;
          }
        }

        @Override
        public void next() {
          Logger.debug(LOG_TAG, "New items uploaded successfully.");

          if (tryFlushChanges()) {
            delegate.next();
          }
        }

        @Override
        public void fail() {
          Logger.warn(LOG_TAG, "Couldn't upload new items.");
          if (tryFlushChanges()) {
            delegate.fail();
          }
        }

        @Override
        public void fail(Exception e) {
          Logger.warn(LOG_TAG, "Couldn't upload new items.", e);
          if (tryFlushChanges()) {
            delegate.fail(e);
          }
        }
      });

      // Handle 201 for success, 400 for invalid, 303 for redirect.
      // TODO: 200 == "was already on the server, we didn't touch it, here it is."
      // ... we need to apply it locally.
      this.remote.add(toUpload, executor, uploadDelegate);
    } catch (Exception e) {
      delegate.fail(e);
      return;
    }
  }

  private void uploadModified(final StageDelegate delegate) {
    // TODO
    delegate.next();
  }

  private void downloadIncoming(final long since, final StageDelegate delegate) {
    final ReadingListChangeAccumulator postDownload = this.local.getChangeAccumulator();

    final FetchSpec spec = new FetchSpec.Builder().setSince(since).build();

    // TODO: should we flush the accumulator if we get a failure?
    ReadingListRecordDelegate recordDelegate = new ReadingListRecordDelegate() {
      @Override
      public void onRecordReceived(ServerReadingListRecord record) {
        postDownload.addDownloadedRecord(record);
      }

      @Override
      public void onRecordMissingOrDeleted(String guid, ReadingListResponse resp) {
        // Should never occur. Deleted records will be processed by onRecordReceived.
      }

      @Override
      public void onFailure(Exception error) {
        Logger.error(LOG_TAG, "Download failed. since = " + since + ".", error);
        delegate.fail(error);
      }

      @Override
      public void onFailure(MozResponse response) {
        final int statusCode = response.getStatusCode();
        Logger.error(LOG_TAG, "Download failed. since = " + since + ". Response: " + statusCode);
        response.logResponseBody(LOG_TAG);
        delegate.fail();
      }

      @Override
      public void onComplete(ReadingListResponse response) {
        long lastModified = response.getLastModified();
        Logger.info(LOG_TAG, "Server last modified: " + lastModified);
        try {
          postDownload.finish();

          // Yay. We do this here so that if writing changes fails, we don't advance.
          advanceLastModified(lastModified);
          delegate.next();
        } catch (Exception e) {
          delegate.fail(e);
        }
      }
    };

    try {
      remote.getAll(spec, recordDelegate, since);
    } catch (URISyntaxException e) {
      delegate.fail(e);
    }
  }

  /**
   * Upload unread changes, then upload new items, then call `done`.
   * Substantially modified records are uploaded last.
   *
   * @param syncDelegate only used for status callbacks.
   */
  private void syncUp(final ReadingListSynchronizerDelegate syncDelegate, final StageDelegate done) {
    // Second.
    final StageDelegate onNewItemsUploaded = new NextDelegate(executor) {
      @Override
      public void doNext() {
        syncDelegate.onNewItemUploadComplete(null, null);
        done.next();
      }

      @Override
      public void doFail(Exception e) {
        done.fail(e);
      }
    };

    // First.
    final StageDelegate onUnreadChangesUploaded = new NextDelegate(executor) {
      @Override
      public void doNext() {
        syncDelegate.onStatusUploadComplete(null, null);
        uploadNewItems(onNewItemsUploaded);
      }

      @Override
      public void doFail(Exception e) {
        Logger.warn(LOG_TAG, "Uploading unread changes failed.", e);
        done.fail(e);
      }
    };

    try {
      uploadUnreadChanges(onUnreadChangesUploaded);
    } catch (Exception ee) {
      done.fail(ee);
    }
  }


  /**
   * Do an upload-only sync.
   * By "upload-only" we mean status-only changes and new items.
   * To upload modifications, use syncAll.
   */
  /*
   // Not yet used
  public void syncUp(final ReadingListSynchronizerDelegate syncDelegate) {
    final StageDelegate onUploadCompleted = new StageDelegate() {
      @Override
      public void next() {
        // TODO
        syncDelegate.onNewItemUploadComplete(null, null);
      }

      @Override
      public void fail(Exception e) {
        syncDelegate.onUnableToSync(e);
      }
    };

    executor.execute(new Runnable() {
      @Override
      public void run() {
        try {
          syncUp(onUploadCompleted);
        } catch (Exception e) {
          syncDelegate.onUnableToSync(e);
          return;
        }
      }
    });
  }
*/

  /**
   * Do a bidirectional sync.
   */
  public void syncAll(final ReadingListSynchronizerDelegate syncDelegate) {
    syncAll(getLastModified(), syncDelegate);
  }

  public void syncAll(final long since, final ReadingListSynchronizerDelegate syncDelegate) {
    // Fourth: call back to the synchronizer delegate.
    final StageDelegate onModifiedUploadComplete = new NextDelegate(executor) {
      @Override
      public void doNext() {
        syncDelegate.onModifiedUploadComplete();
        syncDelegate.onComplete();
      }

      @Override
      public void doFail(Exception e) {
        syncDelegate.onUnableToSync(e);
      }
    };

    // Third: upload modified records.
    final StageDelegate onDownloadCompleted = new NextDelegate(executor) {     // TODO: since.
      @Override
      public void doNext() {
        // TODO: save prefs.
        syncDelegate.onDownloadComplete();
        uploadModified(onModifiedUploadComplete);
      }

      @Override
      public void doFail(Exception e) {
        Logger.warn(LOG_TAG, "Download failed.", e);
        syncDelegate.onUnableToSync(e);
      }
    };

    // Second: download incoming changes.
    final StageDelegate onUploadCompleted = new NextDelegate(executor) {
      @Override
      public void doNext() {
        // N.B., we apply the downloaded versions of all uploaded records.
        // That means the DB server timestamp matches the server's current
        // timestamp when we do a fetch; we skip records in this way.
        // We can also optimize by keeping the (guid, server timestamp) pair
        // in memory, but of course this runs into invalidation issues if
        // concurrent writes are occurring.
        downloadIncoming(since, onDownloadCompleted);
      }

      @Override
      public void doFail(Exception e) {
        Logger.warn(LOG_TAG, "Upload failed.", e);
        syncDelegate.onUnableToSync(e);
      }
    };

    // First: upload changes and new items.
    executor.execute(new Runnable() {
      @Override
      public void run() {
        try {
          syncUp(syncDelegate, onUploadCompleted);
        } catch (Exception e) {
          syncDelegate.onUnableToSync(e);
          return;
        }
      }
    });

    // TODO: ensure that records we identified as conflicts have been downloaded.
  }

  protected long getLastModified() {
    return prefs.getLong(PREF_LAST_MODIFIED, -1L);
  }

  protected void advanceLastModified(final long lastModified) {
    if (getLastModified() > lastModified) {
      return;
    }
    prefs.edit().putLong(PREF_LAST_MODIFIED, lastModified).apply();
  }
}
