/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.background.announcements;

import org.mozilla.gecko.background.BackgroundService;
import org.mozilla.gecko.background.common.log.Logger;

import android.content.Intent;
import android.net.ConnectivityManager;

/**
 * An abstract class which listens for connectivity events, using those to
 * control its activity.
 */
public abstract class NetworkDependentService extends BackgroundService {
  private static final String LOG_TAG = "FxNetworkService";

  protected NetworkDependentService() {
    super();
  }

  protected NetworkDependentService(String threadName) {
    super(threadName);
  }

  protected abstract void onNoConnectivity();
  protected abstract void onConnectivity(Intent intent);

  @Override
  protected void onHandleIntent(Intent intent) {
    final String action = intent.getAction();
    if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
      if (intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false)) {
        this.onNoConnectivity();
      } else {
        // TODO: verify whether connectivity is actually available, if Android doesn't
        // correctly include EXTRA_NO_CONNECTIVITY.
        this.onConnectivity(intent);
      }
      return;
    }

    // Failure case. It's OK to log here, because we'll be at the root of the chain.
    Logger.warn(LOG_TAG, "Unknown intent " + action);
  }
}