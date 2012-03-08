/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.net.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.util.ArrayList;

import org.json.simple.parser.ParseException;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.android.sync.test.helpers.HTTPServerTestHelper;
import org.mozilla.android.sync.test.helpers.MockClientsDataDelegate;
import org.mozilla.android.sync.test.helpers.MockClientsDatabaseAccessor;
import org.mozilla.android.sync.test.helpers.MockGlobalSession;
import org.mozilla.android.sync.test.helpers.MockGlobalSessionCallback;
import org.mozilla.android.sync.test.helpers.MockServer;
import org.mozilla.gecko.sync.CollectionKeys;
import org.mozilla.gecko.sync.CryptoRecord;
import org.mozilla.gecko.sync.NonObjectJSONException;
import org.mozilla.gecko.sync.SyncConfigurationException;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.delegates.ClientsDataDelegate;
import org.mozilla.gecko.sync.delegates.GlobalSessionCallback;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.sync.net.SyncResourceDelegate;
import org.mozilla.gecko.sync.net.SyncStorageResponse;
import org.mozilla.gecko.sync.repositories.domain.ClientRecord;
import org.mozilla.gecko.sync.stage.SyncClientsEngineStage;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

public class TestClientsEngineStage extends SyncClientsEngineStage {

  private static final int TEST_PORT      = 15325;
  private static final String TEST_SERVER = "http://localhost:" + TEST_PORT;

  private static final String USERNAME  = "john";
  private static final String PASSWORD  = "password";
  private static final String SYNC_KEY  = "abcdeabcdeabcdeabcdeabcdea";

  private HTTPServerTestHelper data = new HTTPServerTestHelper(TEST_PORT);
  private int numRecordsFromGetRequest = 0;

  private ArrayList<ClientRecord> expectedClients = new ArrayList<ClientRecord>();
  private ArrayList<ClientRecord> downloadedClients = new ArrayList<ClientRecord>();

  // For test purposes.
  private ClientRecord lastComputedLocalClientRecord;

  @Override
  protected ClientRecord newLocalClientRecord(ClientsDataDelegate delegate) {
    lastComputedLocalClientRecord = super.newLocalClientRecord(delegate);
    return lastComputedLocalClientRecord;
  }

  @Before
  public void setup() {
    MockGlobalSessionCallback callback = new MockGlobalSessionCallback();

    try {
      final KeyBundle bundle = new KeyBundle(USERNAME, SYNC_KEY);
      session = new MockClientsGlobalSession(TEST_SERVER, USERNAME, PASSWORD, bundle, callback);
      session.config.setClusterURL(new URI(TEST_SERVER));
      session.setCollectionKeys(CollectionKeys.generateCollectionKeys());
    } catch (Exception e) {
      e.printStackTrace();
      fail("Unexpected failure in session.");
    }
  }

  @Before
  @Override
  public void init() {
    db = new MockClientsDatabaseAccessor();
  }

  @Override
  protected ClientDownloadDelegate makeClientDownloadDelegate() {
    return new TestClientDownloadDelegate();
  }

  @Override
  protected void downloadClientRecords() {
    BaseResource.rewriteLocalhost = false;
    data.startHTTPServer(new DownloadMockServer());
    super.downloadClientRecords();
  }

  @Override
  protected void uploadClientRecord(CryptoRecord record) {
    BaseResource.rewriteLocalhost = false;
    data.startHTTPServer(new UploadMockServer());
    super.uploadClientRecord(record);
  }

  @Override
  public void checkAndUpload() {
    clientUploadDelegate = new MockClientUploadDelegate();
    super.checkAndUpload();
  }

  public static class MockClientsGlobalSession extends MockGlobalSession {
    private ClientsDataDelegate clientsDataDelegate = new MockClientsDataDelegate();
  
    public MockClientsGlobalSession(String clusterURL,
                                    String username,
                                    String password,
                                    KeyBundle syncKeyBundle,
                                    GlobalSessionCallback callback)
        throws SyncConfigurationException,
               IllegalArgumentException,
               IOException,
               ParseException,
               NonObjectJSONException {
      super(clusterURL, username, password, syncKeyBundle, callback);
    }
  
    @Override
    public ClientsDataDelegate getClientsDelegate() {
      return clientsDataDelegate;
    }
  }

  public class TestClientDownloadDelegate extends ClientDownloadDelegate {
    @Override
    public void handleRequestSuccess(SyncStorageResponse response) {
      data.stopHTTPServer();
      assertTrue(response.wasSuccessful());

      assertEquals(expectedClients.size(), numRecordsFromGetRequest);
      for (int i = 0; i < downloadedClients.size(); i++) {
        assertTrue(expectedClients.get(i).guid.equals(downloadedClients.get(i).guid));
      }

      super.handleRequestSuccess(response);
      assertEquals(Stage.idle, session.currentState);
    }

    @Override
    public void handleRequestFailure(SyncStorageResponse response) {
      super.handleRequestFailure(response);
      assertTrue(((MockClientsDatabaseAccessor)db).closed);
      fail("Should not error.");
      data.stopHTTPServer();
    }

    @Override
    public void handleRequestError(Exception ex) {
      super.handleRequestError(ex);
      assertTrue(((MockClientsDatabaseAccessor)db).closed);
      fail("Should not fail.");
      data.stopHTTPServer();
    }

    @Override
    public void handleWBO(CryptoRecord record) {
      ClientRecord r;
      try {
        r = (ClientRecord) factory.createRecord(record.decrypt());
        downloadedClients.add(r);
        numRecordsFromGetRequest++;
      } catch (Exception e) {
        fail("handleWBO failed.");
      }
    }
  }

  public class MockClientUploadDelegate extends ClientUploadDelegate {
    @Override
    public void handleRequestSuccess(SyncStorageResponse response) {
      assertTrue(response.wasSuccessful());
      // Make sure we consume the entity, so we can reuse the connection.
      SyncResourceDelegate.consumeEntity(response);
      data.stopHTTPServer();
    }

    @Override
    public void handleRequestFailure(SyncStorageResponse response) {
      SyncResourceDelegate.consumeEntity(response);
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

  public class UploadMockServer extends MockServer {
    @Override
    public void handle(Request request, Response response) {
      try {
        CryptoRecord cryptoRecord = CryptoRecord.fromJSONRecord(request.getContent());
        cryptoRecord.keyBundle = session.keyForCollection(COLLECTION_NAME);
        ClientRecord r = (ClientRecord) factory.createRecord(cryptoRecord.decrypt());
  
        // Note: collection is not saved in CryptoRecord.toJSONObject() upon upload.
        // So its value is null and is set here so ClientRecord.equals() may be used.
        r.collection = lastComputedLocalClientRecord.collection;
        assertTrue(lastComputedLocalClientRecord.equals(r));
      } catch (Exception e) {
        fail("Error handling uploaded client record in UploadMockServer.");
      }
      super.handle(request, response);
    }
  }

  public class DownloadMockServer extends MockServer {
    @Override
    public void handle(Request request, Response response) {
      try {
        PrintStream bodyStream = this.handleBasicHeaders(request, response, 200, "application/newlines");
        for (int i = 0; i < 5; i++) {
          ClientRecord record = new ClientRecord();
          expectedClients.add(record);
          CryptoRecord cryptoRecord = cryptoFromClient(record);
          bodyStream.print(cryptoRecord.toJSONString() + "\n");
        }
        bodyStream.close();
      } catch (IOException e) {
        fail("Error handling downloaded client records in DownloadMockServer.");
      }
    }
  }

  private CryptoRecord cryptoFromClient(ClientRecord record) {
    CryptoRecord cryptoRecord = record.getEnvelope();
    cryptoRecord.keyBundle = clientDownloadDelegate.keyBundle();
    try {
      cryptoRecord.encrypt();
    } catch (Exception e) {
      fail("Cannot encrypt client record.");
    }
    return cryptoRecord;
  }

  @Test
  public void testWipeAndStoreShouldNotWipe() {
    assertFalse(shouldWipe);
    wipeAndStore(new ClientRecord());
    assertFalse(shouldWipe);
    assertFalse(((MockClientsDatabaseAccessor)db).wiped);
    assertTrue(((MockClientsDatabaseAccessor)db).storedRecord);

    ((MockClientsDatabaseAccessor)db).resetVars();
  }

  @Test
  public void testWipeAndStoreShouldWipe() {
    assertFalse(shouldWipe);
    shouldWipe = true;
    wipeAndStore(new ClientRecord());
    assertFalse(shouldWipe);
    assertTrue(((MockClientsDatabaseAccessor)db).wiped);
    assertTrue(((MockClientsDatabaseAccessor)db).storedRecord);

    ((MockClientsDatabaseAccessor)db).resetVars();
  }

  @Test
  public void testCheckAndUploadClientRecord() {
    this.checkAndUpload();
  }

  @Test
  public void testDownloadClientRecord() {
    this.downloadClientRecords();
  }
}
