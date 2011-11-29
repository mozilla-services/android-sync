/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.stage.test;

import org.mozilla.android.sync.GlobalSession;
import org.mozilla.android.sync.GlobalSessionCallback;
import org.mozilla.android.sync.SyncConfigurationException;
import org.mozilla.android.sync.crypto.KeyBundle;
import org.mozilla.android.sync.stage.EnsureKeysStage;
import org.mozilla.android.sync.stage.FetchInfoCollectionsStage;
import org.mozilla.android.sync.stage.FetchMetaGlobalStage;
import org.mozilla.android.sync.stage.GlobalSyncStage.Stage;
import org.mozilla.android.sync.stage.NoSuchStageException;
import org.mozilla.android.sync.stage.TemporaryFetchBookmarksStage;

import android.content.Context;

// Mock this out so our tests continue to pass as we hack.
public class MockGlobalSession extends GlobalSession {

  public MockGlobalSession(String clusterURL, String username, String password,
      KeyBundle syncKeyBundle, GlobalSessionCallback callback, Context context)
      throws SyncConfigurationException, IllegalArgumentException {
    super(clusterURL, username, password, syncKeyBundle, callback, context);
  }

  public class MockTemporaryFetchBookmarksStage extends TemporaryFetchBookmarksStage {
    @Override
    public void execute(GlobalSession session) throws NoSuchStageException {
      session.advance();
    }
  }

  public class MockFetchInfoCollectionsStage extends FetchInfoCollectionsStage {
    @Override
    public void execute(GlobalSession session) throws NoSuchStageException {
      session.advance();
    }
  }

  public class MockFetchMetaGlobalStage extends FetchMetaGlobalStage {
    @Override
    public void execute(GlobalSession session) throws NoSuchStageException {
      session.advance();
    }
  }

  public class MockEnsureKeysStage extends EnsureKeysStage {
    @Override
    public void execute(GlobalSession session) throws NoSuchStageException {
      session.advance();
    }
  }

  @Override
  protected void prepareStages() {
    super.prepareStages();
    // Fake whatever stages we don't want to run.
    stages.put(Stage.temporaryFetchBookmarks, new MockTemporaryFetchBookmarksStage());
    stages.put(Stage.fetchInfoCollections,    new MockFetchInfoCollectionsStage());
    stages.put(Stage.fetchMetaGlobal,         new MockFetchMetaGlobalStage());
    stages.put(Stage.ensureKeysStage,         new MockFetchInfoCollectionsStage());

  }

}
