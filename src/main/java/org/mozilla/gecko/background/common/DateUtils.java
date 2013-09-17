/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.background.common;

import java.util.Calendar;
import java.util.Formatter;
import java.util.Locale;
import java.util.TimeZone;

public class DateUtils {
  private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

  public static final class DateFormatter {
    private Calendar calendar;
    private Formatter formatter;

    public DateFormatter() {
      this.calendar = Calendar.getInstance(UTC);
      this.formatter = new Formatter((Locale) null);
    }

    public String getDateString(long time) {
      calendar.setTimeInMillis(time);
      return formatter.format("%04d-%02d-%02d",
                              calendar.get(Calendar.YEAR),
                              calendar.get(Calendar.MONTH) + 1,      // 0-indexed.
                              calendar.get(Calendar.DAY_OF_MONTH))
                      .toString();
    }

    public String getDateStringForDay(long day) {
      return getDateString(GlobalConstants.MILLISECONDS_PER_DAY * day);
    }
  }

  public static int getDay(final long time) {
    return (int) Math.floor(time / GlobalConstants.MILLISECONDS_PER_DAY);
  }

  public static String getDateString(long time) {
    return new DateFormatter().getDateString(time);
  }
}
