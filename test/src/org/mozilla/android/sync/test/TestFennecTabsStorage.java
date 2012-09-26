/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import org.json.simple.JSONArray;
import org.mozilla.gecko.db.BrowserContract;
import org.mozilla.gecko.sync.repositories.android.FennecTabsRepository;
import org.mozilla.gecko.sync.repositories.android.RepoUtils;
import org.mozilla.gecko.sync.repositories.domain.TabsRecord;
import org.mozilla.gecko.sync.repositories.domain.TabsRecord.Tab;

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
  public static final String TEST_CLIENT_GUID = "test guid"; // Real GUIDs never contain spaces.
  public static final String TEST_CLIENT_NAME = "test client name";

  public static final String TABS_CLIENT_GUID_IS = BrowserContract.Tabs.CLIENT_GUID + " = ?";

  protected Tab testTab1;
  protected Tab testTab2;
  protected Tab testTab3;

  private ContentProviderClient getClientsClient() {
    final ContentResolver cr = getApplicationContext().getContentResolver();
    return cr.acquireContentProviderClient(BrowserContract.Clients.CONTENT_URI);
  }

  private ContentProviderClient getTabsClient() {
    final ContentResolver cr = getApplicationContext().getContentResolver();
    return cr.acquireContentProviderClient(BrowserContract.Tabs.CONTENT_URI);
  }

  protected int deleteAllTestTabs(final ContentProviderClient tabsClient) throws RemoteException {
    if (tabsClient == null) {
      return -1;
    }
    return tabsClient.delete(BrowserContract.Tabs.CONTENT_URI, TABS_CLIENT_GUID_IS, new String[] { TEST_CLIENT_GUID });
  }

  protected void tearDown() throws Exception {
    deleteAllTestTabs(getTabsClient());
  }

  @SuppressWarnings("unchecked")
  private void insertSomeTestTabs(ContentProviderClient tabsClient) throws RemoteException {
    final JSONArray history1 = new JSONArray();
    history1.add("http://test.com/test1.html");
    testTab1 = new Tab("test title 1", "http://test.com/test1.png", history1, 1000);

    final JSONArray history2 = new JSONArray();
    history2.add("http://test.com/test2.html#1");
    history2.add("http://test.com/test2.html#2");
    history2.add("http://test.com/test2.html#3");
    testTab2 = new Tab("test title 2", "http://test.com/test2.png", history2, 2000);

    final JSONArray history3 = new JSONArray();
    history3.add("http://test.com/test3.html#1");
    history3.add("http://test.com/test3.html#2");
    testTab3 = new Tab("test title 3", "http://test.com/test3.png", history3, 3000);

    tabsClient.insert(BrowserContract.Tabs.CONTENT_URI, testTab1.toContentValues(TEST_CLIENT_GUID, 0));
    tabsClient.insert(BrowserContract.Tabs.CONTENT_URI, testTab2.toContentValues(TEST_CLIENT_GUID, 1));
    tabsClient.insert(BrowserContract.Tabs.CONTENT_URI, testTab3.toContentValues(TEST_CLIENT_GUID, 2));
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
    final ContentProviderClient tabsClient = getTabsClient();

    // Have to ensure that it's empty…
    deleteAllTestTabs(tabsClient);

    int deleted = deleteAllTestTabs(tabsClient);
    assertEquals(0, deleted);
  }

  public void testStoreAndRetrieveClients() throws RemoteException {
    final Uri uri = BrowserContract.Clients.CONTENT_URI;
    final ContentProviderClient clientsClient = getClientsClient();

    // Have to ensure that it's empty…
    clientsClient.delete(uri, null, null);

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
    }

    int deleted = clientsClient.delete(uri, null, null);
    assertEquals(2, deleted);
  }

  public void testTabFromCursor() throws Exception {
    final ContentProviderClient tabsClient = getTabsClient();

    deleteAllTestTabs(tabsClient);
    insertSomeTestTabs(tabsClient);

    final String positionAscending = BrowserContract.Tabs.POSITION + " ASC";
    Cursor cursor = null;
    try {
      cursor = tabsClient.query(BrowserContract.Tabs.CONTENT_URI, null, TABS_CLIENT_GUID_IS, new String[] { TEST_CLIENT_GUID }, positionAscending);
      assertEquals(3, cursor.getCount());

      cursor.moveToFirst();
      final Tab parsed1 = FennecTabsRepository.tabFromCursor(cursor);
      assertEquals(testTab1, parsed1);

      cursor.moveToNext();
      final Tab parsed2 = FennecTabsRepository.tabFromCursor(cursor);
      assertEquals(testTab2, parsed2);

      cursor.moveToPosition(2);
      final Tab parsed3 = FennecTabsRepository.tabFromCursor(cursor);
      assertEquals(testTab3, parsed3);
    } finally {
      cursor.close();
    }
  }

  public void testTabsRecordFromCursor() throws Exception {
    final ContentProviderClient tabsClient = getTabsClient();

    deleteAllTestTabs(tabsClient);
    insertSomeTestTabs(tabsClient);

    final String positionAscending = BrowserContract.Tabs.POSITION + " ASC";
    Cursor cursor = null;
    try {
      cursor = tabsClient.query(BrowserContract.Tabs.CONTENT_URI, null, TABS_CLIENT_GUID_IS, new String[] { TEST_CLIENT_GUID }, positionAscending);
      assertEquals(3, cursor.getCount());

      cursor.moveToPosition(1);

      final TabsRecord tabsRecord = FennecTabsRepository.tabsRecordFromCursor(cursor, TEST_CLIENT_GUID, TEST_CLIENT_NAME);

      // Make sure we clean up after ourselves.
      assertEquals(1, cursor.getPosition());

      assertEquals(TEST_CLIENT_GUID, tabsRecord.guid);
      assertEquals(TEST_CLIENT_NAME, tabsRecord.clientName);

      assertEquals(3, tabsRecord.tabs.size());
      assertEquals(testTab1, tabsRecord.tabs.get(0));
      assertEquals(testTab2, tabsRecord.tabs.get(1));
      assertEquals(testTab3, tabsRecord.tabs.get(2));

      assertEquals(Math.max(Math.max(testTab1.lastUsed, testTab2.lastUsed), testTab3.lastUsed), tabsRecord.lastModified);
    } finally {
      cursor.close();
    }
  }

  // Verify that we can fetch a record when there are no local tabs at all.
  public void testEmptyTabsRecordFromCursor() throws Exception {
    final ContentProviderClient tabsClient = getTabsClient();

    deleteAllTestTabs(tabsClient);

    final String positionAscending = BrowserContract.Tabs.POSITION + " ASC";
    Cursor cursor = null;
    try {
      cursor = tabsClient.query(BrowserContract.Tabs.CONTENT_URI, null, TABS_CLIENT_GUID_IS, new String[] { TEST_CLIENT_GUID }, positionAscending);
      assertEquals(0, cursor.getCount());

      final TabsRecord tabsRecord = FennecTabsRepository.tabsRecordFromCursor(cursor, TEST_CLIENT_GUID, TEST_CLIENT_NAME);

      assertEquals(TEST_CLIENT_GUID, tabsRecord.guid);
      assertEquals(TEST_CLIENT_NAME, tabsRecord.clientName);

      assertNotNull(tabsRecord.tabs);
      assertEquals(0, tabsRecord.tabs.size());

      assertEquals(0, tabsRecord.lastModified);
    } finally {
      cursor.close();
    }
  }

  // Not much of a test, but verifies the tabs record at least agrees with the
  // disk data and doubles as a database inspector.
  public void testLocalTabs() throws Exception {
    final ContentProviderClient tabsClient = getTabsClient();

    final String positionAscending = BrowserContract.Tabs.POSITION + " ASC";
    Cursor cursor = null;
    try {
      // Keep this in sync with the Fennec schema.
      cursor = tabsClient.query(BrowserContract.Tabs.CONTENT_URI, null, BrowserContract.Tabs.CLIENT_GUID + " IS NULL", null, positionAscending);
      RepoUtils.dumpCursor(cursor);

      final TabsRecord tabsRecord = FennecTabsRepository.tabsRecordFromCursor(cursor, TEST_CLIENT_GUID, TEST_CLIENT_NAME);

      assertEquals(TEST_CLIENT_GUID, tabsRecord.guid);
      assertEquals(TEST_CLIENT_NAME, tabsRecord.clientName);

      assertNotNull(tabsRecord.tabs);
      assertEquals(cursor.getCount(), tabsRecord.tabs.size());
    } finally {
      cursor.close();
    }
  }
}
