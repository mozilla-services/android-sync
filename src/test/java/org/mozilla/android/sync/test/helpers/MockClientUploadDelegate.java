package org.mozilla.android.sync.test.helpers;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.mozilla.gecko.sync.GlobalSession;
import org.mozilla.gecko.sync.delegates.ClientUploadDelegate;
import org.mozilla.gecko.sync.net.SyncResourceDelegate;
import org.mozilla.gecko.sync.net.SyncStorageResponse;

public class MockClientUploadDelegate extends ClientUploadDelegate {

  private HTTPServerTestHelper data;

  public MockClientUploadDelegate(GlobalSession session, HTTPServerTestHelper data) {
    super(session);
    this.data = data;
  }

  @Override
  public void handleRequestSuccess(SyncStorageResponse response) {
    assertTrue(response.wasSuccessful());
    // Make sure we consume the entity, so we can reuse the connection.
    SyncResourceDelegate.consumeEntity(response.httpResponse().getEntity());
    data.stopHTTPServer();
  }

  @Override
  public void handleRequestFailure(SyncStorageResponse response) {
    fail("Should not fail.");
    data.stopHTTPServer();
  }

  @Override
  public void handleRequestError(Exception ex) {
    ex.printStackTrace();
    fail("Should not error.");
    data.stopHTTPServer();
  }
}