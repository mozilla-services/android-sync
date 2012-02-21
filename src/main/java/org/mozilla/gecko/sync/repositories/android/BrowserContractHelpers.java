package org.mozilla.gecko.sync.repositories.android;

import android.net.Uri;

public class BrowserContractHelpers extends BrowserContract {
  public static final Uri IMAGES_CONTENT_URI               = Uri.withAppendedPath(AUTHORITY_URI, "images")
            .buildUpon().appendQueryParameter(PARAM_IS_SYNC, "true")
            .appendQueryParameter(PARAM_SHOW_DELETED, "true")
            .build();
  public static final Uri BOOKMARKS_CONTENT_URI            = Uri.withAppendedPath(AUTHORITY_URI, "bookmarks")
            .buildUpon().appendQueryParameter(PARAM_IS_SYNC, "true")
            .appendQueryParameter(PARAM_SHOW_DELETED, "true")
            .build();
  public static final Uri BOOKMARKS_PARENTS_CONTENT_URI    = Uri.withAppendedPath(BOOKMARKS_CONTENT_URI, "parents")
            .buildUpon().appendQueryParameter(PARAM_IS_SYNC, "true")
            .appendQueryParameter(PARAM_SHOW_DELETED, "true")
            .build();
  public static final Uri BOOKMARKS_POSITIONS_CONTENT_URI  = Uri.withAppendedPath(BOOKMARKS_CONTENT_URI, "positions")
            .buildUpon().appendQueryParameter(PARAM_IS_SYNC, "true")
            .appendQueryParameter(PARAM_SHOW_DELETED, "true")
            .build();
  public static final Uri HISTORY_CONTENT_URI              = Uri.withAppendedPath(AUTHORITY_URI, "history")
            .buildUpon().appendQueryParameter(PARAM_IS_SYNC, "true")
            .appendQueryParameter(PARAM_SHOW_DELETED, "true")
            .build();
  public static final Uri SCHEMA_CONTENT_URI               = Uri.withAppendedPath(AUTHORITY_URI, "schema")
            .buildUpon().appendQueryParameter(PARAM_IS_SYNC, "true")
            .appendQueryParameter(PARAM_SHOW_DELETED, "true")
            .build();
  public static final Uri PASSWORDS_CONTENT_URI            = Uri.withAppendedPath(Uri.parse("content://" + Authorities.PASSWORDS_AUTHORITY), "passwords")
            .buildUpon().appendQueryParameter(PARAM_IS_SYNC, "true")
            .appendQueryParameter(PARAM_SHOW_DELETED, "true")
            .build();
  public static final Uri FORM_HISTORY_CONTENT_URI         = Uri.withAppendedPath(FORM_HISTORY_AUTHORITY_URI, "formhistory")
            .buildUpon().appendQueryParameter(PARAM_IS_SYNC, "true")
            .appendQueryParameter(PARAM_SHOW_DELETED, "true")
            .build();
  public static final Uri DELETED_FORM_HISTORY_CONTENT_URI = Uri.withAppendedPath(DELETED_FORM_HISTORY_AUTHORITY_URI, "deleted-formhistory")
            .buildUpon().appendQueryParameter(PARAM_IS_SYNC, "true")
            .appendQueryParameter(PARAM_SHOW_DELETED, "true")
            .build();

      public static final String[] PasswordColumns = new String[] {
        CommonColumns._ID,
        SyncColumns.GUID,
        SyncColumns.DATE_CREATED,
        SyncColumns.DATE_MODIFIED,
        SyncColumns.IS_DELETED,
        Passwords.HOSTNAME,
        Passwords.HTTP_REALM,
        Passwords.FORM_SUBMIT_URL,
        Passwords.USERNAME_FIELD,
        Passwords.PASSWORD_FIELD,
        Passwords.ENCRYPTED_USERNAME,
        Passwords.ENCRYPTED_PASSWORD,
        Passwords.ENC_TYPE,
        Passwords.TIME_LAST_USED,
        Passwords.TIMES_USED
      };

      public static final String[] HistoryColumns = new String[] {
        CommonColumns._ID,
        SyncColumns.GUID,
        SyncColumns.DATE_CREATED,
        SyncColumns.DATE_MODIFIED,
        SyncColumns.IS_DELETED,
        History.TITLE,
        History.URL,
        History.DATE_LAST_VISITED,
        History.VISITS
      };

      public static final String[] BookmarkColumns = new String[] {
        CommonColumns._ID,
        SyncColumns.GUID,
        SyncColumns.DATE_CREATED,
        SyncColumns.DATE_MODIFIED,
        SyncColumns.IS_DELETED,
        Bookmarks.TITLE,
        Bookmarks.URL,
        Bookmarks.IS_FOLDER,
        Bookmarks.PARENT,
        Bookmarks.POSITION,
        Bookmarks.TAGS,
        Bookmarks.DESCRIPTION,
        Bookmarks.KEYWORD
      };
}
