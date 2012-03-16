/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.stage;

import java.io.IOException;
import java.net.URISyntaxException;

import org.json.simple.parser.ParseException;
import org.mozilla.gecko.sync.GlobalSession;
import org.mozilla.gecko.sync.HTTPFailureException;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.MetaGlobalException;
import org.mozilla.gecko.sync.NoCollectionKeysSetException;
import org.mozilla.gecko.sync.NonObjectJSONException;
import org.mozilla.gecko.sync.SynchronizerConfiguration;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.middleware.Crypto5MiddlewareRepository;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.sync.net.HttpResponseObserver;
import org.mozilla.gecko.sync.net.SyncResponse;
import org.mozilla.gecko.sync.repositories.RecordFactory;
import org.mozilla.gecko.sync.repositories.Repository;
import org.mozilla.gecko.sync.repositories.Server11Repository;
import org.mozilla.gecko.sync.synchronizer.Synchronizer;
import org.mozilla.gecko.sync.synchronizer.SynchronizerDelegate;

import ch.boye.httpclientandroidlib.HttpResponse;

/**
 * Fetch from a server collection into a local repository, encrypting
 * and decrypting along the way.
 *
 * @author rnewman
 *
 */
public abstract class ServerSyncStage implements
    GlobalSyncStage,
    SynchronizerDelegate,
    HttpResponseObserver {

  protected GlobalSession session;
  protected String LOG_TAG = "ServerSyncStage";

  /**
   * Override these in your subclasses.
   *
   * @return true if this stage should be executed.
   * @throws MetaGlobalException
   */
  protected boolean isEnabled() throws MetaGlobalException {
    return session.engineIsEnabled(this.getEngineName());
  }
  protected abstract String getCollection();
  protected abstract String getEngineName();
  protected abstract Repository getLocalRepository();
  protected abstract RecordFactory getRecordFactory();

  // Override this in subclasses.
  protected Repository getRemoteRepository() throws URISyntaxException {
    return new Server11Repository(session.config.getClusterURLString(),
                                  session.config.username,
                                  getCollection(),
                                  session);
  }

  /**
   * Return a Crypto5Middleware-wrapped Server11Repository.
   *
   * @throws NoCollectionKeysSetException
   * @throws URISyntaxException
   */
  protected Repository wrappedServerRepo() throws NoCollectionKeysSetException, URISyntaxException {
    String collection = this.getCollection();
    KeyBundle collectionKey = session.keyForCollection(collection);
    Crypto5MiddlewareRepository cryptoRepo = new Crypto5MiddlewareRepository(getRemoteRepository(), collectionKey);
    cryptoRepo.recordFactory = getRecordFactory();
    return cryptoRepo;
  }

  protected String bundlePrefix() {
    return this.getCollection() + ".";
  }

  public Synchronizer getConfiguredSynchronizer(GlobalSession session) throws NoCollectionKeysSetException, URISyntaxException, NonObjectJSONException, IOException, ParseException {
    Repository remote = wrappedServerRepo();

    Synchronizer synchronizer = new Synchronizer();
    synchronizer.repositoryA = remote;
    synchronizer.repositoryB = this.getLocalRepository();

    SynchronizerConfiguration config = new SynchronizerConfiguration(session.config.getBranch(bundlePrefix()));
    synchronizer.load(config);

    // TODO: should wipe in either direction?
    // TODO: syncID?!
    return synchronizer;
  }

  @Override
  public void execute(GlobalSession session) throws NoSuchStageException {
    final String name = getEngineName();
    Logger.debug(LOG_TAG, "Starting execute for " + name);

    this.session = session;
    try {
      if (!this.isEnabled()) {
        Logger.info(LOG_TAG, "Stage " + name + " disabled; skipping.");
        session.advance();
        return;
      }
    } catch (MetaGlobalException e) {
      session.abort(e, "Inappropriate meta/global; refusing to execute " + name + " stage.");
      return;
    }


    Synchronizer synchronizer;
    try {
      synchronizer = this.getConfiguredSynchronizer(session);
    } catch (NoCollectionKeysSetException e) {
      session.abort(e, "No CollectionKeys.");
      return;
    } catch (URISyntaxException e) {
      session.abort(e, "Invalid URI syntax for server repository.");
      return;
    } catch (NonObjectJSONException e) {
      session.abort(e, "Invalid persisted JSON for config.");
      return;
    } catch (IOException e) {
      session.abort(e, "Invalid persisted JSON for config.");
      return;
    } catch (ParseException e) {
      session.abort(e, "Invalid persisted JSON for config.");
      return;
    }

    installAsHttpResponseObserver(); // Uninstalled by SynchronizerDelegate callbacks.

    Logger.debug(LOG_TAG, "Invoking synchronizer.");
    synchronizer.synchronize(session.getContext(), this);
    Logger.debug(LOG_TAG, "Reached end of execute.");
  }

  @Override
  public void onSynchronized(Synchronizer synchronizer) {
    Logger.info(LOG_TAG, "onSynchronized.");

    uninstallAsHttpResponseObserver();

    if (largestBackoffObserved > 0) {
      requestBackoff(largestBackoffObserved);
      if (!continueAfterBackoff(largestBackoffObserved)) {
        return;
      }
    }

    SynchronizerConfiguration synchronizerConfiguration = synchronizer.save();
    if (synchronizerConfiguration != null) {
      synchronizerConfiguration.persist(session.config.getBranch(bundlePrefix()));
    } else {
      Logger.warn(LOG_TAG, "Didn't get configuration from synchronizer after success");
    }

    Logger.info(LOG_TAG, "Advancing session.");
    session.advance();
  }

  @Override
  public void onSynchronizeFailed(Synchronizer synchronizer,
                                  Exception lastException, String reason) {
    Logger.info(LOG_TAG, "onSynchronizeFailed: " + reason);

    uninstallAsHttpResponseObserver();

    if (largestBackoffObserved > 0) {
      requestBackoff(largestBackoffObserved);
    }

    // This failure could be due to a 503 or a 401 and it could have headers.
    if (lastException instanceof HTTPFailureException) {
      session.handleHTTPError(((HTTPFailureException)lastException).response, reason);
    } else {
      session.abort(lastException, reason);
    }
  }

  @Override
  public void onSynchronizeAborted(Synchronizer synchronize) {
    Logger.info(LOG_TAG, "onSynchronizeAborted.");

    uninstallAsHttpResponseObserver();

    if (largestBackoffObserved > 0) {
      requestBackoff(largestBackoffObserved);
    }

    session.abort(null, "Synchronization was aborted.");
  }

  /**
   * The longest backoff observed to date; -1 means no backoff observed.
   */
  protected long largestBackoffObserved = -1;

  /**
   * Override this in subclasses. Called regardless of status (success, failure,
   * or error) if backoff was requested.
   *
   * By default, requestBackoff from callback.
   *
   * @param backoff
   *          The requested backoff in milliseconds.
   */
  public void requestBackoff(long backoff) {
    Logger.info(LOG_TAG, "Requesting backoff of " + backoff + " milliseconds.");
    session.callback.requestBackoff(backoff);
  }

  /**
   * Override this in subclasses. Called after successful synchronization if a
   * backoff was requested to determine whether to continue.
   *
   * By default, abort and return <code>false</code> to not continue.
   *
   * @param backoff
   *          The requested backoff in milliseconds.
   * @return <code>true</code> to advance session and <code>false</code> to not
   *         advance session. Implementor is responsible for aborting, etc!
   */
  protected boolean continueAfterBackoff(long backoff) {
    Logger.info(LOG_TAG, "Not continuing after backoff of " + backoff + " milliseconds requested.");
    session.abort(null, "Aborting due to backoff request.");
    return false;
  }

  /**
   * Reset any observed backoff and start observing HTTP responses for backoff
   * requests.
   */
  protected synchronized void installAsHttpResponseObserver() {
    Logger.debug(LOG_TAG, "Installing " + this + " as BaseResource HttpResponseObserver.");
    BaseResource.setHttpResponseObserver(this);
    largestBackoffObserved = -1;
  }

  /**
   * Stop observing HttpResponses for backoff requests.
   */
  protected synchronized void uninstallAsHttpResponseObserver() {
    Logger.debug(LOG_TAG, "Uninstalling " + this + " as BaseResource HttpResponseObserver.");
    BaseResource.setHttpResponseObserver(null);
  }

  /**
   * Observe all HTTP response for backoff requests on all status codes, not just errors.
   */
  @Override
  public void observeHttpResponse(HttpResponse response) {
    long responseBackoff = (new SyncResponse(response)).totalBackoffInMilliseconds(); // TODO: don't allocate object?
    if (responseBackoff <= 0) {
      return;
    }

    Logger.debug(LOG_TAG, "Observed " + responseBackoff + " millisecond backoff request.");
    synchronized (this) {
      if (responseBackoff > largestBackoffObserved)
        largestBackoffObserved = responseBackoff;
    }
  }
}
