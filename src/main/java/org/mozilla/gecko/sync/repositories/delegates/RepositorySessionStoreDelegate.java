/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.repositories.delegates;

import java.util.concurrent.ExecutorService;

/**
 * These methods *must* be invoked asynchronously. Use deferredStoreDelegate if you
 * need help doing this.
 *
 * @author rnewman
 *
 */
public interface RepositorySessionStoreDelegate {
  /**
   * Called once for each record that fails to store.
   *
   * @param ex error that occurred while storing record.
   * @param guid the GUID of the failing record.
   */
  public void notifyRecordStoreFailed(Exception ex, String guid);

  /**
   * Called once for each record that successfully stores.
   *
   * @param guid the GUID of the stored record.
   */
  public void notifyRecordStoreSucceeded(String guid);

  public void onStoreCompleted(long storeEnd);

  public RepositorySessionStoreDelegate deferredStoreDelegate(ExecutorService executor);
}
