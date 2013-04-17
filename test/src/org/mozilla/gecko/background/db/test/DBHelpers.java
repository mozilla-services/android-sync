/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.background.db.test;

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
