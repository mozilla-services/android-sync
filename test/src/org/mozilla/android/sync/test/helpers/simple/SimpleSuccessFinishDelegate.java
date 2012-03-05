/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.android.sync.test.helpers.simple;

import java.util.concurrent.ExecutorService;

import org.mozilla.android.sync.test.helpers.DefaultDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionFinishDelegate;

public abstract class SimpleSuccessFinishDelegate extends DefaultDelegate implements RepositorySessionFinishDelegate {
  @Override
  public void onFinishFailed(Exception ex) {
    performNotify("Finish failed", ex);
  }

  @Override
  public RepositorySessionFinishDelegate deferredFinishDelegate(ExecutorService executor) {
    return this;
  }
}
