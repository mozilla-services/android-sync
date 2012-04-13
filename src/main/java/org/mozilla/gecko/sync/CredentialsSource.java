/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync;

import org.mozilla.gecko.sync.crypto.KeyBundle;

public interface CredentialsSource {

  public abstract String credentials();
  public abstract CollectionKeys getCollectionKeys();
  public abstract KeyBundle keyForCollection(String collection) throws NoCollectionKeysSetException;
}
