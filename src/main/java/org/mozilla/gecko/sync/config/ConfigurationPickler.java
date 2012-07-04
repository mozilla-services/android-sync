package org.mozilla.gecko.sync.config;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.setup.Constants;
import org.mozilla.gecko.sync.setup.SyncAccounts;
import org.mozilla.gecko.sync.setup.SyncAccounts.SyncAccountParameters;

import android.accounts.Account;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;

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

  public static class JSONPrefs implements SharedPreferences {
    protected final ExtendedJSONObject json;

    public JSONPrefs(final ExtendedJSONObject json) {
      this.json = json;
    }

    public ExtendedJSONObject getJSONObject() {
      return json;
    }

    @Override
    public boolean contains(String key) {
      return json.containsKey(key);
    }

    @Override
    public Editor edit() {
      return new JSONEditor(json);
    }

    @Override
    public Map<String, ?> getAll() {
      final Map<String, Object> map = new HashMap<String, Object>();
      for (Entry<String, Object> pair : json.entryIterable()) {
        map.put(pair.getKey(), pair.getValue());
      }
      return map;
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
      Boolean value = (Boolean) json.get(key);
      if (value != null) {
        return value.booleanValue();
      }
      return defValue;
    }

    @Override
    public float getFloat(String key, float defValue) {
      Float value = (Float) json.get(key);
      if (value != null) {
        return value.floatValue();
      }
      return defValue;
    }

    @Override
    public int getInt(String key, int defValue) {
      Long value = json.getLong(key);
      if (value != null) {
        return value.intValue();
      }
      return defValue;
    }

    @Override
    public long getLong(String key, long defValue) {
      Long value = json.getLong(key);
      if (value != null) {
        return value.longValue();
      }
      return defValue;
    }

    @Override
    public String getString(String key, String defValue) {
      String value = json.getString(key);
      if (value != null) {
        return value;
      }
      return defValue;
    }

    // Not marking as Override, because Android <= 10 doesn't have
    // getStringSet. Neither can we implement it.
    public Set<String> getStringSet(String key, Set<String> defValue) {
      throw new RuntimeException("getStringSet not available.");
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
      // Ignore.
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
      // Ignore.
    }
  }

  public static class JSONEditor implements Editor {
    protected final ExtendedJSONObject json;

    public JSONEditor(final ExtendedJSONObject json) {
      this.json = json;
    }

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
      json.object.clear();
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
    final JSONEditor jsonEditor = new JSONEditor(new ExtendedJSONObject());
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
   * @param prefs <code>SharedPreferences</code> instance to persist.  Should include keys:
   * <ul>
   * <li><code>Constants.JSON_KEY_ACCOUNT</code>: the Sync account's hashed
   * username;</li>
   *
   * <li><code>Constants.JSON_KEY_PASSWORD</code>: the Sync account's password;</li>
   *
   * <li><code>Constants.JSON_KEY_SERVER</code>: the Sync account's server;</li>
   *
   * <li><code>Constants.JSON_KEY_SYNCKEY</code>: the Sync account's sync key.</li>
   * </ul>
   * @param filename name of file to persist to; must not contain path separators.
   */
  protected static void pickle(final Context context, final SharedPreferences prefs, final String filename) {
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

  /**
   * Persist Sync account, current timestamp, and preferences to disk as a JSON object.
   *
   * @param context Android context.
   * @param prefs <code>SharedPreferences</code> instance to persist.
   * @param filename name of file to persist to; must not contain path separators.
   * @param username the Sync account's un-encoded username, like "test@mozilla.com".
   * @param password the Sync account's password.
   * @param serverURL the Sync account's server.
   * @param syncKey the Sync account's sync key.
   */
  public static void pickle(final Context context, final SharedPreferences prefs, final String filename,
      final String username,
      final String password,
      final String serverURL,
      final String syncKey) {
    prefs.edit()
      .putString(Constants.JSON_KEY_ACCOUNT,  username)
      .putString(Constants.JSON_KEY_PASSWORD, password)
      .putString(Constants.JSON_KEY_SERVER,   serverURL)
      .putString(Constants.JSON_KEY_SYNCKEY,  syncKey)
      .putLong(Constants.JSON_KEY_TIMESTAMP, System.currentTimeMillis())
      .commit();
    pickle(context, prefs, filename);
  }

  public static class UnpickleParameters {
    public final Context context;
    public final String filename;

    /**
     * @param context Android context.
     * @param filename name of file to unpickle; must not contain path separators.
     */
    public UnpickleParameters(final Context context, final String filename) {
      this.context = context;
      this.filename = filename;
    }
  }

  /**
   * This class provides background-thread abstracted access to un-pickling a
   * pickled Firefox Sync account.
   * <p>
   * If an Android Firefox Sync account already exists, returns null immediately.
   * <p>
   * Subclass this task and override `onPostExecute` to act on the result. The
   * <code>Result</code> (of type <code>Account</code>) is null if an error
   * occurred and the account could not be added.
   *
   */
  public static class UnpickleSyncAccountTask extends AsyncTask<UnpickleParameters, Void, Account> {
    @Override
    protected Account doInBackground(UnpickleParameters... params) {
      final Context context = params[0].context;
      final String filename = params[0].filename;

      if (SyncAccounts.syncAccountsExist(context)) {
        Logger.info(LOG_TAG, "A Firefox Sync Android account already exists; not unpickling.");
        return null;
      }

      return unpickle(context, filename);
    }
  }

  public static Account unpickle(final Context context, final String filename) {
    final String jsonString = Utils.readFile(context, filename);
    if (jsonString == null) {
      Logger.info(LOG_TAG, "Pickle file '" + filename + "' not found; aborting.");
      return null;
    }

    ExtendedJSONObject json = null;
    try {
      json = ExtendedJSONObject.parseJSONObject(jsonString);
    } catch (Exception e) {
      Logger.warn(LOG_TAG, "Got exception reading pickle file '" + filename + "'; aborting.", e);
      return null;
    }

    final String username  = json.getString(Constants.JSON_KEY_ACCOUNT); // Un-encoded, like "test@mozilla.com".
    final String password  = json.getString(Constants.JSON_KEY_PASSWORD);
    final String serverURL = json.getString(Constants.JSON_KEY_SERVER);
    final String syncKey   = json.getString(Constants.JSON_KEY_SYNCKEY);

    SyncAccountParameters params = null;
    try {
      // Null checking of inputs is done in constructor.
      params = new SyncAccountParameters(context, null, username, syncKey, password, serverURL);
    } catch (IllegalArgumentException e) {
      Logger.warn(LOG_TAG, "Un-pickled data included null username, password, or serverURL; aborting.", e);
      return null;
    }

    final Account account = SyncAccounts.createSyncAccount(params);
    if (account == null) {
      Logger.warn(LOG_TAG, "Failed to add Android Account; aborting.");
      return null;
    }

    SharedPreferences prefs = null;
    try {
      prefs = Utils.getSharedPreferences(context, username, serverURL);
    } catch (Exception e) {
      Logger.warn(LOG_TAG, "Got exception getting shared preferences; ignoring.", e);
      return account;
    }
    final Editor editor = prefs.edit();

    Map<String, String> map = new HashMap<String, String>();
    for (String pref : json.keySet()) {
      map.put(pref, pref);
    }
    int count = ConfigurationMigrator.copyPreferences(new JSONPrefs(json), map, editor);
    editor.commit();

    Logger.info(LOG_TAG, "Un-pickled Android account named " + username + " and restored " + count + " preferences.");

    return account;
  }
}
