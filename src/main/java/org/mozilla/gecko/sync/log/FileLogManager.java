package org.mozilla.gecko.sync.log;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.log.writers.AndroidFileLogWriter;
import org.mozilla.gecko.sync.log.writers.LevelFilteringLogWriter;
import org.mozilla.gecko.sync.log.writers.LogWriter;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

public class FileLogManager {
  public static final String LOG_TAG = "FileLogManager";

  public static final String PREFS_LOG_LEVEL = "logLevel";
  public static final int PREFS_LOG_LEVEL_MIN = 0;
  public static final int PREFS_LOG_LEVEL_MAX = 3;

  public static final String LOG_PREFIX = "FxSync-";
  public static final String LOG_SUFFIX = ".txt";

  /**
   * Keep this many logs on disk when cleaning.
   */
  public static int MAX_LOGS_ON_DISK = 6;

  /**
   * Directory containing log files.
   * @param context <code>Context</code>.
   * @return a <code>File</code>.
   */
  protected static File getDir(Context context) {
    return AndroidFileLogWriter.getDir(context);
  }

  /**
   * Test if filename is "log-like".
   * @param name the filename to test.
   * @return <code>true</code> if this filename is "log-like".
   */
  public static boolean isLog(String name) {
    return name.startsWith(LOG_PREFIX) && name.endsWith(LOG_SUFFIX);
  }

  /**
   * Generate a log filename.
   * @return a <code>String</code> filename based on the current time.
   */
  public static String logName() {
    return LOG_PREFIX + System.currentTimeMillis() + LOG_SUFFIX;
  }

  /**
   * Are there log files saved in the log directory?
   * @param context <code>Context</code>.
   * @return <code>true</code> if log files are saved.
   */
  public static synchronized boolean hasFileLogs(Context context) {
    File dir = getDir(context);
    String[] list = dir.list();
    if (list == null) {
      return false;
    }

    for (String name : list) {
      if (isLog(name)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Get a list of log filenames.
   * @param context <code>Context</code>.
   * @return list of log filenames, sorted so newer logs are earlier in the list.
   */
  public static synchronized List<String> getFileLogs(Context context) {
    ArrayList<String> fileLogs = new ArrayList<String>();
    String[] list = getDir(context).list();
    if (list == null) {
      return fileLogs;
    }

    for (String name : list) {
      if (isLog(name)) {
        fileLogs.add(name);
      }
    }
    // Newest logs at the top, please.
    Collections.sort(fileLogs);
    Collections.reverse(fileLogs);
    return fileLogs;
  }

  /**
   * Delete specified log files, without throwing an error.
   * @param context <code>Context</code>.
   * @param logNames The log filenames to delete.
   * @return the number of log files deleted.
   */
  public static synchronized int deleteFileLogs(Context context, Collection<String> logNames) {
    int deleted = 0;
    for (String name : logNames) {
      if (isLog(name)) {
        if (context.deleteFile(name)) {
          deleted += 1;
        }
      }
    }
    return deleted;
  }

  /**
   * Delete all log files, without throwing an error.
   * @param context <code>Context</code>.
   * @return the number of log files deleted.
   */
  public static synchronized int deleteAllFileLogs(Context context) {
    return deleteFileLogs(context, getFileLogs(context));
  }

  /**
   * Make sure we don't have too many log files hanging around.
   * @param context <code>Context</code>.
   */
  public static synchronized void cleanFileLogs(Context context) {
    List<String> existing = getFileLogs(context);
    int size = existing.size();
    if (size <= MAX_LOGS_ON_DISK) {
      Logger.info(LOG_TAG, "There are " + size + " <= " + MAX_LOGS_ON_DISK + " logs; not cleaning.");
      return;
    }
    int deleted = deleteFileLogs(context, existing.subList(MAX_LOGS_ON_DISK, size));
    Logger.info(LOG_TAG, "There are " + size + " > " + MAX_LOGS_ON_DISK + " logs; " +
        "deleted " + deleted + " logs.");
  }

  /**
   * Share specified log files using the Android share intent API.
   * @param context <code>Context</code>.
   * @param logNames the title of the created Android chooser menu.
   * @param logNames the log filenames to be shared.
   * @return the number of log files shared.
   */
  public static synchronized int shareFileLogs(Context context, String chooserTitle, List<String> logNames) {
    ArrayList<Uri> uris = new ArrayList<Uri>();
    for (String name : logNames) {
      if (isLog(name)) {
        // This ridiculous hack works around a hard-coded gmail issue: no files
        // can be attached that do not start with file:///mnt/sdcard!
        String uriString = "file:///mnt/sdcard/../.." + getDir(context) + File.separator + name;
        Uri uri = Uri.parse(uriString);
        Logger.debug(LOG_TAG, "Sharing log " + name + " (" + uri + ").");
        uris.add(uri);
      }
    }

    int size = uris.size();
    if (size == 0) {
      return 0;
    }

    if (size == 1) {
      Uri uri = uris.get(0);
      final Intent intent = new Intent(android.content.Intent.ACTION_SEND);
      intent.setType("text/plain");
      intent.putExtra(android.content.Intent.EXTRA_STREAM, uri);
      try {
        context.startActivity(Intent.createChooser(intent, chooserTitle));
        Logger.debug(LOG_TAG, "Sharing single log " + logNames.get(0) + " (" + uri + ").");
      } catch (Exception e) {
        Logger.error(LOG_TAG, "Caught exception starting activity to share one log.", e);
        return 0;
      }
      return 1;
    }

    final Intent intent = new Intent(android.content.Intent.ACTION_SEND_MULTIPLE);
    intent.setType("text/plain");
    intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
    try {
      context.startActivity(Intent.createChooser(intent, chooserTitle));
    } catch (Exception e) {
      Logger.error(LOG_TAG, "Caught exception starting activity to share " + size + " logs.", e);
      return 0;
    }
    return size;
  }

  /**
   * Get the persisted log level.
   * @param context <code>Context</code>.
   * @param prefsPath the per-user preferences path to use.
   * @return the integer log level, between <code>PREFS_LOG_LEVEL_MIN</code> and
   *         <code>PREFS_LOG_LEVEL_MAX</code>, inclusive.
   */
  public static synchronized int getLogLevel(Context context, String prefsPath) {
    SharedPreferences prefs = context.getSharedPreferences(prefsPath, Utils.SHARED_PREFERENCES_MODE);
    int logLevel = prefs.getInt(PREFS_LOG_LEVEL, 0);
    if (logLevel < PREFS_LOG_LEVEL_MIN || logLevel > PREFS_LOG_LEVEL_MAX) {
      Logger.warn(LOG_TAG, "Invalid log level " + logLevel + "found; resetting to 0.");
      logLevel = PREFS_LOG_LEVEL_MIN;
      persistLogLevel(context, prefsPath, logLevel);
    }
    return logLevel;
  }

  /**
   * Set the persisted log level.
   * @param context <code>Context</code>.
   * @param prefsPath the per-user preferences path to use.
   * @param logLevel the integer log level, which will be clipped to be between
   *        <code>PREFS_LOG_LEVEL_MIN</code> and <code>PREFS_LOG_LEVEL_MAX</code>,
   *        inclusive.
   */
  public static synchronized void persistLogLevel(Context context, String prefsPath, int logLevel) {
    SharedPreferences prefs = context.getSharedPreferences(prefsPath, Utils.SHARED_PREFERENCES_MODE);
    prefs.edit().putInt(PREFS_LOG_LEVEL, logLevel).commit();
  }

  /**
   * If the log level is high enough, add an appropriately filtered
   * <code>FileLogWriter</code> to the <code>Logger</code> log writer list.
   * @param context <code>Context</code>.
   * @param prefsPath the per-user preferences path to use.
   */
  public static synchronized void startFileLogging(Context context, String prefsPath) {
    int logLevel = getLogLevel(context, prefsPath);
    if (logLevel < 1) {
      Logger.info(LOG_TAG, "Log level is 0; not logging to file.");
      return;
    }

    LogWriter fileLogWriter = new AndroidFileLogWriter(context, logName());
    LogWriter logWriter = null;
    if (logLevel == 1) {
      logWriter = new LevelFilteringLogWriter(Log.ERROR, fileLogWriter);
    } else if (logLevel == 2) {
      logWriter = new LevelFilteringLogWriter(Log.INFO, fileLogWriter);
    } else if (logLevel == 3) {
      logWriter = new LevelFilteringLogWriter(Log.VERBOSE, fileLogWriter);
    }

    Logger.info(LOG_TAG, "Log level is " + logLevel + "; logging to file.");
    Logger.startLoggingTo(logWriter);
  }
}
