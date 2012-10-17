/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.sync.log.writers.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.log.writers.LevelFilteringLogWriter;
import org.mozilla.gecko.sync.log.writers.LogWriter;
import org.mozilla.gecko.sync.log.writers.PrintLogWriter;
import org.mozilla.gecko.sync.log.writers.SimpleTagLogWriter;
import org.mozilla.gecko.sync.log.writers.StringLogWriter;
import org.mozilla.gecko.sync.log.writers.ThreadLocalTagLogWriter;

import android.util.Log;

public class TestLogWriters {

  public static final String TEST_LOG_TAG_1 = "TestLogTag1";
  public static final String TEST_LOG_TAG_2 = "TestLogTag2";

  public static final String TEST_MESSAGE_1  = "LOG TEST MESSAGE one";
  public static final String TEST_MESSAGE_2  = "LOG TEST MESSAGE two";
  public static final String TEST_MESSAGE_3  = "LOG TEST MESSAGE three";

  @Before
  public void setUp() {
    Logger.stopLoggingToAll();
  }

  @After
  public void tearDown() {
    Logger.stopLoggingToAll();
  }

  @Test
  public void testStringLogWriter() {
    StringLogWriter lw = new StringLogWriter();

    Logger.error(TEST_LOG_TAG_1, TEST_MESSAGE_1, new RuntimeException());
    Logger.startLoggingTo(lw);
    Logger.error(TEST_LOG_TAG_1, TEST_MESSAGE_2);
    Logger.warn(TEST_LOG_TAG_1, TEST_MESSAGE_2);
    Logger.info(TEST_LOG_TAG_1, TEST_MESSAGE_2);
    Logger.debug(TEST_LOG_TAG_1, TEST_MESSAGE_2);
    Logger.trace(TEST_LOG_TAG_1, TEST_MESSAGE_2);
    Logger.stopLoggingTo(lw);
    Logger.error(TEST_LOG_TAG_2, TEST_MESSAGE_3, new RuntimeException());

    String s = lw.toString();
    assertFalse(s.contains("RuntimeException"));
    assertFalse(s.contains(".java"));
    assertTrue(s.contains(TEST_LOG_TAG_1));
    assertFalse(s.contains(TEST_LOG_TAG_2));
    assertFalse(s.contains(TEST_MESSAGE_1));
    assertTrue(s.contains(TEST_MESSAGE_2));
    assertFalse(s.contains(TEST_MESSAGE_3));
  }

  @Test
  public void testSingleTagLogWriter() {
    final String SINGLE_TAG = "XXX";
    StringLogWriter lw = new StringLogWriter();

    Logger.startLoggingTo(new SimpleTagLogWriter(SINGLE_TAG, lw));
    Logger.error(TEST_LOG_TAG_1, TEST_MESSAGE_1);
    Logger.warn(TEST_LOG_TAG_2, TEST_MESSAGE_2);

    String s = lw.toString();
    for (String line : s.split("\n")) {
      assertTrue(line.startsWith(SINGLE_TAG));
    }
    assertTrue(s.startsWith(SINGLE_TAG + " :: E :: " + TEST_LOG_TAG_1));
  }

  @Test
  public void testLevelFilteringLogWriter() {
    StringLogWriter lw = new StringLogWriter();

    assertFalse(new LevelFilteringLogWriter(Log.WARN, lw).shouldLogVerbose(TEST_LOG_TAG_1));
    assertTrue(new LevelFilteringLogWriter(Log.VERBOSE, lw).shouldLogVerbose(TEST_LOG_TAG_1));

    Logger.startLoggingTo(new LevelFilteringLogWriter(Log.WARN, lw));
    Logger.error(TEST_LOG_TAG_1, TEST_MESSAGE_2);
    Logger.warn(TEST_LOG_TAG_1, TEST_MESSAGE_2);
    Logger.info(TEST_LOG_TAG_1, TEST_MESSAGE_2);
    Logger.debug(TEST_LOG_TAG_1, TEST_MESSAGE_2);
    Logger.trace(TEST_LOG_TAG_1, TEST_MESSAGE_2);

    String s = lw.toString();
    assertTrue(s.contains(PrintLogWriter.ERROR));
    assertTrue(s.contains(PrintLogWriter.WARN));
    assertFalse(s.contains(PrintLogWriter.INFO));
    assertFalse(s.contains(PrintLogWriter.DEBUG));
    assertFalse(s.contains(PrintLogWriter.VERBOSE));
  }

  @Test
  public void testThreadLocalLogWriter() throws InterruptedException {
    final InheritableThreadLocal<String> logTag = new InheritableThreadLocal<String>() {
      @Override
      protected String initialValue() {
        return "PARENT";
      }
    };

    final StringLogWriter stringLogWriter = new StringLogWriter();
    final LogWriter logWriter = new ThreadLocalTagLogWriter(logTag, stringLogWriter);

    try {
      Logger.startLoggingTo(logWriter);

      Logger.info("parent tag before", "parent message before");

      int threads = 3;
      final CountDownLatch latch = new CountDownLatch(threads);

      for (int thread = 0; thread < threads; thread++) {
        final int threadNumber = thread;

        new Thread(new Runnable() {
          @Override
          public void run() {
            try {
              logTag.set("CHILD" + threadNumber);
              Logger.info("child tag " + threadNumber, "child message " + threadNumber);
            } finally {
              latch.countDown();
            }
          }
        }).start();
      }

      latch.await();

      Logger.info("parent tag after", "parent message after");

      String s = stringLogWriter.toString();
      String[] lines = s.split("\n");
      assertEquals(5, lines.length);
      assertEquals("PARENT :: I :: parent tag before :: parent message before", lines[0]);
      assertEquals("PARENT :: I :: parent tag after :: parent message after", lines[4]);

      for (int thread = 0; thread < threads; thread++) {
        assertTrue(s.contains("CHILD" + thread + " :: I :: child tag " + thread + " :: child message " + thread));
      }
    } finally {
      Logger.stopLoggingTo(logWriter);
    }
  }
}