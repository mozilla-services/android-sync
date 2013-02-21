/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.picl;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class PICLSyncService extends Service {
  private static final Object syncAdapterLock = new Object();
  private static PICLSyncAdapter syncAdapter = null;

  @Override
  public void onCreate() {
    synchronized (syncAdapterLock) {
      if (syncAdapter == null) {
        syncAdapter = new PICLSyncAdapter(getApplicationContext(), true);
      }
    }
  }

  @Override
  public IBinder onBind(Intent intent) {
    return syncAdapter.getSyncAdapterBinder();
  }
}
