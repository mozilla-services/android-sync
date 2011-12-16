package org.mozilla.gecko.sync.repositories.domain;

import org.mozilla.gecko.sync.CryptoRecord;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.repositories.android.RepoUtils;

public class PasswordRecord extends Record {

  public static final String COLLECTION_NAME = "passwords";

  public PasswordRecord(String guid, String collection, long lastModified,
      boolean deleted) {
    super(guid, collection, lastModified, deleted);
  }
  public PasswordRecord(String guid, String collection, long lastModified) {
    super(guid, collection, lastModified, false);
  }
  public PasswordRecord(String guid, String collection) {
    super(guid, collection, 0, false);
  }
  public PasswordRecord(String guid) {
    super(guid, COLLECTION_NAME, 0, false);
  }
  public PasswordRecord() {
    super(Utils.generateGuid(), COLLECTION_NAME, 0, false);
  }

  public String hostname;
  public String formSubmitURL;
  public String httpRealm;
  // TODO these are encrypted in the passwords content provider,
  // need to figure out what we need to do here.
  public String username;
  public String password;
  public String usernameField;
  public String passwordField;
  public String encType;
  public long   timeLastUsed;
  public long   timesUsed;

  @Override
  public void initFromPayload(CryptoRecord payload) {
    // TODO Auto-generated method stub

  }
  @Override
  public CryptoRecord getPayload() {
    // TODO Auto-generated method stub
    return null;
  }
  
  @Override
  public boolean equals(Object o) {
    if (!o.getClass().equals(PasswordRecord.class)) return false;
    PasswordRecord other = (PasswordRecord) o;
    if (!super.equals(other)) return false;
    return RepoUtils.stringsEqual(this.hostname, other.hostname)
        && RepoUtils.stringsEqual(this.formSubmitURL, other.formSubmitURL)
        && RepoUtils.stringsEqual(this.httpRealm, other.httpRealm)
        && RepoUtils.stringsEqual(this.username, other.username)
        && RepoUtils.stringsEqual(this.password, other.password)
        && RepoUtils.stringsEqual(this.usernameField, other.usernameField)
        && RepoUtils.stringsEqual(this.passwordField, other.passwordField)
        && RepoUtils.stringsEqual(this.encType, other.encType)
        && (this.timeLastUsed == other.timeLastUsed)
        && (this.timesUsed == other.timesUsed);
  }

}
