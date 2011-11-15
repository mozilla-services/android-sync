package org.mozilla.android.sync.test;

import static org.junit.Assert.*;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.android.sync.MainActivity;
import org.mozilla.android.sync.repositories.BookmarksRepository;
import org.mozilla.android.sync.repositories.BookmarksRepositorySession;
import org.mozilla.android.sync.repositories.CollectionType;
import org.mozilla.android.sync.repositories.RepoStatusCode;
import org.mozilla.android.sync.repositories.RepositorySession;
import org.mozilla.android.sync.repositories.SyncCallbackReceiver;
import org.mozilla.android.sync.repositories.Utils;
import org.mozilla.android.sync.repositories.domain.BookmarkRecord;
import org.mozilla.android.sync.test.CallbackResult.CallType;

import android.content.Context;

import com.xtremelabs.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class TestAndroidBookmarksRepo {

  private BookmarksRepositorySession session;

  @Before
  public void setUp() throws Exception {
  }

  @Test
  public void testCreateSession() {

    BookmarksRepository repo = new BookmarksRepository(CollectionType.Bookmarks);
    SyncCallbackReceiver callback = new Callback();
    Context context = new MainActivity().getApplicationContext();
    repo.createSession(context, callback);

    // TODO right now this might work because we aren't using threads...
    // but once we have threads need to figure out how to test with them
    if (getSession() == null) fail();

  }

  @Test
  public void testStore() {
    BookmarksRepository repo = new BookmarksRepository(CollectionType.Bookmarks);
    SyncCallbackReceiver callback = new Callback();
    Context context = new MainActivity().getApplicationContext();
    repo.createSession(context, callback);

    // Create a record to store
    BookmarkRecord record = new BookmarkRecord();
    record.setBmkUri("http://foo.bar.com");
    record.setDescription("This is a description for foo.bar.com");
    record.setType("bookmark");
    record.setTitle("Foo!!!");
    record.setGuid(Utils.generateGuid());

    BookmarkSessionTestWrapper testWrapper = new BookmarkSessionTestWrapper();
    CallbackResult result = testWrapper.doStore(session, record);

    System.out.println("Stored a record and got back id: " + result.getRowId());

    assertEquals(CallType.STORE, result.getCallType());
    assertEquals(RepoStatusCode.DONE, result.getStatusCode());
  }

  @Test
  public void testFetchAll() {
    // Hmmm...we need to do this for every test... look into that setup method to see
    // if that's the answer
    BookmarksRepository repo = new BookmarksRepository(CollectionType.Bookmarks);
    SyncCallbackReceiver callback = new Callback();
    Context context = new MainActivity().getApplicationContext();
    repo.createSession(context, callback);

    // Create a record to store
    BookmarkRecord record = new BookmarkRecord();
    record.setBmkUri("http://foo.bar.com");
    record.setDescription("This is a description for foo.bar.com");
    record.setType("bookmark");
    record.setTitle("Foo!!!");
    record.setGuid(Utils.generateGuid());
    BookmarkSessionTestWrapper testWrapper = new BookmarkSessionTestWrapper();
    CallbackResult result = testWrapper.doStore(session, record);
    System.out.println("Stored record with id: " + result.getRowId());

    // Create a second record to store
    BookmarkRecord record2 = new BookmarkRecord();
    record2.setBmkUri("http://boo.bar.com");
    record2.setDescription("Describe boo.bar.com");
    record2.setType("bookmark");
    record2.setTitle("boo!!!");
    record2.setGuid(Utils.generateGuid());
    result = testWrapper.doStore(session, record2);
    System.out.println("Stored record with id: " + result.getRowId());

    // Get records
    // NOTE: If debugging sometimes this doesn't get called, I think it is a timing thing.
    // TODO consder making timeouts extremely long so that they are never hit? And if they
    // are then it is actually a serious error? I don't see a problem with this...think.
    result = testWrapper.doFetchAll(session);

    System.out.println("Number of records returned: " + result.getRecords().length);

    assertEquals(CallType.FETCH_ALL, result.getCallType());
    assertEquals(RepoStatusCode.DONE, result.getStatusCode());

    // TODO: Do something to check that we got some records here. Need to perform a setup
    // function first to make sure there are records in there to be got.
  }

  @Test
  public void testGuidsSince() {
    // Hmmm...we need to do this for every test... look into that setup method to see
    // if that's the answer
    BookmarksRepository repo = new BookmarksRepository(CollectionType.Bookmarks);
    SyncCallbackReceiver callback = new Callback();
    Context context = new MainActivity().getApplicationContext();
    repo.createSession(context, callback);

    // Create a record to store
    BookmarkRecord record = new BookmarkRecord();
    record.setBmkUri("http://foo.bar.com");
    record.setDescription("This is a description for foo.bar.com");
    record.setType("bookmark");
    record.setTitle("Foo!!!");
    record.setGuid(Utils.generateGuid());

    // Get records
    BookmarkSessionTestWrapper testWrapper = new BookmarkSessionTestWrapper();
    CallbackResult result = testWrapper.doGuidsSince(session, (System.currentTimeMillis() - 1000000)/1000);

    System.out.println("Number of records returned: " + result.getGuids().length);

    assertEquals(CallType.FETCH_SINCE, result.getCallType());
    assertEquals(RepoStatusCode.DONE, result.getStatusCode());

    // TODO: Do something to check that we got some records here. Need to perform a setup
    // function first to make sure there are records in there to be got.
  }

  public BookmarksRepositorySession getSession() {
    return session;
  }

  public void setSession(BookmarksRepositorySession session) {
    this.session = session;
  }

  class Callback implements SyncCallbackReceiver {

    public void sessionCallback(RepoStatusCode error, RepositorySession session) {
      if (error == RepoStatusCode.DONE) {
        setSession((BookmarksRepositorySession) session);
      }

    }

    public void storeCallback(RepoStatusCode error) {
      // TODO Auto-generated method stub

    }

  }

}
