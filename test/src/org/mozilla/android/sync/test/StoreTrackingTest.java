/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.mozilla.android.sync.test.helpers.WBORepository;
import org.mozilla.gecko.sync.CryptoRecord;
import org.mozilla.gecko.sync.StubActivity;
import org.mozilla.gecko.sync.repositories.NoStoreDelegateException;
import org.mozilla.gecko.sync.repositories.RepositorySession;
import org.mozilla.gecko.sync.repositories.RepositorySessionBundle;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionBeginDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionCreationDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionFetchRecordsDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionFinishDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionStoreDelegate;
import org.mozilla.gecko.sync.repositories.domain.BookmarkRecord;
import org.mozilla.gecko.sync.repositories.domain.Record;
import org.mozilla.gecko.sync.synchronizer.Synchronizer;
import org.mozilla.gecko.sync.synchronizer.SynchronizerDelegate;

import android.content.Context;
import android.test.ActivityInstrumentationTestCase2;
import junit.framework.AssertionFailedError;
import android.util.Log;

public class StoreTrackingTest extends
    ActivityInstrumentationTestCase2<StubActivity> {

  public StoreTrackingTest() {
    super(StubActivity.class);
  }

  public Context getApplicationContext() {
    return this.getInstrumentation().getTargetContext().getApplicationContext();
  }

  protected void performWait(Runnable runnable) throws AssertionFailedError {
    AndroidBrowserRepositoryTestHelper.testWaiter.performWait(runnable);
  }

  protected void performNotify(AssertionFailedError e) {
    AndroidBrowserRepositoryTestHelper.testWaiter.performNotify(e);
  }

  protected void performNotify() {
    AndroidBrowserRepositoryTestHelper.testWaiter.performNotify();
  }

  public void assertEq(Object expected, Object actual) {
    try {
      assertEquals(expected, actual);
    } catch (AssertionFailedError e) {
      performNotify(e);
    }
  }

  public class TrackingWBORepository extends WBORepository {
    @Override
    public synchronized boolean shouldTrack() {
      return true;
    }
  }

  public abstract class SuccessBeginDelegate implements RepositorySessionBeginDelegate {
    @Override
    public void onBeginFailed(Exception ex) {
      performNotify(new AssertionFailedError("Begin failed: " + ex.getMessage()));
    }

    @Override
    public RepositorySessionBeginDelegate deferredBeginDelegate(ExecutorService executor) {
      return this;
    }
  }

  public abstract class SuccessCreationDelegate implements
      RepositorySessionCreationDelegate {
    @Override
    public void onSessionCreateFailed(Exception ex) {
      Log.w("SuccessCreationDelegate", "Session creation failed.", ex);
      performNotify(new AssertionFailedError("Session creation failed: "
          + ex.getMessage()));
    }

    @Override
    public RepositorySessionCreationDelegate deferredCreationDelegate() {
      Log.d("SuccessCreationDelegate", "Getting deferred.");
      return this;
    }
  }

  public abstract class SuccessStoreDelegate implements
      RepositorySessionStoreDelegate {
    @Override
    public void onRecordStoreFailed(Exception ex) {
      Log.w("SuccessStoreDelegate", "Store failed.", ex);
      performNotify(new AssertionFailedError("Store failed: " + ex.getMessage()));
    }

    @Override
    public RepositorySessionStoreDelegate deferredStoreDelegate(ExecutorService executor) {
      return this;
    }
  }

  public abstract class SuccessFinishDelegate implements RepositorySessionFinishDelegate {
    @Override
    public void onFinishFailed(Exception ex) {
      performNotify(new AssertionFailedError("Finish failed: " + ex.getMessage()));
    }

    @Override
    public RepositorySessionFinishDelegate deferredFinishDelegate(ExecutorService executor) {
      return this;
    }
  }

  public abstract class SuccessFetchDelegate implements
      RepositorySessionFetchRecordsDelegate {
    @Override
    public void onFetchFailed(Exception ex, Record record) {
      performNotify(new AssertionFailedError("Fetch failed: " + ex.getMessage()));
    }

    @Override
    public void onFetchSucceeded(Record[] records, long end) {
      for (Record record : records) {
        this.onFetchedRecord(record);
      }
      this.onFetchCompleted(end);
    }

    @Override
    public RepositorySessionFetchRecordsDelegate deferredFetchDelegate(ExecutorService executor) {
      return this;
    }
  }


  public void doTestStoreRetrieveByGUID(final WBORepository repository,
                                        final RepositorySession session,
                                        final String expectedGUID,
                                        final Record record) {

    final SuccessStoreDelegate storeDelegate = new SuccessStoreDelegate() {

      @Override
      public void onRecordStoreSucceeded(Record record) {
        Log.d(getName(), "Stored " + record.guid);
        assertEq(expectedGUID, record.guid);
      }

      @Override
      public void onStoreCompleted() {
        Log.d(getName(), "Store completed.");
        session.fetch(new String[] { expectedGUID }, new SuccessFetchDelegate() {
         @Override
          public void onFetchedRecord(Record record) {
            Log.d(getName(), "Hurrah! Fetched record " + record.guid);
            assertEq(expectedGUID, record.guid);
          }

          @Override
          public void onFetchCompleted(long end) {
            Log.d(getName(), "Fetch completed.");

            // But fetching by time returns nothing.
            session.fetchSince(0, new SuccessFetchDelegate() {
              private AtomicBoolean fetched = new AtomicBoolean(false);

              @Override
              public void onFetchedRecord(Record record) {
                Log.d(getName(), "Fetched record " + record.guid);
                fetched.set(true);
                performNotify(new AssertionFailedError("Should have fetched no record!"));
              }

              @Override
              public void onFetchCompleted(long end) {
                if (fetched.get()) {
                  Log.d(getName(), "Not finishing session: record retrieved.");
                  return;
                }
                session.finish(new SuccessFinishDelegate() {
                  @Override
                  public void onFinishSucceeded(RepositorySession session,
                                                RepositorySessionBundle bundle) {
                    performNotify();
                  }
                });
              }
            });
          }
        });
      }
    };

    session.setStoreDelegate(storeDelegate);
    try {
      Log.d(getName(), "Storing...");
      session.store(record);
      session.storeDone();
    } catch (NoStoreDelegateException e) {
      // Should not happen.
    }
  }

  private void doTestNewSessionRetrieveByTime(final WBORepository repository,
                                              final String expectedGUID) {
    final SuccessCreationDelegate createDelegate = new SuccessCreationDelegate() {
      @Override
      public void onSessionCreated(final RepositorySession session) {
        Log.i(getName(), "Session created.");
        session.begin(new SuccessBeginDelegate() {
          @Override
          public void onBeginSucceeded(final RepositorySession session) {
            // Now we get a result.
            session.fetchSince(0, new SuccessFetchDelegate() {

              @Override
              public void onFetchedRecord(Record record) {
                assertEq(expectedGUID, record.guid);
              }

              @Override
              public void onFetchCompleted(long end) {
                session.finish(new SuccessFinishDelegate() {
                  @Override
                  public void onFinishSucceeded(RepositorySession session,
                                                RepositorySessionBundle bundle) {
                    // Hooray!
                    performNotify();
                  }
                });
              }
            });
          }
        });
      }
    };
    Runnable create = new Runnable() {
      @Override
      public void run() {
        repository.createSession(createDelegate, getApplicationContext());
      }
    };

    performWait(create);
  }

  /**
   * Store a record in one session. Verify that fetching by GUID returns
   * the record. Verify that fetching by timestamp fails to return records.
   * Start a new session. Verify that fetching by timestamp returns the
   * stored record.
   *
   * Invokes doTestStoreRetrieveByGUID, doTestNewSessionRetrieveByTime.
   */
  public void testStoreRetrieveByGUID() {
    Log.i(getName(), "Started.");
    final WBORepository r = new TrackingWBORepository();
    final long now = System.currentTimeMillis();
    final String expectedGUID = "abcdefghijkl";
    final Record record = new BookmarkRecord(expectedGUID, "bookmarks", now , false);

    final RepositorySessionCreationDelegate createDelegate = new SuccessCreationDelegate() {
      @Override
      public void onSessionCreated(RepositorySession session) {
        Log.d(getName(), "Session created: " + session);
        session.begin(new SuccessBeginDelegate() {
          @Override
          public void onBeginSucceeded(final RepositorySession session) {
            doTestStoreRetrieveByGUID(r, session, expectedGUID, record);
          }
        });
      }
    };

    final Context applicationContext = getApplicationContext();

    // This has to happen on a new thread so that we
    // can wait for it!
    Runnable create = onThreadRunnable(new Runnable() {
      @Override
      public void run() {
        r.createSession(createDelegate, applicationContext);
      }
    });

    Runnable retrieve = onThreadRunnable(new Runnable() {
      @Override
      public void run() {
        doTestNewSessionRetrieveByTime(r, expectedGUID);
      }
    });

    performWait(create);
    performWait(retrieve);
  }

  private Runnable onThreadRunnable(final Runnable r) {
    return new Runnable() {
      @Override
      public void run() {
        new Thread(r).start();
      }
    };
  }


  public class CountingWBORepository extends TrackingWBORepository {
    public AtomicLong counter = new AtomicLong(0L);
    public class CountingWBORepositorySession extends WBORepositorySession {
      private static final String LOG_TAG = "CountingRepoSession";

      public CountingWBORepositorySession(WBORepository repository) {
        super(repository);
      }

      @Override
      public void store(final Record record) throws NoStoreDelegateException {
        Log.d(LOG_TAG, "Counter now " + counter.incrementAndGet());
        super.store(record);
      }
    }

    @Override
    public void createSession(RepositorySessionCreationDelegate delegate,
                              Context context) {
      delegate.deferredCreationDelegate().onSessionCreated(new CountingWBORepositorySession(this));
    }
  }

  public class TestRecord extends Record {
    public TestRecord(String guid, String collection, long lastModified,
                      boolean deleted) {
      super(guid, collection, lastModified, deleted);
    }

    @Override
    public void initFromPayload(CryptoRecord payload) {
      return;
    }

    @Override
    public CryptoRecord getPayload() {
      return null;
    }

    @Override
    public Record copyWithIDs(String guid, long androidID) {
      return new TestRecord(guid, this.collection, this.lastModified, this.deleted);
    }
  }

  /**
   * Create two repositories, syncing from one to the other. Ensure
   * that records stored from one aren't re-uploaded.
   */
  public void testStoreBetweenRepositories() {
    final CountingWBORepository repoA = new CountingWBORepository();    // "Remote". First source.
    final CountingWBORepository repoB = new CountingWBORepository();    // "Local". First sink.
    long now = System.currentTimeMillis();

    TestRecord recordA1 = new TestRecord("aacdefghiaaa", "coll", now - 30, false);
    TestRecord recordA2 = new TestRecord("aacdefghibbb", "coll", now - 20, false);
    TestRecord recordB1 = new TestRecord("aacdefghiaaa", "coll", now - 10, false);
    TestRecord recordB2 = new TestRecord("aacdefghibbb", "coll", now - 40, false);

    TestRecord recordA3 = new TestRecord("nncdefghibbb", "coll", now, false);
    TestRecord recordB3 = new TestRecord("nncdefghiaaa", "coll", now, false);

    // A1 and B1 are the same, but B's version is newer. We expect A1 to be downloaded
    // and B1 to be uploaded.
    // A2 and B2 are the same, but A's version is newer. We expect A2 to be downloaded
    // and B2 to not be uploaded.
    // Both A3 and B3 are new. We expect them to go in each direction.
    // Expected counts, then:
    // Repo A: B1 + B3
    // Repo B: A1 + A2 + A3
    repoB.wbos.put(recordB1.guid, recordB1);
    repoB.wbos.put(recordB2.guid, recordB2);
    repoB.wbos.put(recordB3.guid, recordB3);
    repoA.wbos.put(recordA1.guid, recordA1);
    repoA.wbos.put(recordA2.guid, recordA2);
    repoA.wbos.put(recordA3.guid, recordA3);

    final Synchronizer s = new Synchronizer();
    s.repositoryA = repoA;
    s.repositoryB = repoB;

    Runnable r = new Runnable() {
      @Override
      public void run() {
        s.synchronize(getApplicationContext(), new SynchronizerDelegate() {

          @Override
          public void onSynchronized(Synchronizer synchronizer) {
            long countA = repoA.counter.get();
            long countB = repoB.counter.get();
            Log.d(getName(), "Counts: " + countA + ", " + countB);
            assertEq(2L, countA);
            assertEq(3L, countB);
            performNotify();
          }

          @Override
          public void onSynchronizeFailed(Synchronizer synchronizer,
                                          Exception lastException, String reason) {
            Log.d(getName(), "Failed.");
            performNotify(new AssertionFailedError("Should not fail."));
          }

          @Override
          public void onSynchronizeAborted(Synchronizer synchronize) {
            Log.d(getName(), "Aborted.");
            performNotify(new AssertionFailedError("Should not abort."));
          }
        });
      }
    };

    performWait(onThreadRunnable(r));
  }
}
