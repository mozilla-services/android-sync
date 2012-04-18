/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.stage;

/**
 * This is simply a stage that is not responsible for synchronizing repositories.
 */
public abstract class AbstractNonRepositorySyncStage implements GlobalSyncStage {
  @Override
  public void resetLocal() {
    // Do nothing.
  }
}
