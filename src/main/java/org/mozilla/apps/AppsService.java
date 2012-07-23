/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.apps;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Service to handle syncing Apps for a Mozilla Persona account. This is invoked
 * with an intent with action ACTION_AUTHENTICATOR_INTENT. It instantiates a
 * <code>AppsSyncAdaptor</code> and returns its IBinder.
 */
public class AppsService extends Service {
  protected static AppsSyncAdapter syncAdapter = null;

  @Override
  public synchronized void onCreate() {
    if (syncAdapter == null) {
      syncAdapter = new AppsSyncAdapter(getApplicationContext(), true);
    }
  }

  @Override
  public IBinder onBind(Intent intent) {
    return syncAdapter.getSyncAdapterBinder();
  }
}
