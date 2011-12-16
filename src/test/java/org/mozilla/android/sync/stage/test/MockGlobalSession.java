/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.stage.test;

import org.mozilla.android.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.GlobalSession;
import org.mozilla.gecko.sync.SyncConfiguration;
import org.mozilla.gecko.sync.SyncConfigurationException;
import org.mozilla.gecko.sync.delegates.GlobalSessionCallback;
import org.mozilla.gecko.sync.repositories.Repository;
import org.mozilla.gecko.sync.stage.EnsureKeysStage;
import org.mozilla.gecko.sync.stage.FetchInfoCollectionsStage;
import org.mozilla.gecko.sync.stage.FetchMetaGlobalStage;
import org.mozilla.gecko.sync.stage.NoSuchStageException;
import org.mozilla.gecko.sync.stage.ServerSyncStage;
import org.mozilla.gecko.sync.stage.GlobalSyncStage.Stage;

import android.content.Context;

// Mock this out so our tests continue to pass as we hack.
public class MockGlobalSession extends GlobalSession {

  public MockGlobalSession(String clusterURL, String username, String password,
      KeyBundle syncKeyBundle, GlobalSessionCallback callback, Context context)
      throws SyncConfigurationException, IllegalArgumentException {
    super(SyncConfiguration.DEFAULT_USER_API, clusterURL, username, password, syncKeyBundle, callback, context);
  }

  public class MockServerSyncStage extends ServerSyncStage {
    @Override
    public boolean isEnabled() {
      return false;
    }

    @Override
    protected String getCollection() {
      return null;
    }

    @Override
    protected Repository getLocalRepository() {
      return null;
    }

    @Override
    protected RecordFactory getRecordFactory() {
      return null;
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
    stages.put(Stage.syncBookmarks,           new MockServerSyncStage());
    stages.put(Stage.fetchInfoCollections,    new MockFetchInfoCollectionsStage());
    stages.put(Stage.fetchMetaGlobal,         new MockFetchMetaGlobalStage());
    stages.put(Stage.ensureKeysStage,         new MockFetchInfoCollectionsStage());

  }

}
