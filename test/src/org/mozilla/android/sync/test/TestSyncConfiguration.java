package org.mozilla.android.sync.test;

import java.util.HashSet;

import org.mozilla.gecko.sync.PrefsSource;
import org.mozilla.gecko.sync.SyncConfiguration;

import android.content.SharedPreferences;

public class TestSyncConfiguration extends AndroidSyncTestCase implements PrefsSource {
  public static final String TEST_PREFS_NAME = "test";

  /*
   * PrefsSource methods.
   */
  @Override
  public SharedPreferences getPrefs(String name, int mode) {
    return this.getApplicationContext().getSharedPreferences(name, mode);
  }

  public void testEnabledEngineNames() {
    SyncConfiguration config = null;
    SharedPreferences prefs = getPrefs(TEST_PREFS_NAME, 0);

    config = new SyncConfiguration(TEST_PREFS_NAME, this);
    config.enabledEngineNames = new HashSet<String>();
    config.enabledEngineNames.add("test1");
    config.enabledEngineNames.add("test2");
    config.persistToPrefs();
    assertTrue(prefs.contains(SyncConfiguration.PREF_ENABLED_ENGINE_NAMES));
    config = new SyncConfiguration(TEST_PREFS_NAME, this);
    config.loadFromPrefs(prefs);
    assertEquals(2, config.enabledEngineNames.size());
    assertTrue(config.enabledEngineNames.contains("test1"));
    assertTrue(config.enabledEngineNames.contains("test2"));

    config.enabledEngineNames = null;
    config.persistToPrefs();
    assertFalse(prefs.contains(SyncConfiguration.PREF_ENABLED_ENGINE_NAMES));
    config.loadFromPrefs(prefs);
    assertNull(config.enabledEngineNames);
  }
}
