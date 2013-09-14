/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.background.common.test;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

import org.mozilla.gecko.background.common.DateUtils;
import org.mozilla.gecko.background.common.GlobalConstants;

//import android.util.SparseArray;

import junit.framework.TestCase;

public class TestDateUtils extends TestCase {
  // Our old, correct implementation -- used to test the new one.
  public static String getDateStringFormatter(long time) {
    final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    format.setTimeZone(TimeZone.getTimeZone("UTC"));
    return format.format(time);
  }

  private void checkDateString(long time) {
    assertEquals(getDateStringFormatter(time),
                 DateUtils.getDateString(time));
  }

  public void testDateImplementations() {
    checkDateString(1L);
    checkDateString(System.currentTimeMillis());
    checkDateString(1379118065844L);
    checkDateString(1379110000000L);
    for (long i = 0L; i < (2 * GlobalConstants.MILLISECONDS_PER_DAY); i += 11000) {
      checkDateString(i);
    }
  }

  // Perf tests. Disabled until you need them.
  /*
  @SuppressWarnings("static-method")
  public void testDateTiming() {
    long start = 1379118000000L;
    long end   = 1379118045844L;

    long t0 = android.os.SystemClock.elapsedRealtime();
    for (long i = start; i < end; ++i) {
      DateUtils.getDateString(i);
    }
    long t1 = android.os.SystemClock.elapsedRealtime();
    System.err.println("CALENDAR: " + (t1 - t0));


    t0 = android.os.SystemClock.elapsedRealtime();
    for (long i = start; i < end; ++i) {
      getDateStringFormatter(i);
    }
    t1 = android.os.SystemClock.elapsedRealtime();
    System.err.println("FORMATTER: " + (t1 - t0));
  }

  @SuppressWarnings("static-method")
  public void testDayTiming() {
    long start = 33 * 365;
    long end   = start + 90;
    int reps   = 50;
    long t0 = android.os.SystemClock.elapsedRealtime();
    for (long i = start; i < end; ++i) {
      for (int j = 0; j < reps; ++j) {
        DateUtils.getDateStringForDay(i);
      }
    }
    long t1 = android.os.SystemClock.elapsedRealtime();
    System.err.println("Non-memo: " + (t1 - t0));

    t0 = android.os.SystemClock.elapsedRealtime();
    SparseArray<String> memo = new SparseArray<String>(90);
    for (long i = start; i < end; ++i) {
      for (int j = 0; j < reps; ++j) {
        DateUtils.getDateStringForDay(i, memo);
      }
    }
    t1 = android.os.SystemClock.elapsedRealtime();
    System.err.println("Memo: " + (t1 - t0));
  }
  */
}
