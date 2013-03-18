/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.picl.sync.stage;

import org.mozilla.gecko.picl.sync.PICLConfig;
import org.mozilla.gecko.sync.repositories.Repository;
import org.mozilla.gecko.sync.synchronizer.ServerLocalSynchronizer;
import org.mozilla.gecko.sync.synchronizer.Synchronizer;
import org.mozilla.gecko.sync.synchronizer.SynchronizerDelegate;

public abstract class PICLServerSyncStage implements SynchronizerDelegate {
  public static final String LOG_TAG = PICLServerSyncStage.class.getSimpleName();

  public final PICLConfig config;

  protected final PICLServerSyncStageDelegate delegate;

  protected Synchronizer synchronizer;

  public PICLServerSyncStage(PICLConfig config, PICLServerSyncStageDelegate delegate) {
    this.config = config;
    this.delegate = delegate;
  }

  /**
   * Create the local <code>Repository</code> instance.
   *
   * @return <code>Repository</code> instance.
   */
  protected abstract Repository makeLocalRepository();

  /**
   * Create the remote <code>Repository</code> instance.
   *
   * @return <code>Repository</code> instance.
   */
  protected abstract Repository makeRemoteRepository();
  
  
  /**
   * Return the name of this stage.
   *
   * @return <code>String</code> name.
   */
  public abstract String name();

  protected Synchronizer makeSynchronizer() {
    Synchronizer synchronizer = new ServerLocalSynchronizer();
    synchronizer.repositoryA = makeRemoteRepository();
    synchronizer.repositoryB = makeLocalRepository();
    return synchronizer;
  }

  public void execute() {
    this.synchronizer = makeSynchronizer();

    synchronizer.synchronize(config.getAndroidContext(), this);
  }

  @Override
  public void onSynchronized(Synchronizer synchronizer) {
    delegate.handleSuccess();
  }

  @Override
  public void onSynchronizeFailed(Synchronizer synchronizer, Exception lastException, String reason) {
    delegate.handleError(lastException);
  }
}
