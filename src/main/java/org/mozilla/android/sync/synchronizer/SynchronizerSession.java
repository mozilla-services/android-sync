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

package org.mozilla.android.sync.synchronizer;

import org.mozilla.android.sync.repositories.RepositorySession;
import org.mozilla.android.sync.repositories.RepositorySessionBundle;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionBeginDelegate;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionCreationDelegate;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionFinishDelegate;

import android.content.Context;
import android.util.Log;

public class SynchronizerSession implements
RecordsChannelDelegate,
RepositorySessionBeginDelegate,
RepositorySessionCreationDelegate,
RepositorySessionFinishDelegate {

  private RepositorySession sessionA;
  private RepositorySession sessionB;

  private Synchronizer synchronizer;
  private SynchronizerSessionDelegate delegate;

  /**
   * Pulls records from `source`, applying them to `sink`.
   * Notifies its delegate of errors and completion.
   *
   * @author rnewman
   *
   */
  protected class RecordsChannel {
    public RepositorySession source;
    public RepositorySession sink;
    public void flow(RecordsChannelDelegate delegate) {

    }
  }

  // TODO: bundle in and out.

  /*
   * Public API: constructor, init, synchronize.
   */
  public SynchronizerSession(Synchronizer synchronizer, SynchronizerSessionDelegate delegate) {
    this.synchronizer = synchronizer;
    this.delegate     = delegate;
  }

  public void init() {
    // Begin sessionA and sessionB, call onInitialized in callbacks.
    // TODO TODO TODO: createSession shouldn't take a timestamp.
    this.synchronizer.repositoryA.createSession(this, this.getContext());
  }

  /**
   * Please don't call this until you've been notified with onInitialized.
   */
  public void synchronize() {
    // TODO: synchronize the two sessions in each direction, call onSynchronized in callbacks.
    this.delegate.onSynchronized(this);
  }


  // TODO: return a real context.
  protected Context getContext() {
    return null;
  }


  @Override
  public void onFlowCompleted() {
    // TODO Auto-generated method stub

  }


  /*
   * RepositorySessionBeginDelegate methods.
   */
  @Override
  public void onBeginFailed(Exception ex) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onBeginSucceeded(RepositorySession session) {
    if (session == this.sessionA) {
      this.sessionB.begin(this);
      return;
    }
    if (session == this.sessionB) {
      // Great!
      // TODO
      return;
    }
    this.delegate.onSessionError(new UnexpectedSessionException(session));
  }

  /*
   * RepositorySessionCreationDelegate methods.
   */
  @Override
  public void onSessionCreateFailed(Exception ex) {
    // Attempt to finish the first session, if the second is the one that failed.
    if (this.sessionA != null) {
      try {
        this.sessionA.finish(this);
      } catch (Exception e) {
        // Never mind; best-effort finish.
      }
    }
    this.delegate.onSessionError(ex);
  }

  @Override
  public void onSessionCreated(RepositorySession session) {
    if (session == null ||
        this.sessionA == session) {
      this.delegate.onSessionError(new UnexpectedSessionException(session));
      return;
    }
    if (this.sessionA == null) {
      this.sessionA = session;
      this.synchronizer.repositoryB.createSession(this, this.getContext());
      return;
    }
    if (this.sessionB == null) {
      this.sessionB = session;
      this.delegate.onInitialized(this);
      return;
    }
    // TODO: need a way to make sure we don't call any more delegate methods.
    this.delegate.onSessionError(new UnexpectedSessionException(session));
  }

  /*
   * RepositorySessionFinishDelegate methods.
   */
  @Override
  public void onFinishFailed(Exception ex) {
    if (this.sessionB == null) {
      // Ah, it was a problem cleaning up. Never mind.
      Log.w("rnewman", "Got exception cleaning up first after second session creation failed.", ex);
      return;
    }
    // TODO
  }

  @Override
  public void onFinishSucceeded(RepositorySessionBundle bundle) {
    if (this.sessionB == null) {
      this.sessionA = null;       // We're done.
      // TODO: persist bundle.
    }
  }

}
