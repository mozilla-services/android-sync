package org.mozilla.gecko.sync;

public class UnknownSynchronizerConfigurationVersionException extends
    SyncConfigurationException {
  public int badVersion;
  private static final long serialVersionUID = -8497255862099517395L;

  public UnknownSynchronizerConfigurationVersionException(int version) {
    super();
    badVersion = version;
  }
}
