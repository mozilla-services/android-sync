/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.stage;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.mozilla.gecko.sync.CommandProcessor;
import org.mozilla.gecko.sync.CommandProcessor.Command;
import org.mozilla.gecko.sync.CryptoRecord;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.GlobalSession;
import org.mozilla.gecko.sync.HTTPFailureException;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.NoCollectionKeysSetException;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.crypto.CryptoException;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.delegates.ClientsDataDelegate;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.sync.net.SyncStorageCollectionRequest;
import org.mozilla.gecko.sync.net.SyncStorageRecordRequest;
import org.mozilla.gecko.sync.net.SyncStorageResponse;
import org.mozilla.gecko.sync.net.WBOCollectionRequestDelegate;
import org.mozilla.gecko.sync.net.WBORequestDelegate;
import org.mozilla.gecko.sync.repositories.NullCursorException;
import org.mozilla.gecko.sync.repositories.android.ClientsDatabaseAccessor;
import org.mozilla.gecko.sync.repositories.android.RepoUtils;
import org.mozilla.gecko.sync.repositories.domain.ClientRecord;
import org.mozilla.gecko.sync.repositories.domain.ClientRecordFactory;

import ch.boye.httpclientandroidlib.HttpStatus;

public class SyncClientsEngineStage implements GlobalSyncStage {
  private static final String LOG_TAG = "SyncClientsEngineStage";

  public static final String COLLECTION_NAME       = "clients";
  public static final int CLIENTS_TTL_REFRESH      = 604800000;   // 7 days in milliseconds.
  public static final int MAX_UPLOAD_FAILURE_COUNT = 5;

  protected GlobalSession session;
  protected final ClientRecordFactory factory = new ClientRecordFactory();
  protected ClientUploadDelegate clientUploadDelegate;
  protected ClientDownloadDelegate clientDownloadDelegate;
  protected ClientsDatabaseAccessor db;

  protected volatile boolean shouldWipe;
  protected volatile boolean commandsProcessedShouldUpload;
  protected final AtomicInteger uploadAttemptsCount = new AtomicInteger();
  protected final List<ClientRecord> toUpload = new ArrayList<ClientRecord>();

  /**
   * The following two delegates, ClientDownloadDelegate and ClientUploadDelegate
   * are both triggered in a chain, starting when execute() calls
   * downloadClientRecords().
   *
   * Client records are downloaded using a get() request. Upon success of the
   * get() request, the local client record is uploaded.
   *
   * @author Marina Samuel
   *
   */
  public class ClientDownloadDelegate extends WBOCollectionRequestDelegate {

    // We use this on each WBO, so lift it out.
    final ClientsDataDelegate clientsDelegate = session.getClientsDelegate();
    boolean localAccountGUIDDownloaded = false;

    @Override
    public String credentials() {
      return session.credentials();
    }

    @Override
    public String ifUnmodifiedSince() {
      // TODO last client download time?
      return null;
    }

    @Override
    public void handleRequestSuccess(SyncStorageResponse response) {
      BaseResource.consumeEntity(response); // We don't need the response at all.

      // Wipe the clients table if it still hasn't been wiped but needs to be.
      wipeAndStore(null);

      // If we successfully downloaded all records but ours was not one of them
      // then reset the timestamp.
      if (!localAccountGUIDDownloaded) {
        Logger.info(LOG_TAG, "Local client GUID does not exist on the server. Upload timestamp will be reset.");
        session.config.persistServerClientRecordTimestamp(0);
      }
      localAccountGUIDDownloaded = false;

      final int clientsCount;
      try {
        clientsCount = db.clientsCount();
      } finally {
        // Close the database to clear cached readableDatabase/writableDatabase
        // after we've completed our last transaction (db.store()).
        db.close();
      }

      Logger.debug(LOG_TAG, "Database contains " + clientsCount + " clients.");
      Logger.debug(LOG_TAG, "Server response asserts " + response.weaveRecords() + " records.");

      // TODO: persist the response timestamp to know whether to download next time (Bug 726055).
      clientUploadDelegate = new ClientUploadDelegate();
      clientsDelegate.setClientsCount(clientsCount);

      // If we upload remote records, checkAndUpload() will be called upon
      // upload success in the delegate. Otherwise call checkAndUpload() now.
      if (toUpload.size() > 0) {
        uploadRemoteRecords(response.normalizedWeaveTimestamp());
        return;
      }
      checkAndUpload();
    }

    @Override
    public void handleRequestFailure(SyncStorageResponse response) {
      BaseResource.consumeEntity(response); // We don't need the response at all, and any exception handling shouldn't need the response body.
      localAccountGUIDDownloaded = false;

      try {
        Logger.info(LOG_TAG, "Client upload failed. Aborting sync.");
        session.abort(new HTTPFailureException(response), "Client download failed.");
      } finally {
        // Close the database upon failure.
        db.close();
      }
    }

    @Override
    public void handleRequestError(Exception ex) {
      localAccountGUIDDownloaded = false;
      try {
        Logger.info(LOG_TAG, "Client upload error. Aborting sync.");
        session.abort(ex, "Failure fetching client record.");
      } finally {
        // Close the database upon error.
        db.close();
      }
    }

    @Override
    public void handleWBO(CryptoRecord record) {
      ClientRecord r;
      try {
        r = (ClientRecord) factory.createRecord(record.decrypt());
        if (clientsDelegate.isLocalGUID(r.guid)) {
          Logger.info(LOG_TAG, "Local client GUID exists on server and was downloaded");
          localAccountGUIDDownloaded = true;
          // Oh hey! Our record is on the server. This is the authoritative
          // server timestamp, so let's hang on to it to decide whether we
          // need to upload.
          session.config.persistServerClientRecordTimestamp(r.lastModified);

          // Process commands.
          processCommands(r.commands);
        } else {
          // Only need to store record if it isn't our local one.
          wipeAndStore(r);
          addCommands(r);
        }
        RepoUtils.logClient(r);
      } catch (Exception e) {
        session.abort(e, "Exception handling client WBO.");
        return;
      }
    }

    @Override
    public KeyBundle keyBundle() {
      try {
        return session.keyBundleForCollection(COLLECTION_NAME);
      } catch (NoCollectionKeysSetException e) {
        session.abort(e, "No collection keys set.");
        return null;
      }
    }
  }

  public class ClientUploadDelegate extends WBORequestDelegate {
    protected static final String LOG_TAG = "ClientUploadDelegate";
    public Long currentlyUploadingRecordTimestamp;
    public boolean currentlyUploadingLocalRecord;

    @Override
    public String credentials() {
      return session.credentials();
    }

    private void setUploadDetails(boolean isLocalRecord, long serverClientRecordTimestamp) {
      currentlyUploadingRecordTimestamp = serverClientRecordTimestamp;
      currentlyUploadingLocalRecord = isLocalRecord;
    }

    @Override
    public String ifUnmodifiedSince() {
      Long timestampInMilliseconds = currentlyUploadingRecordTimestamp;

      // It's the first upload so we don't care about X-If-Unmodified-Since.
      if (timestampInMilliseconds == 0) {
        return null;
      }

      return Utils.millisecondsToDecimalSecondsString(timestampInMilliseconds);
    }

    @Override
    public void handleRequestSuccess(SyncStorageResponse response) {
      Logger.debug(LOG_TAG, "Upload succeeded.");

      commandsProcessedShouldUpload = false;
      uploadAttemptsCount.set(0);

      // This is the case when we are NOT currently uploading our local record.
      if (!currentlyUploadingLocalRecord) {
        clearRecordsToUpload();
        checkAndUpload();
        return;
      }

      try {
        long timestamp = Utils.decimalSecondsToMilliseconds(response.body());
        session.config.persistServerClientRecordTimestamp(timestamp);
        BaseResource.consumeEntity(response);

        Logger.trace(LOG_TAG, "Timestamp from body is: " + timestamp);
        Logger.trace(LOG_TAG, "Timestamp from header is: " + response.normalizedWeaveTimestamp());
      } catch (Exception e) {
        session.abort(e, "Unable to fetch timestamp.");
        return;
      }
      session.advance();
    }

    @Override
    public void handleRequestFailure(SyncStorageResponse response) {
      int statusCode = response.getStatusCode();

      // If upload failed because of `ifUnmodifiedSince` then there are new
      // commands uploaded to our record. We must download and process them first.
      if (!commandsProcessedShouldUpload ||
          statusCode == HttpStatus.SC_PRECONDITION_FAILED ||
          uploadAttemptsCount.incrementAndGet() > MAX_UPLOAD_FAILURE_COUNT) {

        Logger.debug(LOG_TAG, "Client upload failed. Aborting sync.");
        if (!currentlyUploadingLocalRecord) {
          clearRecordsToUpload(); // These will be redownloaded.
        }
        BaseResource.consumeEntity(response); // The exception thrown should need the response body.
        session.abort(new HTTPFailureException(response), "Client upload failed.");
        return;
      }
      Logger.trace(LOG_TAG, "Retrying upload…");
      // Preconditions:
      // commandsProcessedShouldUpload == true &&
      // statusCode != 412 &&
      // uploadAttemptCount < MAX_UPLOAD_FAILURE_COUNT
      checkAndUpload();
    }

    @Override
    public void handleRequestError(Exception ex) {
      Logger.info(LOG_TAG, "Client upload error. Aborting sync.");
      session.abort(ex, "Client upload failed.");
    }

    @Override
    public KeyBundle keyBundle() {
      try {
        return session.keyBundleForCollection(COLLECTION_NAME);
      } catch (NoCollectionKeysSetException e) {
        session.abort(e, "No collection keys set.");
        return null;
      }
    }
  }

  @Override
  public void execute(GlobalSession session) throws NoSuchStageException {
    this.session = session;
    init();

    if (shouldDownload()) {
      downloadClientRecords();   // Will kick off upload, too…
    } else {
      // Upload if necessary.
    }
  }

  protected ClientRecord newLocalClientRecord(ClientsDataDelegate delegate) {
    final String ourGUID = delegate.getAccountGUID();
    final String ourName = delegate.getClientName();

    ClientRecord r = new ClientRecord(ourGUID);
    r.name = ourName;
    return r;    
  }

  protected void init() {
    db = new ClientsDatabaseAccessor(session.getContext());
  }

  // TODO: Bug 726055 - More considered handling of when to sync.
  protected boolean shouldDownload() {
    // Ask info/collections whether a download is needed.
    return true;
  }

  protected boolean shouldUpload() {
    if (commandsProcessedShouldUpload) {
      return true;
    }

    long lastUpload = session.config.getPersistedServerClientRecordTimestamp();   // Defaults to 0.
    if (lastUpload == 0) {
      return true;
    }

    // Note the opportunity for clock drift problems here.
    // TODO: if we track download times, we can use the timestamp of most
    // recent download response instead of the current time.
    long now = System.currentTimeMillis();
    long age = now - lastUpload;
    return age >= CLIENTS_TTL_REFRESH;
  }

  protected void processCommands(JSONArray commands) {
    if (commands == null ||
        commands.size() == 0) {
      return;
    }

    commandsProcessedShouldUpload = true;
    CommandProcessor processor = CommandProcessor.getProcessor();

    for (Object o : commands) {
      processor.processCommand(new ExtendedJSONObject((JSONObject) o));
    }
  }

  @SuppressWarnings("unchecked")
  protected void addCommands(ClientRecord record) throws NullCursorException {
    Logger.trace(LOG_TAG, "Adding commands to " + record.guid);
    List<Command> commands = db.fetchCommandsForClient(record.guid);

    if (commands == null || commands.size() == 0) {
      Logger.trace(LOG_TAG, "No commands to add.");
      return;
    }

    for (Command command : commands) {
      JSONObject jsonCommand = command.asJSONObject();
      if (record.commands == null) {
        record.commands = new JSONArray();
      }
      record.commands.add(jsonCommand);
    }
    toUpload.add(record);
  }

  @SuppressWarnings("unchecked")
  protected void uploadRemoteRecords(long timestamp) {
    Logger.trace(LOG_TAG, "In uploadRemoteRecords. Uploading " + toUpload.size() + " records" );

    if (toUpload.size() == 1) {
      Logger.info(LOG_TAG, "Only 1 remote record to upload.");
      ClientRecord record = toUpload.get(0);
      Logger.info(LOG_TAG, "Record last mod: " + record.lastModified);
      clientUploadDelegate.setUploadDetails(false, record.lastModified);
      CryptoRecord cryptoRecord = encryptClientRecord(record);
      if (cryptoRecord != null) {
        this.uploadClientRecord(cryptoRecord);
      }
      return;
    }

    JSONArray cryptoRecords = new JSONArray();
    for (ClientRecord record : toUpload) {
      Logger.trace(LOG_TAG, "Record " + record.guid + " is being uploaded" );

      CryptoRecord cryptoRecord = encryptClientRecord(record);
      cryptoRecords.add(cryptoRecord.toJSONObject());
    }
    clientUploadDelegate.setUploadDetails(false, timestamp);
    Logger.info(LOG_TAG, "cryptoRecords size: " + cryptoRecords.size());
    this.uploadClientRecords(cryptoRecords);
  }

  protected void checkAndUpload() {
    if (!shouldUpload()) {
      Logger.debug(LOG_TAG, "Not uploading client record.");
      session.advance();
      return;
    }

    final ClientRecord localClient = newLocalClientRecord(session.getClientsDelegate());
    clientUploadDelegate.setUploadDetails(true, session.config.getPersistedServerClientRecordTimestamp());
    CryptoRecord cryptoRecord = encryptClientRecord(localClient);
    if (cryptoRecord != null) {
      this.uploadClientRecord(cryptoRecord);
    }
  }

  protected CryptoRecord encryptClientRecord(ClientRecord recordToUpload) {
    // Generate CryptoRecord from ClientRecord to upload.
    final String encryptionFailure = "Couldn't encrypt new client record.";

    try {
      CryptoRecord cryptoRecord = recordToUpload.getEnvelope();
      cryptoRecord.keyBundle = clientUploadDelegate.keyBundle();
      return cryptoRecord.encrypt();
    } catch (UnsupportedEncodingException e) {
      session.abort(e, encryptionFailure + " Unsupported encoding.");
    } catch (CryptoException e) {
      session.abort(e, encryptionFailure);
    }
    return null;
  }

  public void clearRecordsToUpload() {
    try {
      db.wipeCommandsTable();
      toUpload.clear();
    } finally {
      db.close();
    }
  }

  protected void downloadClientRecords() {
    shouldWipe = true;
    clientDownloadDelegate = makeClientDownloadDelegate();

    try {
      URI getURI = session.config.collectionURI(COLLECTION_NAME, true);

      SyncStorageCollectionRequest request = new SyncStorageCollectionRequest(getURI);
      request.delegate = clientDownloadDelegate;

      Logger.trace(LOG_TAG, "Downloading client records.");
      request.get();
    } catch (URISyntaxException e) {
      session.abort(e, "Invalid URI.");
    }
  }

  protected void uploadClientRecords(JSONArray records) {
    Logger.trace(LOG_TAG, "Uploading client records " + records.toJSONString());
    try {
      URI putURI = session.config.collectionURI(COLLECTION_NAME, false);

      SyncStorageRecordRequest request = new SyncStorageRecordRequest(putURI);
      request.delegate = clientUploadDelegate;
      request.post(records);
    } catch (URISyntaxException e) {
      session.abort(e, "Invalid URI.");
    } catch (Exception e) {
      session.abort(e, "Unable to parse body.");
    }
  }

  protected void uploadClientRecord(CryptoRecord record) {
    Logger.debug(LOG_TAG, "Uploading client record " + record.guid);
    try {
      URI putURI = session.config.wboURI(COLLECTION_NAME, record.guid);

      SyncStorageRecordRequest request = new SyncStorageRecordRequest(putURI);
      request.delegate = clientUploadDelegate;
      request.put(record);
    } catch (URISyntaxException e) {
      session.abort(e, "Invalid URI.");
    }
  }

  protected ClientDownloadDelegate makeClientDownloadDelegate() {
    return new ClientDownloadDelegate();
  }

  protected void wipeAndStore(ClientRecord record) {
    if (shouldWipe) {
      db.wipeClientsTable();
      shouldWipe = false;
    }
    if (record != null) {
      db.store(record);
    }
  }
}
