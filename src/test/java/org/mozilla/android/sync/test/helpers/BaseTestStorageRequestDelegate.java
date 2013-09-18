/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import static org.junit.Assert.fail;

import java.io.IOException;

import org.mozilla.gecko.sync.net.AuthHeaderProvider;
import org.mozilla.gecko.sync.net.BaseResource;
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
  public AuthHeaderProvider getAuthHeaderProvider() {
    return null;
  }

  @Override
  public String ifUnmodifiedSince() {
    return null;
  }

  @Override
  public void handleRequestSuccess(SyncStorageResponse response) {
    BaseResource.consumeEntity(response);
    fail("Should not be called.");
  }

  @Override
  public void handleRequestFailure(SyncStorageResponse response) {
    System.out.println("Response: " + response.httpResponse().getStatusLine().getStatusCode());
    BaseResource.consumeEntity(response);
    fail("Should not be called.");
  }

  @Override
  public void handleRequestError(Exception e) {
    if (e instanceof IOException) {
      System.out.println("WARNING: TEST FAILURE IGNORED!");
      // Assume that this is because Jenkins doesn't have network access.
      return;
    }
    fail("Should not error.");
  }
}
