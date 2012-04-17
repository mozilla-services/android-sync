/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;

import org.json.simple.parser.ParseException;
import org.mozilla.gecko.sync.EngineSettings;
import org.mozilla.gecko.sync.NonObjectJSONException;
import org.mozilla.gecko.sync.SyncConfiguration;
import org.mozilla.gecko.sync.SyncConfigurationException;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.delegates.GlobalSessionCallback;
import org.mozilla.gecko.sync.stage.GlobalSyncStage;
import org.mozilla.gecko.sync.stage.GlobalSyncStage.Stage;


public class MockGlobalSession extends MockPrefsGlobalSession {

  public MockGlobalSession(String clusterURL, String username, String password,
      KeyBundle syncKeyBundle, GlobalSessionCallback callback)
          throws SyncConfigurationException, IllegalArgumentException, IOException, ParseException, NonObjectJSONException {
    super(SyncConfiguration.DEFAULT_USER_API, clusterURL, username, password, null, syncKeyBundle, callback, /* context */ null, null, null);
  }

  @Override
  public boolean engineIsEnabled(String engine, EngineSettings engineSettings) {
    return false;
  }

  @Override
  protected void prepareStages() {
    super.prepareStages();
    HashMap<Stage, GlobalSyncStage> stages = new HashMap<Stage, GlobalSyncStage>(this.stages);

    // Fake whatever stages we don't want to run.
    stages.put(Stage.syncBookmarks,           new MockServerSyncStage(this));
    stages.put(Stage.syncHistory,             new MockServerSyncStage(this));
    stages.put(Stage.syncTabs,                new MockServerSyncStage(this));
    stages.put(Stage.fetchInfoCollections,    new MockServerSyncStage(this));
    stages.put(Stage.fetchMetaGlobal,         new MockServerSyncStage(this));
    stages.put(Stage.ensureKeysStage,         new MockServerSyncStage(this));
    stages.put(Stage.ensureClusterURL,        new MockServerSyncStage(this));
    stages.put(Stage.syncClientsEngine,       new MockServerSyncStage(this));

    this.stages = Collections.unmodifiableMap(stages);
  }
}
