/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.android.sync.test.helpers.WBORepository;
import org.mozilla.android.sync.test.helpers.WaitHelper;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.ThreadPool;
import org.mozilla.gecko.sync.repositories.NoStoreDelegateException;
import org.mozilla.gecko.sync.repositories.RepositorySessionBundle;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionCreationDelegate;
import org.mozilla.gecko.sync.repositories.domain.BookmarkRecord;
import org.mozilla.gecko.sync.repositories.domain.Record;
import org.mozilla.gecko.sync.synchronizer.FlowAbortedException;
import org.mozilla.gecko.sync.synchronizer.Synchronizer;
import org.mozilla.gecko.sync.synchronizer.SynchronizerDelegate;
import org.mozilla.gecko.sync.synchronizer.SynchronizerSession;
import org.mozilla.gecko.sync.synchronizer.SynchronizerSessionDelegate;

import android.content.Context;

public class TestSynchronizer {
  public static final String LOG_TAG = "TestSynchronizer";

  public static void assertInRangeInclusive(long earliest, long value, long latest) {
    assertTrue(earliest <= value);
    assertTrue(latest   >= value);
  }

  public static void recordEquals(BookmarkRecord r, String guid, long lastModified, boolean deleted, String collection) {
    assertEquals(r.guid,         guid);
    assertEquals(r.lastModified, lastModified);
    assertEquals(r.deleted,      deleted);
    assertEquals(r.collection,   collection);
  }

  public static void recordEquals(BookmarkRecord a, BookmarkRecord b) {
    assertEquals(a.guid,         b.guid);
    assertEquals(a.lastModified, b.lastModified);
    assertEquals(a.deleted,      b.deleted);
    assertEquals(a.collection,   b.collection);
  }

  @Before
  public void setUp() {
    Logger.LOG_TO_STDOUT = false;
    WaitHelper.resetTestWaiter();
  }

  @After
  public void tearDown() {
    Logger.LOG_TO_STDOUT = false;
    WaitHelper.resetTestWaiter();
  }

  @Test
  public void testSynchronizerSession() {
    final Object monitor = new Object();

    Context context = null;
    final WBORepository repoA = new WBORepository();
    final WBORepository repoB = new WBORepository();

    final String collection  = "bookmarks";
    final boolean deleted    = false;
    final String guidA       = "abcdabcdabcd";
    final String guidB       = "ffffffffffff";
    final long lastModifiedA = 312345;
    final long lastModifiedB = 412345;
    BookmarkRecord bookmarkRecordA = new BookmarkRecord(guidA, collection, lastModifiedA, deleted);
    BookmarkRecord bookmarkRecordB = new BookmarkRecord(guidB, collection, lastModifiedB, deleted);

    repoA.wbos.put(guidA, bookmarkRecordA);
    repoB.wbos.put(guidB, bookmarkRecordB);
    Synchronizer synchronizer = new Synchronizer();
    synchronizer.repositoryA = repoA;
    synchronizer.repositoryB = repoB;
    SynchronizerSession syncSession = new SynchronizerSession(synchronizer, new SynchronizerSessionDelegate() {

      @Override
      public void onInitialized(SynchronizerSession session) {
        assertFalse(repoA.wbos.containsKey(guidB));
        assertFalse(repoB.wbos.containsKey(guidA));
        assertTrue(repoA.wbos.containsKey(guidA));
        assertTrue(repoB.wbos.containsKey(guidB));
        session.synchronize();
      }

      @Override
      public void onSynchronizedSession(SynchronizerSession session) {
        Logger.trace(LOG_TAG, "onSynchronized. Success!");
        synchronized (monitor) {
          monitor.notify();
        }
      }

      @Override
      public void onSynchronizeSessionFailed(SynchronizerSession session,
                                      Exception lastException, String reason) {
        fail("Synchronization should not fail.");
      }

      @Override
      public void notifyLocalRecordStoreFailed(Exception e, String recordGuid) {
        fail("Should be no store error.");
      }

      @Override
      public void notifyRemoteRecordStoreFailed(Exception e, String recordGuid) {
        fail("Should be no store error.");
      }

      @Override
      public void onSessionError(Exception e) {
        fail("Should be no session error.");
      }

      @Override
      public void onFetchError(Exception e) {
        fail("Should be no fetch error.");
      }

      @Override
      public void onSynchronizeSessionSkipped(SynchronizerSession synchronizerSession) {
        fail("Sync should not be skipped.");
      }
    });
    synchronized (monitor) {
      syncSession.init(context, new RepositorySessionBundle(0), new RepositorySessionBundle(0));
      try {
        monitor.wait();
      } catch (InterruptedException e) {
        fail("Interrupted.");
      }
    }

    // Verify contents.
    assertTrue(repoA.wbos.containsKey(guidA));
    assertTrue(repoA.wbos.containsKey(guidB));
    assertTrue(repoB.wbos.containsKey(guidA));
    assertTrue(repoB.wbos.containsKey(guidB));
    BookmarkRecord aa = (BookmarkRecord) repoA.wbos.get(guidA);
    BookmarkRecord ab = (BookmarkRecord) repoA.wbos.get(guidB);
    BookmarkRecord ba = (BookmarkRecord) repoB.wbos.get(guidA);
    BookmarkRecord bb = (BookmarkRecord) repoB.wbos.get(guidB);
    recordEquals(aa, guidA, lastModifiedA, deleted, collection);
    recordEquals(ab, guidB, lastModifiedB, deleted, collection);
    recordEquals(ba, guidA, lastModifiedA, deleted, collection);
    recordEquals(bb, guidB, lastModifiedB, deleted, collection);
    recordEquals(aa, ba);
    recordEquals(ab, bb);
    Logger.trace(LOG_TAG, "Got to end of test.");
  }

  public abstract class SuccessfulSynchronizerDelegate implements SynchronizerDelegate {
    public long syncAOne = 0;
    public long syncBOne = 0;

    @Override
    public void onSynchronizeFailed(Synchronizer synchronizer,
                                    Exception lastException, String reason) {
      fail("Should not fail.");
    }
  }

  @Test
  public void testSynchronizerPersists() {
    final Object monitor = new Object();
    final long earliest = new Date().getTime();

    Context context = null;
    final WBORepository repoA = new WBORepository();
    final WBORepository repoB = new WBORepository();
    Synchronizer synchronizer = new Synchronizer();
    synchronizer.bundleA     = new RepositorySessionBundle(0);
    synchronizer.bundleB     = new RepositorySessionBundle(0);
    synchronizer.repositoryA = repoA;
    synchronizer.repositoryB = repoB;

    final SuccessfulSynchronizerDelegate delegateOne = new SuccessfulSynchronizerDelegate() {
      @Override
      public void onSynchronized(Synchronizer synchronizer) {
        Logger.trace(LOG_TAG, "onSynchronized. Success!");
        syncAOne = synchronizer.bundleA.getTimestamp();
        syncBOne = synchronizer.bundleB.getTimestamp();
        synchronized (monitor) {
          monitor.notify();
        }
      }
    };
    final SuccessfulSynchronizerDelegate delegateTwo = new SuccessfulSynchronizerDelegate() {
      @Override
      public void onSynchronized(Synchronizer synchronizer) {
        Logger.trace(LOG_TAG, "onSynchronized. Success!");
        syncAOne = synchronizer.bundleA.getTimestamp();
        syncBOne = synchronizer.bundleB.getTimestamp();
        synchronized (monitor) {
          monitor.notify();
        }
      }
    };
    synchronized (monitor) {
      synchronizer.synchronize(context, delegateOne);
      try {
        monitor.wait();
      } catch (InterruptedException e) {
        fail("Interrupted.");
      }
    }
    long now = new Date().getTime();
    Logger.trace(LOG_TAG, "Earliest is " + earliest);
    Logger.trace(LOG_TAG, "syncAOne is " + delegateOne.syncAOne);
    Logger.trace(LOG_TAG, "syncBOne is " + delegateOne.syncBOne);
    Logger.trace(LOG_TAG, "Now: " + now);
    assertInRangeInclusive(earliest, delegateOne.syncAOne, now);
    assertInRangeInclusive(earliest, delegateOne.syncBOne, now);
    try {
      Thread.sleep(10);
    } catch (InterruptedException e) {
      fail("Thread interrupted!");
    }
    synchronized (monitor) {
      synchronizer.synchronize(context, delegateTwo);
      try {
        monitor.wait();
      } catch (InterruptedException e) {
        fail("Interrupted.");
      }
    }
    now = new Date().getTime();
    Logger.trace(LOG_TAG, "Earliest is " + earliest);
    Logger.trace(LOG_TAG, "syncAOne is " + delegateTwo.syncAOne);
    Logger.trace(LOG_TAG, "syncBOne is " + delegateTwo.syncBOne);
    Logger.trace(LOG_TAG, "Now: " + now);
    assertInRangeInclusive(earliest, delegateTwo.syncAOne, now);
    assertInRangeInclusive(earliest, delegateTwo.syncBOne, now);
    assertTrue(delegateTwo.syncAOne > delegateOne.syncAOne);
    assertTrue(delegateTwo.syncBOne > delegateOne.syncBOne);
    Logger.trace(LOG_TAG, "Reached end of test.");
  }

  private Synchronizer getTestSynchronizer(long tsA, long tsB) {
    WBORepository repoA = new WBORepository();
    WBORepository repoB = new WBORepository();
    Synchronizer synchronizer = new Synchronizer();
    synchronizer.bundleA      = new RepositorySessionBundle(tsA);
    synchronizer.bundleB      = new RepositorySessionBundle(tsB);
    synchronizer.repositoryA  = repoA;
    synchronizer.repositoryB  = repoB;
    return synchronizer;
  }

  /**
   * Let's put data in two repos and synchronize them with last sync
   * timestamps later than all of the records. Verify that no records
   * are exchanged.
   */
  @Test
  public void testSynchronizerFakeTimestamps() {
    final Object monitor = new Object();
    Context context = null;

    final String collection  = "bookmarks";
    final boolean deleted    = false;
    final String guidA       = "abcdabcdabcd";
    final String guidB       = "ffffffffffff";
    final long lastModifiedA = 312345;
    final long lastModifiedB = 412345;
    BookmarkRecord bookmarkRecordA = new BookmarkRecord(guidA, collection, lastModifiedA, deleted);
    BookmarkRecord bookmarkRecordB = new BookmarkRecord(guidB, collection, lastModifiedB, deleted);

    Synchronizer synchronizer = getTestSynchronizer(lastModifiedA + 10, lastModifiedB + 10);
    final WBORepository repoA = (WBORepository) synchronizer.repositoryA;
    final WBORepository repoB = (WBORepository) synchronizer.repositoryB;

    repoA.wbos.put(guidA, bookmarkRecordA);
    repoB.wbos.put(guidB, bookmarkRecordB);
    synchronized (monitor) {
      synchronizer.synchronize(context, new SynchronizerDelegate() {

        @Override
        public void onSynchronized(Synchronizer synchronizer) {
          Logger.trace(LOG_TAG, "onSynchronized. Success!");
          synchronized (monitor) {
            monitor.notify();
          }
        }

        @Override
        public void onSynchronizeFailed(Synchronizer synchronizer,
                                        Exception lastException, String reason) {
          fail("Sync should not fail.");
        }
      });
      try {
        monitor.wait();
      } catch (InterruptedException e) {
        fail("Interrupted.");
      }
    }

    // Verify contents.
    assertTrue(repoA.wbos.containsKey(guidA));
    assertTrue(repoB.wbos.containsKey(guidB));
    assertFalse(repoB.wbos.containsKey(guidA));
    assertFalse(repoA.wbos.containsKey(guidB));
    BookmarkRecord aa = (BookmarkRecord) repoA.wbos.get(guidA);
    BookmarkRecord ab = (BookmarkRecord) repoA.wbos.get(guidB);
    BookmarkRecord ba = (BookmarkRecord) repoB.wbos.get(guidA);
    BookmarkRecord bb = (BookmarkRecord) repoB.wbos.get(guidB);
    assertNull(ab);
    assertNull(ba);
    recordEquals(aa, guidA, lastModifiedA, deleted, collection);
    recordEquals(bb, guidB, lastModifiedB, deleted, collection);
    Logger.trace(LOG_TAG, "Reached end of test.");
  }


  @Test
  public void testSynchronizer() {
    final Object monitor = new Object();
    Context context = null;

    final String collection = "bookmarks";
    final boolean deleted = false;
    final String guidA = "abcdabcdabcd";
    final String guidB = "ffffffffffff";
    final long lastModifiedA = 312345;
    final long lastModifiedB = 412345;
    BookmarkRecord bookmarkRecordA = new BookmarkRecord(guidA, collection,
        lastModifiedA, deleted);
    BookmarkRecord bookmarkRecordB = new BookmarkRecord(guidB, collection,
        lastModifiedB, deleted);

    Synchronizer synchronizer = getTestSynchronizer(0, 0);
    final WBORepository repoA = (WBORepository) synchronizer.repositoryA;
    final WBORepository repoB = (WBORepository) synchronizer.repositoryB;

    repoA.wbos.put(guidA, bookmarkRecordA);
    repoB.wbos.put(guidB, bookmarkRecordB);
    synchronized (monitor) {

      synchronizer.synchronize(context, new SynchronizerDelegate() {

        @Override
        public void onSynchronized(Synchronizer synchronizer) {
          Logger.trace(LOG_TAG, "onSynchronized. Success!");
          synchronized (monitor) {
            monitor.notify();
          }
        }

        @Override
        public void onSynchronizeFailed(Synchronizer synchronizer,
                                        Exception lastException, String reason) {
          fail("Sync should not fail.");
        }
      });
      try {
        monitor.wait();
      } catch (InterruptedException e) {
        fail("Interrupted.");
      }
    }

    // Verify contents.
    assertTrue(repoA.wbos.containsKey(guidA));
    assertTrue(repoA.wbos.containsKey(guidB));
    assertTrue(repoB.wbos.containsKey(guidA));
    assertTrue(repoB.wbos.containsKey(guidB));
    BookmarkRecord aa = (BookmarkRecord) repoA.wbos.get(guidA);
    BookmarkRecord ab = (BookmarkRecord) repoA.wbos.get(guidB);
    BookmarkRecord ba = (BookmarkRecord) repoB.wbos.get(guidA);
    BookmarkRecord bb = (BookmarkRecord) repoB.wbos.get(guidB);
    recordEquals(aa, guidA, lastModifiedA, deleted, collection);
    recordEquals(ab, guidB, lastModifiedB, deleted, collection);
    recordEquals(ba, guidA, lastModifiedA, deleted, collection);
    recordEquals(bb, guidB, lastModifiedB, deleted, collection);
    recordEquals(aa, ba);
    recordEquals(ab, bb);
    Logger.trace(LOG_TAG, "Reached end of test.");
  }

  /**
   * Store one at a time, failing if the guid contains "Fail".
   */
  public static class SerialFailStoreWBORepository extends WBORepository {
    @Override
    public void createSession(RepositorySessionCreationDelegate delegate,
                              Context context) {
      delegate.deferredCreationDelegate().onSessionCreated(new WBORepositorySession(this) {
        @Override
        public void store(final Record record) throws NoStoreDelegateException {
          if (delegate == null) {
            throw new NoStoreDelegateException();
          }
          if (record.guid.contains("Fail")) {
            delegate.notifyRecordStoreFailed(new RuntimeException(), record.guid);
          } else {
            delegate.notifyRecordStoreSucceeded(record.guid);
          }
        }
      });
    }
  }

  /**
   * Store in batches, failing if any of the batch guids contains "Fail".
   * <p>
   * This will drop the final batch.
   */
  public static class BatchFailStoreWBORepository extends WBORepository {
    public final int batchSize;
    public ArrayList<Record> batch = new ArrayList<Record>();
    public boolean batchShouldFail = false;

    public BatchFailStoreWBORepository(int batchSize) {
      super();
      this.batchSize = batchSize;
    }

    @Override
    public void createSession(RepositorySessionCreationDelegate delegate,
                              Context context) {
      delegate.deferredCreationDelegate().onSessionCreated(new WBORepositorySession(this) {
        @Override
        public void store(final Record record) throws NoStoreDelegateException {
          if (delegate == null) {
            throw new NoStoreDelegateException();
          }
          synchronized (batch) {
            batch.add(record);
            if (record.guid.contains("Fail")) {
              batchShouldFail = true;
            }

            if (batch.size() >= batchSize) {
              final ArrayList<Record> thisBatch = new ArrayList<Record>(batch);
              final boolean thisBatchShouldFail = batchShouldFail;
              batchShouldFail = false;
              batch.clear();

              ThreadPool.run(new Runnable() {
                @Override
                public void run() {
                  Logger.trace("XXX", "Notifying about batch.  Failure? " + thisBatchShouldFail);
                  for (Record batchRecord : thisBatch) {
                    if (thisBatchShouldFail) {
                      delegate.notifyRecordStoreFailed(new RuntimeException(), batchRecord.guid);
                    } else {
                      delegate.notifyRecordStoreSucceeded(batchRecord.guid);
                    }
                  }
                }
              });
            }
          }
        }
      });
    }
  }

  public static class TrackingWBORepository extends WBORepository {
    @Override
    public synchronized boolean shouldTrack() {
      return true;
    }

    public void store(final Record record) throws NoStoreDelegateException {
    }
  }

  protected Synchronizer getSynchronizer(WBORepository remote, WBORepository local) {
    BookmarkRecord[] inbounds = new BookmarkRecord[] {
        new BookmarkRecord("inboundSucc1", "bookmarks", 1, false),
        new BookmarkRecord("inboundSucc2", "bookmarks", 1, false),
        new BookmarkRecord("inboundFail1", "bookmarks", 1, false),
        new BookmarkRecord("inboundSucc3", "bookmarks", 1, false),
        new BookmarkRecord("inboundSucc4", "bookmarks", 1, false),
        new BookmarkRecord("inboundFail2", "bookmarks", 1, false),
    };
    BookmarkRecord[] outbounds = new BookmarkRecord[] {
        new BookmarkRecord("outboundFail1", "bookmarks", 1, false),
        new BookmarkRecord("outboundFail2", "bookmarks", 1, false),
        new BookmarkRecord("outboundFail3", "bookmarks", 1, false),
        new BookmarkRecord("outboundFail4", "bookmarks", 1, false),
        new BookmarkRecord("outboundFail5", "bookmarks", 1, false),
        new BookmarkRecord("outboundFail6", "bookmarks", 1, false),
    };
    for (BookmarkRecord inbound : inbounds) {
      remote.wbos.put(inbound.guid, inbound);
    }
    for (BookmarkRecord outbound : outbounds) {
      local.wbos.put(outbound.guid, outbound);
    }
    final Synchronizer synchronizer = new Synchronizer();
    synchronizer.repositoryA = remote;
    synchronizer.repositoryB = local;
    return synchronizer;
  }

  protected Exception doSynchronize(final Synchronizer synchronizer) {
    final ArrayList<Exception> a = new ArrayList<Exception>();

    WaitHelper.getTestWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        synchronizer.synchronize(null, new SynchronizerDelegate() {
          @Override
          public void onSynchronized(Synchronizer synchronizer) {
            a.add(null);
            WaitHelper.getTestWaiter().performNotify();
          }

          @Override
          public void onSynchronizeFailed(Synchronizer synchronizer, Exception lastException, String reason) {
            a.add(lastException);
            WaitHelper.getTestWaiter().performNotify();
          }
        });
      }
    });

    assertEquals(1, a.size()); // Should not be called multiple times!
    return a.get(0);
  }

  @Test
  public void testNoErrors() {
    Synchronizer synchronizer = getSynchronizer(new TrackingWBORepository(), new TrackingWBORepository());
    assertNull(doSynchronize(synchronizer));

    assertArrayEquals(new String[] { }, synchronizer.localStoreFailedGuids.toArray(new String[0]));
    assertArrayEquals(new String[] { }, synchronizer.remoteStoreFailedGuids.toArray(new String[0]));
  }

  @Test
  public void testLocalSerialStoreErrorsAreIgnored() {
    Synchronizer synchronizer = getSynchronizer(new TrackingWBORepository(), new SerialFailStoreWBORepository());
    assertNull(doSynchronize(synchronizer));

    assertArrayEquals(new String[] { "inboundFail1", "inboundFail2" }, synchronizer.localStoreFailedGuids.toArray(new String[0]));
    assertArrayEquals(new String[] { }, synchronizer.remoteStoreFailedGuids.toArray(new String[0]));
  }

  @Test
  public void testLocalBatchStoreErrorsAreIgnored() {
    final int BATCH_SIZE = 3;

    Synchronizer synchronizer = getSynchronizer(new TrackingWBORepository(), new BatchFailStoreWBORepository(BATCH_SIZE));
    assertNull(doSynchronize(synchronizer));

    // Should get groups of 3 failures, but can't say exactly how records are batched.
    assertFalse(synchronizer.localStoreFailedGuids.isEmpty());
    assertEquals(0, synchronizer.localStoreFailedGuids.size() % BATCH_SIZE);
    assertArrayEquals(new String[] { }, synchronizer.remoteStoreFailedGuids.toArray(new String[0]));
  }

  @Test
  public void testRemoteSerialStoreErrorsAreNotIgnored() throws Exception {
    Synchronizer synchronizer = getSynchronizer(new SerialFailStoreWBORepository(), new TrackingWBORepository()); // Tracking so we don't send incoming records back.

    // Should get a FlowAbortedException out of this.
    Exception e = doSynchronize(synchronizer);
    assertNotNull(e);
    assertTrue(e instanceof FlowAbortedException);

    assertArrayEquals(new String[] { }, synchronizer.localStoreFailedGuids.toArray(new String[0]));
    assertEquals(1, synchronizer.remoteStoreFailedGuids.size()); // Only one record should fail before we abort, but we can't say which.
    assertTrue(synchronizer.remoteStoreFailedGuids.get(0).contains("Fail"));
  }

  @Test
  public void testRemoteBatchStoreErrorsAreNotIgnored() throws Exception {
    Logger.LOG_TO_STDOUT = true;
    final int BATCH_SIZE = 3;

    Synchronizer synchronizer = getSynchronizer(new BatchFailStoreWBORepository(BATCH_SIZE), new TrackingWBORepository()); // Tracking so we don't send incoming records back.

    // Should get a FlowAbortedException out of this.
    Exception e = doSynchronize(synchronizer);
    assertNotNull(e);
    assertTrue(e instanceof FlowAbortedException);

    assertArrayEquals(new String[] { }, synchronizer.localStoreFailedGuids.toArray(new String[0]));
    // Should get groups of 3 failures, but can't say exactly how many record batches are processed.
    assertFalse(synchronizer.remoteStoreFailedGuids.isEmpty());
    assertEquals(0, synchronizer.remoteStoreFailedGuids.size() % BATCH_SIZE);
  }
}
