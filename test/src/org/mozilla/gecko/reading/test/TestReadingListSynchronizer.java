/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.reading.test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.mozilla.gecko.background.common.PrefsBranch;
import org.mozilla.gecko.background.common.log.Logger;
import org.mozilla.gecko.background.db.CursorDumper;
import org.mozilla.gecko.background.testhelpers.MockSharedPreferences;
import org.mozilla.gecko.db.BrowserContract.ReadingListItems;
import org.mozilla.gecko.reading.LocalReadingListStorage;
import org.mozilla.gecko.reading.ReadingListClient;
import org.mozilla.gecko.reading.ReadingListConstants;
import org.mozilla.gecko.reading.ReadingListDeleteDelegate;
import org.mozilla.gecko.reading.ReadingListRecord;
import org.mozilla.gecko.reading.ReadingListRecordResponse;
import org.mozilla.gecko.reading.ReadingListStorage;
import org.mozilla.gecko.reading.ReadingListStorageResponse;
import org.mozilla.gecko.reading.ReadingListSynchronizer;
import org.mozilla.gecko.reading.ReadingListSynchronizerDelegate;
import org.mozilla.gecko.reading.ReadingListWipeDelegate;
import org.mozilla.gecko.sync.net.BasicAuthHeaderProvider;
import org.mozilla.gecko.sync.net.MozResponse;

import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;

public class TestReadingListSynchronizer extends ReadingListTest {
    static final class TestSynchronizerDelegate implements ReadingListSynchronizerDelegate {
        private final CountDownLatch latch;
        public volatile boolean onDownloadCompleteCalled = false;
        public volatile boolean onModifiedUploadCompleteCalled = false;
        public volatile boolean onNewItemUploadCompleteCalled = false;
        public volatile boolean onStatusUploadCompleteCalled = false;
        public volatile boolean onUnableToSyncCalled = false;

        public TestSynchronizerDelegate(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void onUnableToSync(Exception e) {
            onUnableToSyncCalled = true;
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
    }

    private static final String DEFAULT_SERVICE_URI = ReadingListConstants.DEFAULT_DEV_ENDPOINT;
    private static final long TIMEOUT_SECONDS = 10;

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
            latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
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

            CountDownLatch latch = new CountDownLatch(1);
            final TestSynchronizerDelegate delegate = new TestSynchronizerDelegate(latch);
            synchronizer.syncAll(delegate);
            latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertFalse(delegate.onUnableToSyncCalled);
            assertTrue(delegate.onDownloadCompleteCalled);
            assertTrue(delegate.onModifiedUploadCompleteCalled);
            assertTrue(delegate.onNewItemUploadCompleteCalled);
            assertTrue(delegate.onStatusUploadCompleteCalled);

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

            CountDownLatch latch = new CountDownLatch(1);
            final TestSynchronizerDelegate delegate = new TestSynchronizerDelegate(latch);
            synchronizer.syncAll(delegate);
            latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            assertFalse(delegate.onUnableToSyncCalled);
            assertTrue(delegate.onDownloadCompleteCalled);
            assertTrue(delegate.onModifiedUploadCompleteCalled);
            assertTrue(delegate.onStatusUploadCompleteCalled);
            assertTrue(delegate.onNewItemUploadCompleteCalled);

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

            if (guid != null) {
                blindWipe(remote, guid);
            }
        } finally {
            cpc.release();
        }
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
        }, -1L);
    }
}
