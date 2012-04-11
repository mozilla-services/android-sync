package org.mozilla.gecko.sync.repositories.android.test;

import org.mozilla.android.sync.test.AndroidSyncTestCase;
import org.mozilla.gecko.sync.repositories.android.FennecControlHelper;

public class TestFennecControlHelper extends AndroidSyncTestCase {
  public void testIsMigrated() {
    assertTrue(FennecControlHelper.areBookmarksMigrated(getApplicationContext()));
    // Might take a few shots, but we should be able to migrate if we haven't already.
    int numMigrations = 15;
    int i = numMigrations;
    while (i > 0) {
      if (FennecControlHelper.isHistoryMigrated(getApplicationContext())) {
        return;
      }
      i -= 1;
    }
    fail("Could not migrate Fennec history in " + numMigrations +
        " trial migrations; this could happen if your Fennec history is very large.");
  }
}
