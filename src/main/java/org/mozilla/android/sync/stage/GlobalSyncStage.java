package org.mozilla.android.sync.stage;

import org.mozilla.android.sync.GlobalSession;

public interface GlobalSyncStage {
  public static enum Stage {
    uninitialized,
    checkPreconditions,
    ensureClusterURL,
    fetchInfoCollections,
    ensureSpecialRecords,
    updateEngineTimestamps,
    syncClientsEngine,
    processFirstSyncPref,
    processClientCommands,
    updateEnabledEngines,
    syncEngines,
    completed
  }
  public void execute(GlobalSession session);
}
