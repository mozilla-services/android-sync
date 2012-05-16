package org.mozilla.android.sync.test.helpers;

import static junit.framework.Assert.assertEquals;

import org.mozilla.gecko.sync.repositories.InvalidBookmarkTypeException;

public class ExpectInvalidTypeStoreDelegate extends DefaultStoreDelegate {
  
  @Override
  public void notifyRecordStoreFailed(Exception ex, String guid) {
    assertEquals(InvalidBookmarkTypeException.class, ex.getClass());
    performNotify();
  }
  
}
