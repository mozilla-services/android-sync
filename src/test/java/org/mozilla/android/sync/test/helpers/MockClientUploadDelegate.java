package org.mozilla.android.sync.test.helpers;

import static org.junit.Assert.assertTrue;

import org.mozilla.gecko.sync.GlobalSession;
import org.mozilla.gecko.sync.delegates.ClientUploadDelegate;
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
    // Make sure we consume the rest of the body, so we can reuse the connection.
    try {
      System.out.println("Success body: " + response.body());
    } catch (Exception e) {
      e.printStackTrace();
    }
    data.stopHTTPServer();
  }

  @Override
  public void handleRequestFailure(SyncStorageResponse response) {
    data.stopHTTPServer();
  }

  @Override
  public void handleRequestError(Exception ex) {
    data.stopHTTPServer();
  }
}