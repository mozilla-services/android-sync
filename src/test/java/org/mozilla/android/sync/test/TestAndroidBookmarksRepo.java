/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.android.sync.MainActivity;
import org.mozilla.android.sync.repositories.BookmarksRepository;
import org.mozilla.android.sync.repositories.BookmarksRepositorySession;
import org.mozilla.android.sync.repositories.RepoStatusCode;
import org.mozilla.android.sync.repositories.RepositoryCallbackReceiver;
import org.mozilla.android.sync.repositories.RepositorySession;
import org.mozilla.android.sync.repositories.RepositorySessionDelegate;
import org.mozilla.android.sync.repositories.Utils;
import org.mozilla.android.sync.repositories.domain.Record;

import android.content.Context;

import com.xtremelabs.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class TestAndroidBookmarksRepo {

  private BookmarksRepositorySession session;
  private BookmarksSessionTestWrapper testWrapper;
  private static final long lastSyncTimestamp = Utils.currentEpoch() - 36000;

  public class DefaultRepositorySessionDelegate implements RepositoryCallbackReceiver {
    public void guidsSinceCallback(RepoStatusCode status, String[] guids) {
      fail("Should not be called.");
    }
    public void storeCallback(RepoStatusCode status, long rowId) {
      fail("Should not be called.");
    }
    public void fetchSinceCallback(RepoStatusCode status, Record[] records) {
      fail("Should not be called.");
    }
    public void fetchCallback(RepoStatusCode status, Record[] records) {
      fail("Should not be called.");
    }
    public void fetchAllCallback(RepoStatusCode status, Record[] records) {
      fail("Should not be called.");
    }
    public void wipeCallback(RepoStatusCode status) {
      fail("Should not be called.");
    }
    public void beginCallback(RepoStatusCode status) {
      fail("Should not be called.");
    }
    public void finishCallback(RepoStatusCode status) {
      fail("Should not be called.");
    }
  }

  private class DefaultDelegate implements RepositorySessionDelegate {
    public void onSessionCreateFailed(Exception ex) {
      fail("Should not fail.");
    }

    public void onSessionCreated(RepositorySession session) {
      fail("Should not have been created.");
    }

    public void onStoreFailed(Exception ex) {
      fail("No store.");
    }

    public void onStoreSucceeded() {
      fail("No store.");
    }
  }

  private class SetupDelegate extends DefaultDelegate {
    public void onSessionCreated(RepositorySession sess) {
      assertNotNull(sess);
      session = (BookmarksRepositorySession) sess;
      testWrapper.performNotify();
    }
  }

  @Before
  public void setUp() {
    // Create a testWrapper instance.
    testWrapper = new BookmarksSessionTestWrapper();

    // Create the session used by tests.
    BookmarksRepository repo = new BookmarksRepository();

    Context context = new MainActivity().getApplicationContext();
    repo.createSession(context, new SetupDelegate(), lastSyncTimestamp);
    testWrapper.performWait();
  }

  /*
   * Tests for createSession.
   */
  @Test
  public void testCreateSessionNullContext() {
    BookmarksRepository repo = new BookmarksRepository();
    try {
      repo.createSession(null, new DefaultDelegate(), lastSyncTimestamp);
      fail("Should throw.");
    } catch (Exception ex) {
      assertNotNull(ex);
    }
  }



  public class FetchDelegateHelper extends DefaultRepositorySessionDelegate {
    public boolean expectingStore = false;
    public boolean expectingFetch = false;
    public Record[] records = null;
    public RepoStatusCode code = null;

    public void storeCallback(RepoStatusCode status, long rowId) {
      if (!expectingStore) {
        fail("Not expecting store.");
      }
      assertFalse(rowId == -1);
      testWrapper.performNotify();
    }

    public void fetchAllCallback(RepoStatusCode status, Record[] records) {
      if (!expectingFetch) {
        fail("Not expecting fetch.");
      }
      assertEquals(records.length, 2);
      this.records = records;
      this.code = status;
      testWrapper.performNotify();
    }
  }


  @Test
  public void testFetchAll() {
    FetchDelegateHelper delegate = new FetchDelegateHelper();
    delegate.expectingStore = true;
    session.store(TestUtils.createBookmark1(), delegate);
    testWrapper.performWait();
    session.store(TestUtils.createBookmark2(), delegate);
    testWrapper.performWait();
    delegate.expectingStore = false;
    delegate.expectingFetch = true;
    session.fetchAll(delegate);
    testWrapper.performWait();
    assertEquals(delegate.records.length, 2);
    assertEquals(delegate.code, RepoStatusCode.DONE);
  }

}