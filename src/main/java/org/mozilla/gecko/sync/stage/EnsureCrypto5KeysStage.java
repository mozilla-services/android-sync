/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.stage;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Set;

import org.json.simple.parser.ParseException;
import org.mozilla.gecko.sync.CollectionKeys;
import org.mozilla.gecko.sync.CryptoRecord;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.GlobalSession;
import org.mozilla.gecko.sync.InfoCollections;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.NoCollectionKeysSetException;
import org.mozilla.gecko.sync.NonObjectJSONException;
import org.mozilla.gecko.sync.crypto.CryptoException;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.crypto.PersistedCrypto5Keys;
import org.mozilla.gecko.sync.delegates.KeyUploadDelegate;
import org.mozilla.gecko.sync.net.SyncStorageRecordRequest;
import org.mozilla.gecko.sync.net.SyncStorageRequestDelegate;
import org.mozilla.gecko.sync.net.SyncStorageResponse;

public class EnsureCrypto5KeysStage implements GlobalSyncStage, SyncStorageRequestDelegate, KeyUploadDelegate {
  private static final String LOG_TAG = "EnsureC5KeysStage";
  private static final String CRYPTO_COLLECTION = "crypto";
  protected GlobalSession session;
  protected boolean retrying = false;

  @Override
  public void execute(GlobalSession session) throws NoSuchStageException {
    this.session = session;

    InfoCollections infoCollections = session.getInfoCollections();
    if (infoCollections == null) {
      session.abort(null, "No info/collections set in EnsureCrypto5KeysStage.");
      return;
    }

    PersistedCrypto5Keys pck = session.config.persistedCryptoKeys();
    long lastModified = pck.lastModified();
    if (retrying || !infoCollections.updateNeeded(CRYPTO_COLLECTION, lastModified)) {
      // Try to use our local collection keys for this session.
      Logger.info(LOG_TAG, "Trying to use persisted collection keys for this session.");
      CollectionKeys keys = pck.keys();
      if (keys != null) {
        Logger.info(LOG_TAG, "Using persisted collection keys for this session.");
        session.config.setCollectionKeys(keys);
        session.advance();
        return;
      }
      Logger.info(LOG_TAG, "Failed to use persisted collection keys for this session.");
    }

    // We need an update: fetch or upload keys as necessary.
    Logger.info(LOG_TAG, "Fetching fresh collection keys for this session.");
    try {
      SyncStorageRecordRequest request = new SyncStorageRecordRequest(session.wboURI(CRYPTO_COLLECTION, "keys"));
      request.delegate = this;
      request.get();
    } catch (URISyntaxException e) {
      session.abort(e, "Invalid URI.");
    }
  }

  @Override
  public String credentials() {
    return session.credentials();
  }

  @Override
  public String ifUnmodifiedSince() {
    // TODO: last key time!
    return null;
  }

  @Override
  public void handleRequestSuccess(SyncStorageResponse response) {
    CollectionKeys k = new CollectionKeys();
    try {
      ExtendedJSONObject body = response.jsonObjectBody();
      if (Logger.LOG_PERSONAL_INFORMATION) {
        Logger.pii(LOG_TAG, "Fetched keys: " + body.toJSONString());
      }
      k.setKeyPairsFromWBO(CryptoRecord.fromJSONRecord(body), session.config.syncKeyBundle);

    } catch (IllegalStateException e) {
      session.abort(e, "Invalid keys WBO.");
      return;
    } catch (ParseException e) {
      session.abort(e, "Invalid keys WBO.");
      return;
    } catch (NonObjectJSONException e) {
      session.abort(e, "Invalid keys WBO.");
      return;
    } catch (IOException e) {
      // Some kind of lower-level error.
      session.abort(e, "IOException fetching keys.");
      return;
    } catch (CryptoException e) {
      session.abort(e, "CryptoException handling keys WBO.");
      return;
    }

    PersistedCrypto5Keys pck = session.config.persistedCryptoKeys();
    if (!pck.persistedKeysExist()) {
      // New keys, and no old keys! Persist keys and server timestamp.
      Logger.info(LOG_TAG, "Setting fetched keys for this session.");
      session.config.setCollectionKeys(k);
      Logger.trace(LOG_TAG, "Persisting fetched keys and last modified.");
      pck.persistKeys(k);
      // Take the timestamp from the response since it is later than the timestamp from info/collections.
      pck.persistLastModified(response.normalizedWeaveTimestamp());
      session.advance();
      return;
    }

    // New keys, but we had old keys.  Check for differences.
    CollectionKeys oldKeys = pck.keys();
    boolean defaultKeyChanged = false;
    try {
      KeyBundle a = oldKeys.defaultKeyBundle();
      KeyBundle b = k.defaultKeyBundle();
      defaultKeyChanged = !a.equals(b);
    } catch (NoCollectionKeysSetException e) {
      session.abort(e, "NoCollectionKeysSetException in EnsureCrypto5KeysStage");
      return;
    }

    if (defaultKeyChanged) {
      // New keys with a different default/sync key. Reset all the things!
      Logger.info(LOG_TAG, "Fetched keys default key is not the same as persisted keys default key; " +
          "persisting fetched keys and last modified before resetting everything.");
      session.config.setCollectionKeys(k);
      pck.persistKeys(k);
      pck.persistLastModified(response.normalizedWeaveTimestamp());
      session.resetClient(null);
      session.abort(null, "crypto/keys default key changed on server.");
      return;
    }

    Set<String> changedKeys = CollectionKeys.differences(oldKeys, k);
    if (!changedKeys.isEmpty()) {
      // New keys, different from old keys.
      Logger.info(LOG_TAG, "Fetched keys are not the same as persisted keys; " +
          "setting fetched keys for this session before resetting changed engines.");
      session.config.setCollectionKeys(k);
      Logger.trace(LOG_TAG, "Persisting fetched keys and last modified.");
      pck.persistKeys(k);
      // Take the timestamp from the response since it is later than the timestamp from info/collections.
      pck.persistLastModified(response.normalizedWeaveTimestamp());
      session.resetClient(changedKeys.toArray(new String[changedKeys.size()]));
      session.abort(null, "crypto/keys changed on server.");
      return;
    }

    // New keys don't differ from old keys; persist timestamp and move on.
    Logger.trace(LOG_TAG, "Fetched keys are the same as persisted keys; persisting last modified.");
    session.config.setCollectionKeys(oldKeys);
    pck.persistLastModified(response.normalizedWeaveTimestamp());
    session.advance();
  }

  @Override
  public void handleRequestFailure(SyncStorageResponse response) {
    if (retrying) {
      // Should never happen -- this means we uploaded our crypto/keys
      // successfully, but somehow didn't have them persisted correctly and
      // tried to re-download (unsuccessfully).
      session.handleHTTPError(response, "Failure in refetching uploaded keys.");
      return;
    }

    int statusCode = response.getStatusCode();
    Logger.debug(LOG_TAG, "Got " + statusCode + " fetching keys.");
    if (statusCode == 404) {
      // No keys. Generate and upload, then refetch.
      CollectionKeys keys;
      try {
        keys = CollectionKeys.generateCollectionKeys();
      } catch (CryptoException e) {
        session.abort(e, "Couldn't generate new key bundle.");
        return;
      }
      session.uploadKeys(keys, this);
      return;
    }
    session.handleHTTPError(response, "Failure fetching keys.");
  }

  @Override
  public void handleRequestError(Exception ex) {
    session.abort(ex, "Failure fetching keys.");
  }

  @Override
  public void onKeysUploaded(CollectionKeys keys, long timestamp) {
    Logger.debug(LOG_TAG, "New keys uploaded. Persisting before starting stage again.");
    try {
      retrying = true;
      PersistedCrypto5Keys pck = session.config.persistedCryptoKeys();
      pck.persistKeys(keys);
      pck.persistLastModified(timestamp);
      this.execute(this.session);
    } catch (NoSuchStageException e) {
      session.abort(e, "No such stage.");
    }
  }

  @Override
  public void onKeyUploadFailed(Exception e) {
    Logger.warn(LOG_TAG, "Key upload failed. Aborting sync.");
    session.abort(e, "Key upload failed.");
  }
}
