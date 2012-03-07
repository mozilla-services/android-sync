/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Test;
import org.mozilla.gecko.sync.CryptoRecord;
import org.mozilla.gecko.sync.NonArrayJSONException;
import org.mozilla.gecko.sync.repositories.domain.BookmarkRecord;
import org.mozilla.gecko.sync.repositories.domain.HistoryRecord;
import org.mozilla.gecko.sync.repositories.domain.TabsRecord.Tab;

public class TestRecord {

  @Test
  public void testRecordGUIDs() {
    for (int i = 0; i < 50; ++i) {
      CryptoRecord cryptoRecord = new HistoryRecord().getEnvelope();
      assertEquals(12, cryptoRecord.guid.length());
      System.out.println(cryptoRecord.toJSONString());
    }
  }

  @Test
  public void testRecordEquality() {
    long now = System.currentTimeMillis();
    BookmarkRecord bOne = new BookmarkRecord("abcdefghijkl", "bookmarks", now , false);
    BookmarkRecord bTwo = new BookmarkRecord("abcdefghijkl", "bookmarks", now , false);
    HistoryRecord hOne = new HistoryRecord("mbcdefghijkm", "history", now , false);
    HistoryRecord hTwo = new HistoryRecord("mbcdefghijkm", "history", now , false);

    // Identical records.
    assertFalse(bOne == bTwo);
    assertTrue(bOne.equals(bTwo));
    assertTrue(bOne.equalPayloads(bTwo));
    assertTrue(bOne.congruentWith(bTwo));
    assertTrue(bTwo.equals(bOne));
    assertTrue(bTwo.equalPayloads(bOne));
    assertTrue(bTwo.congruentWith(bOne));

    // Null checking.
    assertFalse(bOne.equals(null));
    assertFalse(bOne.equalPayloads(null));
    assertFalse(bOne.congruentWith(null));

    // Different types.
    hOne.guid = bOne.guid;
    assertFalse(bOne.equals(hOne));
    assertFalse(bOne.equalPayloads(hOne));
    assertFalse(bOne.congruentWith(hOne));
    hOne.guid = hTwo.guid;

    // Congruent androidID.
    bOne.androidID = 1;
    assertFalse(bOne.equals(bTwo));
    assertTrue(bOne.equalPayloads(bTwo));
    assertTrue(bOne.congruentWith(bTwo));
    assertFalse(bTwo.equals(bOne));
    assertTrue(bTwo.equalPayloads(bOne));
    assertTrue(bTwo.congruentWith(bOne));

    // Non-congruent androidID.
    bTwo.androidID = 2;
    assertFalse(bOne.equals(bTwo));
    assertTrue(bOne.equalPayloads(bTwo));
    assertFalse(bOne.congruentWith(bTwo));
    assertFalse(bTwo.equals(bOne));
    assertTrue(bTwo.equalPayloads(bOne));
    assertFalse(bTwo.congruentWith(bOne));

    // Identical androidID.
    bOne.androidID = 2;
    assertTrue(bOne.equals(bTwo));
    assertTrue(bOne.equalPayloads(bTwo));
    assertTrue(bOne.congruentWith(bTwo));
    assertTrue(bTwo.equals(bOne));
    assertTrue(bTwo.equalPayloads(bOne));
    assertTrue(bTwo.congruentWith(bOne));

    // Different times.
    bTwo.lastModified += 1000;
    assertFalse(bOne.equals(bTwo));
    assertTrue(bOne.equalPayloads(bTwo));
    assertTrue(bOne.congruentWith(bTwo));
    assertFalse(bTwo.equals(bOne));
    assertTrue(bTwo.equalPayloads(bOne));
    assertTrue(bTwo.congruentWith(bOne));

    // Add some visits.
    JSONObject v1 = fakeVisit(now - 1000);
    JSONObject v2 = fakeVisit(now - 500);

    hOne.fennecDateVisited = now + 2000;
    hOne.fennecVisitCount  = 1;
    assertFalse(hOne.equals(hTwo));
    assertTrue(hOne.equalPayloads(hTwo));
    assertTrue(hOne.congruentWith(hTwo));
    addVisit(hOne, v1);
    assertFalse(hOne.equals(hTwo));
    assertFalse(hOne.equalPayloads(hTwo));
    assertTrue(hOne.congruentWith(hTwo));
    addVisit(hTwo, v2);
    assertFalse(hOne.equals(hTwo));
    assertFalse(hOne.equalPayloads(hTwo));
    assertTrue(hOne.congruentWith(hTwo));

    // Now merge the visits.
    addVisit(hTwo, v1);
    addVisit(hOne, v2);
    assertFalse(hOne.equals(hTwo));
    assertTrue(hOne.equalPayloads(hTwo));
    assertTrue(hOne.congruentWith(hTwo));
    hTwo.fennecDateVisited = hOne.fennecDateVisited;
    hTwo.fennecVisitCount = hOne.fennecVisitCount = 2;
    assertTrue(hOne.equals(hTwo));
    assertTrue(hOne.equalPayloads(hTwo));
    assertTrue(hOne.congruentWith(hTwo));
  }

  @SuppressWarnings("unchecked")
  private void addVisit(HistoryRecord r, JSONObject visit) {
    if (r.visits == null) {
      r.visits = new JSONArray();
    }
    r.visits.add(visit);
  }

  @SuppressWarnings("unchecked")
  private JSONObject fakeVisit(long time) {
    JSONObject object = new JSONObject();
    object.put("type", 1L);
    object.put("date", time * 1000);
    return object;
  }

  @Test
  public void testTabParsing() throws ParseException, NonArrayJSONException {
    String json = "{\"title\":\"mozilla-central mozilla/browser/base/content/syncSetup.js\"," +
                  " \"urlHistory\":[\"http://mxr.mozilla.org/mozilla-central/source/browser/base/content/syncSetup.js#72\"]," +
                  " \"icon\":\"http://mxr.mozilla.org/mxr.png\"," +
                  " \"lastUsed\":\"1306374531\"}";
    JSONParser p = new JSONParser();
    Tab tab = Tab.fromJSONObject((JSONObject) p.parse(json));
    assertEquals("mozilla-central mozilla/browser/base/content/syncSetup.js", tab.title);
    assertEquals("http://mxr.mozilla.org/mxr.png", tab.icon);
    assertEquals("http://mxr.mozilla.org/mozilla-central/source/browser/base/content/syncSetup.js#72", tab.history.get(0));
    assertEquals(1306374531000L, tab.lastUsed);

    String zeroJSON = "{\"title\":\"a\"," +
        " \"urlHistory\":[\"http://example.com\"]," +
        " \"icon\":\"\"," +
        " \"lastUsed\":0}";
    Tab zero = Tab.fromJSONObject((JSONObject) p.parse(zeroJSON));
    assertEquals("a", zero.title);
    assertEquals("", zero.icon);
    assertEquals("http://example.com", zero.history.get(0));
    assertEquals(0L, zero.lastUsed);
  }
}