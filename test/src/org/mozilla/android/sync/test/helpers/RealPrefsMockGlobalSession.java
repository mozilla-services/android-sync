/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;

import org.json.simple.parser.ParseException;
import org.mozilla.gecko.sync.EngineSettings;
import org.mozilla.gecko.sync.GlobalSession;
import org.mozilla.gecko.sync.NonObjectJSONException;
import org.mozilla.gecko.sync.SyncConfiguration;
import org.mozilla.gecko.sync.SyncConfigurationException;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.delegates.GlobalSessionCallback;
import org.mozilla.gecko.sync.repositories.RecordFactory;
import org.mozilla.gecko.sync.repositories.Repository;
import org.mozilla.gecko.sync.stage.AbstractNonRepositorySyncStage;
import org.mozilla.gecko.sync.stage.GlobalSyncStage;
import org.mozilla.gecko.sync.stage.GlobalSyncStage.Stage;
import org.mozilla.gecko.sync.stage.ServerSyncStage;

import android.content.Context;

public class RealPrefsMockGlobalSession extends GlobalSession {

  public RealPrefsMockGlobalSession(String clusterURL, String username, String password,
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
    public Integer getStorageVersion() {
      return 1;
    }

    @Override
    protected RecordFactory getRecordFactory() {
      return null;
    }
  }

  public class MockStage extends AbstractNonRepositorySyncStage {
    @Override
    public void execute() {
      session.advance();
    }
  }

  @Override
  protected void prepareStages() {
    super.prepareStages();
    HashMap<Stage, GlobalSyncStage> stages = new HashMap<Stage, GlobalSyncStage>(this.stages);

    // Fake whatever stages we don't want to run.
    stages.put(Stage.syncBookmarks,           new MockServerSyncStage());
    stages.put(Stage.syncHistory,             new MockServerSyncStage());
    stages.put(Stage.fetchInfoCollections,    new MockStage());
    stages.put(Stage.fetchMetaGlobal,         new MockStage());
    stages.put(Stage.ensureKeysStage,         new MockStage());
    stages.put(Stage.ensureClusterURL,        new MockStage());
    stages.put(Stage.syncClientsEngine,       new MockStage());

    this.stages = Collections.unmodifiableMap(stages);
  }

  @Override
  public boolean engineIsEnabled(String engine, EngineSettings engineSettings) {
    return false;
  }
}
