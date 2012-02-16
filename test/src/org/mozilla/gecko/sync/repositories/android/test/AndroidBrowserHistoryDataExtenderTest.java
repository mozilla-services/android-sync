/* Any copyright is dedicated to the Public Domain.
http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.sync.repositories.android.test;

import java.io.IOException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.NonArrayJSONException;
import org.mozilla.gecko.sync.NonObjectJSONException;
import org.mozilla.gecko.sync.StubActivity;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.repositories.NullCursorException;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserHistoryDataExtender;
import org.mozilla.gecko.sync.repositories.android.RepoUtils;

import android.content.Context;
import android.database.Cursor;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

public class AndroidBrowserHistoryDataExtenderTest extends ActivityInstrumentationTestCase2<StubActivity> {

  protected AndroidBrowserHistoryDataExtender extender;
  protected static final String LOG_TAG = "SyncHistoryVisitsTest";

  public AndroidBrowserHistoryDataExtenderTest() {
    super(StubActivity.class);
  }

  public Context getApplicationContext() {
    return this.getInstrumentation().getTargetContext().getApplicationContext();
  }

  public void setUp() {
    Log.i(LOG_TAG, "Wiping.");
    extender = new AndroidBrowserHistoryDataExtender(getApplicationContext());
    extender.wipe();
  }

  public void testStoreFetch() throws NullCursorException, NonObjectJSONException, IOException, ParseException {
    String guid = Utils.generateGuid();
    extender.store(Utils.generateGuid(), null);
    extender.store(guid, null);
    extender.store(Utils.generateGuid(), null);

    Cursor cur = null;
    try {
      cur = extender.fetch(guid);
      assertEquals(1, cur.getCount());
      assertTrue(cur.moveToFirst());
      assertEquals(guid, cur.getString(0));
    } finally {
      if (cur != null) {
        cur.close();
      }
    }
  }

  public void testVisitsForGUID() throws NonArrayJSONException, NonObjectJSONException, IOException, ParseException, NullCursorException {
    String guid = Utils.generateGuid();
    JSONArray visits = new ExtendedJSONObject("{ \"visits\": [ { \"key\" : \"value\" } ] }").getArray("visits");

    extender.store(Utils.generateGuid(), null);
    extender.store(guid, visits);
    extender.store(Utils.generateGuid(), null);

    JSONArray fetchedVisits = extender.visitsForGUID(guid);
    assertEquals(1, fetchedVisits.size());
    assertEquals("value", ((JSONObject)fetchedVisits.get(0)).get("key"));
  }

  public void testDeleteHandlesBadGUIDs() {
    String evilGUID = "' or '1'='1";
    extender.store(Utils.generateGuid(), null);
    extender.store(Utils.generateGuid(), null);
    extender.store(evilGUID, null);
    extender.delete(evilGUID);

    Cursor cur = null;
    try {
      cur = extender.fetchAll();
      assertEquals(cur.getCount(), 2);
      assertTrue(cur.moveToFirst());
      while (!cur.isAfterLast()) {
        String guid = RepoUtils.getStringFromCursor(cur, AndroidBrowserHistoryDataExtender.COL_GUID);
        assertFalse(evilGUID.equals(guid));
        cur.moveToNext();
      }
    } catch (NullCursorException e) {
      e.printStackTrace();
      fail("Should not have null cursor.");
    } finally {
      if (cur != null) {
        cur.close();
      }
      extender.close();
    }
  }

  public void testStoreFetchHandlesBadGUIDs() {
    String evilGUID = "' or '1'='1";
    extender.store(Utils.generateGuid(), null);
    extender.store(Utils.generateGuid(), null);
    extender.store(evilGUID, null);

    Cursor cur = null;
    try {
      cur = extender.fetch(evilGUID);
      assertEquals(1, cur.getCount());
      assertTrue(cur.moveToFirst());
      while (!cur.isAfterLast()) {
        String guid = RepoUtils.getStringFromCursor(cur, AndroidBrowserHistoryDataExtender.COL_GUID);
        assertEquals(evilGUID, guid);
        cur.moveToNext();
      }
    } catch (NullCursorException e) {
      e.printStackTrace();
      fail("Should not have null cursor.");
    } finally {
      if (cur != null) {
        cur.close();
      }
      extender.close();
    }
  }
}
