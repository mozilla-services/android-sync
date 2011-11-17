package org.mozilla.android.sync.test;

import static org.junit.Assert.assertEquals;

import org.mozilla.android.sync.repositories.RepoStatusCode;
import org.mozilla.android.sync.repositories.Utils;
import org.mozilla.android.sync.repositories.domain.BookmarkRecord;
import org.mozilla.android.sync.test.CallbackResult.CallType;

public class TestUtils {

  private static String parentId = Utils.generateGuid();
  private static String parentName = "main";

  /*
   * Helpers for creating bookmark records of different types
   */
  public static BookmarkRecord createBookmark1() {
    BookmarkRecord record = new BookmarkRecord();
    record.setGuid(Utils.generateGuid());
    record.setTitle("Foo!!!");
    record.setBmkUri("http://foo.bar.com");
    record.setDescription("This is a description for foo.bar.com");
    record.setLoadInSidebar(true);
    record.setTags("[\"tag1\", \"tag2\", \"tag3\"]");
    record.setKeyword("fooooozzzzz");
    record.setParentId(parentId);
    record.setParentName(parentName);
    record.setType("bookmark");
    return record;
  }

  public static BookmarkRecord createBookmark2() {
    BookmarkRecord record = new BookmarkRecord();
    record.setGuid(Utils.generateGuid());
    record.setTitle("Bar???");
    record.setBmkUri("http://bar.foo.com");
    record.setDescription("This is a description for Bar???");
    record.setLoadInSidebar(false);
    record.setTags("[\"tag1\", \"tag2\"]");
    record.setKeyword("keywordzzz");
    record.setParentId(parentId);
    record.setParentName(parentName);
    record.setType("bookmark");
    return record;
  }

  public static BookmarkRecord createMicrosummary() {
    BookmarkRecord record = new BookmarkRecord();
    record.setGuid(Utils.generateGuid());
    record.setGeneratorUri("http://generatoruri.com");
    record.setStaticTitle("Static Microsummary Title");
    record.setTitle("Microsummary 1");
    record.setBmkUri("www.bmkuri.com");
    record.setDescription("microsummary description");
    record.setLoadInSidebar(false);
    record.setTags("[\"tag1\", \"tag2\"]");
    record.setKeyword("keywordzzz");
    record.setParentId(parentId);
    record.setParentName(parentName);
    record.setType("microsummary");
    return record;
  }

  public static BookmarkRecord createQuery() {
    BookmarkRecord record = new BookmarkRecord();
    record.setGuid(Utils.generateGuid());
    record.setFolderName("Query Folder Name");
    record.setQueryId("OptionalQueryId");
    record.setTitle("Query 1");
    record.setBmkUri("http://www.query.com");
    record.setDescription("Query 1 description");
    record.setLoadInSidebar(true);
    record.setTags("[]");
    record.setKeyword("queryKeyword");
    record.setParentId(parentId);
    record.setParentName(parentName);
    record.setType("query");
    return record;
  }

  public static BookmarkRecord createFolder() {
    // Make this the Menu folder since each DB will
    // have at least this folder
    BookmarkRecord record = new BookmarkRecord();
    record.setGuid(parentId);
    record.setTitle(parentName);
    // No parent since this is the menu folder
    record.setParentId("");
    record.setParentName("");
    // TODO verify how we want to store these string arrays
    // pretty sure I verified that this is actually how other clients do it, but double check
    record.setChildren("[\"" + Utils.generateGuid() + "\", \"" + Utils.generateGuid() + "\"]");
    record.setType("folder");
    return record;
  }

  public static BookmarkRecord createLivemark() {
    BookmarkRecord record = new BookmarkRecord();
    record.setGuid(Utils.generateGuid());
    record.setSiteUri("http://site.uri.com");
    record.setFeedUri("http://rss.site.uri.com");
    record.setTitle("Livemark title");
    record.setParentId(parentId);
    record.setParentName(parentName);
    // TODO verify how we want to store these string arrays
    // pretty sure I verified that this is actually how other clients do it, but double check
    record.setChildren("[\"" + Utils.generateGuid() + "\", \"" + Utils.generateGuid() + "\"]");
    record.setType("livemark");
    return record;
  }

  public static BookmarkRecord createSeparator() {
    BookmarkRecord record = new BookmarkRecord();
    record.setGuid(Utils.generateGuid());
    record.setPos("3");
    record.setParentId(parentId);
    record.setParentName(parentName);
    record.setType("separator");
    return record;
  }

  /*
   * Other helpers
   */
  public static void verifyStoreResult(CallbackResult result) {
    assert(result.getRowId() != CallbackResult.DEFAULT_ROW_ID);
    assertEquals(CallType.STORE, result.getCallType());
    assertEquals(RepoStatusCode.DONE, result.getStatusCode());
  }

}