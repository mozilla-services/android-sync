/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.reading;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.mozilla.gecko.background.common.PrefsBranch;
import org.mozilla.gecko.background.common.log.Logger;
import org.mozilla.gecko.background.fxa.FxAccountClient;
import org.mozilla.gecko.background.fxa.FxAccountClient20;
import org.mozilla.gecko.background.fxa.oauth.FxAccountAbstractClient.RequestDelegate;
import org.mozilla.gecko.background.fxa.oauth.FxAccountAbstractClientException.FxAccountAbstractClientRemoteException;
import org.mozilla.gecko.background.fxa.oauth.FxAccountOAuthClient10;
import org.mozilla.gecko.background.fxa.oauth.FxAccountOAuthClient10.AuthorizationResponse;
import org.mozilla.gecko.browserid.BrowserIDKeyPair;
import org.mozilla.gecko.browserid.JSONWebTokenUtils;
import org.mozilla.gecko.db.BrowserContract.ReadingListItems;
import org.mozilla.gecko.fxa.authenticator.AndroidFxAccount;
import org.mozilla.gecko.fxa.login.FxAccountLoginStateMachine;
import org.mozilla.gecko.fxa.login.FxAccountLoginStateMachine.LoginStateMachineDelegate;
import org.mozilla.gecko.fxa.login.FxAccountLoginTransition.Transition;
import org.mozilla.gecko.fxa.login.Married;
import org.mozilla.gecko.fxa.login.State;
import org.mozilla.gecko.fxa.login.State.StateLabel;
import org.mozilla.gecko.fxa.login.StateFactory;
import org.mozilla.gecko.fxa.sync.FxAccountSyncDelegate;
import org.mozilla.gecko.sync.net.AuthHeaderProvider;
import org.mozilla.gecko.sync.net.BearerAuthHeaderProvider;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.os.Bundle;

public class ReadingListSyncAdapter extends AbstractThreadedSyncAdapter {
  private static final String LOG_TAG = ReadingListSyncAdapter.class.getSimpleName();
  private static final long TIMEOUT_SECONDS = 60;
  protected final ExecutorService executor;

  public ReadingListSyncAdapter(Context context, boolean autoInitialize) {
    super(context, autoInitialize);
    this.executor = Executors.newSingleThreadExecutor();
  }


  static final class SyncAdapterSynchronizerDelegate implements ReadingListSynchronizerDelegate {
    private final FxAccountSyncDelegate syncDelegate;
    private final ContentProviderClient cpc;

    SyncAdapterSynchronizerDelegate(FxAccountSyncDelegate syncDelegate,
                                    ContentProviderClient cpc) {
      this.syncDelegate = syncDelegate;
      this.cpc = cpc;
    }

    @Override
    public void onUnableToSync(Exception e) {
      Logger.info(LOG_TAG, "XXX onUnableToSync.", e);
      cpc.release();
      syncDelegate.handleError(e);
    }

    @Override
    public void onStatusUploadComplete(Collection<String> uploaded,
                                       Collection<String> failed) {
      Logger.info(LOG_TAG, "XXX onStatusUploadComplete");
    }

    @Override
    public void onNewItemUploadComplete(Collection<String> uploaded,
                                        Collection<String> failed) {
      Logger.info(LOG_TAG, "XXX onNewItemUploadComplete");
    }

    @Override
    public void onModifiedUploadComplete() {
      Logger.info(LOG_TAG, "XXX onModifiedUploadComplete");
    }

    @Override
    public void onDownloadComplete() {
      Logger.info(LOG_TAG, "XXX onDownloadComplete");
    }

    @Override
    public void onComplete() {
      Logger.info(LOG_TAG, "XXX onComplete");
      cpc.release();
      syncDelegate.handleSuccess();
    }
  }

  @Override
  public void onPerformSync(final Account account, final Bundle extras, final String authority, final ContentProviderClient provider, final SyncResult syncResult) {
    Logger.setThreadLogTag(ReadingListConstants.GLOBAL_LOG_TAG);
    Logger.resetLogging();

    final Context context = getContext();
    final AndroidFxAccount fxAccount = new AndroidFxAccount(context, account);

    // If this sync was triggered by user action, this will be true.
    final boolean isImmediate = (extras != null) &&
        (extras.getBoolean(ContentResolver.SYNC_EXTRAS_UPLOAD, false) ||
            extras.getBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, false));

    final CountDownLatch latch = new CountDownLatch(1);
    final FxAccountSyncDelegate syncDelegate = new FxAccountSyncDelegate(latch, syncResult, fxAccount);
    try {
      final State state;
      try {
        state = fxAccount.getState();
      } catch (Exception e) {
        Logger.error(LOG_TAG, "Unable to sync.", e);
        return;
      }

      final String audience = fxAccount.getAudience();
      final String authServerEndpoint = fxAccount.getAccountServerURI();

      final SharedPreferences sharedPrefs = fxAccount.getReadingListPrefs();
      final FxAccountClient client = new FxAccountClient20(authServerEndpoint, executor);
      final FxAccountLoginStateMachine stateMachine = new FxAccountLoginStateMachine();

      stateMachine.advance(state, StateLabel.Married, new LoginStateMachineDelegate() {
        @Override
        public FxAccountClient getClient() {
          return client;
        }

        @Override
        public long getCertificateDurationInMilliseconds() {
          return 12 * 60 * 60 * 1000;
        }

        @Override
        public long getAssertionDurationInMilliseconds() {
          return 15 * 60 * 1000;
        }

        @Override
        public BrowserIDKeyPair generateKeyPair() throws NoSuchAlgorithmException {
          return StateFactory.generateKeyPair();
        }

        @Override
        public void handleTransition(Transition transition, State state) {
          Logger.info(LOG_TAG, "handleTransition: " + transition + " to " + state.getStateLabel());
        }

        @Override
        public void handleFinal(State state) {
          Logger.info(LOG_TAG, "handleFinal: in " + state.getStateLabel());
          fxAccount.setState(state);

          // TODO: scheduling, notifications.
          try {
            if (state.getStateLabel() != StateLabel.Married) {
              syncDelegate.handleCannotSync(state);
              return;
            }

            final Married married = (Married) state;
            final String assertion = married.generateAssertion(audience, JSONWebTokenUtils.DEFAULT_ASSERTION_ISSUER);
            Logger.info(LOG_TAG, "XXX Assertion is " + assertion);

            final String clientID = "3332a18d142636cb";
            final String scope = "profile";               // Niiiiiickkk!
            syncWithAssertion(clientID, scope, assertion, sharedPrefs, extras);
          } catch (Exception e) {
            syncDelegate.handleError(e);
            return;
          }
        }

        private void syncWithAssertion(final String client_id, final String scope, final String assertion,
                                       final SharedPreferences sharedPrefs, final Bundle extras) {
          final String oauthServerUri = ReadingListConstants.OAUTH_ENDPOINT_PROD;
          final FxAccountOAuthClient10 oauthClient = new FxAccountOAuthClient10(oauthServerUri, executor);
          Logger.info(LOG_TAG, "XXX oauth fetch");
          oauthClient.authorization(client_id, assertion, null, scope, new RequestDelegate<FxAccountOAuthClient10.AuthorizationResponse>() {
            @Override
            public void handleSuccess(AuthorizationResponse result) {
              Logger.info(LOG_TAG, "XXX oauth success");
              syncWithAuthorization(result, sharedPrefs, extras);
            }

            @Override
            public void handleFailure(FxAccountAbstractClientRemoteException e) {
              Logger.error(LOG_TAG, "XXX oauth failure", e);
              syncDelegate.handleError(e);
            }

            @Override
            public void handleError(Exception e) {
              Logger.error(LOG_TAG, "XXX oauth error", e);
              syncDelegate.handleError(e);
            }
          });
        }

        private void syncWithAuthorization(AuthorizationResponse authResponse,
                                           SharedPreferences sharedPrefs,
                                           Bundle extras) {
          final AuthHeaderProvider auth = new BearerAuthHeaderProvider(authResponse.access_token);

          final String endpointString = ReadingListConstants.DEFAULT_DEV_ENDPOINT;
          final URI endpoint;
          Logger.info(LOG_TAG, "XXX Syncing to " + endpointString);
          try {
            endpoint = new URI(endpointString);
          } catch (URISyntaxException e) {
            // Should never happen.
            Logger.error(LOG_TAG, "Unexpected malformed URI for reading list service: " + endpointString);
            syncDelegate.handleError(e);
            return;
          }

          final PrefsBranch branch = new PrefsBranch(sharedPrefs, "readinglist.");
          final ReadingListClient remote = new ReadingListClient(endpoint, auth);
          final ContentProviderClient cpc = getContentProviderClient(context);     // TODO: make sure I'm always released!

          final ReadingListStorage local = new LocalReadingListStorage(cpc);
          final ReadingListSynchronizer synchronizer = new ReadingListSynchronizer(branch, remote, local);

          synchronizer.syncAll(new SyncAdapterSynchronizerDelegate(syncDelegate, cpc));
          // TODO: backoffs, and everything else handled by a SessionCallback.
        }
      });

      latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
      Logger.info(LOG_TAG, "Reading list sync done.");

    } catch (Exception e) {
      Logger.error(LOG_TAG, "Got error syncing.", e);
      syncDelegate.handleError(e);
    }
    /*
     * TODO:
     * * Account error notifications. How do we avoid these overlapping with Sync?
     * * Pickling. How do we avoid pickling twice if you use both Sync and RL?
     */

    /*
     * TODO:
     * * Auth.
     * * Server URI lookup.
     * * Syncing.
     * * Error handling.
     * * Backoff and retry-after.
     * * Sync scheduling.
     * * Forcing syncs/interactive use.
     */
  }


  private ContentProviderClient getContentProviderClient(Context context) {
    final ContentResolver contentResolver = context.getContentResolver();
    final ContentProviderClient client = contentResolver.acquireContentProviderClient(ReadingListItems.CONTENT_URI);
    return client;
  }
}
