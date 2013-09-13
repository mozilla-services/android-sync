/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.background.common;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

public class DateUtils {
  public static String getDateString(long time) {
    final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    format.setTimeZone(TimeZone.getTimeZone("UTC"));
    return format.format(time);  
  }

  public static int getDay(final long time) {
    return (int) Math.floor(time / GlobalConstants.MILLISECONDS_PER_DAY);
  }

  public static String getDateStringForDay(long day) {
    return DateUtils.getDateString(GlobalConstants.MILLISECONDS_PER_DAY * day);
  }
}
