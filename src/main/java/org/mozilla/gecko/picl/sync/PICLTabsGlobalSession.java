/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.picl.sync;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.mozilla.gecko.background.common.log.Logger;

/**
 * High-level interface to sync tabs.
 */
public class PICLTabsGlobalSession {
  public final static String LOG_TAG = PICLTabsGlobalSession.class.getSimpleName();

  protected final PICLConfig config;

  public PICLTabsGlobalSession(PICLConfig config) {
    if (config == null) {
      throw new IllegalArgumentException("config must not be null");
    }
    this.config = config;
  }

  protected PICLServerSyncStageDelegate makeDelegate(final CountDownLatch latch) {
    if (latch == null) {
      throw new IllegalArgumentException("latch must not be null");
    }

    return new PICLServerSyncStageDelegate() {
      @Override
      public void handleSuccess() {
        Logger.info(LOG_TAG, "Successfully pickled tabs.");
        latch.countDown();
      }

      @Override
      public void handleError(Exception e) {
        Logger.warn(LOG_TAG, "Got exception pickling tabs.", e);
        latch.countDown();
      }
    };
  }

  /**
   * Public API to start syncing tabs and wait for completion.
   */
  public void syncTabs() {
    final CountDownLatch latch = new CountDownLatch(1);

    final PICLTabsServerSyncStage tabsSyncStage = new PICLTabsServerSyncStage(config, makeDelegate(latch));

    config.executor.execute(new Runnable() {
      @Override
      public void run() {
        try {
          tabsSyncStage.execute();
        } catch (Exception e) {
          // Don't let an uncaught exception on a background thread bring us down.
          Logger.warn(LOG_TAG, "Got exception pickling tabs.", e);
          latch.countDown();
        }
      }
    });

    try {
      latch.await((4 * 60 + 30) * 1000, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Logger.warn(LOG_TAG, "Got interrupted while pickling tabs.", e);
    }
  }
}
