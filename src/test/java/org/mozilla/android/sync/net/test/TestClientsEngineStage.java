package org.mozilla.android.sync.net.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.mozilla.android.sync.test.helpers.HTTPServerTestHelper;
import org.mozilla.android.sync.test.helpers.MockClientUploadDelegate;
import org.mozilla.android.sync.test.helpers.MockClientsDatabaseContentProvider;
import org.mozilla.android.sync.test.helpers.MockGlobalSession;
import org.mozilla.android.sync.test.helpers.MockGlobalSessionCallback;
import org.mozilla.gecko.sync.CollectionKeys;
import org.mozilla.gecko.sync.CryptoRecord;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.sync.net.SyncStorageResponse;
import org.mozilla.gecko.sync.repositories.domain.ClientRecord;
import org.mozilla.gecko.sync.setup.Constants;
import org.mozilla.gecko.sync.stage.SyncClientsEngineStage;

public class TestClientsEngineStage extends SyncClientsEngineStage {

  private static final int TEST_PORT             = 15325;
  private static final String TEST_SERVER        = "http://localhost:" + TEST_PORT;

  private static final String USERNAME  = "john";
  private static final String PASSWORD  = "password";
  private static final String SYNC_KEY  = "abcdeabcdeabcdeabcdeabcdea";

  private HTTPServerTestHelper data = new HTTPServerTestHelper();

  @Before
  public void setup() {
    MockGlobalSessionCallback callback = new MockGlobalSessionCallback();

    try {
      session = new MockGlobalSession(TEST_SERVER, USERNAME, PASSWORD,
          new KeyBundle(USERNAME, SYNC_KEY), callback);
      session.config.setClusterURL(TEST_SERVER);
      session.setCollectionKeys(CollectionKeys.generateCollectionKeys());
    } catch (Exception e) {
      e.printStackTrace();
      fail("Unexpected failure in session.");
    }
  }

  @Override
  protected void downloadClientRecords() {
    BaseResource.rewriteLocalhost = false;
    data.startHTTPServer();
    super.downloadClientRecords();
  }

  @Override
  protected void uploadClientRecord(CryptoRecord record) {
    BaseResource.rewriteLocalhost = false;
    data.startHTTPServer();
    super.uploadClientRecord(record);
  }

  @Override
  public void handleRequestSuccess(SyncStorageResponse response) {
    data.stopHTTPServer();
    assertTrue(response.wasSuccessful());
    super.handleRequestSuccess(response);
    assertEquals(Stage.idle, session.currentState);

  }

  @Before
  @Override
  public void init() {
    localClient = new ClientRecord(Constants.PROFILE_ID);
    clientUploadDelegate = new MockClientUploadDelegate(session, data);
    db = new MockClientsDatabaseContentProvider();
  }


  @Test
  public void testCryptoFromClient() {
    CryptoRecord cryptoRecord = cryptoFromClient(localClient);
    try {
      ClientRecord clientRecord = (ClientRecord) factory.createRecord(cryptoRecord.decrypt());
      assertEquals(localClient, clientRecord);
    } catch (Exception e) {
      fail("Should not throw decryption exception.");
    }
  }

  @Test
  public void testUploadClientRecord() {
    CryptoRecord cryptoRecord = cryptoFromClient(localClient);
    this.uploadClientRecord(cryptoRecord);
  }

  @Test
  public void testDownloadClientRecord() {
    this.downloadClientRecords();
  }
}
