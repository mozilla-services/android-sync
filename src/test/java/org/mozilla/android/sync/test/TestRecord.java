/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Test;
import org.mozilla.gecko.sync.CryptoRecord;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.NonArrayJSONException;
import org.mozilla.gecko.sync.NonObjectJSONException;
import org.mozilla.gecko.sync.repositories.domain.BookmarkRecord;
import org.mozilla.gecko.sync.repositories.domain.ClientRecord;
import org.mozilla.gecko.sync.repositories.domain.HistoryRecord;
import org.mozilla.gecko.sync.repositories.domain.Record;
import org.mozilla.gecko.sync.repositories.domain.TabsRecord.Tab;

public class TestRecord {

  @Test
  public void testRecordGUIDs() {
    for (int i = 0; i < 50; ++i) {
      CryptoRecord cryptoRecord = new HistoryRecord().getEnvelope();
      assertEquals(12, cryptoRecord.guid.length());
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

  public static class URITestBookmarkRecord extends BookmarkRecord {
    public static void doTest() {
      assertEquals("places:uri=abc%26def+baz&p1=123&p2=bar+baz",
                   encodeUnsupportedTypeURI("abc&def baz", "p1", "123", "p2", "bar baz"));
      assertEquals("places:uri=abc%26def+baz&p1=123",
                   encodeUnsupportedTypeURI("abc&def baz", "p1", "123", null, "bar baz"));
      assertEquals("places:p1=123&p2=",
                   encodeUnsupportedTypeURI(null, "p1", "123", "p2", null));
    }
  }

  @Test
  public void testEncodeURI() {
    URITestBookmarkRecord.doTest();
  }

  private static final String payload =
     "{\"id\":\"M5bwUKK8hPyF\"," +
      "\"type\":\"livemark\"," +
      "\"siteUri\":\"http://www.bbc.co.uk/go/rss/int/news/-/news/\"," +
      "\"feedUri\":\"http://fxfeeds.mozilla.com/en-US/firefox/headlines.xml\"," +
      "\"parentName\":\"Bookmarks Toolbar\"," +
      "\"parentid\":\"toolbar\"," +
      "\"title\":\"Latest Headlines\"," +
      "\"description\":\"\"," +
      "\"children\":" +
        "[\"7oBdEZB-8BMO\", \"SUd1wktMNCTB\", \"eZe4QWzo1BcY\", \"YNBhGwhVnQsN\"," +
         "\"mNTdpgoRZMbW\", \"-L8Vci6CbkJY\", \"bVzudKSQERc1\", \"Gxl9lb4DXsmL\"," +
         "\"3Qr13GucOtEh\"]}";

  public class PayloadBookmarkRecord extends BookmarkRecord {
    public PayloadBookmarkRecord() {
      super("abcdefghijkl", "bookmarks", 1234, false);
    }

    public void doTest() throws NonObjectJSONException, IOException, ParseException {
      this.initFromPayload(new ExtendedJSONObject(payload));
      assertEquals("abcdefghijkl",      this.guid);              // Ignores payload.
      assertEquals("livemark",          this.type);
      assertEquals("Bookmarks Toolbar", this.parentName);
      assertEquals("toolbar",           this.parentID);
      assertEquals("",                  this.description);
      assertEquals(null,                this.children);

      final String encodedSite = "http%3A%2F%2Fwww.bbc.co.uk%2Fgo%2Frss%2Fint%2Fnews%2F-%2Fnews%2F";
      final String encodedFeed = "http%3A%2F%2Ffxfeeds.mozilla.com%2Fen-US%2Ffirefox%2Fheadlines.xml";
      final String expectedURI = "places:siteUri=" + encodedSite + "&feedUri=" + encodedFeed;
      assertEquals(expectedURI, this.bookmarkURI);
    }
  }

  @Test
  public void testUnusualBookmarkRecords() throws NonObjectJSONException, IOException, ParseException {
    PayloadBookmarkRecord record = new PayloadBookmarkRecord();
    record.doTest();
  }

  @Test
  public void testTTL() {
    Record record = new HistoryRecord();
    assertEquals(HistoryRecord.HISTORY_TTL, record.ttl);

    // ClientRecords are transient, HistoryRecords are not.
    Record clientRecord = new ClientRecord();
    assertTrue(clientRecord.ttl < record.ttl);

    CryptoRecord cryptoRecord = record.getEnvelope();
    assertEquals(record.ttl, cryptoRecord.ttl);
  }
}
