/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.picl.sync;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.mozilla.gecko.background.common.log.Logger;
import org.mozilla.gecko.picl.sync.stage.PICLBookmarksServerSyncStage;
import org.mozilla.gecko.picl.sync.stage.PICLHistoryServerSyncStage;
import org.mozilla.gecko.picl.sync.stage.PICLPasswordsServerSyncStage;
import org.mozilla.gecko.picl.sync.stage.PICLServerSyncStage;
import org.mozilla.gecko.picl.sync.stage.PICLServerSyncStageDelegate;
import org.mozilla.gecko.picl.sync.stage.PICLTabsServerSyncStage;

/**
 * An instance of the act of syncing to a PICL server.
 */
public class PICLGlobalSession {
  public final static String LOG_TAG = PICLGlobalSession.class.getSimpleName();
  
  protected final PICLConfig config;
  
  protected LinkedList<PICLServerSyncStage> stages;
  protected PICLServerSyncStage currentStage;
  
  //protected LinkedList<PICLServerSyncStage> stages;
  protected CountDownLatch latch;
  protected final PICLServerSyncStageDelegate stageDelegate = new PICLServerSyncStageDelegate() {
    @Override
    public void handleSuccess() {
      Logger.info(LOG_TAG, "Successfully pickled stage:" + currentStage.name());
      latch.countDown();
      advance();
    }

    @Override
    public void handleError(Exception e) {
      Logger.warn(LOG_TAG, "Got exception pickling stage: " + currentStage.name(), e);
      latch.countDown();
      advance();
    }
  };

  public PICLGlobalSession(PICLConfig config) {
    if (config == null) {
      throw new IllegalArgumentException("config must not be null");
    }
    this.config = config;
  }


  /**
   * Public API to start syncing this PICL session.
   */
  public void sync() {
    setupStages();
    latch = new CountDownLatch(stages.size());
    advance();

    // we block until all stages are done, so that onPerformSync doesn't finish
    // early.
    // each stage will countdown our latch
    try {
      latch.await((4 * 60 + 30) * 1000, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Logger.warn(LOG_TAG, "Got interrupted while pickling stage:"
          + currentStage.name(), e);
    }
    currentStage = null;
  }
  
  protected void setupStages() {
    stages = new LinkedList<PICLServerSyncStage>();
    stages.add(new PICLTabsServerSyncStage(config, stageDelegate));
    stages.add(new PICLPasswordsServerSyncStage(config, stageDelegate));
    stages.add(new PICLBookmarksServerSyncStage(config, stageDelegate));
    stages.add(new PICLHistoryServerSyncStage(config, stageDelegate));
  }
  
  
  protected void advance() {
    final PICLServerSyncStage syncStage = stages.poll();
	  if (syncStage != null) {
	    currentStage = syncStage;
	    
	    config.executor.execute(new Runnable() {
	      @Override
	      public void run() {
	        try {
	          syncStage.execute();
	        } catch (Exception e) {
	          // Don't let an uncaught exception on a background thread bring us down.
	          Logger.warn(LOG_TAG, "Got exception pickling " + currentStage.name(), e);
	          latch.countDown();
	        }
	      }
	    });
	  }
  }

}
