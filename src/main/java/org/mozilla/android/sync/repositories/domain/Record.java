package org.mozilla.android.sync.repositories.domain;

public abstract class Record {

  private String guid;
  private long lastModTime;

  public String getGuid() {
    return guid;
  }
  public void setGuid(String guid) {
    this.guid = guid;
  }
  public long getLastModTime() {
    return lastModTime;
  }
  public void setLastModTime(long lastModTime) {
    this.lastModTime = lastModTime;
  }

}
