package org.mozilla.gecko.sync.repositories;

import org.mozilla.gecko.sync.ExtendedJSONObject;

public class RepositorySessionBundle extends ExtendedJSONObject {

  public RepositorySessionBundle() {
    super();
  }

  public RepositorySessionBundle(long lastSyncTimestamp) {
    this();
    this.setTimestamp(lastSyncTimestamp);
  }

  public long getTimestamp() {
    if (this.containsKey("timestamp")) {
      return this.getLong("timestamp");
    }
    return -1;
  }

  public void setTimestamp(long timestamp) {
    this.put("timestamp", new Long(timestamp));
  }

  public void bumpTimestamp(long timestamp) {
    if (timestamp > this.getTimestamp()) {
      this.setTimestamp(timestamp);
    }
  }
}
