/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import org.mozilla.gecko.db.BrowserContract;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;

/**
 * Exercise Fennec's tabs provider.
 *
 * @author rnewman
 *
 */
public class TestFennecTabsStorage extends AndroidSyncTestCase {
  private ContentProviderClient getClientsClient() {
    final ContentResolver cr = getApplicationContext().getContentResolver();
    return cr.acquireContentProviderClient(BrowserContract.Clients.CONTENT_URI);
  }

  private ContentProviderClient getTabsClient() {
    final ContentResolver cr = getApplicationContext().getContentResolver();
    return cr.acquireContentProviderClient(BrowserContract.Tabs.CONTENT_URI);
  }

  // Sanity.
  public void testObtainCP() {
    final ContentProviderClient clientsClient = getClientsClient();
    assertNotNull(clientsClient);
    clientsClient.release();

    final ContentProviderClient tabsClient = getTabsClient();
    assertNotNull(tabsClient);
    tabsClient.release();
  }

  public void testWipeClients() throws RemoteException {
    final Uri uri = BrowserContract.Clients.CONTENT_URI;
    final ContentProviderClient clientsClient = getClientsClient();

    // Have to ensure that it's empty…
    clientsClient.delete(uri, null, null);

    int deleted = clientsClient.delete(uri, null, null);
    assertEquals(0, deleted);
  }

  public void testWipeTabs() throws RemoteException {
    final Uri uri = BrowserContract.Tabs.CONTENT_URI;
    final ContentProviderClient tabsClient = getTabsClient();

    // Have to ensure that it's empty…
    tabsClient.delete(uri, null, null);

    int deleted = tabsClient.delete(uri, null, null);
    assertEquals(0, deleted);
  }

  public void testStoreAndRetrieveClients() throws RemoteException {
    final Uri uri = BrowserContract.Clients.CONTENT_URI;
    final ContentProviderClient clientsClient = getClientsClient();

    final long now = System.currentTimeMillis();
    final ContentValues first = new ContentValues();
    final ContentValues second = new ContentValues();
    first.put(BrowserContract.Clients.GUID, "abcdefghijkl");
    first.put(BrowserContract.Clients.NAME, "Frist Psot");
    first.put(BrowserContract.Clients.LAST_MODIFIED, now + 1);
    second.put(BrowserContract.Clients.GUID, "mnopqrstuvwx");
    second.put(BrowserContract.Clients.NAME, "Second!!1!");
    second.put(BrowserContract.Clients.LAST_MODIFIED, now + 2);

    ContentValues[] values = new ContentValues[] { first, second };
    final int inserted = clientsClient.bulkInsert(uri, values);
    assertEquals(2, inserted);

    final String since = BrowserContract.Clients.LAST_MODIFIED + " >= ?";
    final String[] nowArg = new String[] { String.valueOf(now) };
    final String guidAscending = BrowserContract.Clients.GUID + " ASC";
    Cursor cursor = clientsClient.query(uri, null, since, nowArg, guidAscending);

    int deleted = -1;
    assertNotNull(cursor);
    try {
      assertTrue(cursor.moveToFirst());
      assertEquals(2, cursor.getCount());

      final String g1 = cursor.getString(cursor.getColumnIndexOrThrow(BrowserContract.Clients.GUID));
      final String n1 = cursor.getString(cursor.getColumnIndexOrThrow(BrowserContract.Clients.NAME));
      final long m1   = cursor.getLong(cursor.getColumnIndexOrThrow(BrowserContract.Clients.LAST_MODIFIED));
      assertEquals(first.get(BrowserContract.Clients.GUID), g1);
      assertEquals(first.get(BrowserContract.Clients.NAME), n1);
      assertEquals(now + 1, m1);

      assertTrue(cursor.moveToNext());
      final String g2 = cursor.getString(cursor.getColumnIndexOrThrow(BrowserContract.Clients.GUID));
      final String n2 = cursor.getString(cursor.getColumnIndexOrThrow(BrowserContract.Clients.NAME));
      final long m2   = cursor.getLong(cursor.getColumnIndexOrThrow(BrowserContract.Clients.LAST_MODIFIED));
      assertEquals(second.get(BrowserContract.Clients.GUID), g2);
      assertEquals(second.get(BrowserContract.Clients.NAME), n2);
      assertEquals(now + 2, m2);

      assertFalse(cursor.moveToNext());
    } finally {
      cursor.close();

      deleted = clientsClient.delete(uri, null, null);
    }
    assertEquals(2, deleted);
  }
}
