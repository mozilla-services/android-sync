/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.stage.test;

import java.io.IOException;

import org.json.simple.parser.ParseException;
import org.mozilla.gecko.sync.GlobalSession;
import org.mozilla.gecko.sync.NonObjectJSONException;
import org.mozilla.gecko.sync.SyncConfiguration;
import org.mozilla.gecko.sync.SyncConfigurationException;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.delegates.GlobalSessionCallback;
import org.mozilla.gecko.sync.repositories.RecordFactory;
import org.mozilla.gecko.sync.repositories.Repository;
import org.mozilla.gecko.sync.stage.GlobalSyncStage;
import org.mozilla.gecko.sync.stage.GlobalSyncStage.Stage;
import org.mozilla.gecko.sync.stage.ServerSyncStage;

import android.content.Context;

// Mock this out so our tests continue to pass as we hack.
public class MockGlobalSession extends GlobalSession {

  // TODO: mock prefs.
  public MockGlobalSession(String clusterURL, String username, String password,
      KeyBundle syncKeyBundle, GlobalSessionCallback callback, Context context)
      throws SyncConfigurationException, IllegalArgumentException, IOException, ParseException, NonObjectJSONException {
    super(SyncConfiguration.DEFAULT_USER_API, clusterURL, username, password, null, syncKeyBundle, callback, context, null, null);
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
    protected String getEngineName() {
      return null;
    }

    @Override
    protected RecordFactory getRecordFactory() {
      return null;
    }
  }

  public class MockStage implements GlobalSyncStage {
    @Override
    public void execute(GlobalSession session) {
      session.advance();
    }
  }

  @Override
  protected void prepareStages() {
    super.prepareStages();
    // Fake whatever stages we don't want to run.
    stages.put(Stage.syncBookmarks,           new MockServerSyncStage());
    stages.put(Stage.syncHistory,             new MockServerSyncStage());
    stages.put(Stage.fetchInfoCollections,    new MockStage());
    stages.put(Stage.fetchMetaGlobal,         new MockStage());
    stages.put(Stage.ensureKeysStage,         new MockStage());
    stages.put(Stage.ensureClusterURL,        new MockStage());
    stages.put(Stage.syncClientsEngine,       new MockStage());
  }

  @Override
  public boolean engineIsEnabled(String engine) {
    return false;
  }
}
