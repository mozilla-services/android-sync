/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.background.common;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import android.util.SparseArray;

public class DateUtils {
  private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

  public static String getDateString(long time) {
    Calendar d = Calendar.getInstance();
    d.setTimeZone(UTC);
    d.setTimeInMillis(time);
    return String.format(Locale.US,
                         "%04d-%02d-%02d",
                         d.get(Calendar.YEAR),
                         d.get(Calendar.MONTH) + 1,      // 0-indexed.
                         d.get(Calendar.DAY_OF_MONTH));
  }

  public static String getDateStringForDay(long day) {
    return getDateString(GlobalConstants.MILLISECONDS_PER_DAY * day);
  }

  public static String getDateStringForDay(long day, SparseArray<String> memo) {
    // Truncating is fine -- it's a day, not a timestamp -- and necessary
    // to use SparseArray.
    int dd = (int) day;
    String s = memo.get(dd);
    if (s == null) {
      s = getDateString(GlobalConstants.MILLISECONDS_PER_DAY * day);
      memo.put(dd, s);
    }
    return s;
  }

  public static int getDay(final long time) {
    return (int) Math.floor(time / GlobalConstants.MILLISECONDS_PER_DAY);
  }

}
