package org.mozilla.android.sync.repositories.android;

import android.provider.BaseColumns;

public final class PasswordColumns implements BaseColumns {
  
  /*
   * IMPORTANT NOTE
   * This file takes the column names from mobile/android/base/GeckoPassword.java
   * and is included here to avoid creating a compile-time dependency on Fennec.
   */

  public static final String _ID = "id";
  public static final String HOSTNAME = "hostname";
  public static final String HTTP_REALM = "httpRealm";
  public static final String FORM_SUBMIT_URL = "formSubmitURL";
  public static final String USERNAME_FIELD = "usernameField";
  public static final String PASSWORD_FIELD = "passwordField";
  public static final String ENCRYPTED_USERNAME = "encryptedUsername";
  public static final String ENCRYPTED_PASSWORD = "encryptedPassword";
  public static final String GUID = "guid";
  public static final String ENC_TYPE = "encType";
  public static final String TIME_CREATED = "timeCreated";
  public static final String TIME_LAST_USED = "timeLastUsed";
  public static final String TIME_PASSWORD_CHANGED = "timePasswordChanged";
  public static final String TIMES_USED = "timesUsed";
}
