/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.net;

import org.mozilla.gecko.sync.CryptoRecord;

/**
 * Subclass this to handle collection fetches.
 */
public abstract class WBOCollectionRequestDelegate
    extends SyncServer11CollectionRequestDelegate {

  /**
   * Override this to handle an individual Sync record.
   * <p>
   * Each Sync record is as it comes from the server, which (usually) means it
   * is encrypted.
   *
   * @param record
   *          CryptoRecord from the server.
   */
  public abstract void handleWBO(CryptoRecord record);

  @Override
  public void handleRequestProgress(String progress) throws Exception {
    CryptoRecord record = CryptoRecord.fromJSONRecord(progress);
    this.handleWBO(record);
  }
}
