package org.mozilla.gecko.sync.delegates;

public interface ClientsDataDelegate {
  public String getPersistedAccountGUID();
  public void setPersistedAccountGUID(String guid);
  public String getPersistedClientName();
  public void setPersistedClientName(String clientName);
}
