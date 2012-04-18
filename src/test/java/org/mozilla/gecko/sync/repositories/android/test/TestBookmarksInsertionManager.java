package org.mozilla.gecko.sync.repositories.android.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.repositories.android.BookmarksInsertionManager;
import org.mozilla.gecko.sync.repositories.domain.BookmarkRecord;

public class TestBookmarksInsertionManager {
  public BookmarksInsertionManager manager;
  public ArrayList<String> insertions;

  @Before
  public void setUp() {
    Logger.LOG_TO_STDOUT = true;
    insertions = new ArrayList<String>();
    manager = new BookmarksInsertionManager(3) {
      @Override
      protected void insert(BookmarkRecord record) throws Exception {
        insertions.add(record.guid);
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
    assertEquals(3, insertions.size());
    manager.flushAll(0);
    manager.dumpState();
    assertTrue(manager.isClear());
    assertEquals(3, insertions.size());
    assertArrayEquals(new String[] { "folder", "child1", "child2" },
        insertions.toArray(new String[insertions.size()]));
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
    assertEquals(3, insertions.size());
    manager.flushAll(0);
    manager.dumpState();
    assertTrue(manager.isClear());
    assertEquals(3, insertions.size());
    assertArrayEquals(new String[] { "folder", "child1", "child2" },
        insertions.toArray(new String[insertions.size()]));
  }

  @Test
  public void testFolderAfterFolder() {
    BookmarkRecord folder1 = folder("folder1", "mobile");
    BookmarkRecord child1 = bookmark("child1", "folder1");
    BookmarkRecord child2 = bookmark("child2", "folder1");
    BookmarkRecord folder2 = folder("folder2", "folder1");
    BookmarkRecord child3 = bookmark("child3", "folder2");
    BookmarkRecord child4 = bookmark("child4", "folder2");

    manager.insertRecord(child1);
    manager.insertRecord(folder1);
    manager.insertRecord(child3);
    manager.insertRecord(folder2);
    manager.insertRecord(child2);
    manager.insertRecord(child4);
    manager.flushAll(0);
    assertTrue(manager.isClear());
    assertEquals(6, insertions.size());
    assertArrayEquals(new String[] { "folder1", "child1", "folder2", "child3", "child2", "child4" },
        insertions.toArray(new String[insertions.size()]));
  }
}
