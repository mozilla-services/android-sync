package org.mozilla.gecko.sync.stage;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import org.json.simple.parser.ParseException;
import org.mozilla.gecko.sync.CryptoRecord;
import org.mozilla.gecko.sync.GlobalSession;
import org.mozilla.gecko.sync.HTTPFailureException;
import org.mozilla.gecko.sync.NoCollectionKeysSetException;
import org.mozilla.gecko.sync.NonObjectJSONException;
import org.mozilla.gecko.sync.crypto.CryptoException;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.delegates.ClientUploadDelegate;
import org.mozilla.gecko.sync.net.SyncStorageCollectionRequest;
import org.mozilla.gecko.sync.net.SyncStorageRecordRequest;
import org.mozilla.gecko.sync.net.SyncStorageResponse;
import org.mozilla.gecko.sync.net.WBOCollectionRequestDelegate;
import org.mozilla.gecko.sync.repositories.NullCursorException;
import org.mozilla.gecko.sync.repositories.android.ClientsDatabaseContentProvider;
import org.mozilla.gecko.sync.repositories.domain.ClientRecord;
import org.mozilla.gecko.sync.repositories.domain.ClientRecordFactory;
import org.mozilla.gecko.sync.setup.Constants;

import android.util.Log;

public class SyncClientsEngineStage extends WBOCollectionRequestDelegate implements GlobalSyncStage {
  protected static final String LOG_TAG = "SyncClientsEngineStage";
  private static final String COLLECTION_NAME = "clients";

  protected GlobalSession session;
  protected ClientRecordFactory factory = new ClientRecordFactory();
  protected ClientUploadDelegate clientUploadDelegate;
  protected ClientsDatabaseContentProvider db;

  // Account/Profile info
  protected ClientRecord localClient;

  @Override
  public void execute(GlobalSession session) throws NoSuchStageException {
    this.session = session;
    init();

    downloadClientRecords();
  }

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
    try {
      // Response body must be consumed in order to reuse the connection.
      Log.i(LOG_TAG, "get() was successful. Response body: " + response.body());

      // Generate CryptoRecord from ClientRecord to upload.
      CryptoRecord cryptoRecord = cryptoFromClient(localClient);
      if (shouldUpload() && cryptoRecord != null) {
        this.uploadClientRecord(cryptoRecord);
        session.advance();
      }
    } catch (NullCursorException e) {
      session.abort(e, "Got null cursor for fetching client with id " + localClient.guid);
    } catch (Exception e) {
      session.abort(e, "Unable to print response body");
    } finally {
      db.close();
    }
  }

  @Override
  public void handleRequestFailure(SyncStorageResponse response) {
    Log.i(LOG_TAG, "Client upload failed. Aborting sync.");
    session.abort(new HTTPFailureException(response), "Client download failed.");
  }

  @Override
  public void handleRequestError(Exception ex) {
    session.abort(ex, "Failure fetching client record.");
  }

  @Override
  public void handleWBO(CryptoRecord record) {
    ClientRecord r;
    try {
      r = (ClientRecord) factory.createRecord(((CryptoRecord) record).decrypt());
      printRecord(r);
    } catch (IllegalStateException e) {
      session.abort(e, "Invalid client WBO.");
      return;
    } catch (NonObjectJSONException e) {
      session.abort(e, "Invalid client WBO.");
      return;
    } catch (CryptoException e) {
      session.abort(e, "CryptoException handling client WBO.");
      return;
    } catch (IOException e) {
      // Some kind of lower-level error.
      session.abort(e, "IOException fetching client record.");
      return;
    } catch (ParseException e) {
      session.abort(e, "Invalid client WBO.");
      return;
    }
    db.store(r);
  }

  @Override
  public KeyBundle keyBundle() {
    return session.config.syncKeyBundle;
  }

  protected void init() {
    localClient = new ClientRecord(getProfileID());
    localClient.name = session.getClientName();

    clientUploadDelegate = new ClientUploadDelegate(session);
    db = new ClientsDatabaseContentProvider(session.getContext(), session);
    db.wipe();
  }

  protected boolean shouldUpload() throws NullCursorException {
    // If localClient was stored we should also upload it.
    return db.compareAndStore(localClient);
  }

  protected CryptoRecord cryptoFromClient(ClientRecord record) {
    String encryptionFailure = "Couldn't encrypt new client record.";
    CryptoRecord cryptoRecord = record.getPayload();
    try {
      cryptoRecord.keyBundle = session.keyForCollection(COLLECTION_NAME);
      cryptoRecord.encrypt();
    } catch (UnsupportedEncodingException e) {
      session.abort(e, encryptionFailure + " Unsupported encoding.");
      return null;
    } catch (CryptoException e) {
      session.abort(e, encryptionFailure);
      return null;
    } catch (NoCollectionKeysSetException e) {
      session.abort(e, encryptionFailure + " No collection keys set.");
      return null;
    }
    return cryptoRecord;
  }

  protected void downloadClientRecords() {
    try {
      URI getURI = session.config.collectionURI(COLLECTION_NAME, true);

      SyncStorageCollectionRequest request = new SyncStorageCollectionRequest(getURI);
      request.delegate = this;
      request.get();
    } catch (URISyntaxException e) {
      session.abort(e, "Invalid URI.");
    }
  }

  protected void uploadClientRecord(CryptoRecord record) {
    try {
      URI putURI = session.config.collectionURI(COLLECTION_NAME, false);

      SyncStorageRecordRequest request = new SyncStorageRecordRequest(putURI);
      request.delegate = clientUploadDelegate;
      request.put(record);
    } catch (URISyntaxException e) {
      session.abort(e, "Invalid URI.");
    }
  }

  protected String getProfileID() {
    return Constants.PROFILE_ID;
  }

  // TODO: Remove this, only used it for testing.
  private void printRecord(ClientRecord r) {
    System.out.println("GUID: " + r.guid);
    System.out.println("NAME: " + r.name);
    System.out.println("TYPE: " + r.type);
    System.out.println("LASTMOD: " + r.lastModified);
  }
}
