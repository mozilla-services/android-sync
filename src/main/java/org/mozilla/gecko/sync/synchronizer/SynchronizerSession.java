/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Android Sync Client.
 *
 * The Initial Developer of the Original Code is
 * the Mozilla Foundation.
 * Portions created by the Initial Developer are Copyright (C) 2011
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *   Richard Newman <rnewman@mozilla.com>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

package org.mozilla.gecko.sync.synchronizer;


import java.util.concurrent.ExecutorService;

import org.mozilla.gecko.sync.repositories.RepositorySession;
import org.mozilla.gecko.sync.repositories.RepositorySessionBundle;
import org.mozilla.gecko.sync.repositories.delegates.DeferrableRepositorySessionCreationDelegate;
import org.mozilla.gecko.sync.repositories.delegates.DeferredRepositorySessionFinishDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionFinishDelegate;

import android.content.Context;
import android.util.Log;

/**
 * I coordinate the moving parts of a sync started by
 * {@link Synchronizer#synchronize}.
 *
 * I flow records twice: first from A to B, and then from B to A. I provide
 * fine-grained feedback by calling my delegate's callback methods.
 *
 * Initialize me by creating me with a Synchronizer and a
 * SynchronizerSessionDelegate. Kick things off by calling `init` with two
 * RepositorySessionBundles, and then call `synchronize` in your `onInitialized`
 * callback.
 *
 * I always call exactly one of my delegate's `onInitialized` or
 * `onSessionError` callback methods from `init`.
 *
 * I call my delegate's `onSynchronizeSkipped` callback method if there is no
 * data to be synchronized in `synchronize`.
 *
 * In addition, I call `onFetchError`, `onStoreError`, and `onSessionError` when
 * I encounter a fetch, store, or session error while synchronizing.
 *
 * Typically my delegate will call `abort` in its error callbacks, which will
 * call my delegate's `onSynchronizeAborted` method and halt the sync.
 *
 * I always call exactly one of my delegate's `onSynchronized` or
 * `onSynchronizeFailed` callback methods if I have not seen an error.
 */
public class SynchronizerSession
extends DeferrableRepositorySessionCreationDelegate
implements RecordsChannelDelegate,
           RepositorySessionFinishDelegate {

  protected static final String LOG_TAG = "SynchronizerSession";
  private Synchronizer synchronizer;
  private SynchronizerSessionDelegate delegate;
  private Context context;

  /*
   * Computed during init.
   */
  private RepositorySession sessionA;
  private RepositorySession sessionB;
  private RepositorySessionBundle bundleA;
  private RepositorySessionBundle bundleB;

  // Bug 726054: just like desktop, we track our last interaction with the server,
  // not the last record timestamp that we fetched. This ensures that we don't re-
  // download the records we just uploaded, at the cost of skipping any records
  // that a concurrently syncing client has uploaded.
  private long pendingATimestamp = -1;
  private long pendingBTimestamp = -1;
  private long storeEndATimestamp = -1;
  private long storeEndBTimestamp = -1;
  private boolean flowAToBCompleted = false;
  private boolean flowBToACompleted = false;

  private static void warn(String msg, Exception e) {
    System.out.println("WARN: " + msg);
    e.printStackTrace(System.err);
    Log.w(LOG_TAG, msg, e);
  }
  private static void warn(String msg) {
    System.out.println("WARN: " + msg);
    Log.w(LOG_TAG, msg);
  }
  private static void info(String msg) {
    System.out.println("INFO: " + msg);
    Log.i(LOG_TAG, msg);
  }

  /*
   * Public API: constructor, init, synchronize.
   */
  public SynchronizerSession(Synchronizer synchronizer, SynchronizerSessionDelegate delegate) {
    this.setSynchronizer(synchronizer);
    this.delegate = delegate;
  }

  public Synchronizer getSynchronizer() {
    return synchronizer;
  }

  public void setSynchronizer(Synchronizer synchronizer) {
    this.synchronizer = synchronizer;
  }

  public void init(Context context, RepositorySessionBundle bundleA, RepositorySessionBundle bundleB) {
    this.context = context;
    this.bundleA = bundleA;
    this.bundleB = bundleB;
    // Begin sessionA and sessionB, call onInitialized in callbacks.
    this.getSynchronizer().repositoryA.createSession(this, context);
  }

  // TODO: implement aborting.
  public void abort() {
    this.delegate.onSynchronizeAborted(this);
  }

  /**
   * Please don't call this until you've been notified with onInitialized.
   */
  public void synchronize() {
    // First thing: decide whether we should.
    if (!sessionA.dataAvailable() &&
        !sessionB.dataAvailable()) {
      info("Neither session reports data available. Short-circuiting sync.");
      sessionA.abort();
      sessionB.abort();
      this.delegate.onSynchronizeSkipped(this);
      return;
    }

    final SynchronizerSession session = this;

    // TODO: failed record handling.

    // This is the *second* record channel to flow.
    // I, SynchronizerSession, am the delegate for the *second* flow.
    final RecordsChannel channelBToA = new RecordsChannel(this.sessionB, this.sessionA, this);

    // This is the delegate for the *first* flow.
    RecordsChannelDelegate channelAToBDelegate = new RecordsChannelDelegate() {
      public void onFlowCompleted(RecordsChannel recordsChannel, long fetchEnd, long storeEnd) {
        info("First RecordsChannel onFlowCompleted. Fetch end is " + fetchEnd +
             ". Store end is " + storeEnd + ". Starting next.");
        pendingATimestamp = fetchEnd;
        storeEndBTimestamp = storeEnd;
        flowAToBCompleted = true;
        channelBToA.flow();
      }

      @Override
      public void onFlowBeginFailed(RecordsChannel recordsChannel, Exception ex) {
        warn("First RecordsChannel onFlowBeginFailed. Reporting session error.", ex);
        session.delegate.onSessionError(ex);
      }

      @Override
      public void onFlowFetchFailed(RecordsChannel recordsChannel, Exception ex) {
        // TODO: clean up, tear down, abort.
        warn("First RecordsChannel onFlowFetchFailed. Reporting store error.", ex);
        session.delegate.onFetchError(ex);
      }

      @Override
      public void onFlowStoreFailed(RecordsChannel recordsChannel, Exception ex) {
        // TODO: clean up, tear down, abort.
        warn("First RecordsChannel onFlowStoreFailed. Reporting store error.", ex);
        session.delegate.onStoreError(ex);
      }

      @Override
      public void onFlowFinishFailed(RecordsChannel recordsChannel, Exception ex) {
        warn("First RecordsChannel onFlowFinishedFailed. Reporting store error.", ex);
        session.delegate.onStoreError(ex);
      }
    };

    // This is the *first* channel to flow.
    final RecordsChannel channelAToB = new RecordsChannel(this.sessionA, this.sessionB, channelAToBDelegate);

    info("Starting A to B flow. Channel is " + channelAToB);
    channelAToB.beginAndFlow();
  }

  @Override
  public void onFlowCompleted(RecordsChannel channel, long fetchEnd, long storeEnd) {
    info("Second RecordsChannel onFlowCompleted. Fetch end is " + fetchEnd +
         ". Store end is " + storeEnd + ". Finishing.");

    pendingBTimestamp = fetchEnd;
    storeEndATimestamp = storeEnd;
    flowBToACompleted = true;

    // Finish the two sessions.
    this.sessionA.finish(this);
  }

  @Override
  public void onFlowBeginFailed(RecordsChannel recordsChannel, Exception ex) {
    warn("Second RecordsChannel onFlowBeginFailed. Not reporting error.", ex);
  }

  @Override
  public void onFlowFetchFailed(RecordsChannel recordsChannel, Exception ex) {
    // TODO Auto-generated method stub
    warn("Second RecordsChannel onFlowFetchFailed. Not reporting error.", ex);
  }

  @Override
  public void onFlowStoreFailed(RecordsChannel recordsChannel, Exception ex) {
    // TODO Auto-generated method stub
    warn("Second RecordsChannel onFlowStoreFailed. Not reporting error.", ex);
  }

  @Override
  public void onFlowFinishFailed(RecordsChannel recordsChannel, Exception ex) {
    // TODO Auto-generated method stub
    warn("Second RecordsChannel onFlowFinishFailed. Not reporting error.", ex);
  }

  /*
   * RepositorySessionCreationDelegate methods.
   */

  /**
   * I could be called twice: once for sessionA and once for sessionB.
   *
   * I try to clean up sessionA if it is not null, since it is create sessionB
   * that failed.
   */
  @Override
  public void onSessionCreateFailed(Exception ex) {
    // Attempt to finish the first session, if the second is the one that failed.
    if (this.sessionA != null) {
      try {
        // We no longer need a reference to our context.
        this.context = null;
        this.sessionA.finish(this);
      } catch (Exception e) {
        // Never mind; best-effort finish.
      }
    }
    // We no longer need a reference to our context.
    this.context = null;
    this.delegate.onSessionError(ex);
  }

  /**
   * I should be called twice: first for sessionA and second for sessionB.
   *
   * If I am called for sessionB, I call my delegate's `onInitialized` callback
   * method because my repository sessions are correctly initialized.
   */
  // TODO: some of this "finish and clean up" code can be refactored out.
  @Override
  public void onSessionCreated(RepositorySession session) {
    if (session == null ||
        this.sessionA == session) {
      // TODO: clean up sessionA.
      this.delegate.onSessionError(new UnexpectedSessionException(session));
      return;
    }
    if (this.sessionA == null) {
      this.sessionA = session;

      // Unbundle.
      try {
        this.sessionA.unbundle(this.bundleA);
      } catch (Exception e) {
        this.delegate.onSessionError(new UnbundleError(e, sessionA));
        // TODO: abort
        return;
      }
      this.getSynchronizer().repositoryB.createSession(this, this.context);
      return;
    }
    if (this.sessionB == null) {
      this.sessionB = session;
      // We no longer need a reference to our context.
      this.context = null;

      // Unbundle. We unbundled sessionA when that session was created.
      try {
        this.sessionB.unbundle(this.bundleB);
      } catch (Exception e) {
        // TODO: abort
        this.delegate.onSessionError(new UnbundleError(e, sessionB));
        return;
      }

      this.delegate.onInitialized(this);
      return;
    }
    // TODO: need a way to make sure we don't call any more delegate methods.
    this.delegate.onSessionError(new UnexpectedSessionException(session));
  }

  /*
   * RepositorySessionFinishDelegate methods.
   */

  /**
   * I could be called twice: once for sessionA and once for sessionB.
   *
   * If sessionB couldn't be created, I don't fail again.
   */
  @Override
  public void onFinishFailed(Exception ex) {
    if (this.sessionB == null) {
      // Ah, it was a problem cleaning up. Never mind.
      warn("Got exception cleaning up first after second session creation failed.", ex);
      return;
    }
    String session = (this.sessionA == null) ? "B" : "A";
    this.delegate.onSynchronizeFailed(this, ex, "Finish of session " + session + " failed.");
  }

  /**
   * I should be called twice: first for sessionA and second for sessionB.
   *
   * If I am called for sessionA, I try to finish sessionB.
   *
   * If I am called for sessionB, I call my delegate's `onSynchronized` callback
   * method because my flows should have completed.
   */
  @Override
  public void onFinishSucceeded(RepositorySession session,
                                RepositorySessionBundle bundle) {
    info("onFinishSucceeded. Flows? " +
         flowAToBCompleted + ", " + flowBToACompleted);

    if (session == sessionA) {
      if (flowAToBCompleted) {
        info("onFinishSucceeded: bumping session A's timestamp to " + pendingATimestamp + " or " + storeEndATimestamp);
        bundle.bumpTimestamp(Math.max(pendingATimestamp, storeEndATimestamp));
        this.synchronizer.bundleA = bundle;
      } else {
        // Should not happen!
        this.delegate.onSessionError(new UnexpectedSessionException(sessionA));
      }
      if (this.sessionB != null) {
        info("Finishing session B.");
        // On to the next.
        this.sessionB.finish(this);
      }
    } else if (session == sessionB) {
      if (flowBToACompleted) {
        info("onFinishSucceeded: bumping session B's timestamp to " + pendingBTimestamp + " or " + storeEndBTimestamp);
        bundle.bumpTimestamp(Math.max(pendingBTimestamp, storeEndBTimestamp));
        this.synchronizer.bundleB = bundle;
        info("Notifying delegate.onSynchronized.");
        this.delegate.onSynchronized(this);
      } else {
        // Should not happen!
        this.delegate.onSessionError(new UnexpectedSessionException(sessionB));
      }
    } else {
      // TODO: hurrrrrr...
    }

    if (this.sessionB == null) {
      this.sessionA = null; // We're done.
    }
  }

  @Override
  public RepositorySessionFinishDelegate deferredFinishDelegate(final ExecutorService executor) {
    return new DeferredRepositorySessionFinishDelegate(this, executor);
  }
}
