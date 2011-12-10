package org.mozilla.android.sync.repositories.android;

import org.mozilla.android.sync.repositories.domain.PasswordRecord;
import org.mozilla.android.sync.repositories.domain.Record;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

public class AndroidBrowserPasswordsDataAccessor extends AndroidBrowserRepositoryDataAccessor {

  private static final Uri PROVIDER_URI = Uri.parse("content://org.mozilla.gecko.providers.passwordsprovider/password");
  
  public AndroidBrowserPasswordsDataAccessor(Context context) {
    super(context);
  }

  @Override
  protected ContentValues getContentValues(Record record) {
    ContentValues cv = new ContentValues();
    PasswordRecord rec = (PasswordRecord) record;
    cv.put(PasswordColumns.HOSTNAME, rec.hostname);
    cv.put(PasswordColumns.HTTP_REALM, rec.httpRealm);
    cv.put(PasswordColumns.FORM_SUBMIT_URL, rec.formSubmitURL);
    cv.put(PasswordColumns.USERNAME_FIELD, rec.usernameField);
    cv.put(PasswordColumns.PASSWORD_FIELD, rec.passwordField);
    cv.put(PasswordColumns.GUID,          rec.guid);
    // TODO has to do with encryption of username/password
    // figure out how to deal with this
    //cv.put(PasswordColumns.ENC_TYPE, rec.)
    cv.put(PasswordColumns.ENCRYPTED_USERNAME, rec.username);
    cv.put(PasswordColumns.ENCRYPTED_PASSWORD, rec.password);
    cv.put(PasswordColumns.TIME_PASSWORD_CHANGED, rec.lastModified);
    return cv;
  }

  @Override
  protected Uri getUri() {
    return PROVIDER_URI;
  }

  /*
  @Override
  protected String[] getAllColumns() {
    // TODO Auto-generated method stub
    return null;
  }
  */

  @Override
  protected String getGuidColumn() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  protected String getDateModifiedColumn() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  protected String getDeletedColumn() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  protected String getAndroidIDColumn() {
    // TODO Auto-generated method stub
    return null;
  }

  
  // Old test code used as a POC for accessing passwords content provider
  // Working to get content provider up and running
//  Log.i("cprovider", "test");
//  Uri uri = Uri.parse("content://org.mozilla.gecko.providers.passwordsprovider/password");
//  
//  ContentValues cv = new ContentValues();
//  cv.put("hostname", "host name");
//  cv.put("usernameField", "Username field");
//  cv.put("passwordField", "Password field");
//  cv.put("encryptedUsername", "Encrypted username");
//  cv.put("encryptedPassword", "Ecnrypted password");
//  cv.put("guid", Utils.generateGuid());
//    
//  Uri inserted = getContentResolver().insert(uri, cv);
//  
//  Cursor cur = getContentResolver().query(uri, null, null, null, null);
//  String[] columns = cur.getColumnNames();
//  String colList = "";
//  for(String column: columns) {
//    colList = colList + column + ", ";
//  }
//  Log.i("cprovider", colList);
//  cur.moveToFirst();
//  while (!cur.isAfterLast()) {
//    Log.i("cprovider", cur.getString(1));        
//    cur.moveToNext();
//  }

  // Required permission in manifest
  //<uses-permission android:name="org.mozilla.gecko.PASSWORDS"/>
}
