package org.mozilla.gecko.sync;

import android.content.SyncResult;

public class NoCollectionKeysSetException extends SyncException {
  private static final long serialVersionUID = -6185128075412771120L;

  @Override
  public void updateStats(GlobalSession globalSession, SyncResult syncResult) {
    syncResult.stats.numAuthExceptions++;
  }
}
