 /* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.reading.test;

import org.mozilla.gecko.db.BrowserContract.ReadingListItems;
import org.mozilla.gecko.reading.ClientReadingListRecord;
import org.mozilla.gecko.reading.LocalReadingListStorage;
import org.mozilla.gecko.reading.ReadingListClientRecordFactory;

import android.content.ContentProviderClient;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

public class TestLocalReadingListStorage extends ReadingListTest {
    private static ContentValues getRecordA() {
        final ContentValues values = new ContentValues();
        values.put("url", "http://example.org/");
        values.put("title", "Example");
        values.put("content_status", ReadingListItems.STATUS_UNFETCHED);
        values.put(ReadingListItems.ADDED_ON, System.currentTimeMillis());
        values.put(ReadingListItems.ADDED_BY, "$local");
        return values;
    }

    // This is how ReadingListHelper does it.
    private long addRecordA(ContentProviderClient client) throws Exception {
        final ContentValues recordA = getRecordA();
        Uri inserted = client.insert(CONTENT_URI, recordA);
        assertNotNull(inserted);
        return ContentUris.parseId(inserted);
    }

    private long addRecordASynced(ContentProviderClient client) throws Exception {
        ContentValues values = getRecordA();
        values.put(ReadingListItems.SYNC_STATUS, ReadingListItems.SYNC_STATUS_SYNCED);
        values.put(ReadingListItems.SYNC_CHANGE_FLAGS, ReadingListItems.SYNC_CHANGE_NONE);
        values.put(ReadingListItems.GUID, "abcdefghi");
        Uri inserted = client.insert(CONTENT_URI_IS_SYNC, values);
        assertNotNull(inserted);
        return ContentUris.parseId(inserted);
    }

    public final void testGetModified() throws Exception {
        final ContentProviderClient client = getWipedLocalClient();
        try {
            final LocalReadingListStorage storage = new LocalReadingListStorage(client);
            assertIsEmpty(storage);

            addRecordA(client);
            assertTrue(1 == getCount(client));
            assertCursorCount(0, storage.getModified());
            assertCursorCount(1, storage.getNew());
        } finally {
            client.release();
        }
    }

    public final void testGetStatusChanges() throws Exception {
        final ContentProviderClient client = getWipedLocalClient();
        try {
            final LocalReadingListStorage storage = new LocalReadingListStorage(client);
            assertIsEmpty(storage);

            long id = addRecordASynced(client);
            assertTrue(1 == getCount(client));

            assertCursorCount(0, storage.getModified());
            assertCursorCount(0, storage.getNew());

            // Make a status change.
            ContentValues v = new ContentValues();
            v.put(ReadingListItems.SYNC_CHANGE_FLAGS, ReadingListItems.SYNC_CHANGE_UNREAD_CHANGED);
            v.put(ReadingListItems.MARKED_READ_ON, System.currentTimeMillis());
            v.put(ReadingListItems.MARKED_READ_BY, "$this");        // TODO: test this substitution.
            v.put(ReadingListItems.IS_UNREAD, 0);
            assertEquals(1, client.update(CONTENT_URI, v, ReadingListItems._ID + " = " + id, null));
            assertCursorCount(0, storage.getNew());
            assertCursorCount(1, storage.getStatusChanges());
            assertCursorCount(0, storage.getNonStatusModified());
            assertCursorCount(1, storage.getModified());           // Modified includes status.
        } finally {
            client.release();
        }
    }

    public final void testGetNonStatusChanges() throws Exception {
      final ContentProviderClient client = getWipedLocalClient();
      try {
          final LocalReadingListStorage storage = new LocalReadingListStorage(client);
          assertIsEmpty(storage);

          long id = addRecordASynced(client);
          assertTrue(1 == getCount(client));

          assertCursorCount(0, storage.getModified());
          assertCursorCount(0, storage.getNew());

          // Make a material change.
          ContentValues v = new ContentValues();
          v.put(ReadingListItems.SYNC_CHANGE_FLAGS, ReadingListItems.SYNC_CHANGE_RESOLVED);
          v.put(ReadingListItems.EXCERPT, Long.toString(System.currentTimeMillis()));
          assertEquals(1, client.update(CONTENT_URI, v, ReadingListItems._ID + " = " + id, null));
          assertCursorCount(0, storage.getNew());
          assertCursorCount(0, storage.getStatusChanges());
          assertCursorCount(1, storage.getNonStatusModified());
          assertCursorCount(1, storage.getModified());            // Modified includes material/non-status.
      } finally {
        client.release();
      }
    }

    /**
     * This exercises the in-place $local -> device name translation that we
     * use to avoid figuring out the client name in multiple places.
     */
    public final void testNameRewriting() throws Exception {
        final ContentProviderClient client = getWipedLocalClient();
        try {
            final LocalReadingListStorage storage = new LocalReadingListStorage(client);
            assertIsEmpty(storage);

            addRecordA(client);
            assertTrue(1 == getCount(client));
            assertCursorCount(0, storage.getModified());

            storage.updateLocalNames("Foo Bar");
            Cursor cursor = storage.getNew();
            try {
                assertTrue(cursor.moveToFirst());
                String addedBy = cursor.getString(cursor.getColumnIndexOrThrow(ReadingListItems.ADDED_BY));
                assertEquals("Foo Bar", addedBy);
            } finally {
                cursor.close();
            }
        } finally {
            client.release();
        }
    }

    public final void testGetNew() throws Exception {
        final ContentProviderClient client = getWipedLocalClient();
        try {
            final LocalReadingListStorage storage = new LocalReadingListStorage(client);
            assertIsEmpty(storage);

            addRecordA(client);
            assertTrue(1 == getCount(client));
            assertCursorCount(0, storage.getModified());
            final Cursor cursor = storage.getNew();
            try {
                assertTrue(1 == cursor.getCount());
                cursor.moveToFirst();
                ClientReadingListRecord record = new ReadingListClientRecordFactory(cursor).fromCursorRow();

                final String url = "http://example.org/";
                final String title = "Example";
                final int contentStatus = ReadingListItems.STATUS_UNFETCHED;
                final String addedBy = "$local";
                final int syncStatus = ReadingListItems.SYNC_STATUS_NEW;
                final int syncChanges = ReadingListItems.SYNC_CHANGE_NONE;

                assertEquals(syncStatus, cursor.getInt(cursor.getColumnIndex(ReadingListItems.SYNC_STATUS)));
                assertEquals(syncChanges, cursor.getInt(cursor.getColumnIndex(ReadingListItems.SYNC_CHANGE_FLAGS)));
                assertEquals(contentStatus, cursor.getInt(cursor.getColumnIndex(ReadingListItems.CONTENT_STATUS)));

                assertEquals(title, cursor.getString(cursor.getColumnIndex(ReadingListItems.TITLE)));
                assertEquals(title, record.getTitle());
                assertEquals(url, record.getURL());
                assertEquals(addedBy, record.getAddedBy());
            } finally {
                cursor.close();
            }
        } finally {
            client.release();
        }
    }
}
