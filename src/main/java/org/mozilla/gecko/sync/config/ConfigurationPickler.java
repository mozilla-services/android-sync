package org.mozilla.gecko.sync.config;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.Logger;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

/**
 * Android deletes Account objects when the Authenticator that owns the Account
 * disappears. This happens when an App is installed to the SD card and the SD
 * card is un-mounted.
 * <p>
 * We work around this by pickling the current Sync account data every sync and
 * un-pickling from within Fennec.
 * <p>
 * See Bugs 768102, 769745, and 769749.
 */
public class ConfigurationPickler {
  public static final String LOG_TAG = "ConfigurationPickler";

  public static class JSONEditor implements Editor {
    protected ExtendedJSONObject json = new ExtendedJSONObject();

    public ExtendedJSONObject getJSONObject() {
      return json;
    }

    public void apply() {
      // Android <=r8 SharedPreferences.Editor does not contain apply() for overriding.
      this.commit();
    }

    public Editor putStringSet(String key, Set<String> value) {
      // Android <=r10 SharedPreferences.Editor does not contain putStringSet() for overriding.
      throw new RuntimeException("putStringSet not available.");
    }

    @Override
    public Editor clear() {
      json = new ExtendedJSONObject();
      return this;
    }

    @Override
    public boolean commit() {
      return false;
    }

    @Override
    public Editor putBoolean(String arg0, boolean arg1) {
      json.put(arg0, new Boolean(arg1));
      return this;
    }

    @Override
    public Editor putFloat(String arg0, float arg1) {
      json.put(arg0, new Float(arg1));
      return this;
    }

    @Override
    public Editor putInt(String arg0, int arg1) {
      json.put(arg0, new Integer(arg1));
      return this;
    }

    @Override
    public Editor putLong(String arg0, long arg1) {
      json.put(arg0, new Long(arg1));
      return this;
    }

    @Override
    public Editor putString(String arg0, String arg1) {
      json.put(arg0, arg1);
      return this;
    }

    @Override
    public Editor remove(String arg0) {
      json.put(arg0, null);
      return this;
    }
  };

  /**
   * Return preferences as a JSON object.
   *
   * @param prefs <code>SharedPreferences</code> instance to encode.
   * @return <code>ExtendedJSONObject</code> instance.
   */
  public static ExtendedJSONObject asJSON(final SharedPreferences prefs) {
    Map<String, String> map = new HashMap<String, String>();
    for (String pref : prefs.getAll().keySet()) {
      map.put(pref, pref);
    }
    final JSONEditor jsonEditor = new JSONEditor();
    ConfigurationMigrator.copyPreferences(prefs, map, jsonEditor);

    return jsonEditor.getJSONObject();
  }

  /**
   * Remove preferences persisted to disk, if there are any.
   *
   * @param context Android context.
   * @param filename name of persisted pickle file; must not contain path separators.
   * @return <code>true</code> if given pickle existed and was successfully deleted.
   */
  public static boolean deletePickle(final Context context, final String filename) {
    return context.deleteFile(filename);
  }

  /**
   * Persist preferences to disk as a JSON object.
   *
   * @param context Android context.
   * @param prefs <code>SharedPreferences</code> instance to persist.
   * @param filename name of file to persist to; must not contain path separators.
   */
  public static void pickle(final Context context, final SharedPreferences prefs, final String filename) {
    final ExtendedJSONObject o = asJSON(prefs);
    try {
      final FileOutputStream fos = context.openFileOutput(filename, Context.MODE_PRIVATE);
      final PrintStream ps = (new PrintStream(fos));
      ps.print(o.toJSONString());
      ps.close();
      fos.close();
      Logger.debug(LOG_TAG, "Persisted " + o.keySet().size() + " preferences to " + filename + ".");
    } catch (Exception e) {
      Logger.warn(LOG_TAG, "Caught exception persisting preferences to " + filename + "; ignoring.", e);
    }
  }
}
