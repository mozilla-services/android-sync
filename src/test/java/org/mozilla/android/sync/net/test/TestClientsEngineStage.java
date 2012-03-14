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
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.android.sync.test.helpers.HTTPServerTestHelper;
import org.mozilla.android.sync.test.helpers.MockClientsDataDelegate;
import org.mozilla.android.sync.test.helpers.MockClientsDatabaseAccessor;
import org.mozilla.android.sync.test.helpers.MockGlobalSession;
import org.mozilla.android.sync.test.helpers.MockGlobalSessionCallback;
import org.mozilla.android.sync.test.helpers.MockServer;
import org.mozilla.android.sync.test.helpers.MockSyncClientsEngineStage;
import org.mozilla.android.sync.test.helpers.WaitHelper;
import org.mozilla.gecko.sync.CollectionKeys;
import org.mozilla.gecko.sync.CryptoRecord;
import org.mozilla.gecko.sync.NonObjectJSONException;
import org.mozilla.gecko.sync.SyncConfigurationException;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.delegates.ClientsDataDelegate;
import org.mozilla.gecko.sync.delegates.GlobalSessionCallback;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.sync.net.SyncStorageResponse;
import org.mozilla.gecko.sync.repositories.NullCursorException;
import org.mozilla.gecko.sync.repositories.domain.ClientRecord;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

import ch.boye.httpclientandroidlib.HttpStatus;

public class TestClientsEngineStage extends MockSyncClientsEngineStage {

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
  private ClientRecord uploadedRecord;
  private MockServer currentUploadMockServer;
  private MockServer currentDownloadMockServer;
  private MockGlobalSessionCallback callback;

  protected WaitHelper testWaiter() {
    return WaitHelper.getTestWaiter();
  }

  @Override
  protected ClientRecord newLocalClientRecord(ClientsDataDelegate delegate) {
    lastComputedLocalClientRecord = super.newLocalClientRecord(delegate);
    return lastComputedLocalClientRecord;
  }

  @Before
  public void setup() {
    callback = new MockGlobalSessionCallback();

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
    return clientDownloadDelegate;
  }

  @Override
  protected void downloadClientRecords() {
    BaseResource.rewriteLocalhost = false;
    data.startHTTPServer(currentDownloadMockServer);
    super.downloadClientRecords();
  }

  @Override
  protected void uploadClientRecord(CryptoRecord record) {
    BaseResource.rewriteLocalhost = false;
    data.startHTTPServer(currentUploadMockServer);
    super.uploadClientRecord(record);
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

  public class TestSuccessClientDownloadDelegate extends TestClientDownloadDelegate {
    public TestSuccessClientDownloadDelegate(HTTPServerTestHelper data) {
      super(data);
    }

    @Override
    public void handleRequestFailure(SyncStorageResponse response) {
      super.handleRequestFailure(response);
      assertTrue(((MockClientsDatabaseAccessor)db).closed);
      fail("Should not error.");
    }

    @Override
    public void handleRequestError(Exception ex) {
      super.handleRequestError(ex);
      assertTrue(((MockClientsDatabaseAccessor)db).closed);
      fail("Should not fail.");
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

  public class MockSuccessClientUploadDelegate extends MockClientUploadDelegate {
    public MockSuccessClientUploadDelegate(HTTPServerTestHelper data) {
      super(data);
    }

    @Override
    public void handleRequestFailure(SyncStorageResponse response) {
      super.handleRequestFailure(response);
      fail("Should not error.");
    }

    @Override
    public void handleRequestError(Exception ex) {
      super.handleRequestError(ex);
      fail("Should not fail.");
    }
  }

  public class MockFailureClientUploadDelegate extends MockClientUploadDelegate {
    public MockFailureClientUploadDelegate(HTTPServerTestHelper data) {
      super(data);
    }

    @Override
    public void handleRequestSuccess(SyncStorageResponse response) {
      super.handleRequestSuccess(response);
      fail("Should not succeed.");
    }

    @Override
    public void handleRequestError(Exception ex) {
      super.handleRequestError(ex);
      fail("Should not fail.");
    }
  }

  public class UploadMockServer extends MockServer {
    @Override
    public void handle(Request request, Response response) {
      try {
        CryptoRecord cryptoRecord = CryptoRecord.fromJSONRecord(request.getContent());
        cryptoRecord.keyBundle = session.keyForCollection(COLLECTION_NAME);
        uploadedRecord = (ClientRecord) factory.createRecord(cryptoRecord.decrypt());
  
        // Note: collection is not saved in CryptoRecord.toJSONObject() upon upload.
        // So its value is null and is set here so ClientRecord.equals() may be used.
        uploadedRecord.collection = lastComputedLocalClientRecord.collection;
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

  private long setRecentClientRecordTimestamp() {
    long timestamp = System.currentTimeMillis() - (CLIENTS_TTL_REFRESH - 1000);
    session.config.persistServerClientRecordTimestamp(timestamp);
      return timestamp;
  }

  private void performFailingUpload() {
    // performNotify() occurs in MockGlobalSessionCallback.
    testWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        clientUploadDelegate = new MockFailureClientUploadDelegate(data);
        checkAndUpload();
      }
    });
  }

  @Test
  public void testShouldUploadNoCommandsToProcess() throws NullCursorException {
    // shouldUpload() returns true.
    assertEquals(0, session.config.getPersistedServerClientRecordTimestamp());
    assertFalse(commandsProcessedShouldUpload);
    assertTrue(shouldUpload());

    // Set the timestamp to be a little earlier than refresh time,
    // so shouldUpload() returns false.
    setRecentClientRecordTimestamp();
    assertFalse(0 == session.config.getPersistedServerClientRecordTimestamp());
    assertFalse(commandsProcessedShouldUpload);
    assertFalse(shouldUpload());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testShouldUploadProcessCommands() throws NullCursorException {
    // shouldUpload() returns false since array is size 0 and
    // it has not been long enough yet to require an upload.
    processCommands(new JSONArray());
    setRecentClientRecordTimestamp();
    assertFalse(commandsProcessedShouldUpload);
    assertFalse(shouldUpload());

    // shouldUpload() returns true since array is size 1 even though
    // it has not been long enough yet to require an upload.
    JSONArray commands = new JSONArray();
    commands.add(new JSONObject());
    processCommands(commands);
    setRecentClientRecordTimestamp();
    assertEquals(1, commands.size());
    assertTrue(commandsProcessedShouldUpload);
    assertTrue(shouldUpload());
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
  public void testDownloadClientRecord() {
    // Make sure no upload occurs after a download so we can
    // test download in isolation.
    long initialTimestamp = setRecentClientRecordTimestamp();
    assertFalse(commandsProcessedShouldUpload);

    currentDownloadMockServer = new DownloadMockServer();
    // performNotify() occurs in MockGlobalSessionCallback.
    testWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        clientDownloadDelegate = new TestSuccessClientDownloadDelegate(data);
        downloadClientRecords();
      }
    });

    assertEquals(expectedClients.size(), numRecordsFromGetRequest);
    for (int i = 0; i < downloadedClients.size(); i++) {
      assertTrue(expectedClients.get(i).guid.equals(downloadedClients.get(i).guid));
    }
    assertEquals(initialTimestamp, session.config.getPersistedServerClientRecordTimestamp());
    assertTrue(((MockClientsDatabaseAccessor)db).closed);
  }

  @Test
  public void testCheckAndUploadClientRecord() {
    uploadAttemptsCount.set(MAX_UPLOAD_FAILURE_COUNT);
    assertFalse(commandsProcessedShouldUpload);
    assertEquals(0, session.config.getPersistedServerClientRecordTimestamp());
    currentUploadMockServer = new UploadMockServer();
    // performNotify() occurs in MockGlobalSessionCallback.
    testWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        clientUploadDelegate = new MockSuccessClientUploadDelegate(data);
        checkAndUpload();
      }
    });

    // Test ClientUploadDelegate.handleRequestSuccess().
    assertTrue(lastComputedLocalClientRecord.equals(uploadedRecord));
    assertEquals(0, uploadAttemptsCount.get());
    assertTrue(callback.calledSuccess);
    assertFalse(0 == session.config.getPersistedServerClientRecordTimestamp());
  }

  /**
   * The following 8 tests are for ClientUploadDelegate.handleRequestFailure().
   * for the varying values of uploadAttemptsCount, commandsProcessedShouldUpload,
   * and the type of server error.
   *
   * The first 4 are for 412 Precondition Failures.
   * The second 4 represent the functionality given any other type of variable.
   */
  @Test
  public void testHandle412UploadFailureLowCount() {
    assertFalse(commandsProcessedShouldUpload);
    currentUploadMockServer = new MockServer(HttpStatus.SC_PRECONDITION_FAILED, null);
    assertEquals(0, uploadAttemptsCount.get());
    performFailingUpload();
    assertEquals(0, uploadAttemptsCount.get());
    assertTrue(callback.calledError);
  }

  @Test
  public void testHandle412UploadFailureHighCount() {
    assertFalse(commandsProcessedShouldUpload);
    currentUploadMockServer = new MockServer(HttpStatus.SC_PRECONDITION_FAILED, null);
    uploadAttemptsCount.set(MAX_UPLOAD_FAILURE_COUNT);
    performFailingUpload();
    assertEquals(MAX_UPLOAD_FAILURE_COUNT, uploadAttemptsCount.get());
    assertTrue(callback.calledError);
  }

  @Test
  public void testHandle412UploadFailureLowCountWithCommand() {
    commandsProcessedShouldUpload = true;
    currentUploadMockServer = new MockServer(HttpStatus.SC_PRECONDITION_FAILED, null);
    assertEquals(0, uploadAttemptsCount.get());
    performFailingUpload();
    assertEquals(0, uploadAttemptsCount.get());
    assertTrue(callback.calledError);
  }

  @Test
  public void testHandle412UploadFailureHighCountWithCommand() {
    commandsProcessedShouldUpload = true;
    currentUploadMockServer = new MockServer(HttpStatus.SC_PRECONDITION_FAILED, null);
    uploadAttemptsCount.set(MAX_UPLOAD_FAILURE_COUNT);
    performFailingUpload();
    assertEquals(MAX_UPLOAD_FAILURE_COUNT, uploadAttemptsCount.get());
    assertTrue(callback.calledError);
  }

  @Test
  public void testHandleMiscUploadFailureLowCount() {
    currentUploadMockServer = new MockServer(HttpStatus.SC_BAD_REQUEST, null);
    assertFalse(commandsProcessedShouldUpload);
    assertEquals(0, uploadAttemptsCount.get());
    performFailingUpload();
    assertEquals(0, uploadAttemptsCount.get());
    assertTrue(callback.calledError);
  }

  @Test
  public void testHandleMiscUploadFailureHighCount() {
    currentUploadMockServer = new MockServer(HttpStatus.SC_BAD_REQUEST, null);
    assertFalse(commandsProcessedShouldUpload);
    uploadAttemptsCount.set(MAX_UPLOAD_FAILURE_COUNT);
    performFailingUpload();
    assertEquals(MAX_UPLOAD_FAILURE_COUNT, uploadAttemptsCount.get());
    assertTrue(callback.calledError);
  }

  @Test
  public void testHandleMiscUploadFailureHighCountWithCommands() {
    currentUploadMockServer = new MockServer(HttpStatus.SC_BAD_REQUEST, null);
    commandsProcessedShouldUpload = true;
    uploadAttemptsCount.set(MAX_UPLOAD_FAILURE_COUNT);
    performFailingUpload();
    assertEquals(MAX_UPLOAD_FAILURE_COUNT, uploadAttemptsCount.get());
    assertTrue(callback.calledError);
  }

  @Test
  public void testHandleMiscUploadFailureMaxAttempts() {
    currentUploadMockServer = new MockServer(HttpStatus.SC_BAD_REQUEST, null);
    commandsProcessedShouldUpload = true;
    assertEquals(0, uploadAttemptsCount.get());
    performFailingUpload();
    assertEquals(MAX_UPLOAD_FAILURE_COUNT, uploadAttemptsCount.get());
    assertTrue(callback.calledError);
  }
}
