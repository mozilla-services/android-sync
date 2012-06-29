/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.synchronizer;

/**
 * A <code>Synchronizer</code> that downloads fresh GUIDs and then downloads
 * records in batches.
 */
public class FillingServerLocalSynchronizer extends ServerLocalSynchronizer {
  protected final FillingGuidsManager manager;

  public FillingServerLocalSynchronizer(final FillingGuidsManager manager) {
    super();
    this.manager = manager;
  }

  public SynchronizerSession getSynchronizerSession() {
    return new FillingServerLocalSynchronizerSession(this, this, manager);
  }
}
