package org.mozilla.gecko.sync;

import org.mozilla.gecko.sync.repositories.RepositorySessionBundle;

public class SynchronizerConfiguration {

  public String syncID;
  public RepositorySessionBundle remoteBundle;
  public RepositorySessionBundle localBundle;

  public SynchronizerConfiguration(String syncID, RepositorySessionBundle remoteBundle, RepositorySessionBundle localBundle) {
    this.syncID       = syncID;
    this.remoteBundle = remoteBundle;
    this.localBundle  = localBundle;
  }

  public String[] toStringValues() {
    String[] out = new String[3];
    out[0] = syncID;
    out[1] = remoteBundle.toJSONString();
    out[2] = localBundle.toJSONString();
    return out;
  }
}
