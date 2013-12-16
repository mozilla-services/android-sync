/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.fxa.sync;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.mozilla.gecko.background.common.log.Logger;
import org.mozilla.gecko.background.fxa.FxAccountClient;
import org.mozilla.gecko.browserid.BrowserIDKeyPair;
import org.mozilla.gecko.browserid.JSONWebTokenUtils;
import org.mozilla.gecko.fxa.authenticator.FxAccountAuthenticator;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.HTTPFailureException;
import org.mozilla.gecko.sync.net.AuthHeaderProvider;
import org.mozilla.gecko.sync.net.HawkAuthHeaderProvider;
import org.mozilla.gecko.sync.net.SyncStorageResponse;
import org.mozilla.gecko.sync.setup.SyncAccounts.SyncAccountParameters;
import org.mozilla.gecko.tokenserver.TokenServerClient;
import org.mozilla.gecko.tokenserver.TokenServerClientDelegate;
import org.mozilla.gecko.tokenserver.TokenServerException;
import org.mozilla.gecko.tokenserver.TokenServerToken;

import android.content.Context;
import ch.boye.httpclientandroidlib.HttpResponse;

/**
 * Represent a Firefox Account.
 *
 * This is the FxAccounts equivalent of {@link SyncAccountParameters}. 
 */
public class FxAccount {
  private static final String LOG_TAG = FxAccount.class.getSimpleName();

  public interface Delegate {
    public void handleSuccess(String uid, String endpoint, AuthHeaderProvider authHeaderProvider);
    public void handleError(Exception e);
  }

  protected final String email;
  protected final byte[] sessionTokenBytes;
  protected final byte[] kA;
  protected final byte[] kB;
  protected final String idpEndpoint;
  protected final String authEndpoint;
  protected final ExecutorService executor;

  public FxAccount(String email, byte[] sessionTokenBytes, byte[] kA, byte[] kB, String idpEndpoint, String authEndpoint) {
    this.email = email;
    this.sessionTokenBytes = sessionTokenBytes;
    this.kA = kA;
    this.kB = kB;
    this.idpEndpoint = idpEndpoint;
    this.authEndpoint = authEndpoint;
    this.executor = Executors.newSingleThreadExecutor();
  }

  /**
   * Request a signed certificate and exchange it for a token.
   *
   * This is temporary code that does not do production-ready caching. This
   * should be made obsolete by a fully featured {@link FxAccountAuthenticator}.
   *
   * @param context to use.
   * @param keyPair to sign certificate for.
   * @param delegate to callback to.
   */
  public void login(final Context context, final BrowserIDKeyPair keyPair, final Delegate delegate) {
    ExtendedJSONObject keyPairObject;
    try {
      keyPairObject = new ExtendedJSONObject(keyPair.getPublic().serialize());
    } catch (Exception e) {
      delegate.handleError(e);
      return;
    }

    FxAccountClient fxAccountClient = new FxAccountClient(idpEndpoint, Executors.newSingleThreadExecutor());
    fxAccountClient.sign(sessionTokenBytes, keyPairObject, JSONWebTokenUtils.DEFAULT_CERTIFICATE_DURATION_IN_MILLISECONDS,
        new FxAccountClient.RequestDelegate<String>() {
      @Override
      public void handleError(Exception e) {
        Logger.error(LOG_TAG, "Failed to sign.", e);
        delegate.handleError(e);
      }

      @Override
      public void handleFailure(int status, HttpResponse response) {
        HTTPFailureException e = new HTTPFailureException(new SyncStorageResponse(response));
        Logger.error(LOG_TAG, "Failed to sign.", e);
        delegate.handleError(e);
      }

      @Override
      public void handleSuccess(String certificate) {
        Logger.pii(LOG_TAG, "Got certificate " + certificate);

        try {
          String assertion = JSONWebTokenUtils.createAssertion(keyPair.getPrivate(), certificate, authEndpoint);
          Logger.pii(LOG_TAG, "Generated assertion " + assertion);

          JSONWebTokenUtils.dumpAssertion(assertion);

          String uri = authEndpoint + (authEndpoint.endsWith("/") ? "" : "/") + "1.0/sync/1.1";
          TokenServerClient tokenServerclient = new TokenServerClient(new URI(uri), executor);

          tokenServerclient.getTokenFromBrowserIDAssertion(assertion, true, new TokenServerClientDelegate() {
            @Override
            public void handleSuccess(TokenServerToken token) {
              AuthHeaderProvider authHeaderProvider;
              try {
                authHeaderProvider = new HawkAuthHeaderProvider(token.id, token.key.getBytes("UTF-8"), false);
              } catch (UnsupportedEncodingException e) {
                Logger.error(LOG_TAG, "Failed to sync.", e);
                delegate.handleError(e);
                return;
              }

              delegate.handleSuccess(token.uid, token.endpoint, authHeaderProvider);
            }

            @Override
            public void handleFailure(TokenServerException e) {
              Logger.error(LOG_TAG, "Failed fetching server token.", e);
              delegate.handleError(e);
            }

            @Override
            public void handleError(Exception e) {
              Logger.error(LOG_TAG, "Got error fetching token server token.", e);
              delegate.handleError(e);
            }
          });
        } catch (Exception e) {
          Logger.error(LOG_TAG, "Got error doing stuff.", e);
          delegate.handleError(e);
        }
      }
    });
  }
}
