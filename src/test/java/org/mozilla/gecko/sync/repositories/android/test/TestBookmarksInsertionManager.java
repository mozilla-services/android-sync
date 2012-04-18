package org.mozilla.gecko.sync.repositories.android.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.repositories.android.BookmarksInsertionManager;
import org.mozilla.gecko.sync.repositories.domain.BookmarkRecord;

public class TestBookmarksInsertionManager {
  public BookmarksInsertionManager manager;
  public ArrayList<String[]> insertions;

  @Before
  public void setUp() {
    Logger.LOG_TO_STDOUT = true;
    insertions = new ArrayList<String[]>();
    manager = new BookmarksInsertionManager(3) {
      @Override
      protected void insert(BookmarkRecord record) throws Exception {
        insertions.add(new String[] { record.guid });
      }

      @Override
      protected void bulkInsert(List<BookmarkRecord> records) throws Exception {
        ArrayList<String> guids = new ArrayList<String>();
        for (BookmarkRecord record : records) {
          guids.add(record.guid);
        }
        insertions.add(guids.toArray(new String[guids.size()]));
      }
    };
  }

  protected static BookmarkRecord bookmark(String guid, String parent) {
    BookmarkRecord bookmark = new BookmarkRecord(guid);
    bookmark.type = "bookmark";
    bookmark.parentID = parent;
    return bookmark;
  }

  protected static BookmarkRecord folder(String guid, String parent) {
    BookmarkRecord bookmark = new BookmarkRecord(guid);
    bookmark.type = "folder";
    bookmark.parentID = parent;
    return bookmark;
  }

  @Test
  public void testChildrenBeforeFolder() {
    BookmarkRecord folder = folder("folder", "mobile");
    BookmarkRecord child1 = bookmark("child1", "folder");
    BookmarkRecord child2 = bookmark("child2", "folder");

    manager.insertRecord(child1);
    manager.dumpState();
    assertTrue(insertions.isEmpty());
    manager.insertRecord(child2);
    manager.dumpState();
    assertTrue(insertions.isEmpty());
    manager.insertRecord(folder);
    manager.dumpState();
    assertEquals(2, insertions.size());
    manager.flushAll(0);
    manager.dumpState();
    assertTrue(manager.isClear());
    assertEquals(2, insertions.size());
    assertArrayEquals(new String[] { "folder" }, insertions.get(0));
    assertArrayEquals(new String[] { "child1", "child2" }, insertions.get(1));
  }

  @Test
  public void testChildAfterFolder() {
    BookmarkRecord folder = folder("folder", "mobile");
    BookmarkRecord child1 = bookmark("child1", "folder");
    BookmarkRecord child2 = bookmark("child2", "folder");

    manager.insertRecord(child1);
    manager.dumpState();
    assertTrue(insertions.isEmpty());
    manager.insertRecord(folder);
    manager.dumpState();
    assertEquals(0, insertions.size());
    manager.insertRecord(child2);
    manager.dumpState();
    assertEquals(2, insertions.size());
    manager.flushAll(0);
    manager.dumpState();
    assertTrue(manager.isClear());
    assertEquals(2, insertions.size());
    assertArrayEquals(new String[] { "folder" }, insertions.get(0));
    assertArrayEquals(new String[] { "child1", "child2" }, insertions.get(1));
  }

  @Test
  public void testFolderAfterFolder() {
    manager.insertRecord(bookmark("child1", "folder1"));
    assertEquals(0, insertions.size());
    manager.insertRecord(folder("folder1", "mobile"));
    assertEquals(0, insertions.size());
    manager.insertRecord(bookmark("child3", "folder2"));
    assertEquals(0, insertions.size());
    manager.insertRecord(folder("folder2", "folder1"));
    assertEquals(3, insertions.size()); // 2 folders and 1 regular record.
    manager.insertRecord(bookmark("child2", "folder1"));
    manager.insertRecord(bookmark("child4", "folder2"));
    assertEquals(3, insertions.size());

    manager.flushAll(0);
    assertTrue(manager.isClear());
    assertEquals(4, insertions.size());
    assertArrayEquals(new String[] { "folder1" }, insertions.get(0));
    assertArrayEquals(new String[] { "folder2" }, insertions.get(1));
    assertArrayEquals(new String[] { "child1", "child3" }, insertions.get(2));
    assertArrayEquals(new String[] { "child2", "child4" }, insertions.get(3));
  }
}
