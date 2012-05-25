/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.mozilla.gecko.sync.log.writers.LogWriter;

import android.util.Log;

/**
 * Logging helper class. Serializes all log operations (by synchronizing),
 * and caches log level settings.
 *
 * Ultimately this will also be a hook point for our own logging system.
 *
 * @author rnewman
 *
 */
public class Logger {
  public static final String LOG_TAG = "Logger";

  // For extra debugging.
  public static boolean LOG_PERSONAL_INFORMATION = false;

  protected final static Set<LogWriter> logWriters = new LinkedHashSet<LogWriter>();
  static {
    logWriters.addAll(Logger.defaultLogWriters());
  }

  protected static Set<LogWriter> defaultLogWriters() {
    Set<LogWriter> defaultLogWriters = new LinkedHashSet<LogWriter>();
    return defaultLogWriters;
  }

  public static synchronized void startLoggingTo(LogWriter logWriter) {
    logWriters.add(logWriter);
  }

  public static synchronized void stopLoggingTo(LogWriter logWriter) {
    try {
      logWriter.close();
    } catch (Exception e) {
      Log.e(LOG_TAG, "Got exception closing and removing LogWriter " + logWriter + ".", e);
    }
    logWriters.remove(logWriter);
  }

  public static synchronized void stopLoggingToAll() {
    for (LogWriter logWriter : logWriters) {
      try {
        logWriter.close();
      } catch (Exception e) {
        Log.e(LOG_TAG, "Got exception closing and removing LogWriter " + logWriter + ".", e);
      }
    }
    logWriters.clear();
  }

  public static synchronized void resetLogging() {
    stopLoggingToAll();
    logWriters.addAll(Logger.defaultLogWriters());
  }

  // Synchronized version for other classes to use.
  public static synchronized boolean logVerbose(String logTag) {
    for (LogWriter logWriter : logWriters) {
      if (logWriter.shouldLogVerbose(logTag)) {
        return true;
      }
    }
    return false;
  }

  public static void error(String logTag, String message) {
    Logger.error(logTag, message, null);
  }

  public static void warn(String logTag, String message) {
    Logger.warn(logTag, message, null);
  }

  public static void info(String logTag, String message) {
    Logger.info(logTag, message, null);
  }

  public static void debug(String logTag, String message) {
    Logger.debug(logTag, message, null);
  }

  public static void trace(String logTag, String message) {
    Logger.trace(logTag, message, null);
  }

  public static void pii(String logTag, String message) {
    if (LOG_PERSONAL_INFORMATION) {
      Logger.debug(logTag, "$$PII$$: " + message);
    }
  }

  public static synchronized void error(String logTag, String message, Throwable error) {
    Iterator<LogWriter> it = logWriters.iterator();
    while (it.hasNext()) {
      LogWriter writer = it.next();
      try {
        writer.error(logTag, message, error);
      } catch (Exception e) {
        Log.e(LOG_TAG, "Got exception logging; removing LogWriter " + writer + ".", e);
        it.remove();
      }
    }
  }

  public static synchronized void warn(String logTag, String message, Throwable error) {
    Iterator<LogWriter> it = logWriters.iterator();
    while (it.hasNext()) {
      LogWriter writer = it.next();
      try {
        writer.warn(logTag, message, error);
      } catch (Exception e) {
        Log.e(LOG_TAG, "Got exception logging; removing LogWriter " + writer + ".", e);
        it.remove();
      }
    }
  }

  public static synchronized void info(String logTag, String message, Throwable error) {
    Iterator<LogWriter> it = logWriters.iterator();
    while (it.hasNext()) {
      LogWriter writer = it.next();
      try {
        writer.info(logTag, message, error);
      } catch (Exception e) {
        Log.e(LOG_TAG, "Got exception logging; removing LogWriter " + writer + ".", e);
        it.remove();
      }
    }
  }

  public static synchronized void debug(String logTag, String message, Throwable error) {
    Iterator<LogWriter> it = logWriters.iterator();
    while (it.hasNext()) {
      LogWriter writer = it.next();
      try {
        writer.debug(logTag, message, error);
      } catch (Exception e) {
        Log.e(LOG_TAG, "Got exception logging; removing LogWriter " + writer + ".", e);
        it.remove();
      }
    }
  }

  public static synchronized void trace(String logTag, String message, Throwable error) {
    Iterator<LogWriter> it = logWriters.iterator();
    while (it.hasNext()) {
      LogWriter writer = it.next();
      try {
        writer.trace(logTag, message, error);
      } catch (Exception e) {
        Log.e(LOG_TAG, "Got exception logging; removing LogWriter " + writer + ".", e);
        it.remove();
      }
    }
  }
}
