/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */package org.mozilla.android.sync.test.helpers.simple;

import java.util.concurrent.ExecutorService;

import org.mozilla.android.sync.test.AndroidBrowserRepositoryTest;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionBeginDelegate;

public abstract class SimpleSuccessBeginDelegate implements RepositorySessionBeginDelegate {
  @Override
  public void onBeginFailed(Exception ex) {
    AndroidBrowserRepositoryTest.performNotify("Begin failed", ex);
  }

  @Override
  public RepositorySessionBeginDelegate deferredBeginDelegate(ExecutorService executor) {
    return this;
  }
}
