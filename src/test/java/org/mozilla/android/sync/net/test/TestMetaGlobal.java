/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.net.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.mozilla.gecko.sync.CryptoRecord;
import org.mozilla.gecko.sync.EngineSettings;
import org.mozilla.gecko.sync.MetaGlobal;

public class TestMetaGlobal {
  private static final String TEST_SYNC_ID = "foobar";
  private static final int TEST_STORAGE_VERSION = 117;
  private static final String TEST_META_GLOBAL_RESPONSE = "{\"id\":\"global\",\"payload\":" +
      "\"{\\\"syncID\\\":\\\"" + TEST_SYNC_ID + "\\\",\\\"storageVersion\\\":" + TEST_STORAGE_VERSION + "," +
      "\\\"engines\\\":{" +
      "\\\"clients\\\":{\\\"version\\\":1,\\\"syncID\\\":\\\"fDg0MS5bDtV7\\\"}," +
      "\\\"bookmarks\\\":{\\\"version\\\":2,\\\"syncID\\\":\\\"NNaQr6_F-9dm\\\"}," +
      "\\\"forms\\\":{\\\"version\\\":1,\\\"syncID\\\":\\\"GXF29AFprnvc\\\"}," +
      "\\\"history\\\":{\\\"version\\\":1,\\\"syncID\\\":\\\"av75g4vm-_rp\\\"}," +
      "\\\"passwords\\\":{\\\"version\\\":1,\\\"syncID\\\":\\\"LT_ACGpuKZ6a\\\"}," +
      "\\\"prefs\\\":{\\\"version\\\":2,\\\"syncID\\\":\\\"-3nsksP9wSAs\\\"}," +
      "\\\"tabs\\\":{\\\"version\\\":1,\\\"syncID\\\":\\\"W4H5lOMChkYA\\\"}}}\"," +
      "\"username\":\"5817483\",\"modified\":1.32046073744E9}";

  public void assertMetaGlobalIsCorrect(MetaGlobal mg) {
    assertEquals(TEST_SYNC_ID, mg.syncID);
    assertEquals(TEST_STORAGE_VERSION, mg.storageVersion);
    List<String> engineNames = new ArrayList<String>(mg.getEngineNames());
    Collections.sort(engineNames);
    String[] names = engineNames.toArray(new String[engineNames.size()]);
    String[] expectedNames = new String[] { "bookmarks", "clients", "forms", "history", "passwords", "prefs", "tabs" };
    assertArrayEquals(expectedNames, names);
  }

  @Test
  public void testMetaGlobalSetFromRecord() throws Exception {
    MetaGlobal mg = new MetaGlobal();
    mg.setFromRecord(CryptoRecord.fromJSONRecord(TEST_META_GLOBAL_RESPONSE));
    assertMetaGlobalIsCorrect(mg);
  }

  @Test
  public void testMetaGlobalAsCryptoRecord() throws Exception {
    MetaGlobal mg = new MetaGlobal(CryptoRecord.fromJSONRecord(TEST_META_GLOBAL_RESPONSE));
    CryptoRecord rec = mg.asCryptoRecord();
    assertEquals("meta",   rec.collection);
    assertEquals("global", rec.guid);
    mg = new MetaGlobal();
    mg.setFromRecord(rec);
    assertMetaGlobalIsCorrect(mg);
  }

  @Test
  public void testMetaGlobalEngineSettings() throws Exception {
    MetaGlobal mg = new MetaGlobal(CryptoRecord.fromJSONRecord(TEST_META_GLOBAL_RESPONSE));
    assertMetaGlobalIsCorrect(mg);
    EngineSettings engineSettings = mg.getEngineSettings("XXX NOT HERE");
    assertNull(engineSettings);
    engineSettings = mg.getEngineSettings("prefs");
    assertNotNull(engineSettings);
    assertEquals("-3nsksP9wSAs", engineSettings.syncID);
    assertEquals(2, engineSettings.version);
  }
}
