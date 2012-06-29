/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.mozilla.android.sync.test.SynchronizerHelpers.FailFetchWBORepository;
import org.mozilla.android.sync.test.helpers.WBORepository;
import org.mozilla.gecko.sync.repositories.domain.BookmarkRecord;

public class TestRecordsChannel extends RecordsChannelHelper {

  public static final BookmarkRecord[] inbounds = new BookmarkRecord[] {
    new BookmarkRecord("inboundSucc1", "bookmarks", 1, false),
    new BookmarkRecord("inboundSucc2", "bookmarks", 1, false),
    new BookmarkRecord("inboundFail1", "bookmarks", 1, false),
    new BookmarkRecord("inboundSucc3", "bookmarks", 1, false),
    new BookmarkRecord("inboundSucc4", "bookmarks", 1, false),
    new BookmarkRecord("inboundFail2", "bookmarks", 1, false),
  };
  public static final BookmarkRecord[] outbounds = new BookmarkRecord[] {
      new BookmarkRecord("outboundSucc1", "bookmarks", 1, false),
      new BookmarkRecord("outboundSucc2", "bookmarks", 1, false),
      new BookmarkRecord("outboundSucc3", "bookmarks", 1, false),
      new BookmarkRecord("outboundSucc4", "bookmarks", 1, false),
      new BookmarkRecord("outboundSucc5", "bookmarks", 1, false),
      new BookmarkRecord("outboundFail6", "bookmarks", 1, false),
  };

  protected WBORepository empty() {
    WBORepository repo = new SynchronizerHelpers.TrackingWBORepository();
    return repo;
  }

  protected WBORepository full() {
    WBORepository repo = new SynchronizerHelpers.TrackingWBORepository();
    for (BookmarkRecord outbound : outbounds) {
      repo.wbos.put(outbound.guid, outbound);
    }
    return repo;
  }

  protected WBORepository failingFetch() {
    WBORepository repo = new FailFetchWBORepository();
    for (BookmarkRecord outbound : outbounds) {
      repo.wbos.put(outbound.guid, outbound);
    }
    return repo;
  }

  @Test
  public void testSuccess() throws Exception {
    WBORepository source = full();
    WBORepository sink = empty();
    doFlow(source, sink);
    assertEquals(1, numFlowCompleted.get());
    assertEquals(0, numFlowFetchFailed.get());
    assertEquals(0, numFlowStoreFailed.get());
    assertEquals(source.wbos, sink.wbos);
  }

  @Test
  public void testFetchFail() throws Exception {
    WBORepository source = failingFetch();
    WBORepository sink = empty();
    doFlow(source, sink);
    assertEquals(1, numFlowCompleted.get());
    assertTrue(numFlowFetchFailed.get() > 0);
    assertEquals(0, numFlowStoreFailed.get());
    assertTrue(sink.wbos.size() < 6);
  }

  @Test
  public void testStoreSerialFail() throws Exception {
    WBORepository source = full();
    WBORepository sink = new SynchronizerHelpers.SerialFailStoreWBORepository();
    doFlow(source, sink);
    assertEquals(1, numFlowCompleted.get());
    assertEquals(0, numFlowFetchFailed.get());
    assertEquals(1, numFlowStoreFailed.get());
    assertEquals(5, sink.wbos.size());
  }

  @Test
  public void testStoreBatchesFail() throws Exception {
    WBORepository source = full();
    WBORepository sink = new SynchronizerHelpers.BatchFailStoreWBORepository(3);
    doFlow(source, sink);
    assertEquals(1, numFlowCompleted.get());
    assertEquals(0, numFlowFetchFailed.get());
    assertEquals(3, numFlowStoreFailed.get()); // One batch fails.
    assertEquals(3, sink.wbos.size()); // One batch succeeds.
  }


  @Test
  public void testStoreOneBigBatchFail() throws Exception {
    WBORepository source = full();
    WBORepository sink = new SynchronizerHelpers.BatchFailStoreWBORepository(50);
    doFlow(source, sink);
    assertEquals(1, numFlowCompleted.get());
    assertEquals(0, numFlowFetchFailed.get());
    assertEquals(6, numFlowStoreFailed.get()); // One (big) batch fails.
    assertEquals(0, sink.wbos.size()); // No batches succeed.
  }
}
