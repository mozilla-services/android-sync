package org.mozilla.gecko.sync.config;

import java.io.FileOutputStream;
import java.io.PrintStream;

import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.setup.Constants;
import org.mozilla.gecko.sync.setup.SyncAccounts;
import org.mozilla.gecko.sync.setup.SyncAccounts.SyncAccountParameters;

import android.accounts.Account;
import android.content.Context;
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
public class AccountPickler {
  public static final String LOG_TAG = "AccountPickler";

  /**
   * Remove Sync account persisted to disk.
   *
   * @param context Android context.
   * @param filename name of persisted pickle file; must not contain path separators.
   * @return <code>true</code> if given pickle existed and was successfully deleted.
   */
  public static boolean deletePickle(final Context context, final String filename) {
    return context.deleteFile(filename);
  }

  /**
   * Persist Sync account to disk as a JSON object.
   * <p>
   * JSON object has keys:
   * <ul>
   * <li><code>Constants.JSON_KEY_ACCOUNT</code>: the Sync account's hashed
   * username;</li>
   *
   * <li><code>Constants.JSON_KEY_PASSWORD</code>: the Sync account's password;</li>
   *
   * <li><code>Constants.JSON_KEY_SERVER</code>: the Sync account's server;</li>
   *
   * <li><code>Constants.JSON_KEY_SYNCKEY</code>: the Sync account's sync key.</li>
   *
   * <li><code>Constants.JSON_KEY_TIMESTAMP</code>: when this file was written.</li>
   * </ul>
   *
   *
   * @param context Android context.
   * @param filename name of file to persist to; must not contain path separators.
   * @param username the Sync account's un-encoded username, like "test@mozilla.com".
   * @param password the Sync account's password.
   * @param serverURL the Sync account's server.
   * @param syncKey the Sync account's sync key.
   */
  public static void pickle(final Context context, final String filename,
      final String username,
      final String password,
      final String serverURL,
      final String syncKey) {
    final ExtendedJSONObject o = new ExtendedJSONObject();
    o.put(Constants.JSON_KEY_ACCOUNT, username);
    o.put(Constants.JSON_KEY_PASSWORD, password);
    o.put(Constants.JSON_KEY_SERVER, serverURL);
    o.put(Constants.JSON_KEY_SYNCKEY, syncKey);
    o.put(Constants.JSON_KEY_TIMESTAMP, new Long(System.currentTimeMillis()));

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
   * Create Android account from saved JSON object.
   *
   * @param context
   *          Android context.
   * @param filename
   *          name of file to read from; must not contain path separators.
   * @param syncAutomatically
   *          <code>true</code> to start syncing automatically;
   *          <code>false</code> is intended for testing.
   * @return created Android account, or null on error.
   */
  public static Account unpickle(final Context context, final String filename, final boolean syncAutomatically) {
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
    final long timestamp   = json.getLong(Constants.JSON_KEY_TIMESTAMP);

    SyncAccountParameters params = null;
    try {
      // Null checking of inputs is done in constructor.
      params = new SyncAccountParameters(context, null, username, syncKey, password, serverURL);
    } catch (IllegalArgumentException e) {
      Logger.warn(LOG_TAG, "Un-pickled data included null username, password, or serverURL; aborting.", e);
      return null;
    }

    final Account account = SyncAccounts.createSyncAccount(params, syncAutomatically);
    if (account == null) {
      Logger.warn(LOG_TAG, "Failed to add Android Account; aborting.");
      return null;
    }

    Logger.info(LOG_TAG, "Un-pickled Android account named " + username + ", pickled at " + timestamp + ".");

    return account;
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
    protected final boolean syncAutomatically;

    public UnpickleSyncAccountTask() {
      syncAutomatically = true;
    }

    public UnpickleSyncAccountTask(final boolean syncAutomatically) {
      this.syncAutomatically = syncAutomatically;
    }

    @Override
    protected Account doInBackground(UnpickleParameters... params) {
      final Context context = params[0].context;
      final String filename = params[0].filename;

      if (SyncAccounts.syncAccountsExist(context)) {
        Logger.info(LOG_TAG, "A Firefox Sync Android account already exists; not unpickling.");
        return null;
      }

      return unpickle(context, filename, syncAutomatically);
    }
  }
}
