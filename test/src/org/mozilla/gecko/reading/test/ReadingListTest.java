/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.reading.test;

import org.mozilla.gecko.background.helpers.AndroidSyncTestCase;
import org.mozilla.gecko.db.BrowserContract;
import org.mozilla.gecko.db.BrowserContract.ReadingListItems;
import org.mozilla.gecko.reading.LocalReadingListStorage;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;

//TODO: this class needs to make sure Gecko isn't running, else it'll try fetching the items!
public class ReadingListTest extends AndroidSyncTestCase {

    protected static final Uri CONTENT_URI = ReadingListItems.CONTENT_URI;
    protected static final Uri CONTENT_URI_IS_SYNC = CONTENT_URI.buildUpon()
                                                                  .appendQueryParameter(BrowserContract.PARAM_IS_SYNC, "1")
                                                                  .build();

    public ReadingListTest() {
        super();
    }

    private ContentProviderClient getClient() {
        final ContentResolver contentResolver = getApplicationContext().getContentResolver();
        final ContentProviderClient client = contentResolver.acquireContentProviderClient(ReadingListItems.CONTENT_URI);
        return client;
    }

    protected ContentProviderClient getWipedLocalClient() throws RemoteException {
        final ContentProviderClient client = getClient();
        client.delete(CONTENT_URI_IS_SYNC, null, null);
        assertTrue(0 == getCount(client));
        return client;
    }

    protected int getCount(ContentProviderClient client) throws RemoteException {
        Cursor cursor = client.query(CONTENT_URI_IS_SYNC, null, null, null, null);
        try {
            return cursor.getCount();
        } finally {
            cursor.close();
        }
    }

    protected void assertCursorCount(int expected, Cursor cursor) {
        try {
            assertTrue(expected == cursor.getCount());
        } finally {
            cursor.close();
        }
    }

    protected void assertIsEmpty(LocalReadingListStorage storage) throws Exception {
        Cursor modified = storage.getModified();
        try {
            assertTrue(0 == modified.getCount());
        } finally {
            modified.close();
        }
    }

}