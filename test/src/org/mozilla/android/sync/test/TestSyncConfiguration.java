/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import java.util.HashSet;
import java.util.Set;

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
    Set<String> expected = new HashSet<String>();
    for (String name : new String[] { "test1", "test2" }) {
      expected.add(name);
    }
    assertEquals(expected, config.enabledEngineNames);

    config.enabledEngineNames = null;
    config.persistToPrefs();
    assertFalse(prefs.contains(SyncConfiguration.PREF_ENABLED_ENGINE_NAMES));
    config = new SyncConfiguration(TEST_PREFS_NAME, this);
    assertNull(config.enabledEngineNames);
  }

  public void testSyncID() {
    SyncConfiguration config = null;
    SharedPreferences prefs = getPrefs(TEST_PREFS_NAME, 0);

    config = new SyncConfiguration(TEST_PREFS_NAME, this);
    config.syncID = "test1";
    config.persistToPrefs();
    assertTrue(prefs.contains(SyncConfiguration.PREF_SYNC_ID));
    config = new SyncConfiguration(TEST_PREFS_NAME, this);
    assertEquals("test1", config.syncID);
  }
}
