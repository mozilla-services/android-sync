/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.picl.sync;

import java.util.concurrent.ExecutorService;

import android.content.Context;

public class PICLConfig {
  public final Context context;
  public final ExecutorService executor;

  public final String email;
  public final String kA;

  public PICLConfig(Context context, ExecutorService executor, String email, String kA) {
    if (context == null) {
      throw new IllegalArgumentException("context cannot be null");
    }
    if (executor == null) {
      throw new IllegalArgumentException("executor cannot be null");
    }
    if (email == null) {
      throw new IllegalArgumentException("email cannot be null");
    }

    this.context = context;
    this.executor = executor;
    this.email = email;
    this.kA = kA;
  }

  public Context getAndroidContext() {
    return context;
  }
}
