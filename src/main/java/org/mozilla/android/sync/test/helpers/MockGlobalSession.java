/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.parser.ParseException;
import org.mozilla.gecko.sync.EngineSettings;
import org.mozilla.gecko.sync.MetaGlobalException;
import org.mozilla.gecko.sync.NonObjectJSONException;
import org.mozilla.gecko.sync.SyncConfiguration;
import org.mozilla.gecko.sync.SyncConfigurationException;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.delegates.GlobalSessionCallback;
import org.mozilla.gecko.sync.stage.CompletedStage;
import org.mozilla.gecko.sync.stage.GlobalSyncStage;
import org.mozilla.gecko.sync.stage.GlobalSyncStage.Stage;
import org.mozilla.gecko.sync.stage.GlobalSyncStageFactory;


public class MockGlobalSession extends MockPrefsGlobalSession {
  public Map<Stage, GlobalSyncStage> stageOverrides;

  public MockGlobalSession(String clusterURL, String username, String password,
      KeyBundle syncKeyBundle, GlobalSessionCallback callback)
          throws SyncConfigurationException, IllegalArgumentException, IOException, ParseException, NonObjectJSONException {
    super(SyncConfiguration.DEFAULT_USER_API, clusterURL, username, password, null, syncKeyBundle, callback, /* context */ null, null, null);
  }

  @Override
  public boolean engineIsEnabled(String engine, EngineSettings engineSettings) throws MetaGlobalException {
    return false;
  }

  @Override
  protected void prepareStages() {
    this.stageOverrides = new HashMap<Stage, GlobalSyncStage>();
    this.stageFactory = new GlobalSyncStageFactory() {
      @Override
      public GlobalSyncStage createGlobalSyncStage(Stage stage) {
        if (stageOverrides.containsKey(stage)) {
          return stageOverrides.get(stage);
        }

        if (stage.equals(Stage.completed)) {
          return new CompletedStage();
        }

        return new MockServerSyncStage();
      }
    };
  };

  public MockGlobalSession withStage(Stage stage, GlobalSyncStage syncStage) {
    stageOverrides.put(stage, syncStage);

    return this;
  }
}
