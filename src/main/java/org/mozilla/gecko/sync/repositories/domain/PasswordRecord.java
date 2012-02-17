/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

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
  public Record copyWithIDs(String guid, long androidID) {
    PasswordRecord out = new PasswordRecord(guid, this.collection, this.lastModified, this.deleted);
    out.androidID = androidID;
    out.sortIndex = this.sortIndex;

    // Copy HistoryRecord fields.
    out.hostname      = this.hostname;
    out.formSubmitURL = this.formSubmitURL;
    out.httpRealm     = this.httpRealm;
    out.username      = this.username;
    out.password      = this.password;
    out.usernameField = this.usernameField;
    out.passwordField = this.passwordField;
    out.encType       = this.encType;
    out.timeLastUsed  = this.timeLastUsed;
    out.timesUsed     = this.timesUsed;

    return out;
  }

  @Override
  public void initFromPayload(CryptoRecord payload) {
    // TODO Auto-generated method stub

  }
  @Override
  public CryptoRecord getPayload() {
    // TODO Auto-generated method stub
    // TODO: don't forget to set "id" to our GUID.
    return null;
  }
  
  @Override
  public boolean congruentWith(Object o) {
    if (o == null || !(o instanceof PasswordRecord)) {
      return false;
    }
    PasswordRecord other = (PasswordRecord) o;
    if (!super.congruentWith(other)) {
      return false;
    }
    return RepoUtils.stringsEqual(this.hostname, other.hostname)
        && RepoUtils.stringsEqual(this.formSubmitURL, other.formSubmitURL)
        && RepoUtils.stringsEqual(this.httpRealm, other.httpRealm)
        && RepoUtils.stringsEqual(this.username, other.username)
        && RepoUtils.stringsEqual(this.password, other.password)
        && RepoUtils.stringsEqual(this.usernameField, other.usernameField)
        && RepoUtils.stringsEqual(this.passwordField, other.passwordField)
        && RepoUtils.stringsEqual(this.encType, other.encType);
  }

  @Override
  public boolean equalPayloads(Object o) {
    if (o == null || !(o instanceof PasswordRecord)) {
      return false;
    }
    PasswordRecord other = (PasswordRecord) o;
    if (!super.equalPayloads(other)) {
      return false;
    }
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
