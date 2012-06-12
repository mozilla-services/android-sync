package org.mozilla.android.sync.test;

import static org.junit.Assert.assertArrayEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.mozilla.android.sync.test.helpers.ExpectSuccessRepositorySessionBeginDelegate;
import org.mozilla.android.sync.test.helpers.ExpectSuccessRepositorySessionCreationDelegate;
import org.mozilla.android.sync.test.helpers.WBORepository;
import org.mozilla.android.sync.test.helpers.WaitHelper;
import org.mozilla.gecko.sync.repositories.InvalidSessionTransitionException;
import org.mozilla.gecko.sync.repositories.Repository;
import org.mozilla.gecko.sync.repositories.RepositorySession;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionGuidsSinceDelegate;
import org.mozilla.gecko.sync.repositories.domain.BookmarkRecord;

public class TestWBORepository {
  protected RepositorySession source;

  public static class ExpectGuidsSince implements RepositorySessionGuidsSinceDelegate {
    protected final List<String> expected = new ArrayList<String>();

    public ExpectGuidsSince(final String[] guids) {
      for (String guid : guids) {
        expected.add(guid);
      }
    }

    @Override
    public void onGuidsSinceSucceeded(Collection<String> guids) {
      try {
        assertArrayEquals(expected.toArray(new String[0]), guids.toArray(new String[0]));
      } catch (Throwable e) {
        WaitHelper.getTestWaiter().performNotify(e);
        return;
      }
      WaitHelper.getTestWaiter().performNotify();
    }

    @Override
    public void onGuidsSinceFailed(Exception ex) {
      WaitHelper.getTestWaiter().performNotify(ex);
    }
  }

  protected void doGuidsSince(final Repository repo, final int timestamp, final String[] expected) {
    WaitHelper.getTestWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        repo.createSession(new ExpectSuccessRepositorySessionCreationDelegate(WaitHelper.getTestWaiter()) {
          @Override
          public void onSessionCreated(RepositorySession session) {
            source = session;
            WaitHelper.getTestWaiter().performNotify();
          }
        }, null);
      }
    });

    WaitHelper.getTestWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        try {
          source.begin(new ExpectSuccessRepositorySessionBeginDelegate(WaitHelper.getTestWaiter()) {
            @Override
            public void onBeginSucceeded(RepositorySession session) {
              source.guidsSince(timestamp, new ExpectGuidsSince(expected));
            }
          });
        } catch (InvalidSessionTransitionException e) {
          WaitHelper.getTestWaiter().performNotify(e);
        }
      }
    });
  }

  @Test
  public void testGuidsSince() {
    final WBORepository local = new WBORepository();
    local.wbos.put("test1", new BookmarkRecord("test1", "test", 1, false));
    local.wbos.put("test2", new BookmarkRecord("test2", "test", 2, false));
    local.wbos.put("test3", new BookmarkRecord("test3", "test", 3, false));
    local.wbos.put("test4", new BookmarkRecord("test4", "test", 4, false));

    doGuidsSince(local, 0, new String[] { "test1", "test2", "test3", "test4" });
    doGuidsSince(local, 1, new String[] { "test1", "test2", "test3", "test4" });
    doGuidsSince(local, 2, new String[] { "test2", "test3", "test4" });
    doGuidsSince(local, 3, new String[] { "test3", "test4" });
    doGuidsSince(local, 4, new String[] { "test4" });
    doGuidsSince(local, 5, new String[] { });
  }
}
