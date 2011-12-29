/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.net.test;

import static org.junit.Assert.fail;

import org.mozilla.gecko.sync.net.SyncStorageRequestDelegate;
import org.mozilla.gecko.sync.net.SyncStorageResponse;

public class BaseTestStorageRequestDelegate implements
    SyncStorageRequestDelegate {
  public String _credentials;

  @Override
  public String credentials() {
    return _credentials;
  }

  @Override
  public String ifUnmodifiedSince() {
    return null;
  }

  @Override
  public void handleRequestSuccess(SyncStorageResponse res) {
    fail("Should not be called.");
  }

  @Override
  public void handleRequestFailure(SyncStorageResponse response) {
    System.out.println("Response: "
        + response.httpResponse().getStatusLine().getStatusCode());
    fail("Should not be called.");
  }

  @Override
  public void handleRequestError(Exception e) {
    fail("Should not be called.");
  }
}