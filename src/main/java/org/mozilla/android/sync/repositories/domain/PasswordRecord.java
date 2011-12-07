package org.mozilla.android.sync.repositories.domain;

import org.mozilla.android.sync.CryptoRecord;
import org.mozilla.android.sync.repositories.Utils;

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
  
  @Override
  public void initFromPayload(CryptoRecord payload) {
    // TODO Auto-generated method stub
    
  }
  @Override
  public CryptoRecord getPayload() {
    // TODO Auto-generated method stub
    return null;
  }

}
