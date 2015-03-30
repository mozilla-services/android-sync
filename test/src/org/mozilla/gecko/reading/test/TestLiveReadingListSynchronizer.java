/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.reading.test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;

import org.mozilla.gecko.background.common.PrefsBranch;
import org.mozilla.gecko.background.common.log.Logger;
import org.mozilla.gecko.background.db.CursorDumper;
import org.mozilla.gecko.background.testhelpers.MockSharedPreferences;
import org.mozilla.gecko.background.testhelpers.WaitHelper;
import org.mozilla.gecko.background.testhelpers.WaitHelper.InnerError;
import org.mozilla.gecko.db.BrowserContract.ReadingListItems;
import org.mozilla.gecko.fxa.FxAccountConstants;
import org.mozilla.gecko.reading.ClientMetadata;
import org.mozilla.gecko.reading.ClientReadingListRecord;
import org.mozilla.gecko.reading.LocalReadingListStorage;
import org.mozilla.gecko.reading.ReadingListClient;
import org.mozilla.gecko.reading.ReadingListDeleteDelegate;
import org.mozilla.gecko.reading.ReadingListRecord;
import org.mozilla.gecko.reading.ReadingListRecord.ServerMetadata;
import org.mozilla.gecko.reading.ReadingListRecordDelegate;
import org.mozilla.gecko.reading.ReadingListRecordResponse;
import org.mozilla.gecko.reading.ReadingListRecordUploadDelegate;
import org.mozilla.gecko.reading.ReadingListResponse;
import org.mozilla.gecko.reading.ReadingListStorage;
import org.mozilla.gecko.reading.ReadingListStorageResponse;
import org.mozilla.gecko.reading.ReadingListSynchronizer;
import org.mozilla.gecko.reading.ReadingListSynchronizerDelegate;
import org.mozilla.gecko.reading.ReadingListWipeDelegate;
import org.mozilla.gecko.reading.ServerReadingListRecord;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.net.BasicAuthHeaderProvider;
import org.mozilla.gecko.sync.net.MozResponse;

import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;

public class TestLiveReadingListSynchronizer extends ReadingListTest {

static final class TestSynchronizerDelegate implements ReadingListSynchronizerDelegate {
    private final CountDownLatch latch;
    public volatile boolean onDownloadCompleteCalled = false;
    public volatile boolean onModifiedUploadCompleteCalled = false;
    public volatile boolean onNewItemUploadCompleteCalled = false;
    public volatile boolean onStatusUploadCompleteCalled = false;
    public volatile boolean onUnableToSyncCalled = false;
    public volatile boolean onDeletionsUploadCompleteCalled = false;
    public volatile Exception onUnableToSyncException = null;

    public TestSynchronizerDelegate(CountDownLatch latch) {
      this.latch = latch;
    }

    @Override
    public void onUnableToSync(Exception e) {
      Logger.warn(LOG_TAG, "onUnableToSync", e);
      onUnableToSyncException = e;
      onUnableToSyncCalled = true;
      latch.countDown();
    }

    @Override
    public void onStatusUploadComplete(Collection<String> uploaded,
                                       Collection<String> failed) {
      onStatusUploadCompleteCalled = true;
    }

    @Override
    public void onNewItemUploadComplete(Collection<String> uploaded,
                                        Collection<String> failed) {
      onNewItemUploadCompleteCalled = true;
    }

    @Override
    public void onModifiedUploadComplete() {
      onModifiedUploadCompleteCalled = true;
    }

    @Override
    public void onDownloadComplete() {
      onDownloadCompleteCalled = true;
    }

    @Override
    public void onComplete() {
      latch.countDown();
    }

    @Override
    public void onDeletionsUploadComplete() {
        onDeletionsUploadCompleteCalled = true;
    }
  }

  private static final String DEFAULT_SERVICE_URI = FxAccountConstants.STAGE_READING_LIST_SERVER_ENDPOINT;

  private static ReadingListClient getTestClient(final String username) throws URISyntaxException, InterruptedException {
    return getTestClient(username, false);
  }

  private static ReadingListClient getTestClient(String username, boolean wiped) throws URISyntaxException, InterruptedException {
    final ReadingListClient client = new ReadingListClient(new URI(DEFAULT_SERVICE_URI), new BasicAuthHeaderProvider(username, "nopassword"));
    if (wiped) {
      final CountDownLatch latch = new CountDownLatch(1);
      ReadingListWipeDelegate delegate = new ReadingListWipeDelegate() {
        @Override
        public void onSuccess(ReadingListStorageResponse response) {
          Logger.info(LOG_TAG, "Got wipe success.");
          latch.countDown();
        }

        @Override
        public void onPreconditionFailed(MozResponse response) {
          // Should never occur.
          fail();
          latch.countDown();
        }

        @Override
        public void onFailure(MozResponse response) {
          Logger.error(LOG_TAG, "Wipe failed: " + response.getStatusCode());
          // Oh well.
          fail();
          latch.countDown();
        }

        @Override
        public void onFailure(Exception e) {
          Logger.error(LOG_TAG, "Wipe failed: " +  e);
          // Oh well.
          fail();
          latch.countDown();
        }
      };
      client.wipe(delegate);
      latch.await();
    }
    return client;
  }

  public final void testBlankSync() throws Exception {
    final ContentProviderClient cpc = getWipedLocalClient();
    try {
      final ReadingListStorage local = new LocalReadingListStorage(cpc);
      final SharedPreferences prefs = new MockSharedPreferences();
      final ReadingListClient remote = getTestClient("test_android_blank");
      final PrefsBranch branch = new PrefsBranch(prefs, "foo.");
      final ReadingListSynchronizer synchronizer = new ReadingListSynchronizer(branch, remote, local);

      assertFalse(prefs.contains("foo." + ReadingListSynchronizer.PREF_LAST_MODIFIED));
      assertFalse(branch.contains(ReadingListSynchronizer.PREF_LAST_MODIFIED));

      assertSuccessfulSync(synchronizer);

      // We should have a new LM in prefs.
      assertTrue(prefs.contains("foo." + ReadingListSynchronizer.PREF_LAST_MODIFIED));
      assertTrue(branch.contains(ReadingListSynchronizer.PREF_LAST_MODIFIED));
      assertTrue(branch.getLong(ReadingListSynchronizer.PREF_LAST_MODIFIED, -1L) > 1425428783535L);
    } finally {
      cpc.release();
    }
  }

  public final void testNewUp() throws Exception {
    final ContentProviderClient cpc = getWipedLocalClient();
    try {
      final ReadingListStorage local = new LocalReadingListStorage(cpc);
      final SharedPreferences prefs = new MockSharedPreferences();
      final ReadingListClient remote = getTestClient("test_android_new_up_2", true);
      final PrefsBranch branch = new PrefsBranch(prefs, "foo_new.");
      final ReadingListSynchronizer synchronizer = new ReadingListSynchronizer(branch, remote, local);

      // Populate a record.
      final ContentValues values = new ContentValues();
      values.put("url", "http://example.org/reading");
      values.put("title", "Example Reading");
      values.put("content_status", ReadingListItems.STATUS_FETCH_FAILED_PERMANENT);   // So that Gecko won't fetch!
      cpc.insert(CONTENT_URI, values);

      assertCursorCount(1, local.getNew());
      assertCursorCount(0, local.getModified());
      assertCursorCount(0, local.getStatusChanges());
      assertCursorCount(1, local.getAll());

      assertSuccessfulSync(synchronizer);

      CursorDumper.dumpCursor(local.getNew());
      assertCursorCount(0, local.getNew());
      assertCursorCount(0, local.getModified());
      assertCursorCount(0, local.getStatusChanges());
      assertCursorCount(1, local.getAll());

      // Now we applied the remote record, and we can see the changes.
      Cursor c = cpc.query(CONTENT_URI_IS_SYNC, null, null, null, null);
      String guid = null;
      try {
        final int colContentStatus = c.getColumnIndexOrThrow(ReadingListItems.CONTENT_STATUS);
        final int colTitle = c.getColumnIndexOrThrow(ReadingListItems.TITLE);
        final int colGUID = c.getColumnIndexOrThrow(ReadingListItems.GUID);
        final int colServerLastModified = c.getColumnIndexOrThrow(ReadingListItems.SERVER_LAST_MODIFIED);

        assertTrue(c.moveToFirst());
        assertEquals(1, c.getCount());
        assertEquals(ReadingListItems.STATUS_FETCH_FAILED_PERMANENT, c.getInt(colContentStatus));
        assertEquals("Example Reading", c.getString(colTitle));
        assertFalse(c.isNull(colGUID));
        guid = c.getString(colGUID);
        assertTrue(0 < c.getLong(colServerLastModified));
      } finally {
        c.close();
      }

      // Now delete the record locally as Fennec would. The deletion should be uploaded during a sync.
      final Uri uri = ReadingListItems.CONTENT_URI;
      int deleted = cpc.delete(uri, ReadingListItems.GUID + " = ?", new String[] { guid });
      assertEquals(1, deleted);
      final Cursor deletedCursor = local.getDeletedItems();
      try {
        assertEquals(1, deletedCursor.getCount());
        deletedCursor.moveToFirst();
        assertEquals(guid, deletedCursor.getString(0));
      } finally {
        deletedCursor.close();
      }

      assertSuccessfulSync(synchronizer);

      assertCursorCount(0, local.getNew());
      assertCursorCount(0, local.getModified());
      assertCursorCount(0, local.getStatusChanges());
      assertCursorCount(0, local.getAll());

      // Now touch the server to verify that the item is missing.
      try {
        getOne(remote, guid);
        fail("Should be 404.");
      } catch (InnerError e) {
        assertEquals("onRecordMissingOrDeleted", e.innerError.getMessage());
      }

      if (guid != null) {
        blindWipe(remote, guid);
      }
    } finally {
      cpc.release();
    }
  }

  protected TestSynchronizerDelegate assertSuccessfulSync(ReadingListSynchronizer synchronizer) throws InterruptedException {
    final CountDownLatch latch = new CountDownLatch(1);
    final TestSynchronizerDelegate delegate = new TestSynchronizerDelegate(latch);
    synchronizer.syncAll(delegate);
    latch.await();

    if (delegate.onUnableToSyncException != null) {
      throw new RuntimeException(delegate.onUnableToSyncException);
    }
    assertFalse(delegate.onUnableToSyncCalled);
    assertTrue(delegate.onDownloadCompleteCalled);
    assertTrue(delegate.onModifiedUploadCompleteCalled);
    assertTrue(delegate.onStatusUploadCompleteCalled);
    assertTrue(delegate.onDeletionsUploadCompleteCalled);
    assertTrue(delegate.onNewItemUploadCompleteCalled);
    return delegate;
  }

  public final void testUploadModified() throws Exception {
    final ContentProviderClient cpc = getWipedLocalClient();
    try {
      final ReadingListStorage local = new LocalReadingListStorage(cpc);
      final SharedPreferences prefs = new MockSharedPreferences();
      final ReadingListClient remote = getTestClient("test_android_upload_modified", true);
      final PrefsBranch branch = new PrefsBranch(prefs, "foo_upload_modified.");
      final ReadingListSynchronizer synchronizer = new ReadingListSynchronizer(branch, remote, local);

      // Populate a record.
      final ContentValues values = new ContentValues();
      values.put("url", "http://example.org/reading");
      values.put("title", "Example Reading");
      values.put("content_status", ReadingListItems.STATUS_FETCH_FAILED_PERMANENT);   // So that Gecko won't fetch!
      cpc.insert(CONTENT_URI, values);

      assertCursorCount(1, local.getNew());
      assertCursorCount(0, local.getModified());
      assertCursorCount(0, local.getStatusChanges());
      assertCursorCount(1, local.getAll());

      Logger.info(LOG_TAG, "Uploading new item.");
      assertSuccessfulSync(synchronizer);

      assertCursorCount(0, local.getNew());
      assertCursorCount(0, local.getModified());
      assertCursorCount(0, local.getStatusChanges());
      assertCursorCount(1, local.getAll());

      Cursor c = cpc.query(CONTENT_URI_IS_SYNC, null, null, null, null);
      String guid = null;
      try {
        final int colGUID = c.getColumnIndexOrThrow(ReadingListItems.GUID);
        assertEquals(1, c.getCount());
        assertTrue(c.moveToFirst());
        assertFalse(c.isNull(colGUID));
        guid = c.getString(colGUID);
      } finally {
        c.close();
      }
      Logger.info(LOG_TAG, "Uploaded item was assigned GUID: " + guid);

      assertEquals("", getExcerpt(cpc, guid)); // Why not null?

      // We should have applied the remote record.  Let's make a material change locally.
      final String TEST_EXCERPT = "Example reading list excerpt.";
      final ContentValues w = new ContentValues();
      w.put(ReadingListItems.EXCERPT, TEST_EXCERPT);
      assertEquals(1, cpc.update(CONTENT_URI, w, "guid = ?", new String[] { guid }));

      Logger.info(LOG_TAG, "Uploading item with material change.");
      assertSuccessfulSync(synchronizer);

      // We should have no changes remaining to upload.
      assertCursorCount(0, local.getNew());
      assertCursorCount(0, local.getModified());
      assertCursorCount(0, local.getStatusChanges());
      assertCursorCount(1, local.getAll());

      // Fetch the remote record and verify that our change arrived.
      final ServerReadingListRecord record = getOne(remote, guid);
      assertNotNull(record);
      assertNotNull(record.getExcerpt());
      assertEquals(TEST_EXCERPT, record.getExcerpt());

      // Now modify the remote record to generate a conflict locally.
      final String TEST_PATCHED_EXCERPT = TEST_EXCERPT + " CHANGED";
      final ExtendedJSONObject o = new ExtendedJSONObject();
      o.put("id", guid);
      o.put("excerpt", TEST_PATCHED_EXCERPT);

      final ClientMetadata cm = null;
      final ServerMetadata sm = new ServerMetadata(guid, -1L);
      final ClientReadingListRecord conflictingRecord = new ClientReadingListRecord(sm, cm, o);
      final ServerReadingListRecord patchedRecord = patchOne(remote, conflictingRecord);
      assertEquals(TEST_PATCHED_EXCERPT, patchedRecord.getExcerpt());

      // Now let's make the material change locally again. The server's record
      // will obliterate this change; we lose.
      assertEquals(1, cpc.update(CONTENT_URI, w, "guid = ?", new String[] { guid }));

      assertCursorCount(0, local.getNew());
      assertCursorCount(1, local.getModified());
      assertCursorCount(0, local.getStatusChanges());
      assertCursorCount(1, local.getAll());

      Logger.info(LOG_TAG, "Uploading item with conflicting material change.");
      assertSuccessfulSync(synchronizer);

      // We should have no changes remaining to upload.
      assertCursorCount(0, local.getNew());
      assertCursorCount(0, local.getModified());
      assertCursorCount(0, local.getStatusChanges());
      assertCursorCount(1, local.getAll());

      // Our local record should have the server's excerpt.
      assertEquals(TEST_PATCHED_EXCERPT, getExcerpt(cpc, guid)); // Why not null?

      // Fetch the remote record and verify that our change did not make it to the server.
      final ServerReadingListRecord fetchedPatchedRecord = getOne(remote, guid);
      assertNotNull(fetchedPatchedRecord);
      assertNotNull(fetchedPatchedRecord.getExcerpt());
      assertEquals(TEST_PATCHED_EXCERPT, fetchedPatchedRecord.getExcerpt());

      if (guid != null) {
        blindWipe(remote, guid);
      }
    } finally {
      cpc.release();
    }
  }

  private String getExcerpt(ContentProviderClient cpc, String guid) throws RemoteException {
    Cursor c = cpc.query(CONTENT_URI_IS_SYNC, null, "guid is ?", new String[] { guid }, null);
    try {
      final int colExcerpt = c.getColumnIndexOrThrow(ReadingListItems.EXCERPT);
      assertEquals(1, c.getCount());
      assertTrue(c.moveToFirst());
      final String excerpt = c.getString(colExcerpt);
      return excerpt;
    } finally {
      c.close();
    }
  }

  private ServerReadingListRecord getOne(final ReadingListClient remote, final String guid) throws InterruptedException {
    final ServerReadingListRecord result[] = new ServerReadingListRecord[1];
    WaitHelper.getTestWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        remote.getOne(guid, new ReadingListRecordDelegate() {
          @Override
          public void onRecordReceived(ServerReadingListRecord record) {
            result[0] = record;
            WaitHelper.getTestWaiter().performNotify();
          }

          @Override
          public void onRecordMissingOrDeleted(String guid, ReadingListResponse resp) {
            WaitHelper.getTestWaiter().performNotify(new RuntimeException("onRecordMissingOrDeleted"));
          }

          @Override
          public void onFailure(Exception error) {
            WaitHelper.getTestWaiter().performNotify(error);
          }

          @Override
          public void onFailure(MozResponse response) {
            WaitHelper.getTestWaiter().performNotify(new RuntimeException("onFailure"));
          }

          @Override
          public void onComplete(ReadingListResponse response) {
            // Ignore -- we should get one of the other callbacks.
          }
        }, -1);
      }
    });
    return result[0];
  }

  private ServerReadingListRecord patchOne(final ReadingListClient remote, final ClientReadingListRecord record) throws InterruptedException {
    final ServerReadingListRecord result[] = new ServerReadingListRecord[1];
    WaitHelper.getTestWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        remote.patch(record, new ReadingListRecordUploadDelegate() {
          @Override
          public void onSuccess(ClientReadingListRecord up,
                                ReadingListRecordResponse response,
                                ServerReadingListRecord down) {
            result[0] = down;
            WaitHelper.getTestWaiter().performNotify();
          }

          @Override
          public void onInvalidUpload(ClientReadingListRecord up,
                                      ReadingListResponse response) {
            WaitHelper.getTestWaiter().performNotify(new RuntimeException("onInvalidUpload"));
          }

          @Override
          public void onFailure(ClientReadingListRecord up, MozResponse response) {
            WaitHelper.getTestWaiter().performNotify(new RuntimeException("onFailure"));
          }

          @Override
          public void onFailure(ClientReadingListRecord up, Exception ex) {
            WaitHelper.getTestWaiter().performNotify(ex);
          }

          @Override
          public void onConflict(ClientReadingListRecord up,
                                 ReadingListResponse response) {
            WaitHelper.getTestWaiter().performNotify(new RuntimeException("onFailure"));
          }

          @Override
          public void onBatchDone() {
            // Ignore -- we should get a different callback.
          }

          @Override
          public void onBadRequest(ClientReadingListRecord up, MozResponse response) {
            WaitHelper.getTestWaiter().performNotify(new RuntimeException("onFailure"));
          }
        });
      }
    });
    return result[0];
  }

  private void blindWipe(final ReadingListClient remote, String guid) {
    // Delete it from the server to clean up.
    // Eventually we'll have wipe...
    remote.delete(guid, new ReadingListDeleteDelegate() {
      @Override
      public void onSuccess(ReadingListRecordResponse response,
                            ReadingListRecord record) {
      }

      @Override
      public void onRecordMissingOrDeleted(String guid, MozResponse response) {
      }

      @Override
      public void onPreconditionFailed(String guid, MozResponse response) {
      }

      @Override
      public void onFailure(MozResponse response) {
      }

      @Override
      public void onFailure(Exception e) {
      }

      @Override
      public void onBatchDone() {
      }
    }, -1L);
  }
}
