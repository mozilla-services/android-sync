/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.fxa;

import org.mozilla.gecko.background.common.log.Logger;
import org.mozilla.gecko.fxa.activities.FxAccountAbstractSetupActivity;
import org.mozilla.gecko.sync.ExtendedJSONObject;

import android.content.Intent;
import android.os.Bundle;

public class FxAccountServerConfiguration {
  private static final String LOG_TAG = FxAccountServerConfiguration.class.getSimpleName();

  public static final String JSON_KEY_AUTH = "auth";
  public static final String JSON_KEY_OAUTH = "oauth";
  public static final String JSON_KEY_SERVICES = "services";
  public static final String JSON_KEY_SYNC = "sync";
  public static final String JSON_KEY_READING_LIST = "reading_list";

  public static final FxAccountServerConfiguration Stage = new FxAccountServerConfiguration(
      FxAccountConstants.STAGE_AUTH_SERVER_ENDPOINT,
      FxAccountConstants.STAGE_OAUTH_SERVER_ENDPOINT,
      FxAccountConstants.STAGE_TOKEN_SERVER_ENDPOINT,
      FxAccountConstants.STAGE_READING_LIST_SERVER_ENDPOINT);

  public final String authServerEndpoint;
  public final String oauthServerEndpoint;
  public final String syncServerEndpoint;
  public final String readingListServerEndpoint;

  public FxAccountServerConfiguration(String authServerEndpoint, String oauthServerEndpoint,
                                      String syncServerEndpoint, String readingListServerEndpoint) {
    if (authServerEndpoint == null) {
      throw new IllegalArgumentException("authServerEndpoint must not be null");
    }
    if (syncServerEndpoint == null) {
      throw new IllegalArgumentException("syncServerEndpoint must not be null");
    }
    // The OAuth and RL server endpoints may be null, which means that we won't request
    // OAuth tokens and won't sync Reading List.
    this.authServerEndpoint = authServerEndpoint;
    this.oauthServerEndpoint = oauthServerEndpoint;
    this.syncServerEndpoint = syncServerEndpoint;
    this.readingListServerEndpoint = readingListServerEndpoint;
  }

  public static FxAccountServerConfiguration withDefaultsFrom(String authServerEndpoint, String syncServerEndpoint) {
    final boolean usingDefaultAuthServer = FxAccountConstants.DEFAULT_AUTH_SERVER_ENDPOINT.equals(authServerEndpoint);
    final boolean usingStageAuthServer = FxAccountConstants.STAGE_AUTH_SERVER_ENDPOINT.equals(authServerEndpoint);

    // If the user isn't using Mozilla's Firefox Account endpoint, we won't
    // request OAuth tokens at all for now. In future, the account may have a
    // custom OAuth server attached.
    final String oauthServerEndpoint;
    if (usingDefaultAuthServer) {
      oauthServerEndpoint = FxAccountConstants.DEFAULT_OAUTH_SERVER_ENDPOINT;
    } else if (usingStageAuthServer) {
      oauthServerEndpoint = FxAccountConstants.STAGE_OAUTH_SERVER_ENDPOINT;
    } else {
      // OAuth server endpoint can be null: we just won't request OAuth tokens.
      // (For any service, including future services.)
      oauthServerEndpoint = null;
    }

    final boolean usingDefaultSyncServer = FxAccountConstants.DEFAULT_TOKEN_SERVER_ENDPOINT.equals(syncServerEndpoint);
    final boolean usingStageSyncServer = FxAccountConstants.STAGE_TOKEN_SERVER_ENDPOINT.equals(syncServerEndpoint);

    // We interpret the user not using both Mozilla's Firefox Account endpoint
    // and Mozilla's storage as a sign we should not upload their Reading List
    // data to Mozilla's Reading List storage.
    final String readingListServerEndpoint;
    if (usingDefaultAuthServer && usingDefaultSyncServer) {
      readingListServerEndpoint = FxAccountConstants.DEFAULT_READING_LIST_SERVER_ENDPOINT;
    } else if (usingStageAuthServer && usingStageSyncServer) {
      readingListServerEndpoint = FxAccountConstants.STAGE_READING_LIST_SERVER_ENDPOINT;
    } else {
      // Reading List server endpoint can be null: we just won't sync Reading List.
      readingListServerEndpoint = null;
    }

    return new FxAccountServerConfiguration(
        authServerEndpoint,
        oauthServerEndpoint,
        syncServerEndpoint,
        readingListServerEndpoint);
  }

  public static FxAccountServerConfiguration fromIntent(Intent intent) {
    String authServerEndpoint = FxAccountConstants.DEFAULT_AUTH_SERVER_ENDPOINT;
    String oauthServerEndpoint = FxAccountConstants.DEFAULT_OAUTH_SERVER_ENDPOINT;
    String syncServerEndpoint = FxAccountConstants.DEFAULT_TOKEN_SERVER_ENDPOINT;
    String readingListServerEndpoint = FxAccountConstants.DEFAULT_READING_LIST_SERVER_ENDPOINT;

    if (intent == null) {
      Logger.warn(LOG_TAG, "Intent is null; ignoring and using default servers.");
      return new FxAccountServerConfiguration(authServerEndpoint, syncServerEndpoint, oauthServerEndpoint, readingListServerEndpoint);
    }

    final String extrasString = intent.getStringExtra(FxAccountAbstractSetupActivity.EXTRA_EXTRAS);

    if (extrasString == null) {
      return new FxAccountServerConfiguration(authServerEndpoint, syncServerEndpoint, oauthServerEndpoint, readingListServerEndpoint);
    }

    final ExtendedJSONObject extras;
    final ExtendedJSONObject services;
    try {
      extras = new ExtendedJSONObject(extrasString);
      services = extras.getObject(JSON_KEY_SERVICES);
    } catch (Exception e) {
      Logger.warn(LOG_TAG, "Got exception parsing extras; ignoring and using default servers.");
      return new FxAccountServerConfiguration(authServerEndpoint, syncServerEndpoint, oauthServerEndpoint, readingListServerEndpoint);
    }

    // Auth and Sync are both optional (may not be included, which means use default); and null is treated as not included.
    String authServer = extras.getString(JSON_KEY_AUTH);
    String syncServer = services == null ? null : services.getString(JSON_KEY_SYNC);
    if (authServer != null) {
      authServerEndpoint = authServer;
    }
    if (syncServer != null) {
      syncServerEndpoint = syncServer;
    }
    // OAuth and Reading List are both optional (may not be included, which means use default); and can be null (which means don't use at all).
    if (services != null) {
      if (services.containsKey(JSON_KEY_OAUTH)) {
        oauthServerEndpoint = extras.getString(JSON_KEY_OAUTH);
      }
      if (services.containsKey(JSON_KEY_READING_LIST)) {
        readingListServerEndpoint = extras.getString(JSON_KEY_READING_LIST);
      }
    }

    return new FxAccountServerConfiguration(authServerEndpoint, syncServerEndpoint, oauthServerEndpoint, readingListServerEndpoint);
  }

  public Bundle toBundle() {
    final Bundle extras = new Bundle();
    final ExtendedJSONObject o = new ExtendedJSONObject();
    final ExtendedJSONObject services = new ExtendedJSONObject();
    services.put(JSON_KEY_SYNC, syncServerEndpoint);
    services.put(JSON_KEY_OAUTH, oauthServerEndpoint);
    services.put(JSON_KEY_READING_LIST, readingListServerEndpoint);
    o.put(JSON_KEY_AUTH, authServerEndpoint);
    o.put(JSON_KEY_SERVICES, services);
    extras.putString(FxAccountAbstractSetupActivity.EXTRA_EXTRAS, o.toJSONString());
    return extras;
  }
}
