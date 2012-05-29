package org.mozilla.android.sync.log.test;

import java.io.File;
import java.io.FileInputStream;

import org.mozilla.android.sync.test.AndroidSyncTestCase;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.log.writers.AndroidFileLogWriter;
import org.mozilla.gecko.sync.log.writers.AndroidLevelCachingLogWriter;
import org.mozilla.gecko.sync.log.writers.AndroidLogWriter;
import org.mozilla.gecko.sync.log.writers.LevelFilteringLogWriter;
import org.mozilla.gecko.sync.log.writers.LogWriter;
import org.mozilla.gecko.sync.log.writers.PrintLogWriter;

import android.util.Log;

public class TestAndroidLogWriters extends AndroidSyncTestCase {
  public static final String TEST_LOG_TAG = "TestAndroidLogWriters";

  public static final String TEST_MESSAGE_1  = "LOG TEST MESSAGE one";
  public static final String TEST_MESSAGE_2  = "LOG TEST MESSAGE two";
  public static final String TEST_MESSAGE_3  = "LOG TEST MESSAGE three";

  public void setUp() {
    Logger.stopLoggingToAll();
  }

  /**
   * Verify these *all* appear in the Android log by using
   * <code>adb logcat | grep TestAndroidLogWriters</code> after executing
   * <code>adb shell setprop log.tag.TestAndroidLogWriters ERROR</code>.
   * <p>
   * This writer does not use the Android log levels!
   */
  public void testAndroidLogWriter() {
    LogWriter lw = new AndroidLogWriter();

    Logger.error(TEST_LOG_TAG, TEST_MESSAGE_1, new RuntimeException());
    Logger.startLoggingTo(lw);
    Logger.error(TEST_LOG_TAG, TEST_MESSAGE_2);
    Logger.warn(TEST_LOG_TAG, TEST_MESSAGE_2);
    Logger.info(TEST_LOG_TAG, TEST_MESSAGE_2);
    Logger.debug(TEST_LOG_TAG, TEST_MESSAGE_2);
    Logger.trace(TEST_LOG_TAG, TEST_MESSAGE_2);
    Logger.stopLoggingTo(lw);
    Logger.error(TEST_LOG_TAG, TEST_MESSAGE_3, new RuntimeException());
  }

  /**
   * Verify only *some* of these appear in the Android log by using
   * <code>adb logcat | grep TestAndroidLogWriters</code> after executing
   * <code>adb shell setprop log.tag.TestAndroidLogWriters INFO</code>.
   * <p>
   * This writer should use the Android log levels!
   */
  public void testAndroidLevelCachingLogWriter() throws Exception {
    LogWriter lw = new AndroidLevelCachingLogWriter(new AndroidLogWriter());

    Logger.error(TEST_LOG_TAG, TEST_MESSAGE_1, new RuntimeException());
    Logger.startLoggingTo(lw);
    Logger.error(TEST_LOG_TAG, TEST_MESSAGE_2);
    Logger.warn(TEST_LOG_TAG, TEST_MESSAGE_2);
    Logger.info(TEST_LOG_TAG, TEST_MESSAGE_2);
    Logger.debug(TEST_LOG_TAG, TEST_MESSAGE_2);
    Logger.trace(TEST_LOG_TAG, TEST_MESSAGE_2);
    Logger.stopLoggingTo(lw);
    Logger.error(TEST_LOG_TAG, TEST_MESSAGE_3, new RuntimeException());
  }

  /**
   * Verify AndroidFileLogWriter does in fact write.
   */
  public void testAndroidFileLogWriterWrites() {
    final String TEST_FILENAME = "test.txt";

    File file = getApplicationContext().getFileStreamPath(TEST_FILENAME);
    assertFalse(file.exists());

    try {
      LogWriter lw = new AndroidFileLogWriter(getApplicationContext(), TEST_FILENAME);

      Logger.error(TEST_LOG_TAG, TEST_MESSAGE_1, new RuntimeException());
      Logger.startLoggingTo(lw);
      Logger.error(TEST_LOG_TAG, TEST_MESSAGE_2);
      Logger.stopLoggingTo(lw);
      Logger.error(TEST_LOG_TAG, TEST_MESSAGE_3, new RuntimeException());

      assertTrue(file.exists());
      FileInputStream fis = new FileInputStream(file);
      byte[] buffer = new byte[2048];
      int read = fis.read(buffer);
      assertTrue(read > 0);
      String s = new String(buffer);
      assertTrue(s.startsWith(TEST_LOG_TAG + PrintLogWriter.ERROR + TEST_MESSAGE_2));
    } catch (Exception e) {
      fail("Exception: " + e);
    } finally {
      getApplicationContext().deleteFile(TEST_FILENAME);
    }
  }

  /**
   * Verify AndroidFileLogWriter does not write if it doesn't open a file if it doesn't need to.
   */
  public void testAndroidFileLogWriterDoesntAlwaysWrite() {
    final String TEST_FILENAME = "test.txt";

    File file = getApplicationContext().getFileStreamPath(TEST_FILENAME);
    assertFalse(file.exists());

    try {
      LogWriter lw = new LevelFilteringLogWriter(Log.WARN, new AndroidFileLogWriter(getApplicationContext(), TEST_FILENAME));

      Logger.error(TEST_LOG_TAG, TEST_MESSAGE_1, new RuntimeException());
      Logger.startLoggingTo(lw);
      assertFalse(file.exists());
      Logger.debug(TEST_LOG_TAG, TEST_MESSAGE_2); // Should not be written!
      assertFalse(file.exists());
      Logger.error(TEST_LOG_TAG, TEST_MESSAGE_2); // Should be written.
      Logger.stopLoggingTo(lw);
      Logger.error(TEST_LOG_TAG, TEST_MESSAGE_3, new RuntimeException());

      assertTrue(file.exists());
    } finally {
      getApplicationContext().deleteFile(TEST_FILENAME);
    }
  }
}
