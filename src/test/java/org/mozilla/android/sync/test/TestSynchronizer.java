/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.mozilla.android.sync.test.helpers.WBORepository;
import org.mozilla.android.sync.test.helpers.WaitHelper;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.repositories.NoStoreDelegateException;
import org.mozilla.gecko.sync.repositories.RepositorySessionBundle;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionCreationDelegate;
import org.mozilla.gecko.sync.repositories.domain.BookmarkRecord;
import org.mozilla.gecko.sync.repositories.domain.Record;
import org.mozilla.gecko.sync.synchronizer.Synchronizer;
import org.mozilla.gecko.sync.synchronizer.SynchronizerDelegate;
import org.mozilla.gecko.sync.synchronizer.SynchronizerSession;
import org.mozilla.gecko.sync.synchronizer.SynchronizerSessionDelegate;

import android.content.Context;

public class TestSynchronizer {

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
        System.out.println("onSynchronized. Success!");
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
    System.out.println("Got to end of test.");
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
        System.out.println("onSynchronized. Success!");
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
        System.out.println("onSynchronized. Success!");
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
    System.out.println("Earliest is " + earliest);
    System.out.println("syncAOne is " + delegateOne.syncAOne);
    System.out.println("syncBOne is " + delegateOne.syncBOne);
    System.out.println("Now: " + now);
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
    System.out.println("Earliest is " + earliest);
    System.out.println("syncAOne is " + delegateTwo.syncAOne);
    System.out.println("syncBOne is " + delegateTwo.syncBOne);
    System.out.println("Now: " + now);
    assertInRangeInclusive(earliest, delegateTwo.syncAOne, now);
    assertInRangeInclusive(earliest, delegateTwo.syncBOne, now);
    assertTrue(delegateTwo.syncAOne > delegateOne.syncAOne);
    assertTrue(delegateTwo.syncBOne > delegateOne.syncBOne);
    System.out.println("Reached end of test.");
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
          System.out.println("onSynchronized. Success!");
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
    System.out.println("Reached end of test.");
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
          System.out.println("onSynchronized. Success!");
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
    System.out.println("Reached end of test.");
  }

  public static class FailStoreWBORepository extends WBORepository {
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

  public static class TrackingWBORepository extends WBORepository {
    @Override
    public synchronized boolean shouldTrack() {
      return true;
    }

    public void store(final Record record) throws NoStoreDelegateException {
    }
  }

  protected Synchronizer getSynchronizer(WBORepository remote, WBORepository local) {
    BookmarkRecord inbound1 =  new BookmarkRecord("inboundFail",  "bookmarks", 1, false);
    BookmarkRecord inbound2 =  new BookmarkRecord("inboundSucc",  "bookmarks", 1, false);
    BookmarkRecord outbound1 = new BookmarkRecord("outboundFail", "bookmarks", 1, false);
    BookmarkRecord outbound2 = new BookmarkRecord("outboundSucc", "bookmarks", 1, false);

    remote.wbos.put(inbound1.guid, inbound1);
    remote.wbos.put(inbound2.guid, inbound2);
    local.wbos.put(outbound1.guid, outbound1);
    local.wbos.put(outbound2.guid, outbound2);
    final Synchronizer synchronizer = new Synchronizer();
    synchronizer.repositoryA = remote;
    synchronizer.repositoryB = local;
    return synchronizer;
  }

  protected boolean doSynchronize(final Synchronizer synchronizer) {
    final AtomicBoolean success = new AtomicBoolean(false);

    WaitHelper.getTestWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        synchronizer.synchronize(null, new SynchronizerDelegate() {
          @Override
          public void onSynchronized(Synchronizer synchronizer) {
            success.set(true);
            WaitHelper.getTestWaiter().performNotify();
          }

          @Override
          public void onSynchronizeFailed(Synchronizer synchronizer, Exception lastException, String reason) {
            success.set(false);
            WaitHelper.getTestWaiter().performNotify();
          }
        });
      }
    });
    return success.get();
  }

  @Test
  public void testErrors() {
    Logger.LOG_TO_STDOUT = true;

    Synchronizer synchronizer;
    synchronizer = getSynchronizer(new WBORepository(), new WBORepository());
    assertTrue(doSynchronize(synchronizer));
    assertArrayEquals(new String[] { }, synchronizer.localStoreFailedGuids.toArray(new String[0]));
    assertArrayEquals(new String[] { }, synchronizer.remoteStoreFailedGuids.toArray(new String[0]));

    // Local store failures should be ignored.
    synchronizer = getSynchronizer(new WBORepository(), new FailStoreWBORepository());
    assertTrue(doSynchronize(synchronizer));
    assertArrayEquals(new String[] { "inboundFail" }, synchronizer.localStoreFailedGuids.toArray(new String[0]));
    assertArrayEquals(new String[] { }, synchronizer.remoteStoreFailedGuids.toArray(new String[0]));

    // Remote store failures should be ignored.
    synchronizer = getSynchronizer(new FailStoreWBORepository(), new TrackingWBORepository());
    assertTrue(doSynchronize(synchronizer));
    assertArrayEquals(new String[] { }, synchronizer.localStoreFailedGuids.toArray(new String[0]));
    assertArrayEquals(new String[] { "outboundFail" }, synchronizer.remoteStoreFailedGuids.toArray(new String[0]));
  }
}
