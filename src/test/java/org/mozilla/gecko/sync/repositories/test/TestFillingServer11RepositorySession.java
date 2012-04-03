/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.sync.repositories.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;

import org.junit.Before;
import org.junit.Test;
import org.mozilla.android.sync.test.helpers.ExpectSuccessRepositorySessionBeginDelegate;
import org.mozilla.android.sync.test.helpers.ExpectSuccessRepositorySessionCreationDelegate;
import org.mozilla.android.sync.test.helpers.WaitHelper;
import org.mozilla.gecko.sync.CollectionKeys;
import org.mozilla.gecko.sync.CredentialsSource;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.crypto.CryptoException;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.repositories.FillingServer11Repository;
import org.mozilla.gecko.sync.repositories.FillingServer11RepositorySession;
import org.mozilla.gecko.sync.repositories.RepositorySession;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionFetchRecordsDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionGuidsSinceDelegate;
import org.mozilla.gecko.sync.repositories.domain.Record;

import android.content.Context;

public class TestFillingServer11RepositorySession implements CredentialsSource {
  public static WaitHelper getTestWaiter() {
    return WaitHelper.getTestWaiter();
  }

  public static void performWait(Runnable runnable) {
    getTestWaiter().performWait(runnable);
  }

  protected static void performNotify() {
    getTestWaiter().performNotify();
  }

  protected static void performNotify(Exception e) {
    getTestWaiter().performNotify(e);
  }

  public Runnable onThreadRunnable(Runnable runnable) {
    return WaitHelper.onThreadRunnable(runnable);
  }

  static final String REMOTE_CLUSTER_URL = "https://phx-sync545.services.mozilla.com/";

  // Corresponds to rnewman+testandroid@mozilla.com.
  static final String USERNAME     = "c6o7dvmr2c4ud2fyv6woz2u4zi22bcyd";
  static final String USER_PASS    = "c6o7dvmr2c4ud2fyv6woz2u4zi22bcyd:password";
  static final String SYNC_KEY     = "6m8mv8ex2brqnrmsb9fjuvfg7y";

  static int EXISTING = 88; // Number of records on server.
  static int MIN;
  static int MAX;
  static int LIM;
  public String[] persistedGuids = null;

  final KeyBundle syncKeyBundle;
  final CollectionKeys collectionKeys;

  FillingServer11Repository repo;
  FillingServer11RepositorySession session;

  public TestFillingServer11RepositorySession() throws CryptoException {
    syncKeyBundle = new KeyBundle(USERNAME, SYNC_KEY);
    collectionKeys = new CollectionKeys();
    collectionKeys.setDefaultKeyBundle(syncKeyBundle);
  }

  @Before
  public void setUp() throws URISyntaxException {
    Logger.LOG_TO_STDOUT = true;

    persistedGuids = null;
    LIM = 100;
    MIN = 10;
    MAX = 20;

    repo = new FillingServer11Repository(REMOTE_CLUSTER_URL, USERNAME, "history", this, LIM, "index") {
      @Override
      public void persistGuidsRemaining(String[] guids, Context context) {
        System.out.println("Asked to persist " + guids.length + " guids.");
        persistedGuids = guids;
      }

      @Override
      public String[] guidsRemaining(Context context) throws Exception {
        System.out.println("Asked for persist guids.");
        return persistedGuids;
      }

      @Override
      protected long getDefaultFetchLimit() {
        return LIM;
      }

      @Override
      protected int getDefaultPerFillMaximum() {
        return MAX;
      }

      @Override
      protected int getDefaultPerFillMinimum() {
        return MIN;
      }
    };
  }

  @Override
  public String credentials() {
    return USER_PASS;
  }

  public void runInOnBeginSucceeded(final Runnable runnable) {
    final TestFillingServer11RepositorySession self = this;
    performWait(WaitHelper.onThreadRunnable(new Runnable() {
      @Override
      public void run() {
        repo.createSession(new ExpectSuccessRepositorySessionCreationDelegate(getTestWaiter()) {
          @Override
          public void onSessionCreated(RepositorySession session) {
            try {
              session.begin(new ExpectSuccessRepositorySessionBeginDelegate(getTestWaiter()) {
                @Override
                public void onBeginSucceeded(RepositorySession _session) {
                  self.session = (FillingServer11RepositorySession) _session;
                  runnable.run();
                }
              });
            } catch (Exception e) {
              TestFillingServer11RepositorySession.performNotify(e);
            }
          }
        }, null);
      }
    }));
  }

  @Test
  public void testGuidsSince() {
    runInOnBeginSucceeded(new Runnable() {
      @Override
      public void run() {
        session.guidsSince(0, new RepositorySessionGuidsSinceDelegate() {
          @Override
          public void onGuidsSinceSucceeded(String[] guids) {
            System.out.println("Got " + guids.length + " guids.");
            Arrays.sort(guids);
            for (String guid : guids) {
              assertEquals(12, guid.length());
              assertTrue('"' != guid.charAt(0));
            }
            assertEquals(88, guids.length);
            performNotify();
          }

          @Override
          public void onGuidsSinceFailed(Exception ex) {
            performNotify(ex);
          }
        });
      }
    });
  }

  public void doFetchSince(final long timestamp) {
    runInOnBeginSucceeded(new Runnable() {
      @Override
      public void run() {
        session.fetchSince(timestamp, new RepositorySessionFetchRecordsDelegate () {

          @Override
          public void onFetchFailed(Exception ex, Record record) {
            System.out.println("onFetchFailed.");
            performNotify(ex);
          }

          @Override
          public void onFetchedRecord(Record record) {
            System.out.println("Fetched " + record.guid + ".");
          }

          @Override
          public void onFetchCompleted(long fetchEnd) {
            System.out.println("onFetchCompleted.");
            performNotify();
          }

          @Override
          public RepositorySessionFetchRecordsDelegate deferredFetchDelegate(ExecutorService executor) {
            return this;
          }
        });
      }
    });
  }

  @Test
  public void testFetchSinceNow() {
    // All records persisted, no records fetched => MAX records are filled.
    LIM = 100;
    MAX = 10;
    MIN = 0;
    doFetchSince(System.currentTimeMillis());
    assertEquals(EXISTING - MAX, persistedGuids.length);
  }

  @Test
  public void testFetchSince0() {
    LIM = 60;
    MAX = 10;
    MIN = 0;
    doFetchSince(0);
    assertEquals(EXISTING - LIM, persistedGuids.length);
  }

  @Test
  public void testFetchSinceTimestampNoFill() {
    LIM = 80;
    MAX = 0;
    MIN = 0;
    long timestamp = 1331956713000L;
    long numSinceTimestamp = 36;
    doFetchSince(timestamp);
    assertEquals(EXISTING - numSinceTimestamp, persistedGuids.length);
  }

  @Test
  public void testFetchSinceTimestamp() {
    LIM = 80;
    MAX = 10;
    long timestamp = 1331956713000L;
    long numSinceTimestamp = 36;
    doFetchSince(timestamp);
    assertEquals(EXISTING - numSinceTimestamp - MAX, persistedGuids.length);
  }


  @Test
  public void testPartialFetchSinceNow() {
    LIM = 20;
    MIN = 10;
    MAX = 10;
    doFetchSince(System.currentTimeMillis());
    assertEquals(EXISTING - MAX, persistedGuids.length);
    doFetchSince(System.currentTimeMillis());
    assertEquals(EXISTING - 2*MAX, persistedGuids.length);
  }

  @Test
  public void testFetchSinceBadPersistedGUID() {
    LIM = 20;
    MIN = 10;
    // All records persisted, no records fetched => 20 records are filled.
    doFetchSince(System.currentTimeMillis());
    assertEquals(EXISTING - LIM, persistedGuids.length);
    persistedGuids[0] = "NONEXISTENT"; // Ignored by the server.
    doFetchSince(System.currentTimeMillis());
    assertEquals(EXISTING - 2*LIM, persistedGuids.length); // And removed from persisted list!
  }
}
