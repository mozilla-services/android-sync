package org.mozilla.gecko.sync;

import org.mozilla.gecko.sync.crypto.KeyBundle;

public interface CredentialsSource {

  public abstract String credentials();
  public abstract CollectionKeys getCollectionKeys();
  public abstract KeyBundle keyForCollection(String collection) throws NoCollectionKeysSetException;
}
