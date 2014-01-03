/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync;

import java.net.URISyntaxException;

import org.mozilla.gecko.background.common.log.Logger;
import org.mozilla.gecko.fxa.authenticator.FxAccountAuthenticator;
import org.mozilla.gecko.sync.net.AuthHeaderProvider;
import org.mozilla.gecko.sync.net.SyncStorageRecordRequest;
import org.mozilla.gecko.sync.net.SyncStorageRequestDelegate;
import org.mozilla.gecko.sync.net.SyncStorageResponse;
import org.mozilla.gecko.sync.stage.AbstractNonRepositorySyncStage;
import org.mozilla.gecko.sync.stage.NoSuchStageException;

import android.accounts.Account;

/**
 * The purpose of this class is to talk to a Sync 1.1 server and check
 * for a Firefox Accounts migration sentinel.
 *
 * If one is found, a Firefox Account is created, and the existing
 * Firefox Sync account disabled (or deleted).
 */
public class MigrationSentinelSyncStage extends AbstractNonRepositorySyncStage {
  private static final String LOG_TAG = "MigrationStage";
  private static final String META_COLLECTION = "meta";

  public static class MigrationChecker {
    private static final String META_CREDENTIALS = "/meta/credentials";
    private static final String CREDENTIALS_KEY_EMAIL = "email";
    private static final String CREDENTIALS_KEY_UID = "uid";
    private static final String CREDENTIALS_KEY_SESSION_TOKEN = "sessionToken";
    private static final String CREDENTIALS_KEY_KA = "kA";
    private static final String CREDENTIALS_KEY_KB = "kB";

    private final GlobalSession session;
    private long fetchTimestamp = -1L;

    MigrationChecker(GlobalSession session) {
      this.session = session;
    }

    private void setTimestamp(long timestamp) {
      if (timestamp == -1L) {
        this.fetchTimestamp = System.currentTimeMillis();
      } else {
        this.fetchTimestamp = timestamp;
      }
    }

    private void migrate(CryptoRecord record) throws Exception {
      // If something goes wrong, we don't want to try this again.
      session.config.persistLastMigrationSentinelCheckTimestamp(fetchTimestamp);

      record.keyBundle = session.config.syncKeyBundle;
      record.decrypt();
      ExtendedJSONObject payload = record.payload;
      final String email = payload.getString(CREDENTIALS_KEY_EMAIL);
      final String uid = payload.getString(CREDENTIALS_KEY_UID);
      final String sessionToken = payload.getString(CREDENTIALS_KEY_SESSION_TOKEN);
      final String kA = payload.getString(CREDENTIALS_KEY_KA);
      final String kB = payload.getString(CREDENTIALS_KEY_KB);
      Account account = FxAccountAuthenticator.addAccount(session.context, email, uid, sessionToken, kA, kB);
      if (account == null) {
        onError(null, "Couldn't add account.");
      } else {
        onMigrated();
      }
    }

    private void onMigrated() {
      session.config.persistLastMigrationSentinelCheckTimestamp(fetchTimestamp);
      session.abort(null, "Account migrated.");
    }

    private void onCompletedUneventfully() {
      session.config.persistLastMigrationSentinelCheckTimestamp(fetchTimestamp);
      session.advance();
    }

    private void onError(Exception ex, String reason) {
      session.abort(ex, reason);
    }

    public void check() {
      String url = session.config.storageURL() + META_CREDENTIALS;
      try {
        SyncStorageRecordRequest request = new SyncStorageRecordRequest(url);
        request.delegate = new SyncStorageRequestDelegate() {
          
          @Override
          public String ifUnmodifiedSince() {
            return null;
          }
          
          @Override
          public void handleRequestSuccess(SyncStorageResponse response) {
            setTimestamp(response.normalizedWeaveTimestamp());
            try {
              migrate(CryptoRecord.fromJSONRecord(response.body()));
            } catch (Exception e) {
              onError(e, "Unable to parse credential response.");
            }
          }
          
          @Override
          public void handleRequestFailure(SyncStorageResponse response) {
            if (response.getStatusCode() == 404) {
              // Great!
              onCompletedUneventfully();
              return;
            }
            onError(null, "Failed to fetch.");
          }
          
          @Override
          public void handleRequestError(Exception ex) {
            onError(ex, "Failed to fetch.");
          }
          
          @Override
          public AuthHeaderProvider getAuthHeaderProvider() {
            return session.getAuthHeaderProvider();
          }
        };

        request.get();
      } catch (URISyntaxException e) {
        onError(e, "Malformed credentials URI.");
      }
    }
  }

  public MigrationSentinelSyncStage() {
  }

  @Override
  protected void execute() throws NoSuchStageException {
    InfoCollections infoCollections = session.config.infoCollections;
    if (infoCollections == null) {
      session.abort(null, "No info/collections set in MigrationSentinelSyncStage.");
      return;
    }

    long lastModified = session.config.getLastMigrationSentinelCheckTimestamp();
    if (!infoCollections.updateNeeded(META_COLLECTION, lastModified)) {
      return;
    }

    // Let's try a fetch.
    Logger.info(LOG_TAG, "Fetching meta/credentials to check for migration sentinel.");
    new MigrationChecker(session).check();
  }
}
