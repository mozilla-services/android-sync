/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import static junit.framework.Assert.assertEquals;

import org.mozilla.gecko.sync.repositories.InvalidBookmarkTypeException;

public class ExpectInvalidTypeStoreDelegate extends DefaultStoreDelegate {
  
  @Override
  public void onRecordStoreFailed(Exception ex) {
    assertEquals(InvalidBookmarkTypeException.class, ex.getClass());
    testWaiter().performNotify();
  }
  
}
