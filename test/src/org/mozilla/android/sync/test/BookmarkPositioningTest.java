/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import junit.framework.AssertionFailedError;

import org.json.simple.JSONArray;
import org.mozilla.android.sync.test.helpers.simple.SimpleSuccessBeginDelegate;
import org.mozilla.android.sync.test.helpers.simple.SimpleSuccessCreationDelegate;
import org.mozilla.android.sync.test.helpers.simple.SimpleSuccessFetchDelegate;
import org.mozilla.android.sync.test.helpers.simple.SimpleSuccessFinishDelegate;
import org.mozilla.android.sync.test.helpers.simple.SimpleSuccessStoreDelegate;
import org.mozilla.gecko.sync.StubActivity;
import org.mozilla.gecko.sync.repositories.NoStoreDelegateException;
import org.mozilla.gecko.sync.repositories.RepositorySession;
import org.mozilla.gecko.sync.repositories.RepositorySessionBundle;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserBookmarksDataAccessor;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserBookmarksRepository;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionBeginDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionCreationDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionFetchRecordsDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionStoreDelegate;
import org.mozilla.gecko.sync.repositories.domain.BookmarkRecord;
import org.mozilla.gecko.sync.repositories.domain.Record;

import android.content.Context;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

public class BookmarkPositioningTest extends ActivityInstrumentationTestCase2<StubActivity> {

  protected static final String tag = "BookmarkPositioningTest";

  public BookmarkPositioningTest() {
    super(StubActivity.class);
  }

  public Context getApplicationContext() {
    return this.getInstrumentation().getTargetContext().getApplicationContext();
  }

  protected void performWait(Runnable runnable) throws AssertionError {
    AndroidBrowserRepositoryTestHelper.testWaiter.performWait(runnable);
  }

  protected void performNotify() {
    AndroidBrowserRepositoryTestHelper.testWaiter.performNotify();
  }

  protected void performNotify(AssertionFailedError e) {
    AndroidBrowserRepositoryTestHelper.testWaiter.performNotify(e);
  }

  protected void notifyException(String reason, Exception ex) {
    final AssertionFailedError e = new AssertionFailedError(reason + " : " + ex.getMessage());
    e.initCause(ex);
    performNotify(e);
  }

  /**
   * Create and begin a new session, handing control to the delegate when started.
   * Returns when the delegate has notified.
   *
   * @param delegate
   */
  public void inBegunSession(final AndroidBrowserBookmarksRepository repo,
                             final RepositorySessionBeginDelegate beginDelegate) {
    Runnable go = new Runnable() {
      @Override
      public void run() {
        RepositorySessionCreationDelegate delegate = new SimpleSuccessCreationDelegate() {
          @Override
          public void onSessionCreated(final RepositorySession session) {
            session.begin(beginDelegate);
          }
        };
        repo.createSession(delegate, getApplicationContext());
      }
    };
    performWait(go);
  }

  /**
   * Finish the provided session, notifying on success.
   *
   * @param session
   */
  public void finishAndNotify(final RepositorySession session) {
    session.finish(new SimpleSuccessFinishDelegate() {
      @Override
      public void onFinishSucceeded(RepositorySession session,
                                    RepositorySessionBundle bundle) {
        performNotify();
      }
    });
  }

  /**
   * Simple helper class for fetching a single record by GUID.
   * The fetched record is stored in `fetchedRecord`.
   *
   */
  public class SimpleFetchOneBeginDelegate extends SimpleSuccessBeginDelegate {
    public final String guid;
    public Record fetchedRecord = null;

    public SimpleFetchOneBeginDelegate(String guid) {
      this.guid = guid;
    }

    @Override
    public void onBeginSucceeded(final RepositorySession session) {
      RepositorySessionFetchRecordsDelegate fetchDelegate = new SimpleSuccessFetchDelegate() {

        @Override
        public void onFetchedRecord(Record record) {
          fetchedRecord = record;
        }

        @Override
        public void onFetchCompleted(long end) {
          finishAndNotify(session);
        }
      };
      session.fetch(new String[] { guid }, fetchDelegate);
    }
  }

  public JSONArray fetchChildrenForGUID(AndroidBrowserBookmarksRepository repo,
                                        final String guid) {
    SimpleFetchOneBeginDelegate beginDelegate = new SimpleFetchOneBeginDelegate(guid);
    inBegunSession(repo, beginDelegate);
    assertTrue(beginDelegate.fetchedRecord != null);
    return ((BookmarkRecord) (beginDelegate.fetchedRecord)).children;
  }

  /**
   * Create a new session for the given repository, storing each record
   * from the provided array. Notifies on failure or success.
   *
   * @param repo
   * @param records
   */
  public void storeRecordsInSession(AndroidBrowserBookmarksRepository repo,
                                    final BookmarkRecord[] records) {
    SimpleSuccessBeginDelegate beginDelegate = new SimpleSuccessBeginDelegate() {
      @Override
      public void onBeginSucceeded(final RepositorySession session) {
        RepositorySessionStoreDelegate storeDelegate = new SimpleSuccessStoreDelegate() {

          @Override
          public void onStoreCompleted() {
            finishAndNotify(session);
          }

          @Override
          public void onRecordStoreSucceeded(Record record) {
            // Great.
          }
        };
        session.setStoreDelegate(storeDelegate);
        for (BookmarkRecord record : records) {
          try {
            session.store(record);
          } catch (NoStoreDelegateException e) {
            // Never happens.
          }
        }
        session.storeDone();
      }
    };
    inBegunSession(repo, beginDelegate);
  }

  @SuppressWarnings("unchecked")
  protected JSONArray childrenFromRecords(BookmarkRecord... records) {
    JSONArray children = new JSONArray();
    for (BookmarkRecord record : records) {
      children.add(record.guid);
    }
    return children;
  }

  public void testRetrieveFolderHasAccurateChildren() {
    AndroidBrowserBookmarksRepository repo = new AndroidBrowserBookmarksRepository();

    long now = System.currentTimeMillis();

    final String folderGUID = "eaaaaaaaafff";
    BookmarkRecord folder    = new BookmarkRecord(folderGUID,     "bookmarks", now -5, false);
    BookmarkRecord bookmarkA = new BookmarkRecord("daaaaaaaaaaa", "bookmarks", now -1, false);
    BookmarkRecord bookmarkB = new BookmarkRecord("baaaaaaaabbb", "bookmarks", now -3, false);
    BookmarkRecord bookmarkC = new BookmarkRecord("aaaaaaaaaccc", "bookmarks", now -2, false);

    folder.children   = childrenFromRecords(bookmarkA, bookmarkB, bookmarkC);
    folder.sortIndex  = 150;
    folder.title      = "Test items";
    folder.parentID   = "toolbar";
    folder.parentName = "Bookmarks Toolbar";
    folder.type       = "folder";

    bookmarkA.parentID    = folderGUID;
    bookmarkA.bookmarkURI = "http://example.com/A";
    bookmarkA.title       = "Title A";
    bookmarkA.type        = "bookmark";

    bookmarkB.parentID    = folderGUID;
    bookmarkB.bookmarkURI = "http://example.com/B";
    bookmarkB.title       = "Title B";
    bookmarkB.type        = "bookmark";

    bookmarkC.parentID    = folderGUID;
    bookmarkC.bookmarkURI = "http://example.com/C";
    bookmarkC.title       = "Title C";
    bookmarkC.type        = "bookmark";

    BookmarkRecord[] folderOnly = new BookmarkRecord[1];
    BookmarkRecord[] children   = new BookmarkRecord[3];

    folderOnly[0] = folder;

    children[0] = bookmarkA;
    children[1] = bookmarkB;
    children[2] = bookmarkC;

    wipe();
    Log.d(getName(), "Storing just folder...");
    storeRecordsInSession(repo, folderOnly);

    // We don't have any children, despite our insistence upon storing.
    assertChildrenAreOrdered(repo, folderGUID, new Record[] {});

    // Now store the children.
    Log.d(getName(), "Storing children...");
    storeRecordsInSession(repo, children);

    // Now we have children, but their order is not determined, because
    // they were stored out-of-session with the original folder.
    assertChildrenAreUnordered(repo, folderGUID, children);

    // Now if we store the folder record again, they'll be put in the
    // right place.
    folder.lastModified++;
    Log.d(getName(), "Storing just folder again...");
    storeRecordsInSession(repo, folderOnly);
    Log.d(getName(), "Fetching children yet again...");
    assertChildrenAreOrdered(repo, folderGUID, children);

    // Now let's see what happens when we see records in the same session.
    BookmarkRecord[] parentMixed = new BookmarkRecord[4];
    BookmarkRecord[] parentFirst = new BookmarkRecord[4];
    BookmarkRecord[] parentLast  = new BookmarkRecord[4];

    // None of our records have a position set.
    assertTrue(bookmarkA.androidPosition <= 0);
    assertTrue(bookmarkB.androidPosition <= 0);
    assertTrue(bookmarkC.androidPosition <= 0);

    parentMixed[1] = folder;
    parentMixed[0] = bookmarkA;
    parentMixed[2] = bookmarkC;
    parentMixed[3] = bookmarkB;

    parentFirst[0] = folder;
    parentFirst[1] = bookmarkC;
    parentFirst[2] = bookmarkA;
    parentFirst[3] = bookmarkB;

    parentLast[3]  = folder;
    parentLast[0]  = bookmarkB;
    parentLast[1]  = bookmarkA;
    parentLast[2]  = bookmarkC;

    wipe();
    storeRecordsInSession(repo, parentMixed);
    assertChildrenAreOrdered(repo, folderGUID, children);

    wipe();
    storeRecordsInSession(repo, parentFirst);
    assertChildrenAreOrdered(repo, folderGUID, children);

    wipe();
    storeRecordsInSession(repo, parentLast);
    assertChildrenAreOrdered(repo, folderGUID, children);

    // Ensure that records are ordered even if we re-process the folder.
    wipe();
    storeRecordsInSession(repo, parentLast);
    folder.lastModified++;
    storeRecordsInSession(repo, folderOnly);
    assertChildrenAreOrdered(repo, folderGUID, children);
    wipe();
  }

  protected void wipe() {
    Log.d(getName(), "Wiping.");
    new AndroidBrowserBookmarksDataAccessor(getApplicationContext()).wipe();
  }

  protected void assertChildrenAreOrdered(AndroidBrowserBookmarksRepository repo, String guid, Record[] expected) {
    Log.d(getName(), "Fetching children...");
    JSONArray folderChildren = fetchChildrenForGUID(repo, guid);

    assertTrue(folderChildren != null);
    Log.d(getName(), "Children are " + folderChildren.toJSONString());
    assertEquals(expected.length, folderChildren.size());
    for (int i = 0; i < expected.length; ++i) {
      assertEquals(expected[i].guid, ((String) folderChildren.get(i)));
    }
  }

  protected void assertChildrenAreUnordered(AndroidBrowserBookmarksRepository repo, String guid, Record[] expected) {
    Log.d(getName(), "Fetching children...");
    JSONArray folderChildren = fetchChildrenForGUID(repo, guid);

    assertTrue(folderChildren != null);
    Log.d(getName(), "Children are " + folderChildren.toJSONString());
    assertEquals(expected.length, folderChildren.size());
    for (Record record : expected) {
      folderChildren.contains(record.guid);
    }
  }
}

/**
TODO

Test for storing a record that will reconcile to mobile; postcondition is
that there's still a directory called mobile that includes all the items that
it used to.

Unsorted put in mobile???
Tests for children retrieval
Tests for children merge
Tests for modify retrieve parent when child added, removed, reordered (oh, reorder is hard! Any change, then. )
Safety mode?
Test storing folder first, contents first.
Store folder in next session. Verify order recovery.


*/