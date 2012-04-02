package org.mozilla.gecko.sync.repositories.android.test;

import java.net.URISyntaxException;

import org.mozilla.android.sync.test.AndroidSyncTestCase;
import org.mozilla.gecko.sync.repositories.FillingServer11Repository;

import android.content.Context;

public class TestFillingServer11Repository extends
    AndroidSyncTestCase {

  public void testPersisting() throws Exception {
    Context context = getApplicationContext();
    String[] guids;

    FillingServer11Repository repo = new FillingServer11Repository(null, null, "test", null, 1, "index");
    try {
      context.deleteFile(repo.getFileName());

      assertNull(repo.guidsRemaining(context));

      repo.persistGuidsRemaining(new String[] { "test" }, context);
      guids = repo.guidsRemaining(context);
      assertEquals(1, guids.length);
      assertEquals("test", guids[0]);

      repo.persistGuidsRemaining(new String[] { "first", "second" }, context);
      guids = repo.guidsRemaining(context);
      assertEquals(2, guids.length);
      assertEquals("first", guids[0]);
      assertEquals("second", guids[1]);

      repo.persistGuidsRemaining(new String[0], context);
      guids = repo.guidsRemaining(context);
      assertEquals(0, guids.length);
    } finally {
      context.deleteFile(repo.getFileName());
    }
  }

  public void testToFill() throws URISyntaxException {
    FillingServer11Repository repo = new FillingServer11Repository(null, null, "test", null, 1, "index") {
      @Override
      protected long getDefaultFetchLimit() {
        return 5;
      }

      @Override
      protected int getDefaultPerFillMaximum() {
        return 4;
      }

      @Override
      protected int getDefaultPerFillMinimum() {
        return 2;
      }
    };

    String[] guids = new String[] { "a", "b", "c", "d", "e", "f" };
    assertEquals(4, repo.guidsToFillThisSession(guids, 0).length);
    assertEquals(4, repo.guidsToFillThisSession(guids, 1).length);
    assertEquals(3, repo.guidsToFillThisSession(guids, 2).length);
    assertEquals(2, repo.guidsToFillThisSession(guids, 3).length);
    assertEquals(2, repo.guidsToFillThisSession(guids, 4).length);
    assertNull(repo.guidsToFillThisSession(guids, 5)); // Already fetched max, not filling at all.
    guids = new String[] { "a", "b", "c" };
    assertEquals(3, repo.guidsToFillThisSession(guids, 0).length);
    assertEquals(3, repo.guidsToFillThisSession(guids, 1).length);
    assertEquals(3, repo.guidsToFillThisSession(guids, 2).length);
    assertEquals(2, repo.guidsToFillThisSession(guids, 3).length);
  }
}
