/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.background.healthreport.upload.test;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;
import org.mozilla.android.sync.test.helpers.MockSharedPreferences;
import org.mozilla.gecko.background.healthreport.upload.ObsoleteDocumentTracker;
import org.mozilla.gecko.sync.ExtendedJSONObject;

public class TestObsoleteDocumentTracker {
  public MockSharedPreferences sharedPrefs;
  public ObsoleteDocumentTracker tracker;

  @Before
  public void setUp() {
    sharedPrefs = new MockSharedPreferences();
    tracker = new ObsoleteDocumentTracker(sharedPrefs);
  }

  @Test
  public void testDecrementObsoleteIdAttempts() {
    ExtendedJSONObject ids = new ExtendedJSONObject();
    ids.put("id1", 5L);
    ids.put("id2", 5L);
    tracker.setObsoleteIds(ids);
    assertEquals(ids, tracker.getObsoleteIds());

    tracker.decrementObsoleteIdAttempts("id1");
    ids.put("id1", 4L);
    assertEquals(ids, tracker.getObsoleteIds());

    tracker.decrementObsoleteIdAttempts("id1"); // 3
    tracker.decrementObsoleteIdAttempts("id1"); // 2
    tracker.decrementObsoleteIdAttempts("id1"); // 1
    tracker.decrementObsoleteIdAttempts("id1"); // 0 (should be gone).
    ids.remove("id1");
    assertEquals(ids, tracker.getObsoleteIds());

    tracker.removeObsoleteId("id2");
    ids.remove("id2");
    assertEquals(ids, tracker.getObsoleteIds());
  }

  @Test
  public void testAddObsoleteId() {
    ExtendedJSONObject ids = new ExtendedJSONObject();
    ids.put("id1", 5L);
    tracker.addObsoleteId("id1");
    assertEquals(ids, tracker.getObsoleteIds());
  }

  @Test
  public void testDecrementObsoleteIdAttemptsSet() {
    ExtendedJSONObject ids = new ExtendedJSONObject();
    ids.put("id1", 5L);
    ids.put("id2", 1L);
    ids.put("id3", -1L); // This should never happen, but just in case.
    tracker.setObsoleteIds(ids);
    assertEquals(ids, tracker.getObsoleteIds());

    HashSet<String> oldIds = new HashSet<String>();
    oldIds.add("id1");
    oldIds.add("id2");
    tracker.decrementObsoleteIdAttempts(oldIds);
    ids.put("id1", 4L);
    ids.remove("id2");
    assertEquals(ids, tracker.getObsoleteIds());
  }
}
