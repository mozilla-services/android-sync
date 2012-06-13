/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.sync.synchronizer.managers.test;

import java.util.ArrayList;
import java.util.HashSet;

import org.mozilla.android.sync.test.AndroidSyncTestCase;
import org.mozilla.gecko.sync.synchronizer.managers.SQLiteGuidsManager;

public class TestSQLiteGuidsManager extends AndroidSyncTestCase {
  protected SQLiteGuidsManager manager = null;

  protected void setUp() throws Exception {
    super.setUp();

    // Set up a separate collection that should be ignored.
    manager = new SQLiteGuidsManager(getApplicationContext(), "separate", 5, 2);
    manager.wipeDB();

    ArrayList<String> guids = new ArrayList<String>();
    for (int i = 0; i < 10; i++) {
      guids.add("test" + i);
    }
    manager.addFreshGuids(guids);
    manager.close();

    manager = new SQLiteGuidsManager(getApplicationContext(), "test", 5, 2);
  }

  protected void tearDown() throws Exception {
    if (manager != null) {
      manager.close();
    }

    // Verify separate collection is untouched.
    try {
      manager = new SQLiteGuidsManager(getApplicationContext(), "separate", 5, 2);
      assertEquals(5, manager.nextGuids().size());
    } finally {
      if (manager != null) {
        manager.close();
      }
    }
    super.tearDown();
  }

  /**
   * Verify that adding GUIDs works.
   */
  public void testAddFreshGuids() throws Exception {
    ArrayList<String> guids = new ArrayList<String>();
    for (int i = 0; i < 10; i++) {
      guids.add("test" + i);
    }
    manager.addFreshGuids(guids);

    guids.clear();
    for (int i = 5; i < 10; i++) {
      guids.add("test" + i);
    }
    assertEquals(new HashSet<String>(guids), new HashSet<String>(manager.nextGuids()));

    // Now add some new GUIDs and check they come out first.
    guids.clear();
    for (int i = 10; i < 20; i++) {
      guids.add("test" + i);
    }
    manager.addFreshGuids(guids);

    guids.clear();
    for (int i = 15; i < 20; i++) {
      guids.add("test" + i);
    }
    assertEquals(new HashSet<String>(guids), new HashSet<String>(manager.nextGuids()));
  }

  /**
   * Verify that adding existing GUIDs works.
   */
  public void testFreshenGuids() throws Exception {
    ArrayList<String> guids = new ArrayList<String>();
    for (int i = 0; i < 10; i++) {
      guids.add("test" + i);
    }
    manager.addFreshGuids(guids);

    guids.clear();
    for (int i = 5; i < 10; i++) {
      guids.add("test" + i);
    }
    assertEquals(new HashSet<String>(guids), new HashSet<String>(manager.nextGuids()));

    // Now add some of the existing and re-add some of the deleted GUIDs and see
    // they come out correctly.
    guids.clear();
    for (int i = 3; i < 8; i++) {
      guids.add("test" + i);
    }
    manager.addFreshGuids(guids);
    assertEquals(new HashSet<String>(guids), new HashSet<String>(manager.nextGuids()));
  }

  /**
   * Verify that removing existing GUIDs works.
   */
  public void testRemoveGuids() throws Exception {
    ArrayList<String> guids = new ArrayList<String>();
    for (int i = 0; i < 10; i++) {
      guids.add("test" + i);
    }
    manager.addFreshGuids(guids);

    // Remove GUIDs.
    guids.clear();
    for (int i = 3; i < 9; i++) {
      guids.add("test" + i);
    }
    manager.removeGuids(guids);

    // Check remaining GUIDs.
    guids.clear();
    for (int i = 0; i < 3; i++) {
      guids.add("test" + i);
    }
    guids.add("test" + 9);
    assertEquals(new HashSet<String>(guids), new HashSet<String>(manager.nextGuids()));
  }

  /**
   * Verify that refreshing existing GUIDs works.
   */
  public void testRefreshGuids() throws Exception {
    ArrayList<String> guids = new ArrayList<String>();
    for (int i = 0; i < 10; i++) {
      guids.add("test" + i);
    }
    manager.addFreshGuids(guids);

    // Retry GUIDs.
    manager.retryGuids(guids);

    guids.clear();
    for (int i = 5; i < 10; i++) {
      guids.add("test" + i);
    }
    manager.retryGuids(guids);

    // Check that we've expired most GUIDs.
    guids.clear();
    for (int i = 0; i < 5; i++) {
      guids.add("test" + i);
    }
    assertEquals(new HashSet<String>(guids), new HashSet<String>(manager.nextGuids()));

    // If we re-add, they should freshen back up.
    guids.clear();
    for (int i = 8; i < 10; i++) {
      guids.add("test" + i);
    }
    manager.addFreshGuids(guids);

    // And some old ones are still fresh.
    guids.add("test2");
    guids.add("test3");
    guids.add("test4");
    assertEquals(new HashSet<String>(guids), new HashSet<String>(manager.nextGuids()));

    // Expire everything; only 8 and 9 should remain, since they were re-freshed.
    guids.clear();
    for (int i = 0; i < 10; i++) {
      guids.add("test" + i);
    }
    manager.retryGuids(guids);

    guids.clear();
    for (int i = 8; i < 10; i++) {
      guids.add("test" + i);
    }
    assertEquals(new HashSet<String>(guids), new HashSet<String>(manager.nextGuids()));
  }

  public void testNumGuidsRemaining() throws Exception {
    ArrayList<String> guids = new ArrayList<String>();
    for (int i = 0; i < 10; i++) {
      guids.add("test" + i);
    }
    manager.addFreshGuids(guids);
    assertEquals(10, manager.numGuidsRemaining());

    guids.clear();
    for (int i = 8; i < 10; i++) {
      guids.add("test" + i);
    }
    manager.removeGuids(guids);
    assertEquals(8, manager.numGuidsRemaining());

    guids.clear();
    for (int i = 0; i < 3; i++) {
      guids.add("test" + i);
    }
    manager.removeGuids(guids);
    assertEquals(5, manager.numGuidsRemaining());

    // Add some GUIDs back.
    guids.clear();
    for (int i = 2; i < 9; i++) {
      guids.add("test" + i);
    }
    manager.addFreshGuids(guids);
    assertEquals(7, manager.numGuidsRemaining());
  }
}
