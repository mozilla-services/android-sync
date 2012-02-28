/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.stage;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import org.mozilla.gecko.sync.CryptoRecord;
import org.mozilla.gecko.sync.GlobalSession;
import org.mozilla.gecko.sync.HTTPFailureException;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.NoCollectionKeysSetException;
import org.mozilla.gecko.sync.crypto.CryptoException;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.net.SyncResourceDelegate;
import org.mozilla.gecko.sync.net.SyncStorageCollectionRequest;
import org.mozilla.gecko.sync.net.SyncStorageRecordRequest;
import org.mozilla.gecko.sync.net.SyncStorageRequestDelegate;
import org.mozilla.gecko.sync.net.SyncStorageResponse;
import org.mozilla.gecko.sync.net.WBOCollectionRequestDelegate;
import org.mozilla.gecko.sync.repositories.android.ClientsDatabaseAccessor;
import org.mozilla.gecko.sync.repositories.android.RepoUtils;
import org.mozilla.gecko.sync.repositories.domain.ClientRecord;
import org.mozilla.gecko.sync.repositories.domain.ClientRecordFactory;

import ch.boye.httpclientandroidlib.HttpResponse;

public class SyncClientsEngineStage implements GlobalSyncStage {
  protected static final String LOG_TAG = "SyncClientsEngineStage";
  protected static final String COLLECTION_NAME = "clients";

  protected GlobalSession session;
  protected ClientRecordFactory factory = new ClientRecordFactory();
  protected ClientUploadDelegate clientUploadDelegate;
  protected ClientDownloadDelegate clientDownloadDelegate;
  protected ClientsDatabaseAccessor db;

  // Account/Profile info
  protected ClientRecord localClient;
  protected boolean shouldWipe;

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
      session.setNumClients(db.numClients());
      checkAndUpload();

      // Close the database to clear cached readableDatabase/writeableDatabase
      // after we've completed our last transaction (db.store()).
      db.close();
    }

    @Override
    public void handleRequestFailure(SyncStorageResponse response) {
      Logger.info(LOG_TAG, "Client upload failed. Aborting sync.");
      session.abort(new HTTPFailureException(response), "Client download failed.");

      // Close the database upon failure.
      db.close();
    }

    @Override
    public void handleRequestError(Exception ex) {
      Logger.info(LOG_TAG, "Client upload error. Aborting sync.");
      session.abort(ex, "Failure fetching client record.");

      // Close the database upon error.
      db.close();
    }

    @Override
    public void handleWBO(CryptoRecord record) {
      ClientRecord r;
      try {
        r = (ClientRecord) factory.createRecord(record.decrypt());
        RepoUtils.logClient(r);
      } catch (Exception e) {
        session.abort(e, "Exception handling client WBO.");
        return;
      }
      wipeAndStore(r);
    }

    @Override
    public KeyBundle keyBundle() {
      try {
        return session.keyForCollection(COLLECTION_NAME);
      } catch (NoCollectionKeysSetException e) {
        session.abort(e, "No collection keys set.");
        return null;
      }
    }
  }

  public class ClientUploadDelegate implements SyncStorageRequestDelegate {
    protected static final String LOG_TAG = "ClientUploadDelegate";

    @Override
    public String credentials() {
      return session.credentials();
    }

    @Override
    public String ifUnmodifiedSince() {
      // TODO last client upload time?
      return null;
    }

    @Override
    public void handleRequestSuccess(SyncStorageResponse response) {
      // Response entity must be consumed in order to reuse the connection.
      HttpResponse httpResponse = response.httpResponse();
      if (httpResponse != null) {
        SyncResourceDelegate.consumeEntity(httpResponse.getEntity());
      }
      session.advance();
    }

    @Override
    public void handleRequestFailure(SyncStorageResponse response) {
      Logger.info(LOG_TAG, "Client upload failed. Aborting sync.");
      session.abort(new HTTPFailureException(response), "Client upload failed.");
    }

    @Override
    public void handleRequestError(Exception ex) {
      Logger.info(LOG_TAG, "Client upload error. Aborting sync.");
      session.abort(ex, "Client upload failed.");
    }
  }

  @Override
  public void execute(GlobalSession session) throws NoSuchStageException {
    this.session = session;
    init();

    downloadClientRecords();
  }

  protected void init() {
    localClient = new ClientRecord(session.getAccountGUID());
    localClient.name = session.getClientName();

    clientDownloadDelegate = new ClientDownloadDelegate();
    clientUploadDelegate = new ClientUploadDelegate();
    db = new ClientsDatabaseAccessor(session.getContext());
    shouldWipe = true;
  }

  // TODO: Bug 729248 - Smarter upload of client records.
  protected boolean shouldUpload() {
    return true;
  }

  public void checkAndUpload() {
    // Generate CryptoRecord from ClientRecord to upload.
    CryptoRecord cryptoRecord = cryptoFromClient(localClient);
    if (shouldUpload() && cryptoRecord != null) {
      this.wipeAndStore(localClient);
      this.uploadClientRecord(cryptoRecord);
      session.advance();
    }
  }

  protected CryptoRecord cryptoFromClient(ClientRecord record) {
    String encryptionFailure = "Couldn't encrypt new client record.";
    CryptoRecord cryptoRecord = record.getPayload();
    try {
      cryptoRecord.keyBundle = clientDownloadDelegate.keyBundle();
      cryptoRecord.encrypt();
    } catch (UnsupportedEncodingException e) {
      session.abort(e, encryptionFailure + " Unsupported encoding.");
      return null;
    } catch (CryptoException e) {
      session.abort(e, encryptionFailure);
      return null;
    }
    return cryptoRecord;
  }

  protected void downloadClientRecords() {
    try {
      URI getURI = session.config.collectionURI(COLLECTION_NAME, true);

      SyncStorageCollectionRequest request = new SyncStorageCollectionRequest(getURI);
      request.delegate = clientDownloadDelegate;
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

  protected void wipeAndStore(ClientRecord record) {
    if (shouldWipe) {
      db.wipe();
      shouldWipe = false;
    }
    db.store(record);
  }
}
