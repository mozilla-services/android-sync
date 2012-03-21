/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Android Sync Client.
 *
 * The Initial Developer of the Original Code is
 * the Mozilla Foundation.
 * Portions created by the Initial Developer are Copyright (C) 2011
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *   Jason Voll <jvoll@mozilla.com>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

package org.mozilla.gecko.sync.repositories.domain;

import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.repositories.android.RepoUtils;

public class PasswordRecord extends Record {

  public static final String COLLECTION_NAME = "passwords";

  // Payload strings.
  public static final String PAYLOAD_HOSTNAME = "hostname";
  public static final String PAYLOAD_FORM_SUBMIT_URL = "formSubmitURL";
  public static final String PAYLOAD_HTTP_REALM = "httpRealm";
  public static final String PAYLOAD_USERNAME = "username";
  public static final String PAYLOAD_PASSWORD = "password";
  public static final String PAYLOAD_USERNAME_FIELD = "usernameField";
  public static final String PAYLOAD_PASSWORD_FIELD = "passwordField";

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

  public String id;
  public String hostname;
  public String formSubmitURL;
  public String httpRealm;
  // TODO these are encrypted in the passwords content provider,
  // need to figure out what we need to do here.
  public String usernameField;
  public String passwordField;
  public String encryptedUsername;
  public String encryptedPassword;
  public String encType;

  public long   timeCreated;
  public long   timeLastUsed;
  public long   timePasswordChanged;
  public long   timesUsed;


  @Override
  public Record copyWithIDs(String guid, long androidID) {
    PasswordRecord out = new PasswordRecord(guid, this.collection, this.lastModified, this.deleted);
    out.androidID = androidID;
    out.sortIndex = this.sortIndex;

    // Copy HistoryRecord fields.
    out.id            = this.id;
    out.hostname      = this.hostname;
    out.formSubmitURL = this.formSubmitURL;
    out.httpRealm     = this.httpRealm;

    out.usernameField = this.usernameField;
    out.passwordField = this.passwordField;
    out.encryptedUsername      = this.encryptedUsername;
    out.encryptedPassword      = this.encryptedPassword;
    out.encType       = this.encType;

    out.timeCreated   = this.timeCreated;
    out.timeLastUsed  = this.timeLastUsed;
    out.timePasswordChanged = this.timePasswordChanged;
    out.timesUsed     = this.timesUsed;

    return out;
  }

  @Override
  public void initFromPayload(ExtendedJSONObject payload) {
    this.hostname = payload.getString(PAYLOAD_HOSTNAME);
    this.formSubmitURL = payload.getString(PAYLOAD_FORM_SUBMIT_URL);
    this.httpRealm = payload.getString(PAYLOAD_HTTP_REALM);
    this.encryptedUsername = payload.getString(PAYLOAD_USERNAME);
    this.encryptedPassword = payload.getString(PAYLOAD_PASSWORD);
    this.usernameField = payload.getString(PAYLOAD_USERNAME_FIELD);
    this.passwordField = payload.getString(PAYLOAD_PASSWORD_FIELD);
  }

  @Override
  public void populatePayload(ExtendedJSONObject payload) {
    putPayload(payload, PAYLOAD_HOSTNAME, this.hostname);
    putPayload(payload, PAYLOAD_FORM_SUBMIT_URL, this.formSubmitURL);
    putPayload(payload, PAYLOAD_HTTP_REALM, this.httpRealm);
    putPayload(payload, PAYLOAD_USERNAME, this.encryptedUsername);
    putPayload(payload, PAYLOAD_PASSWORD, this.encryptedPassword);
    putPayload(payload, PAYLOAD_USERNAME_FIELD, this.usernameField);
    putPayload(payload, PAYLOAD_PASSWORD_FIELD, this.passwordField);
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
        && RepoUtils.stringsEqual(this.usernameField, other.usernameField)
        && RepoUtils.stringsEqual(this.passwordField, other.passwordField)
        && RepoUtils.stringsEqual(this.encryptedUsername, other.encryptedUsername)
        && RepoUtils.stringsEqual(this.encryptedPassword, other.encryptedPassword)
        && RepoUtils.stringsEqual(this.encType, other.encType);
  }

  @Override
  public boolean equalPayloads(Object o) {
    if (o == null || !(o instanceof PasswordRecord)) {
      return false;
    }

    PasswordRecord other = (PasswordRecord) o;

    return RepoUtils.stringsEqual(this.hostname, other.hostname)
        && RepoUtils.stringsEqual(this.formSubmitURL, other.formSubmitURL)
        && RepoUtils.stringsEqual(this.httpRealm, other.httpRealm)
        && RepoUtils.stringsEqual(this.usernameField, other.usernameField)
        && RepoUtils.stringsEqual(this.passwordField, other.passwordField)
        && RepoUtils.stringsEqual(this.encryptedUsername, other.encryptedUsername)
        && RepoUtils.stringsEqual(this.encryptedPassword, other.encryptedPassword)
        && RepoUtils.stringsEqual(this.encType, other.encType)
        && (this.timeCreated == other.timeCreated)
        && (this.timeLastUsed == other.timeLastUsed)
        && (this.timePasswordChanged == other.timePasswordChanged)
        && (this.timesUsed == other.timesUsed);
  }

  @Override
  public String toString() {
    return "PasswordRecord {"
        + "id: " + this.id + ", "
        + "hostname: " + this.hostname + ", "
        + "formSubmitURL: " + this.formSubmitURL + ", "
        + "httpRealm: " + this.httpRealm + ", "
        + "usernameField: " + this.usernameField + ", "
        + "passwordField: " + this.passwordField + ", "
        + "encryptedUsername: " + this.encryptedUsername + ", "
        + "encryptedPassword: " + this.encryptedPassword + ", "
        + "encType: " + this.encType + ", "
        + "timeCreated: " + this.timeCreated + ", "
        + "timeLastUsed: " + this.timeLastUsed + ", "
        + "timePasswordChanged: " + this.timePasswordChanged + ", "
        + "timesUsed: " + this.timesUsed;
  }

}
