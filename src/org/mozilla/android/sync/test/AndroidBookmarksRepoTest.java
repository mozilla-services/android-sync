/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import org.mozilla.android.sync.MainActivity;
import org.mozilla.android.sync.repositories.InactiveSessionException;
import org.mozilla.android.sync.repositories.RepositorySession;
import org.mozilla.android.sync.repositories.Utils;
import org.mozilla.android.sync.repositories.android.AndroidBrowserBookmarksDatabaseHelper;
import org.mozilla.android.sync.repositories.android.AndroidBrowserBookmarksRepository;
import org.mozilla.android.sync.repositories.android.AndroidBrowserBookmarksRepositorySession;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionFetchRecordsDelegate;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionGuidsSinceDelegate;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionWipeDelegate;
import org.mozilla.android.sync.repositories.domain.BookmarkRecord;
import org.mozilla.android.sync.repositories.domain.Record;
import org.mozilla.android.sync.test.helpers.BookmarkHelpers;
import org.mozilla.android.sync.test.helpers.DefaultSessionCreationDelegate;
import org.mozilla.android.sync.test.helpers.ExpectBeginDelegate;
import org.mozilla.android.sync.test.helpers.ExpectBeginFailDelegate;
import org.mozilla.android.sync.test.helpers.ExpectFetchDelegate;
import org.mozilla.android.sync.test.helpers.ExpectFetchSinceDelegate;
import org.mozilla.android.sync.test.helpers.ExpectFinishDelegate;
import org.mozilla.android.sync.test.helpers.ExpectFinishFailDelegate;
import org.mozilla.android.sync.test.helpers.ExpectGuidsSinceDelegate;
import org.mozilla.android.sync.test.helpers.ExpectInvalidRequestFetchDelegate;
import org.mozilla.android.sync.test.helpers.ExpectStoredDelegate;

import android.content.Context;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

public class AndroidBookmarksRepoTest extends ActivityInstrumentationTestCase2<MainActivity> {
  public AndroidBookmarksRepoTest() {
    super(MainActivity.class);
  }

  public Context getApplicationContext() {
    return this.getInstrumentation().getTargetContext().getApplicationContext();
  }

  private void performWait(Runnable run) {
    AndroidBookmarksTestHelper.testWaiter.performWait(run);
  }
  private void performNotify() {
    AndroidBookmarksTestHelper.testWaiter.performNotify();
  }
  private AndroidBrowserBookmarksRepositorySession getSession() {
    return AndroidBookmarksTestHelper.session;
  }

  private void prepEmptySession() {
    AndroidBookmarksTestHelper.prepEmptySession(getApplicationContext());
  }
  
  private void prepEmptySessionWithoutBegin() {
    AndroidBookmarksTestHelper.prepEmptySessionWithoutBegin(getApplicationContext());
  }
  
  private static AndroidBrowserBookmarksDatabaseHelper helper;

  private void wipe() {
    if (helper == null) {
      helper = new AndroidBrowserBookmarksDatabaseHelper(getApplicationContext());
    }
    helper.wipe();
  }

  public void setUp() {
    Log.i("rnewman", "Wiping.");
    wipe();
  }
  public void tearDown() {
    if (helper != null) {
      helper.close();
    }
  }

  /*
   * Tests for createSession.
   */
  public void testCreateSessionNullContext() {
    Log.i("rnewman", "In testCreateSessionNullContext.");
    AndroidBrowserBookmarksRepository repo = new AndroidBrowserBookmarksRepository();
    try {
      repo.createSession(new DefaultSessionCreationDelegate(), null, 0);
      fail("Should throw.");
    } catch (Exception ex) {
      assertNotNull(ex);
    }
  }

  public static Runnable storeRunnable(final RepositorySession session, final Record record) {
    return new Runnable() {
      @Override
      public void run() {
        session.store(record, new ExpectStoredDelegate(record.guid));
      }
    };
  }

  public static Runnable fetchAllRunnable(final RepositorySession session, final ExpectFetchDelegate delegate) {
    return new Runnable() {
      @Override
      public void run() {
        session.fetchAll(delegate);        
      }
    };
  }
  public static Runnable fetchAllRunnable(final RepositorySession session, final String[] expectedGUIDs) {
    return fetchAllRunnable(session, new ExpectFetchDelegate(expectedGUIDs));
  }
  
  public static Runnable guidsSinceRunnable(final RepositorySession session, final long timestamp, final String[] expected) {
    return new Runnable() {
      @Override
      public void run() {
        session.guidsSince(timestamp, new ExpectGuidsSinceDelegate(expected));
      }
    };
  }  
  public static Runnable fetchSinceRunnable(final RepositorySession session, final long timestamp, final String[] expected) {
    return new Runnable() {
      @Override
      public void run() {
        session.fetchSince(timestamp, new ExpectFetchSinceDelegate(timestamp, expected));
      }
    };
  }
  public static Runnable fetchRunnable(final RepositorySession session, final String[] guids) {
    return fetchRunnable(session, guids, guids);
  }
  public static Runnable fetchRunnable(final RepositorySession session, final String[] guids, final String[] expected) {
    return new Runnable() {
      @Override
      public void run() {
        session.fetch(guids, new ExpectFetchDelegate(expected));
      }
    };
  }

  public void testFetchAll() {
    Log.i("rnewman", "Starting testFetchAll.");
    AndroidBookmarksTestHelper.prepareRepositorySession(getApplicationContext(), new SetupDelegate(), 0, true);
    Log.i("rnewman", "Prepared.");
    Record[] expected = new Record[2];
    String[] expectedGUIDs = new String[2];
    expected[0] = BookmarkHelpers.createBookmark1();
    expected[1] = BookmarkHelpers.createBookmark2();
    expectedGUIDs[0] = expected[0].guid;
    expectedGUIDs[1] = expected[1].guid;

    AndroidBrowserBookmarksRepositorySession session = getSession();
    performWait(storeRunnable(session, expected[0]));
    performWait(storeRunnable(session, expected[1]));
    ExpectFetchDelegate delegate = new ExpectFetchDelegate(expectedGUIDs);
    performWait(fetchAllRunnable(session, delegate));
    assertEquals(delegate.recordCount(), 2);
  }

  /*
   * Tests for fetching GUIDs since a timestamp.
   */
  public void testGuidsSinceReturnMultipleRecords() {
    prepEmptySession();
    long timestamp = System.currentTimeMillis();

    //  Store 2 records in the future.
    BookmarkRecord record0 = BookmarkHelpers.createLivemark();
    BookmarkRecord record1 = BookmarkHelpers.createMicrosummary();

    String[] expected = new String[2];
    expected[0] = record0.guid;
    expected[1] = record1.guid;
    record0.lastModified = timestamp + 1000;
    record1.lastModified = timestamp + 1500;

    AndroidBrowserBookmarksRepositorySession session = getSession();
    performWait(storeRunnable(session, record0));
    performWait(storeRunnable(session, record1));
    performWait(guidsSinceRunnable(session, timestamp, expected));
  }
  
  public void testGuidsSinceReturnNoRecords() {
    prepEmptySession();
    long timestamp = System.currentTimeMillis();

    //  Store 1 record in the past.
    BookmarkRecord record0 = BookmarkHelpers.createLivemark();
    record0.lastModified = timestamp - 1000;
    String[] expected = {};

    AndroidBrowserBookmarksRepositorySession session = getSession();
    performWait(storeRunnable(session, record0));
    performWait(guidsSinceRunnable(session, timestamp, expected));
  }

  /*
   * Tests for fetchSince
   */  
  public void testFetchSinceOneRecord() {
    prepEmptySession();
    long timestamp = System.currentTimeMillis();
    AndroidBrowserBookmarksRepositorySession session = getSession();

    // Store a folder.
    BookmarkRecord folder = BookmarkHelpers.createFolder();
    folder.lastModified = timestamp;       // Verify inclusive retrieval.
    performWait(storeRunnable(session, folder));

    // Store a bookmark.
    BookmarkRecord bookmark = BookmarkHelpers.createBookmark2();
    bookmark.lastModified = timestamp + 3000;
    performWait(storeRunnable(session, bookmark));

    // Fetch just the bookmark.
    String[] expectedOne = new String[1];
    expectedOne[0] = bookmark.guid;
    performWait(fetchSinceRunnable(session, timestamp + 1, expectedOne));

    // Fetch both, relying on inclusiveness.
    String[] expectedBoth = new String[2];
    expectedBoth[0] = folder.guid;
    expectedBoth[1] = bookmark.guid;
    performWait(fetchSinceRunnable(session, timestamp, expectedBoth));
  }

  public void testFetchSinceReturnNoRecords() {
    prepEmptySession();
    BookmarkRecord record0 = BookmarkHelpers.createBookmark1();
    AndroidBrowserBookmarksRepositorySession session = getSession();
    
    performWait(storeRunnable(session, record0));

    long timestamp = System.currentTimeMillis() / 1000;

    performWait(fetchSinceRunnable(session, timestamp, new String[] {}));
  }

  /*
   * Tests for fetch(guid)
   */
  
  public void testFetchOneRecordByGuid() {
    prepEmptySession();
    AndroidBrowserBookmarksRepositorySession session = getSession();
    BookmarkRecord record0 = BookmarkHelpers.createBookmark1();
    BookmarkRecord record1 = BookmarkHelpers.createBookmark2();
    
    performWait(storeRunnable(session, record0));
    performWait(storeRunnable(session, record1));
    
    String[] expected = new String[] { record0.guid };
    performWait(fetchRunnable(session, expected));
  }
  
  public void testFetchMultipleRecordsByGuids() {
    prepEmptySession();
    AndroidBrowserBookmarksRepositorySession session = getSession();
    BookmarkRecord record0 = BookmarkHelpers.createBookmark1();
    BookmarkRecord record1 = BookmarkHelpers.createBookmark2();
    BookmarkRecord record2 = BookmarkHelpers.createQuery();

    performWait(storeRunnable(session, record0));
    performWait(storeRunnable(session, record1));
    performWait(storeRunnable(session, record2));
    
    String[] expected = new String[] { record0.guid, record2.guid };
    performWait(fetchRunnable(session, expected));
  }
  
  public void testFetchNoRecordByGuid() {
    prepEmptySession();
    AndroidBrowserBookmarksRepositorySession session = getSession();
    
    BookmarkRecord record0 = BookmarkHelpers.createSeparator();
    performWait(storeRunnable(session, record0));
    String[] expected = new String[] {};
    String[] guids = new String[] { Utils.generateGuid() };
    performWait(fetchRunnable(session, guids, expected));
  }
  
  public void testFetchNoGuids() {
    prepEmptySession();
    final AndroidBrowserBookmarksRepositorySession session = getSession();
    performWait(new Runnable() {
      @Override
      public void run() {
        session.fetch(new String[] {}, new ExpectInvalidRequestFetchDelegate());
      }
    });
  }
  
  public void testFetchNullGuids() {
    prepEmptySession();
    final AndroidBrowserBookmarksRepositorySession session = getSession();
    performWait(new Runnable() {
      @Override
      public void run() {
        session.fetch(null, new ExpectInvalidRequestFetchDelegate());
      }
    });
  }
  
  /*
   * Test begin/finish
   */
  public void testBeginOnNewSession() {
    prepEmptySessionWithoutBegin();
    getSession().begin(new ExpectBeginDelegate());
  }
  
  public void testBeginOnRunningSession() {
    prepEmptySession();
    getSession().begin(new ExpectBeginFailDelegate());
  }
  
  public void testBeginOnFinishedSession() {
    prepEmptySession();
    getSession().finish(new ExpectFinishDelegate());
    getSession().begin(new ExpectBeginFailDelegate());
  }
  
  public void testFinishOnFinishedSession() {
    prepEmptySession();
    getSession().finish(new ExpectFinishDelegate());
    getSession().finish(new ExpectFinishFailDelegate());
  }
  
  public void testFetchOnInactiveSession() {
    prepEmptySessionWithoutBegin();
    getSession().finish(new ExpectFinishFailDelegate());
  }
  
  public void testFetchOnFinishedSession() {
    prepEmptySession();
    Runnable run = new Runnable() {
      @Override
      public void run() {
        getSession().finish(new ExpectFinishDelegate());
        getSession().fetch(new String[] { Utils.generateGuid() },
            new RepositorySessionFetchRecordsDelegate() {
              @Override
              public void onFetchSucceeded(Record[] records) {
                fail("Session inactive, should fail");
                performNotify();
              }

              @Override
              public void onFetchFailed(Exception ex, Record record) {
                verifyInactiveException(ex);
                performNotify();
              }

              @Override
              public void onFetchedRecord(Record record) {
                fail("Session inactive, should fail");
                performNotify();
              }

              @Override
              public void onFetchCompleted() {
                fail("Session inactive, should fail");
                performNotify();
              }
            });
      }
    };
    performWait(run);
  }
  
  public void testGuidsSinceOnUnstartedSession() {
    prepEmptySessionWithoutBegin();
    Runnable run = new Runnable() {
      @Override
      public void run() {
        getSession().guidsSince(System.currentTimeMillis(),
            new RepositorySessionGuidsSinceDelegate() {
              public void onGuidsSinceSucceeded(String[] guids) {
                fail("Session inactive, should fail");
                performNotify();
              }

              public void onGuidsSinceFailed(Exception ex) {
                verifyInactiveException(ex);
                performNotify();
              }
            });
      }
    };
    performWait(run);
  }
  
  private void verifyInactiveException(Exception ex) {
    if (ex.getClass() != InactiveSessionException.class) {
      fail("Wrong exception type");
    }
  }
  
  /*
   * Test wipe
   */
  public void testWipe() {
    prepEmptySession();
    
    BookmarkRecord record0 = BookmarkHelpers.createFolder();
    BookmarkRecord record1 = BookmarkHelpers.createBookmark2();
    
    // Store 2 records
    AndroidBrowserBookmarksRepositorySession session = getSession();
    performWait(storeRunnable(session, record0));
    performWait(storeRunnable(session, record1));
    performWait(fetchAllRunnable(session, new String[] {
        record0.guid, record1.guid
    }));

    performWait(new Runnable() {
      @Override
      public void run() {
        
        // Wipe
        getSession().wipe(new RepositorySessionWipeDelegate() {
          public void onWipeSucceeded() {
            performNotify();
          }
          public void onWipeFailed(Exception ex) {
            fail("wipe should have succeeded");
            performNotify();
          }
        });
      }
    });
  }

}