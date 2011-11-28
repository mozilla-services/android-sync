package org.mozilla.android.sync.stage.test;

import java.util.HashMap;

import org.mozilla.android.sync.GlobalSession;
import org.mozilla.android.sync.GlobalSessionCallback;
import org.mozilla.android.sync.SyncConfigurationException;
import org.mozilla.android.sync.TemporaryFetchBookmarksStage;
import org.mozilla.android.sync.crypto.KeyBundle;
import org.mozilla.android.sync.stage.CheckPreconditionsStage;
import org.mozilla.android.sync.stage.CompletedStage;
import org.mozilla.android.sync.stage.EnsureClusterURLStage;
import org.mozilla.android.sync.stage.GlobalSyncStage;
import org.mozilla.android.sync.stage.GlobalSyncStage.Stage;
import org.mozilla.android.sync.stage.NoSuchStageException;

// Mock this out so our tests continue to pass as we hack.
public class MockGlobalSession extends GlobalSession {

  public MockGlobalSession(String clusterURL, String username, String password,
      KeyBundle syncKeyBundle, GlobalSessionCallback callback)
      throws SyncConfigurationException, IllegalArgumentException {
    super(clusterURL, username, password, syncKeyBundle, callback);
  }

  public class MockTemporaryFetchBookmarksStage extends TemporaryFetchBookmarksStage {
    @Override
    public void execute(GlobalSession session) throws NoSuchStageException {
      session.advance();
    }
  }

  @Override
  protected void prepareStages() {
    stages = new HashMap<Stage, GlobalSyncStage>();
    stages.put(Stage.checkPreconditions, new CheckPreconditionsStage());
    stages.put(Stage.ensureClusterURL, new EnsureClusterURLStage());
    stages.put(Stage.temporaryFetchBookmarks, new MockTemporaryFetchBookmarksStage());
    stages.put(Stage.completed, new CompletedStage());
  }

}
