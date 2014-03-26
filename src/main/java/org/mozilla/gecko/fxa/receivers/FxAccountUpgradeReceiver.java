/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.fxa.receivers;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.mozilla.gecko.background.common.log.Logger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * A receiver that takes action when our Android package is upgraded (replaced).
 */
public class FxAccountUpgradeReceiver extends BroadcastReceiver {
  private static final String LOG_TAG = FxAccountUpgradeReceiver.class.getSimpleName();

  /**
   * Produce a list of Runnable instances to be executed sequentially on
   * upgrade.
   * <p>
   * Each Runnable will be executed sequentially on a background thread. Any
   * unchecked Exception thrown will be caught and ignored.
   *
   * @param context Android context.
   * @return list of Runnable instances.
   */
  protected List<Runnable> onUpgradeRunnables(Context context) {
    List<Runnable> runnables = new LinkedList<Runnable>();
    return runnables;
  }

  @Override
  public void onReceive(final Context context, Intent intent) {
    Logger.setThreadLogTag(FxAccountConstants.GLOBAL_LOG_TAG);
    Logger.info(LOG_TAG, "Upgrade broadcast received.");

    // Iterate Runnable instances one at a time.
    final Executor executor = Executors.newSingleThreadExecutor();
    for (final Runnable runnable : onUpgradeRunnables(context)) {
      executor.execute(new Runnable() {
        @Override
        public void run() {
          try {
            runnable.run();
          } catch (Exception e) {
            // We really don't want to throw on a background thread, so we
            // catch, log, and move on.
            Logger.error(LOG_TAG, "Got exception executing background upgrade Runnable; ignoring.", e);
          }
        }
      });
    }
  }
}
