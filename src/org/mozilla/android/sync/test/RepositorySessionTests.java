package org.mozilla.android.sync.test;

import org.mozilla.android.sync.repositories.InactiveSessionException;
import org.mozilla.android.sync.repositories.Utils;
import org.mozilla.android.sync.repositories.android.AndroidBrowserBookmarksRepository;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionFetchRecordsDelegate;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionGuidsSinceDelegate;
import org.mozilla.android.sync.repositories.domain.Record;
import org.mozilla.android.sync.test.helpers.DefaultSessionCreationDelegate;
import org.mozilla.android.sync.test.helpers.ExpectBeginDelegate;
import org.mozilla.android.sync.test.helpers.ExpectBeginFailDelegate;
import org.mozilla.android.sync.test.helpers.ExpectFinishDelegate;
import org.mozilla.android.sync.test.helpers.ExpectFinishFailDelegate;

import android.util.Log;

public class RepositorySessionTests extends AndroidBookmarksTestBase {
  
  // Although we use a bookmarks session here to test, these tests apply to all
  // session types. The reason we can do this is that these tests exercise
  // functions that lay only at the Repository and RepositorySession level and
  // not below that.

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
  
  /*
   * Tests for begin/finish
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
    getSession().finish(new ExpectFinishDelegate());
    getSession().fetch(new String[] { Utils.generateGuid() }, new RepositorySessionFetchRecordsDelegate() {
      public void onFetchSucceeded(Record[] records) {
        fail("Session inactive, should fail");
        performNotfiy();
      }
      public void onFetchFailed(Exception ex) {
        verifyInactiveException(ex);
        performNotfiy();
      }
    });
    performWait();
  }
  
  public void testGuidsSinceOnUnstartedSession() {
    prepEmptySessionWithoutBegin();
    getSession().guidsSince(System.currentTimeMillis(), new RepositorySessionGuidsSinceDelegate() {
      public void onGuidsSinceSucceeded(String[] guids) {
        fail("Session inactive, should fail");
        performNotfiy();
      }
      public void onGuidsSinceFailed(Exception ex) {
        verifyInactiveException(ex);
        performNotfiy();
      }
    });
    performWait();
  }
  
  private void verifyInactiveException(Exception ex) {
    if (ex.getClass() != InactiveSessionException.class) {
      fail("Wrong exception type");
    }
  }

}
