package org.mozilla.android.sync.test.helpers;

import static org.junit.Assert.fail;

import org.mozilla.android.sync.repositories.RepoStatusCode;
import org.mozilla.android.sync.repositories.RepositorySessionDelegate;
import org.mozilla.android.sync.repositories.domain.Record;

public class DefaultRepositorySessionDelegate implements RepositorySessionDelegate {
  protected WaitHelper testWaiter() {
    return WaitHelper.getTestWaiter();
  }
  public void guidsSinceCallback(RepoStatusCode status, String[] guids) {
    fail("Should not be called.");
  }
  public void storeCallback(RepoStatusCode status, long rowId) {
    fail("Should not be called.");
  }
  public void fetchSinceCallback(RepoStatusCode status, Record[] records) {
    fail("Should not be called.");
  }
  public void fetchCallback(RepoStatusCode status, Record[] records) {
    fail("Should not be called.");
  }
  public void fetchAllCallback(RepoStatusCode status, Record[] records) {
    fail("Should not be called.");
  }
  public void wipeCallback(RepoStatusCode status) {
    fail("Should not be called.");
  }
  public void beginCallback(RepoStatusCode status) {
    fail("Should not be called.");
  }
  public void finishCallback(RepoStatusCode status) {
    fail("Should not be called.");
  }
}