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
  public static final String JSON_KEY_SERVICES = "services";
  public static final String JSON_KEY_SYNC = "sync";

  public static final FxAccountServerConfiguration Stage = new FxAccountServerConfiguration(
      FxAccountConstants.STAGE_AUTH_SERVER_ENDPOINT,
      FxAccountConstants.STAGE_TOKEN_SERVER_ENDPOINT);

  public final String authServerEndpoint;
  public final String syncServerEndpoint;

  public FxAccountServerConfiguration(String authServerEndpoint, String syncServerEndpoint) {
    if (authServerEndpoint == null) {
      throw new IllegalArgumentException("authServerEndpoint must not be null");
    }
    if (syncServerEndpoint == null) {
      throw new IllegalArgumentException("syncServerEndpoint must not be null");
    }
    this.authServerEndpoint = authServerEndpoint;
    this.syncServerEndpoint = syncServerEndpoint;
  }

  public static FxAccountServerConfiguration fromIntent(Intent intent) {
    String authServerEndpoint = FxAccountConstants.DEFAULT_AUTH_SERVER_ENDPOINT;
    String syncServerEndpoint = FxAccountConstants.DEFAULT_TOKEN_SERVER_ENDPOINT;

    if (intent == null) {
      Logger.warn(LOG_TAG, "Intent is null; ignoring and using default servers.");
      return new FxAccountServerConfiguration(authServerEndpoint, syncServerEndpoint);
    }

    final String extrasString = intent.getStringExtra(FxAccountAbstractSetupActivity.EXTRA_EXTRAS);

    if (extrasString == null) {
      return new FxAccountServerConfiguration(authServerEndpoint, syncServerEndpoint);
    }

    final ExtendedJSONObject extras;
    final ExtendedJSONObject services;
    try {
      extras = new ExtendedJSONObject(extrasString);
      services = extras.getObject(JSON_KEY_SERVICES);
    } catch (Exception e) {
      Logger.warn(LOG_TAG, "Got exception parsing extras; ignoring and using default servers.");
      return new FxAccountServerConfiguration(authServerEndpoint, syncServerEndpoint);
    }

    String authServer = extras.getString(JSON_KEY_AUTH);
    String syncServer = services == null ? null : services.getString(JSON_KEY_SYNC);

    if (authServer != null) {
      authServerEndpoint = authServer;
    }
    if (syncServer != null) {
      syncServerEndpoint = syncServer;
    }

    return new FxAccountServerConfiguration(authServerEndpoint, syncServerEndpoint);
  }

  public Bundle toBundle() {
    final Bundle extras = new Bundle();
    final ExtendedJSONObject o = new ExtendedJSONObject();
    if (!FxAccountConstants.DEFAULT_AUTH_SERVER_ENDPOINT.equals(authServerEndpoint)) {
      o.put(JSON_KEY_AUTH, authServerEndpoint);
    }
    final ExtendedJSONObject services = new ExtendedJSONObject();
    if (!FxAccountConstants.DEFAULT_TOKEN_SERVER_ENDPOINT.equals(syncServerEndpoint)) {
      services.put(JSON_KEY_SYNC, syncServerEndpoint);
    }
    o.put(JSON_KEY_SERVICES, services);
    extras.putString(FxAccountAbstractSetupActivity.EXTRA_EXTRAS, o.toJSONString());
    return extras;
  }
}
