/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.mozilla.android.sync.test.helpers.WBORepository;
import org.mozilla.gecko.sync.repositories.RepositorySession;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionCreationDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionGuidsSinceDelegate;
import org.mozilla.gecko.sync.repositories.domain.BookmarkRecord;
import org.mozilla.gecko.sync.synchronizer.FillingRecordsChannel;
import org.mozilla.gecko.sync.synchronizer.RecordsChannel;
import org.mozilla.gecko.sync.synchronizer.RecordsChannelDelegate;
import org.mozilla.gecko.sync.synchronizer.managers.ArrayListGuidsManager;

import android.content.Context;

public class TestFillingRecordsChannel extends RecordsChannelHelper {
  protected ArrayListGuidsManager manager;

  protected RecordsChannel newRecordsChannel(final RepositorySession source, final RepositorySession sink, final RecordsChannelDelegate rcDelegate) {
    return new FillingRecordsChannel(source,  sink, rcDelegate, manager);
  }

  public static class CountGuidsSinceWBORepository extends WBORepository {
    final AtomicInteger numGuidsSince = new AtomicInteger(0);
    final ArrayList<Long> timestamps = new ArrayList<Long>();

    @Override
    public void createSession(RepositorySessionCreationDelegate delegate,
                              Context context) {
      delegate.deferredCreationDelegate().onSessionCreated(new WBORepositorySession(this) {
        @Override
        public void guidsSince(long timestamp, final RepositorySessionGuidsSinceDelegate delegate) {
          numGuidsSince.incrementAndGet();
          timestamps.add(new Long(timestamp));
          super.guidsSince(timestamp, delegate);
        }
      });
    }
  }

  /**
   * Verify that flow fetches fresh GUIDs using guidsSince and calls fetch
   * correctly.
   */
  @Test
  public void testFlow() throws Exception {
    final int FIRST = 2;
    final int SECOND = 20;
    final int THIRD = 30;

    CountGuidsSinceWBORepository source = new CountGuidsSinceWBORepository();
    for (int i = 0; i < 10; i++) {
      source.wbos.put("test" + i, new BookmarkRecord("test" + i, "test", i, false));
    }
    WBORepository sink = new WBORepository();

    manager = new ArrayListGuidsManager(5);
    doFlow(FIRST, source, sink);
    assertEquals(1, numFlowCompleted.get());

    Set<String> guids = new HashSet<String>();
    for (String guid : new String[] { "test5", "test6", "test7", "test8", "test9" }) {
      guids.add(guid);
    }
    assertEquals(guids, sink.wbos.keySet());
    assertEquals(3, manager.guids.size());
    assertArrayEquals(new String[] { "test2", "test3", "test4" }, manager.guids.toArray(new String[0]));

    assertEquals(1, source.numGuidsSince.get());
    assertEquals(FIRST, source.timestamps.get(0).longValue());

    // Now flow again. We should find no new GUIDs, but we should grab the
    // remaining old ones.
    sink.wbos.clear();
    doFlow(SECOND, source, sink);
    assertEquals(1, numFlowCompleted.get());
    assertEquals(2, source.numGuidsSince.get());
    assertEquals(SECOND, source.timestamps.get(1).longValue());

    guids = new HashSet<String>();
    for (String guid : new String[] { "test2", "test3", "test4"  }) {
      guids.add(guid);
    }
    assertEquals(guids, sink.wbos.keySet());
    assertEquals(0, manager.guids.size());

    // Final flow.  No new GUIDs, nothing to fetch.
    sink.wbos.clear();
    doFlow(THIRD, source, sink);
    assertEquals(1, numFlowCompleted.get());
    assertEquals(3, source.numGuidsSince.get());
    assertEquals(THIRD, source.timestamps.get(2).longValue());
    assertTrue(sink.wbos.isEmpty());
  }

  /**
   * Verify that GUIDs failing to store are retried.
   */
  @Test
  public void testRetry() throws Exception {
    final int FIRST = 2;

    CountGuidsSinceWBORepository source = new CountGuidsSinceWBORepository();
    for (int i = 0; i < 10; i++) {
      source.wbos.put(SynchronizerHelpers.FAIL_SENTINEL + i, new BookmarkRecord(SynchronizerHelpers.FAIL_SENTINEL + i, "test", i, false));
    }
    for (int i = 10; i < 12; i++) {
      source.wbos.put("test" + i, new BookmarkRecord("test" + i, "test", i, false));
    }
    WBORepository sink = new SynchronizerHelpers.SerialFailStoreWBORepository();

    final Set<String> retryGuids = new HashSet<String>();
    manager = new ArrayListGuidsManager(5) {
      @Override
      public void retryGuids(final Collection<String> guids) throws Exception {
        retryGuids.addAll(guids);
        super.retryGuids(guids);
      }
    };
    doFlow(FIRST, source, sink);
    assertEquals(1, numFlowCompleted.get());

    // Successes...
    Set<String> guids = new HashSet<String>();
    for (String guid : new String[] { "test10", "test11" }) {
      guids.add(guid);
      assertFalse(manager.guids.contains(guid));
    }
    assertEquals(guids, sink.wbos.keySet());

    // Retries...
    guids = new HashSet<String>();
    for (String guid : new String[] { SynchronizerHelpers.FAIL_SENTINEL + 7, SynchronizerHelpers.FAIL_SENTINEL + 8, SynchronizerHelpers.FAIL_SENTINEL + 9 }) {
      guids.add(guid);
      assertTrue(manager.guids.contains(guid));
    }
    assertEquals(guids, retryGuids);
  }
}
