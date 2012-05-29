package org.mozilla.gecko.sync.log.writers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import android.content.Context;
import android.util.Log;

/**
 * Log to an internal Android file.
 */
public class AndroidFileLogWriter extends LogWriter {
  public static final String LOG_TAG = "FileLogWriter";

  /**
   * Buffer size in bytes. We want a large buffer to minimize disk writes,
   * which can be very costly on certain phones.
   */
  public static final int BUFFER_SIZE = 64000;

  public static File getDir(Context context) {
    return context.getFilesDir();
  }

  protected final Context context;
  protected BufferedWriter logFile;
  protected LogWriter inner;
  protected String filename;

  public AndroidFileLogWriter(Context context, String filename) {
    this.context = context;
    this.filename = filename;
    logFile = null;
    inner = null;
  }

  protected boolean open() {
    if (filename == null) {
      return false;
    }
    if (logFile != null) {
      return true;
    }

    try {
      FileOutputStream fs = context.openFileOutput(filename, Context.MODE_WORLD_READABLE);
      OutputStreamWriter ow = new OutputStreamWriter(fs);
      logFile = new BufferedWriter(ow, BUFFER_SIZE);
      inner = new PrintLogWriter(new PrintWriter(logFile));
      return true;
    } catch (Exception e) {
      Log.e(LOG_TAG, "Got exception opening log file.", e);
      filename = null;
      logFile = null;
      inner = null;
    }
    return false;
  }

  public void close() {
    if (logFile == null) {
      return;
    }

    try {
      logFile.close();
    } catch (Exception e) {
      Log.e(LOG_TAG, "Got exception closing log file.", e);
    }
    filename = null;
    logFile = null;
    inner = null;
  }

  @Override
  public void error(String tag, String message, Throwable error) {
    if (open()) {
      inner.error(tag, message, error);
    }
  }

  @Override
  public void warn(String tag, String message, Throwable error) {
    if (open()) {
      inner.warn(tag, message, error);
    }
  }

  @Override
  public void info(String tag, String message, Throwable error) {
    if (open()) {
      inner.info(tag, message, error);
    }
  }

  @Override
  public void debug(String tag, String message, Throwable error) {
    if (open()) {
      inner.debug(tag, message, error);
    }
  }

  @Override
  public void trace(String tag, String message, Throwable error) {
    if (open()) {
      inner.trace(tag, message, error);
    }
  }

  @Override
  public boolean shouldLogVerbose(String tag) {
    return true;
  }
}
