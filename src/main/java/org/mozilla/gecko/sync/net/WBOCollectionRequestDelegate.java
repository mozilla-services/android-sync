/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.net;

import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.CryptoRecord;

/**
 * Subclass this to handle collection fetches.
 * @author rnewman
 *
 */
public abstract class WBOCollectionRequestDelegate extends
    SyncStorageCollectionRequestDelegate {

  public abstract void handleWBO(CryptoRecord record);
  public abstract KeyBundle keyBundle();

  @Override
  public void handleRequestProgress(String progress) {
    try {
      CryptoRecord record = CryptoRecord.fromJSONRecord(progress);
      record.keyBundle = this.keyBundle();
      this.handleWBO(record);
    } catch (Exception e) {
      this.handleRequestError(e);
      // TODO: abort?! Allow exception to propagate to fail?
    }
  }
}
