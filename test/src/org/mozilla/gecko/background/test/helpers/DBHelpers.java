/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.background.test.helpers;

import android.database.Cursor;
import junit.framework.Assert;

public class DBHelpers {

  /*
   * Works for strings and int-ish values.
   */
  public static void assertCursorContains(Object[][] expected, Cursor actual) {
    Assert.assertEquals(expected.length, actual.getCount());
    int i = 0, j = 0;
    Object[] row;

    do {
      row = expected[i];
      for (j = 0; j < row.length; ++j) {
        Object atIndex = row[j];
        if (atIndex == null) {
          continue;
        }
        if (atIndex instanceof String) {
          Assert.assertEquals(atIndex, actual.getString(j));
        } else {
          Assert.assertEquals(atIndex, actual.getInt(j));
        }
      }
      ++i;
    } while (actual.moveToPosition(i));
  }
}
