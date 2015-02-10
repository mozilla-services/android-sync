/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.reading;

import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.mozilla.gecko.background.common.log.Logger;
import org.mozilla.gecko.background.fxa.FxAccountClient;
import org.mozilla.gecko.background.fxa.FxAccountClient20;
import org.mozilla.gecko.browserid.BrowserIDKeyPair;
import org.mozilla.gecko.browserid.JSONWebTokenUtils;
import org.mozilla.gecko.fxa.authenticator.AndroidFxAccount;
import org.mozilla.gecko.fxa.login.FxAccountLoginStateMachine;
import org.mozilla.gecko.fxa.login.FxAccountLoginStateMachine.LoginStateMachineDelegate;
import org.mozilla.gecko.fxa.login.FxAccountLoginTransition.Transition;
import org.mozilla.gecko.fxa.login.Married;
import org.mozilla.gecko.fxa.login.State;
import org.mozilla.gecko.fxa.login.State.StateLabel;
import org.mozilla.gecko.fxa.login.StateFactory;
import org.mozilla.gecko.fxa.sync.FxAccountSyncDelegate;

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

    protected final ExecutorService executor;

    public ReadingListSyncAdapter(Context context, boolean autoInitialize) {
      super(context, autoInitialize);
      this.executor = Executors.newSingleThreadExecutor();
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
              syncWithAssertion(audience, assertion, sharedPrefs, extras);
            } catch (Exception e) {
              syncDelegate.handleError(e);
              return;
            }
          }

          private void syncWithAssertion(String audience, String assertion,
                                         SharedPreferences sharedPrefs,
                                         Bundle extras) {
            Logger.info(LOG_TAG, "syncWithAssertion. Nowt to do!");

            // TODO: backoffs, and everything else handled by a SessionCallback.
            syncDelegate.handleSuccess();
          }
        });

        latch.await();
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
}
