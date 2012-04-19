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
      protected void insertFolder(BookmarkRecord record) throws Exception {
        Logger.debug(BookmarksInsertionManager.LOG_TAG, "Inserted folder (" + record.guid + ").");
        insertions.add(new String[] { record.guid });
      }

      @Override
      protected void bulkInsertNonFolders(List<BookmarkRecord> records) throws Exception {
        ArrayList<String> guids = new ArrayList<String>();
        for (BookmarkRecord record : records) {
          guids.add(record.guid);
        }
        String[] guidList = guids.toArray(new String[guids.size()]);
        insertions.add(guidList);
        Logger.debug(BookmarksInsertionManager.LOG_TAG, "Inserted non-folders (" + BookmarksInsertionManager.join(", ", guidList) + ").");
      }
    };
    BookmarksInsertionManager.DEBUG = true;
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

    manager.enqueueRecord(child1);
    assertTrue(insertions.isEmpty());
    manager.enqueueRecord(child2);
    assertTrue(insertions.isEmpty());
    manager.enqueueRecord(folder);
    assertEquals(2, insertions.size());
    manager.flushAll(0);
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

    manager.enqueueRecord(child1);
    assertTrue(insertions.isEmpty());
    manager.enqueueRecord(folder);
    assertEquals(0, insertions.size());
    manager.enqueueRecord(child2);
    assertEquals(2, insertions.size());
    manager.flushAll(0);
    assertTrue(manager.isClear());
    assertEquals(2, insertions.size());
    assertArrayEquals(new String[] { "folder" }, insertions.get(0));
    assertArrayEquals(new String[] { "child1", "child2" }, insertions.get(1));
  }

  @Test
  public void testFolderAfterFolder() {
    manager.enqueueRecord(bookmark("child1", "folder1"));
    assertEquals(0, insertions.size());
    manager.enqueueRecord(folder("folder1", "mobile"));
    assertEquals(0, insertions.size());
    manager.enqueueRecord(bookmark("child3", "folder2"));
    assertEquals(0, insertions.size());
    manager.enqueueRecord(folder("folder2", "folder1"));
    assertEquals(3, insertions.size()); // 2 folders and 1 regular record.
    manager.enqueueRecord(bookmark("child2", "folder1"));
    manager.enqueueRecord(bookmark("child4", "folder2"));
    assertEquals(3, insertions.size());

    manager.flushAll(0);
    assertTrue(manager.isClear());
    assertEquals(4, insertions.size());
    assertArrayEquals(new String[] { "folder1" }, insertions.get(0));
    assertArrayEquals(new String[] { "folder2" }, insertions.get(1));
    assertArrayEquals(new String[] { "child1", "child3" }, insertions.get(2));
    assertArrayEquals(new String[] { "child2", "child4" }, insertions.get(3));
  }

  @Test
  public void testFolderRecursion() {
    manager.enqueueRecord(folder("1", "mobile"));
    manager.enqueueRecord(folder("2", "1"));
    manager.enqueueRecord(bookmark("3a", "3"));
    manager.enqueueRecord(bookmark("3b", "3"));
    manager.enqueueRecord(bookmark("3c", "3"));
    manager.enqueueRecord(bookmark("3d", "3"));
    manager.enqueueRecord(bookmark("3e", "3"));
    manager.enqueueRecord(bookmark("4a", "4"));
    manager.enqueueRecord(bookmark("4b", "4"));
    manager.enqueueRecord(bookmark("4c", "4"));
    assertEquals(0, insertions.size());
    manager.enqueueRecord(folder("3", "2"));
    assertEquals(5, insertions.size());
    manager.enqueueRecord(folder("4", "2"));
    assertEquals(7, insertions.size());

    assertTrue(manager.isClear());
    manager.flushAll(0);
    assertTrue(manager.isClear());
    // Folders in order.
    assertArrayEquals(new String[] { "1" }, insertions.get(0));
    assertArrayEquals(new String[] { "2" }, insertions.get(1));
    assertArrayEquals(new String[] { "3" }, insertions.get(2));
    // Then children in batches of 3.
    assertArrayEquals(new String[] { "3a", "3b", "3c" }, insertions.get(3));
    assertArrayEquals(new String[] { "3d", "3e" }, insertions.get(4));
    // Then last folder.
    assertArrayEquals(new String[] { "4" }, insertions.get(5));
    assertArrayEquals(new String[] { "4a", "4b", "4c" }, insertions.get(6));
  }
}
