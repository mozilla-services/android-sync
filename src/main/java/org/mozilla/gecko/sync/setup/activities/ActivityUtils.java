/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.setup.activities;

import org.mozilla.gecko.sync.setup.InvalidSyncKeyException;

public class ActivityUtils {
  /**
   * Sync key should be a 26-character string, and can include arbitrary capitalization and hyphenation. 
   * 
   * @param key
   *        Sync key
   * @return true if sync key is of the correct format, false otherwise.
   */
  public static String validateSyncKey(String key) throws InvalidSyncKeyException {
    String charKey = key.trim().replace("-", "").toLowerCase();
    if (!charKey.matches("^[abcdefghijkmnpqrstuvwxyz23456789]{26}$")) {
      throw new InvalidSyncKeyException();
    }
    return charKey;
  }
}
