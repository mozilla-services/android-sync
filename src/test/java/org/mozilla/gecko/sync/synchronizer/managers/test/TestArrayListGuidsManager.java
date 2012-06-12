package org.mozilla.gecko.sync.synchronizer.managers.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.mozilla.gecko.sync.synchronizer.managers.ArrayListGuidsManager;

public class TestArrayListGuidsManager {

  @Test
  public void testAddFreshGuids() throws Exception {
    ArrayListGuidsManager manager = new ArrayListGuidsManager(5);
    ArrayList<String> guids = new ArrayList<String>();
    for (int i = 0; i < 10; i++) {
      guids.add("test" + i);
    }
    manager.addFreshGuids(guids);
    assertEquals(10, manager.guids.size());
    assertEquals("test0", manager.guids.get(0));
    assertEquals("test9", manager.guids.get(9));

    guids.clear();
    for (int i = 10; i < 20; i++) {
      guids.add("test" + i);
    }
    manager.addFreshGuids(guids);
    assertEquals(20, manager.guids.size());
    assertEquals("test0", manager.guids.get(0));
    assertEquals("test9", manager.guids.get(9));
    assertEquals("test10", manager.guids.get(10));
    assertEquals("test19", manager.guids.get(19));
  }

  @Test
  public void testNextGuids() throws Exception {
    ArrayListGuidsManager manager = new ArrayListGuidsManager(5);
    ArrayList<String> guids = new ArrayList<String>();
    for (int i = 0; i < 9; i++) {
      guids.add("test" + i);
    }
    manager.addFreshGuids(guids);
    assertEquals(9, manager.guids.size());
    assertEquals("test0", manager.guids.get(0));
    assertEquals("test8", manager.guids.get(8));

    List<String> next = manager.nextGuids();
    assertEquals(5, next.size());
    assertEquals("test4", next.get(0));
    assertEquals("test8", next.get(4));
    assertEquals(4, manager.guids.size());

    next = manager.nextGuids();
    assertEquals(4, next.size());
    assertEquals("test0", next.get(0));
    assertEquals("test3", next.get(3));
    assertEquals(0, manager.guids.size());

    assertNotNull(manager.nextGuids());
    assertTrue(manager.nextGuids().isEmpty());
  }

  @Test
  public void testRemoveGuids() throws Exception {
    ArrayListGuidsManager manager = new ArrayListGuidsManager(5);
    ArrayList<String> guids = new ArrayList<String>();
    for (int i = 0; i < 9; i++) {
      guids.add("test" + i);
    }
    manager.addFreshGuids(guids);
    assertEquals(9, manager.guids.size());

    guids.clear();
    guids.add("test1");
    guids.add("test2");
    guids.add("missing1");
    guids.add("missing2");
    manager.removeGuids(guids);
    assertEquals(7, manager.guids.size());
    assertEquals("test0", manager.guids.get(0));
    assertEquals("test3", manager.guids.get(1));
  }
}
