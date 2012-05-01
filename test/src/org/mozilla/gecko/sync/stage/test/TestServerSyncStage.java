package org.mozilla.gecko.sync.stage.test;

import org.mozilla.android.sync.test.AndroidSyncTestCase;
import org.mozilla.android.sync.test.helpers.DefaultGlobalSessionCallback;
import org.mozilla.gecko.sync.CryptoRecord;
import org.mozilla.gecko.sync.EngineSettings;
import org.mozilla.gecko.sync.GlobalSession;
import org.mozilla.gecko.sync.MetaGlobal;
import org.mozilla.gecko.sync.MetaGlobalException;
import org.mozilla.gecko.sync.SynchronizerConfiguration;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.stage.FormHistoryServerSyncStage;

/**
 * Test that <code>ServerSyncStage.isEnabled</code> is persisting correctly.
 * <p>
 * Since we really want to test persistence, this is an Android integration test
 * rather than an off-device unit test.
 */
public class TestServerSyncStage extends AndroidSyncTestCase {
  @SuppressWarnings("unused")
  private static final String  LOG_TAG          = "TestMetaGlobalStage";

  private static final String TEST_USERNAME            = "johndoe";
  private static final String TEST_PASSWORD            = "password";
  private static final String TEST_SYNC_KEY            = "abcdeabcdeabcdeabcdeabcdea";

  private static final String TEST_SYNC_ID             = "testSyncID";
  private static final int    TEST_VERSION             = 131;
  private static final String TEST_META_GLOBAL_JSON    = "{\"id\":\"global\",\"payload\":\"{" +
      "\\\"syncID\\\":\\\"zPSQTm7WBVWB\\\",\\\"storageVersion\\\":5,\\\"engines\\\":{" +
      "\\\"clients\\\":{\\\"version\\\":1,\\\"syncID\\\":\\\"fDg0MS5bDtV7\\\"}," +
      "\\\"bookmarks\\\":{\\\"version\\\":2,\\\"syncID\\\":\\\"fEExyQ10MS5_\\\"}," +
      "\\\"forms\\\":{\\\"version\\\":" + TEST_VERSION + ",\\\"syncID\\\":\\\"" + TEST_SYNC_ID + "\\\"}," +
      "\\\"history\\\":{\\\"version\\\":1,\\\"syncID\\\":\\\"av75g4vm-_rp\\\"}," +
      "\\\"passwords\\\":{\\\"version\\\":1,\\\"syncID\\\":\\\"LT_ACGpuKZ6a\\\"}," +
      "\\\"prefs\\\":{\\\"version\\\":2,\\\"syncID\\\":\\\"-3nsksP9wSAs\\\"}," +
      "\\\"tabs\\\":{\\\"version\\\":1,\\\"syncID\\\":\\\"W4H5lOMChkYA\\\"}}}\"," +
      "\"username\":\"5817483\",\"modified\":1.32046073744E9}";
  private static final String TEST_META_GLOBAL_NO_FORMS_JSON = "{\"id\":\"global\",\"payload\":\"{" +
      "\\\"syncID\\\":\\\"zPSQTm7WBVWB\\\",\\\"storageVersion\\\":5,\\\"engines\\\":{" +
      "\\\"clients\\\":{\\\"version\\\":1,\\\"syncID\\\":\\\"fDg0MS5bDtV7\\\"}," +
      "\\\"bookmarks\\\":{\\\"version\\\":2,\\\"syncID\\\":\\\"fEExyQ10MS5_\\\"}," +
      "\\\"tabs\\\":{\\\"version\\\":1,\\\"syncID\\\":\\\"W4H5lOMChkYA\\\"}}}\"," +
      "\"username\":\"5817483\",\"modified\":1.32046073744E9}";

  private KeyBundle syncKeyBundle;
  private DefaultGlobalSessionCallback callback;
  private GlobalSession session;
  private EngineSettings engineSettings;
  private LeakGlobalSyncStage  stage;

  public class LeakGlobalSyncStage extends FormHistoryServerSyncStage {
    public LeakGlobalSyncStage(GlobalSession session) {
      super(session);
    }

    @Override
    public EngineSettings getEngineSettings(SynchronizerConfiguration config) {
      return engineSettings;
    }

    public boolean leakIsEnabled() throws MetaGlobalException {
      return isEnabled();
    }

    public SynchronizerConfiguration leakConfig() throws Exception {
      return getConfig();
    }
  }

  public void setUp() throws Exception {
    syncKeyBundle = new KeyBundle(TEST_USERNAME, TEST_SYNC_KEY);
    callback = new DefaultGlobalSessionCallback();
    session = new GlobalSession(null, null, TEST_USERNAME, TEST_PASSWORD,
        null, syncKeyBundle, callback, getApplicationContext(), null, null);
    session.config.metaGlobal = new MetaGlobal(CryptoRecord.fromJSONRecord(TEST_META_GLOBAL_JSON));
    stage = new LeakGlobalSyncStage(session);
  }

  public void assertPersistedConfigurationIs(boolean enabled, String syncID, int version) throws Exception {
    SynchronizerConfiguration config = stage.leakConfig();
    assertNotNull(config);
    assertEquals(enabled, config.enabled);
    assertEquals(syncID,  config.syncID);
    assertEquals(version, config.version);
  }

  public void testEnabledOnServer() throws Exception {
    engineSettings = new EngineSettings(TEST_SYNC_ID, TEST_VERSION);
    assertTrue(stage.leakIsEnabled());
    assertPersistedConfigurationIs(true, TEST_SYNC_ID, TEST_VERSION);
  }

  public void testDisabledOnServer() throws Exception {
    session.config.metaGlobal = new MetaGlobal(CryptoRecord.fromJSONRecord(TEST_META_GLOBAL_NO_FORMS_JSON));
    engineSettings = new EngineSettings(TEST_SYNC_ID, TEST_VERSION);
    assertFalse(stage.leakIsEnabled());
    assertPersistedConfigurationIs(false, TEST_SYNC_ID, TEST_VERSION);
  }

  public void testNoEngineSettings() throws Exception {
    engineSettings = null;
    assertTrue(stage.leakIsEnabled());
    assertPersistedConfigurationIs(true, TEST_SYNC_ID, TEST_VERSION);
  }

  public void testStaleClientVersion() throws Exception {
    engineSettings = new EngineSettings(TEST_SYNC_ID, TEST_VERSION - 1);
    try {
      stage.leakIsEnabled();
      fail("Shouldn't get here.");
    } catch (MetaGlobalException e) {
      assertTrue(e instanceof MetaGlobalException.MetaGlobalStaleClientVersionException);
    }
  }

  public void testStaleClientSyncID() throws Exception {
    engineSettings = new EngineSettings(TEST_SYNC_ID + "NOT", TEST_VERSION);
    try {
      stage.leakIsEnabled();
      fail("Shouldn't get here.");
    } catch (MetaGlobalException e) {
      assertTrue(e instanceof MetaGlobalException.MetaGlobalStaleClientSyncIDException);
    }
  }
}
