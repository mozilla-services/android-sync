package org.mozilla.android.sync.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;

import org.junit.Test;
import org.mozilla.android.sync.test.SynchronizerHelpers.BatchFailStoreWBORepository;
import org.mozilla.android.sync.test.SynchronizerHelpers.FailFetchWBORepository;
import org.mozilla.android.sync.test.SynchronizerHelpers.SerialFailStoreWBORepository;
import org.mozilla.android.sync.test.SynchronizerHelpers.TrackingWBORepository;
import org.mozilla.android.sync.test.helpers.WBORepository;
import org.mozilla.android.sync.test.helpers.WaitHelper;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.repositories.FetchFailedException;
import org.mozilla.gecko.sync.repositories.StoreFailedException;
import org.mozilla.gecko.sync.repositories.domain.BookmarkRecord;
import org.mozilla.gecko.sync.synchronizer.ServerLocalSynchronizer;
import org.mozilla.gecko.sync.synchronizer.Synchronizer;
import org.mozilla.gecko.sync.synchronizer.SynchronizerDelegate;

public class TestServerLocalSynchronizer {
  public static final String LOG_TAG = "TestServLocSync";

  protected Synchronizer getSynchronizer(WBORepository remote, WBORepository local) {
    BookmarkRecord[] inbounds = new BookmarkRecord[] {
        new BookmarkRecord("inboundSucc1", "bookmarks", 1, false),
        new BookmarkRecord("inboundSucc2", "bookmarks", 1, false),
        new BookmarkRecord("inboundFail1", "bookmarks", 1, false),
        new BookmarkRecord("inboundSucc3", "bookmarks", 1, false),
        new BookmarkRecord("inboundFail2", "bookmarks", 1, false),
        new BookmarkRecord("inboundFail3", "bookmarks", 1, false),
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

    final Synchronizer synchronizer = new ServerLocalSynchronizer();
    synchronizer.repositoryA = remote;
    synchronizer.repositoryB = local;
    return synchronizer;
  }

  protected static Exception doSynchronize(final Synchronizer synchronizer) {
    final ArrayList<Exception> a = new ArrayList<Exception>();

    WaitHelper.getTestWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        synchronizer.synchronize(null, new SynchronizerDelegate() {
          @Override
          public void onSynchronized(Synchronizer synchronizer) {
            Logger.trace(LOG_TAG, "Got onSynchronized.");
            a.add(null);
            WaitHelper.getTestWaiter().performNotify();
          }

          @Override
          public void onSynchronizeFailed(Synchronizer synchronizer, Exception lastException, String reason) {
            Logger.trace(LOG_TAG, "Got onSynchronizedFailed.");
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
    WBORepository remote = new TrackingWBORepository();
    WBORepository local  = new TrackingWBORepository();

    Synchronizer synchronizer = getSynchronizer(remote, local);
    assertNull(doSynchronize(synchronizer));

    assertEquals(12, local.wbos.size());
    assertEquals(12, remote.wbos.size());
  }

  @Test
  public void testLocalFetchErrors() {
    WBORepository remote = new TrackingWBORepository();
    WBORepository local  = new FailFetchWBORepository();

    Synchronizer synchronizer = getSynchronizer(remote, local);
    Exception e = doSynchronize(synchronizer);
    assertNotNull(e);
    assertEquals(FetchFailedException.class, e.getClass());

    // Neither session gets finished successfully, so all records are dropped.
    assertEquals(6, local.wbos.size());
    assertEquals(6, remote.wbos.size());
  }

  @Test
  public void testRemoteFetchErrors() {
    WBORepository remote = new FailFetchWBORepository();
    WBORepository local  = new TrackingWBORepository();

    Synchronizer synchronizer = getSynchronizer(remote, local);
    Exception e = doSynchronize(synchronizer);
    assertNotNull(e);
    assertEquals(FetchFailedException.class, e.getClass());

    // Neither session gets finished successfully, so all records are dropped.
    assertEquals(6, local.wbos.size());
    assertEquals(6, remote.wbos.size());
  }

  @Test
  public void testLocalSerialStoreErrorsAreIgnored() {
    Logger.LOG_TO_STDOUT = true;

    WBORepository remote = new TrackingWBORepository();
    WBORepository local  = new SerialFailStoreWBORepository();

    Synchronizer synchronizer = getSynchronizer(remote, local);
    assertNull(doSynchronize(synchronizer));

    assertEquals(9,  local.wbos.size());
    assertEquals(12, remote.wbos.size());
  }

  @Test
  public void testLocalBatchStoreErrorsAreIgnored() {
    final int BATCH_SIZE = 3;

    Synchronizer synchronizer = getSynchronizer(new TrackingWBORepository(), new BatchFailStoreWBORepository(BATCH_SIZE));

    Exception e = doSynchronize(synchronizer);
    assertNull(e);
  }

  @Test
  public void testRemoteSerialStoreErrorsAreNotIgnored() throws Exception {
    Synchronizer synchronizer = getSynchronizer(new SerialFailStoreWBORepository(), new TrackingWBORepository()); // Tracking so we don't send incoming records back.

    Exception e = doSynchronize(synchronizer);
    assertNotNull(e);
    assertEquals(StoreFailedException.class, e.getClass());
  }

  @Test
  public void testRemoteBatchStoreErrorsAreNotIgnoredManyBatches() throws Exception {
    Logger.LOG_TO_STDOUT = true;
    final int BATCH_SIZE = 3;

    Synchronizer synchronizer = getSynchronizer(new BatchFailStoreWBORepository(BATCH_SIZE), new TrackingWBORepository()); // Tracking so we don't send incoming records back.

    Exception e = doSynchronize(synchronizer);
    assertNotNull(e);
    assertEquals(StoreFailedException.class, e.getClass());
  }

  @Test
  public void testRemoteBatchStoreErrorsAreNotIgnoredOneBigBatch() throws Exception {
    Logger.LOG_TO_STDOUT = true;
    final int BATCH_SIZE = 20;

    Synchronizer synchronizer = getSynchronizer(new BatchFailStoreWBORepository(BATCH_SIZE), new TrackingWBORepository()); // Tracking so we don't send incoming records back.

    Exception e = doSynchronize(synchronizer);
    assertNotNull(e);
    assertEquals(StoreFailedException.class, e.getClass());
  }

}
