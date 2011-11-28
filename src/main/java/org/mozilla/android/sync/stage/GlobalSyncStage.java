package org.mozilla.android.sync.stage;

import org.mozilla.android.sync.GlobalSession;

public interface GlobalSyncStage {
  public static enum Stage {
    idle,                       // Start state.
    checkPreconditions,         // Preparation of the basics. TODO: clear status
    ensureClusterURL,           // Setting up where we talk to.
    temporaryFetchBookmarks,    // TODO: XXX: TEMP: woohoo!
/*
    fetchInfoCollections,       // Take a look at timestamps.
    ensureSpecialRecords,
    updateEngineTimestamps,
    syncClientsEngine,
    processFirstSyncPref,
    processClientCommands,
    updateEnabledEngines,
    syncEngines,
    */
    completed
  }
  public void execute(GlobalSession session) throws NoSuchStageException;
}
