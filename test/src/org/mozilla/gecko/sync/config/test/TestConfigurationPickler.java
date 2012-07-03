package org.mozilla.gecko.sync.config.test;

import java.io.FileNotFoundException;

import org.mozilla.android.sync.test.AndroidSyncTestCase;
import org.mozilla.android.sync.test.helpers.MockSharedPreferences;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.config.ConfigurationPickler;

import android.content.Context;
import android.content.SharedPreferences;

public class TestConfigurationPickler extends AndroidSyncTestCase {
  public static final String TEST_FILENAME = "test.json";

  public static void assertObject(final ExtendedJSONObject o) {
    assertEquals("test", o.get("test1"));
    assertEquals(new Boolean(true), o.get("test2"));
    assertEquals(new Long(111), o.get("test3"));
    assertEquals(3, o.keySet().size());
  }

  public void testPickle() {
    final SharedPreferences prefs = new MockSharedPreferences();
    prefs.edit().putString("test1", "test").putBoolean("test2", true).putLong("test3", 111L).commit();

    final ExtendedJSONObject o = ConfigurationPickler.asJSON(prefs);

    assertObject(o);
  }

  public void testPersist() throws Exception {
    final Context context = getApplicationContext();

    final SharedPreferences prefs = new MockSharedPreferences();
    prefs.edit().putString("test1", "test").putBoolean("test2", true).putLong("test3", 111L).commit();

    context.deleteFile(TEST_FILENAME);
    try {
      context.openFileInput(TEST_FILENAME);
      fail("Should get FileNotFoundException.");
    } catch (FileNotFoundException e) {
      // Do nothing; file should not exist.
    }

    ConfigurationPickler.pickle(context, prefs, TEST_FILENAME);

    final String s = Utils.readFile(context, TEST_FILENAME);
    assertEquals("{\"test1\":\"test\",\"test2\":true,\"test3\":111}", s);

    final ExtendedJSONObject o = ExtendedJSONObject.parseJSONObject(s);
    assertObject(o);
  }
}
