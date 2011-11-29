/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.fail;

import java.util.Arrays;

import org.mozilla.android.sync.SyncException;
import org.mozilla.android.sync.repositories.RepoStatusCode;
import org.mozilla.android.sync.repositories.RepositorySessionDelegate;
import org.mozilla.android.sync.repositories.domain.Record;

import android.util.Log;

public class DefaultRepositorySessionDelegate implements RepositorySessionDelegate {
  protected WaitHelper testWaiter() {
    return WaitHelper.getTestWaiter();
  }
  private void sharedFail() {
    try {
      fail("Should not be called.");
    } catch (AssertionError e) {
      testWaiter().performNotify(e);
    }
  }
  public void guidsSinceCallback(RepoStatusCode status, String[] guids) {
    sharedFail();
  }
  public void storeCallback(RepoStatusCode status, long rowId) {
    sharedFail();
  }
  public void fetchSinceCallback(RepoStatusCode status, Record[] records) {
    sharedFail();
  }
  public void fetchCallback(RepoStatusCode status, Record[] records) {
    sharedFail();
  }
  public void fetchAllCallback(RepoStatusCode status, Record[] records) {
    sharedFail();
  }
  public void wipeCallback(RepoStatusCode status) {
    sharedFail();
  }
  public void beginCallback(RepoStatusCode status) {
    //sharedFail();
    // TODO: Temporary to get tests passing until after refactoring
    // of delegates when this will be fixed.
  }
  public void finishCallback(RepoStatusCode status) {
    //sharedFail();
    // TODO: Temporary to get tests passing until after refactoring
    // of delegates when this will be fixed.
  }
  public void handleException(SyncException ex) {
    sharedFail();
  }
  protected void onDone(Record[] records, String[] expected) {
    Log.i("rnewman", "onDone. Test Waiter is " + testWaiter());
    try {
      assertEquals(expected.length, records.length);
      for (Record record : records) {
        assertFalse(-1 == Arrays.binarySearch(expected, record.guid));
      }
      Log.i("rnewman", "Notifying success.");
      testWaiter().performNotify();
    } catch (AssertionError e) {
      Log.i("rnewman", "Notifying assertion failure.");
      testWaiter().performNotify(e);
    } catch (Exception e) {
      Log.i("rnewman", "Fucking no.");
      testWaiter().performNotify();
    }
  }
}
