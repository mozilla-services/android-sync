package org.mozilla.android.sync.test;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.android.sync.MainActivity;
import org.mozilla.android.sync.repositories.BookmarksRepository;
import org.mozilla.android.sync.repositories.BookmarksRepositorySession;
import org.mozilla.android.sync.repositories.CollectionType;
import org.mozilla.android.sync.repositories.RepoStatusCode;
import org.mozilla.android.sync.repositories.Repository;
import org.mozilla.android.sync.repositories.Utils;
import org.mozilla.android.sync.repositories.domain.BookmarkRecord;
import org.mozilla.android.sync.repositories.domain.Record;
import org.mozilla.android.sync.test.CallbackResult.CallType;

import android.content.Context;

import com.xtremelabs.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class TestAndroidBookmarksRepo {

  private BookmarksRepositorySession session;
  private BookmarksSessionTestWrapper testWrapper;
  private static String parentId;
  private static final String parentName = "Menu";
  private static final long lastSyncTimestamp = Utils.currentEpoch() - 36000;

  @BeforeClass
  public static void oneTimeSetUp() {
    parentId = Utils.generateGuid();
  }

  @Before
  public void setUp() {

    // Create a testWrapper instance
    setTestWrapper(new BookmarksSessionTestWrapper());

    // Create the session used by tests
    BookmarksRepository repo = (BookmarksRepository) Repository.makeRepository(CollectionType.Bookmarks);
    Context context = new MainActivity().getApplicationContext();
    CallbackResult result = testWrapper.doCreateSessionSync(repo, context, lastSyncTimestamp);

    // Check that we got a valid session back
    assertEquals(result.getStatusCode(), RepoStatusCode.DONE);
    assertEquals(result.getCallType(), CallType.CREATE_SESSION);
    assert(result.getSession() != null);

    // Set the session
    setSession((BookmarksRepositorySession) result.getSession());

  }

  /*
   * Tests for createSession
   */
  @Test
  public void testCreateSessionNullContext() {
    BookmarksRepository repo = (BookmarksRepository) Repository.makeRepository(CollectionType.Bookmarks);
    CallbackResult result = testWrapper.doCreateSessionSync(repo, null, Utils.currentEpoch() - lastSyncTimestamp);
    assertEquals(RepoStatusCode.NULL_CONTEXT, result.getStatusCode());
  }

  /*
   * Tests for store
   *
   * Test storing a record for each different type of Bookmark record
   */
  @Test
  public void testStoreBookmark() {
    CallbackResult result = getTestWrapper().doStoreSync(session, createBookmark1());
    verifyStoreResult(result);
  }

  @Test
  public void testStoreMicrosummary() {
    CallbackResult result = getTestWrapper().doStoreSync(session, createMicrosummary());
    verifyStoreResult(result);
  }

  @Test
  public void testStoreQuery() {
    CallbackResult result = getTestWrapper().doStoreSync(session, createQuery());
    verifyStoreResult(result);
  }

  @Test
  public void testStoreFolder() {
    CallbackResult result = getTestWrapper().doStoreSync(session, createFolder());
    verifyStoreResult(result);
  }

  @Test
  public void testStoreLivemark() {
    CallbackResult result = getTestWrapper().doStoreSync(session, createLivemark());
    verifyStoreResult(result);
  }

  @Test
  public void testStoreSeparator() {
    CallbackResult result = getTestWrapper().doStoreSync(session, createSeparator());
    verifyStoreResult(result);
  }

  // TODO we don't need this call, but it has been useful for testing, leave it for now
  @Test
  public void testFetchAll() {

    // Create a record and store it
    CallbackResult result = testWrapper.doStoreSync(session, createBookmark1());
    verifyStoreResult(result);

    // Create a second record and store it
    result = testWrapper.doStoreSync(session, createBookmark2());
    verifyStoreResult(result);

    // Get records
    result = testWrapper.doFetchAllSync(session);

    assertEquals(CallType.FETCH_ALL, result.getCallType());
    assertEquals(RepoStatusCode.DONE, result.getStatusCode());
    assertEquals(2, result.getRecords().length);
  }

  /*
   * Tests for guids since
   */
  @Test
  public void testGuidsSinceReturnMultipleRecords() {

    // Create a record and store it
    CallbackResult result = testWrapper.doStoreSync(session, createBookmark1());
    verifyStoreResult(result);

    // Wait 2 seconds and then store 2 more records
    perform2SecondWait();
    long timestamp = System.currentTimeMillis()/1000;

    //  Store 2 more records
    BookmarkRecord record2 = createLivemark();
    result = testWrapper.doStoreSync(session, record2);
    verifyStoreResult(result);
    BookmarkRecord record3 = createMicrosummary();
    result = testWrapper.doStoreSync(session, record3);
    verifyStoreResult(result);

    // Get records
    result = testWrapper.doGuidsSinceSync(session, timestamp);

    // Verify that only two guid comes back (record2 and record3)
    assertEquals(2, result.getGuids().length);
    assertEquals(record2.getGuid(), result.getGuids()[0]);
    assertEquals(record3.getGuid(), result.getGuids()[1]);
    assertEquals(CallType.GUIDS_SINCE, result.getCallType());
    assertEquals(RepoStatusCode.DONE, result.getStatusCode());
  }

  @Test
  public void testGuidsSinceReturnNoRecords() {

    // Create a record and store it
    CallbackResult result = testWrapper.doStoreSync(session, createBookmark1());
    verifyStoreResult(result);

    // Wait 2 seconds
    perform2SecondWait();
    long timestamp = System.currentTimeMillis()/1000;

    // Get records
    result = testWrapper.doGuidsSinceSync(session, timestamp);

    // Verify that no guids come back
    assertEquals(0, result.getGuids().length);
    assertEquals(CallType.GUIDS_SINCE, result.getCallType());
    assertEquals(RepoStatusCode.DONE, result.getStatusCode());
  }

  /*
   * Tests for fetchSince
   */
  @Test
  public void testFetchSinceOneRecord() {
    // Create one record and store it
    CallbackResult result = testWrapper.doStoreSync(session, createFolder());
    verifyStoreResult(result);

    // Wait 2 seconds and then store another record
    perform2SecondWait();
    long timestamp = System.currentTimeMillis()/1000;
    BookmarkRecord record2 = createBookmark2();
    result = testWrapper.doStoreSync(session, record2);
    verifyStoreResult(result);

    // Fetch since using timestamp and ensure we only get back one record
    result = testWrapper.doFetchSinceSync(session, timestamp);

    // Check that only one record was returned and that it is the right one
    assertEquals(1, result.getRecords().length);
    assertEquals(record2.getGuid(), ((BookmarkRecord) result.getRecords()[0]).getGuid());
    assertEquals(CallType.FETCH_SINCE, result.getCallType());
    assertEquals(RepoStatusCode.DONE, result.getStatusCode());
  }

  @Test
  public void testFetchSinceReturnNoRecords() {

    // Create a record and store it
    CallbackResult result = testWrapper.doStoreSync(session, createBookmark1());
    verifyStoreResult(result);

    // Wait 2 seconds
    perform2SecondWait();
    long timestamp = System.currentTimeMillis()/1000;

    // Get records
    result = testWrapper.doFetchSinceSync(session, timestamp);

    // Verify that no guids come back
    assertEquals(0, result.getRecords().length);
    assertEquals(CallType.FETCH_SINCE, result.getCallType());
    assertEquals(RepoStatusCode.DONE, result.getStatusCode());
  }

  /*
   * Tests for fetch(guid)
   */
  @Test
  public void testFetchOneRecordByGuid() {
    // Create two records and store them
    BookmarkRecord record = createBookmark1();
    String guid = record.getGuid();
    CallbackResult result = testWrapper.doStoreSync(session, record);
    verifyStoreResult(result);
    result = testWrapper.doStoreSync(session, createBookmark2());
    verifyStoreResult(result);

    // Fetch record with guid from above and ensure we only get back one record
    result = testWrapper.doFetchSync(session, new String[] { guid });

    // Check that only one record was returned and that it is the correct one
    Record[] returnedRecords = result.getRecords();
    assertEquals(1, returnedRecords.length);
    BookmarkRecord fetched = (BookmarkRecord) returnedRecords[0];
    assertEquals(guid, fetched.getGuid());
    assertEquals(record.getBmkUri(), fetched.getBmkUri());
    assertEquals(record.getDescription(), fetched.getDescription());
    assertEquals(record.getTitle(), fetched.getTitle());
    assertEquals(CallType.FETCH, result.getCallType());
    assertEquals(RepoStatusCode.DONE, result.getStatusCode());
  }

  @Test
  public void testFetchMultipleRecordsByGuid() {
    // Create three records and store them
    BookmarkRecord record = createBookmark1();
    BookmarkRecord record2 = createQuery();
    BookmarkRecord record3 = createSeparator();
    CallbackResult result = testWrapper.doStoreSync(session, record);
    verifyStoreResult(result);
    result = testWrapper.doStoreSync(session, record2);
    verifyStoreResult(result);
    result = testWrapper.doStoreSync(session, record3);
    verifyStoreResult(result);

    // Fetch records with 2 guids from above
    result = testWrapper.doFetchSync(session, new String[] { record.getGuid(), record3.getGuid() });

    // Check that only one record was returned and that it is the correct one
    Record[] returnedRecords = result.getRecords();
    assertEquals(2, returnedRecords.length);
    BookmarkRecord fetched = (BookmarkRecord) returnedRecords[0];
    BookmarkRecord fetched2 = (BookmarkRecord) returnedRecords[1];
    assertEquals(record.getGuid(), fetched.getGuid());
    assertEquals(record.getBmkUri(), fetched.getBmkUri());
    assertEquals(record.getDescription(), fetched.getDescription());
    assertEquals(record.getTitle(), fetched.getTitle());
    assertEquals(record3.getGuid(), fetched2.getGuid());
    assertEquals(record3.getBmkUri(), fetched2.getBmkUri());
    assertEquals(record3.getDescription(), fetched2.getDescription());
    assertEquals(record3.getTitle(), fetched2.getTitle());
    assertEquals(CallType.FETCH, result.getCallType());
    assertEquals(RepoStatusCode.DONE, result.getStatusCode());
  }

  @Test
  public void testFetchNoRecordByGuid() {
    // Create a record and store it
    CallbackResult result = testWrapper.doStoreSync(session, createMicrosummary());
    verifyStoreResult(result);

    // Fetch a record that doesn't exist
    result = testWrapper.doFetchSync(session, new String[] { Utils.generateGuid() });

    // Ensure no recrods are returned
    assertEquals(0, result.getRecords().length);
    assertEquals(CallType.FETCH, result.getCallType());
    assertEquals(RepoStatusCode.DONE, result.getStatusCode());
  }

  @Test
  public void testFetchNoGuids() {

    // Fetch with empty guids list
    CallbackResult result = testWrapper.doFetchSync(session, new String[] { });

    // Ensure no records are returned
    assertEquals(RepoStatusCode.INVALID_REQUEST, result.getStatusCode());
    assertEquals(0, result.getRecords().length);
    assertEquals(CallType.FETCH, result.getCallType());
  }

  @Test
  public void testFetchNullGuids() {

    // Fetch with empty guids list
    CallbackResult result = testWrapper.doFetchSync(session, null);

    // Ensure no records are returned
    assertEquals(RepoStatusCode.INVALID_REQUEST, result.getStatusCode());
    assertEquals(0, result.getRecords().length);
    assertEquals(CallType.FETCH, result.getCallType());
  }

  /*
   * Helpers for creating bookmark records of different types
   */
  private static BookmarkRecord createBookmark1() {
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

  private static BookmarkRecord createBookmark2() {
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

  private static BookmarkRecord createMicrosummary() {
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

  private static BookmarkRecord createQuery() {
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

  private static BookmarkRecord createFolder() {
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

  private static BookmarkRecord createLivemark() {
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

  private static BookmarkRecord createSeparator() {
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
  private void verifyStoreResult(CallbackResult result) {
    assert(result.getRowId() != CallbackResult.DEFAULT_ROW_ID);
    assertEquals(CallType.STORE, result.getCallType());
    assertEquals(RepoStatusCode.DONE, result.getStatusCode());
  }

  private void perform2SecondWait() {
    try {
      synchronized(this) {
        this.wait(2000);
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  // Accessors and mutators
  public BookmarksRepositorySession getSession() {
    return session;
  }

  public void setSession(BookmarksRepositorySession session) {
    this.session = session;
  }

  public BookmarksSessionTestWrapper getTestWrapper() {
    return testWrapper;
  }

  public void setTestWrapper(BookmarksSessionTestWrapper testWrapper) {
    this.testWrapper = testWrapper;
  }

}