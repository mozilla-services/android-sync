/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.repositories.android;

import android.net.Uri;

/*
 * IMPORTANT NOTE
 * This file is a copy of mobile/android/base/db/BrowserContract.java
 * and is included here to avoid creating a compile-time dependency on
 * Fennec.
 */

public class BrowserContract {
    // Local change: use generated authority URI.
    public static final Uri AUTHORITY_URI = Uri.parse("content://" + Authorities.BROWSER_AUTHORITY);
    
    public static final Uri PASSWORDS_AUTHORITY = Uri.parse("content://" + Authorities.PASSWORDS_AUTHORITY);

    public static final String DEFAULT_PROFILE = "default";

    public static final String PARAM_PROFILE = "profile";

    public static final String PARAM_LIMIT = "limit";

    public static final String PARAM_IS_SYNC = "sync";

    public static final String PARAM_SHOW_DELETED = "show_deleted";

    interface CommonColumns {
        public static final String _ID = "_id";
    }

    public interface SyncColumns {
        public static final String GUID = "guid";

        public static final String DATE_CREATED = "created";

        public static final String DATE_MODIFIED = "modified";

        public static final String IS_DELETED = "deleted";
    }

    interface URLColumns {
        public static final String URL = "url";

        public static final String TITLE = "title";
    }

    interface ImageColumns {
        public static final String FAVICON = "favicon";

        public static final String THUMBNAIL = "thumbnail";
    }

    public static final class Images implements CommonColumns, ImageColumns, SyncColumns {
        private Images() {}

        public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, "images");

        public static final String URL = "url_key";

        public static final String FAVICON_URL = "favicon_url";
    }

    public static final class Bookmarks implements CommonColumns, URLColumns, ImageColumns, SyncColumns {
        private Bookmarks() {}

        public static final String MOBILE_FOLDER_GUID = "mobile";
        public static final String PLACES_FOLDER_GUID = "places";
        public static final String MENU_FOLDER_GUID = "menu";
        public static final String TAGS_FOLDER_GUID = "tags";
        public static final String TOOLBAR_FOLDER_GUID = "toolbar";
        public static final String UNFILED_FOLDER_GUID = "unfiled";

        public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, "bookmarks")
            .buildUpon().appendQueryParameter(PARAM_IS_SYNC, "true")
            .appendQueryParameter(PARAM_SHOW_DELETED, "true")
            .build();

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/bookmark";

        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/bookmark";

        public static final String IS_FOLDER = "folder";

        public static final String PARENT = "parent";

        public static final String POSITION = "position";

        public static final String TAGS = "tags";

        public static final String DESCRIPTION = "description";

        public static final String KEYWORD = "keyword";
        
        public static final String[] BookmarkColumns = new String[] {
          _ID,
          GUID,
          DATE_CREATED,
          DATE_MODIFIED,
          IS_DELETED,
          TITLE,
          URL,
          IS_FOLDER,
          PARENT,
          POSITION,
          TAGS,
          DESCRIPTION,
          KEYWORD
        };
    }
    
    

    public static final class History implements CommonColumns, URLColumns, ImageColumns, SyncColumns {
        private History() {}

        public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, "history")
            .buildUpon().appendQueryParameter(PARAM_IS_SYNC, "true")
            .appendQueryParameter(PARAM_SHOW_DELETED, "true")
            .build();

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/browser-history";

        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/browser-history";

        public static final String DATE_LAST_VISITED = "date";

        public static final String VISITS = "visits";
        
        public static final String[] HistoryColumns = new String[] {
          _ID,
          GUID,
          DATE_CREATED,
          DATE_MODIFIED,
          IS_DELETED,
          TITLE,
          URL,
          DATE_LAST_VISITED,
          VISITS
        };
    }

    public static final class Schema {
        private Schema() {}

        public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, "schema");

        public static final String VERSION = "version";
    }
    
    public static final class Passwords {
      private Passwords() {}

      public static final Uri CONTENT_URI = Uri.withAppendedPath(PASSWORDS_AUTHORITY, "passwords")
          .buildUpon().appendQueryParameter(PARAM_IS_SYNC, "true")
          .appendQueryParameter(PARAM_SHOW_DELETED, "true")
          .build();

      public static final String CONTENT_TYPE = "vnd.android.cursor.dir/passwords";

      public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/passwords";

      public static final String _ID = "id";

      public static final String HOSTNAME = "hostname";

      public static final String HTTP_REALM = "httpRealm";

      public static final String FORM_SUBMIT_URL = "formSubmitURL";

      public static final String USERNAME_FIELD = "usernameField";

      public static final String PASSWORD_FIELD = "passwordField";

      public static final String ENCRYPTED_USERNAME = "encryptedUsername";

      public static final String ENCRYPTED_PASSWORD = "encryptedPassword";

      public static final String ENC_TYPE = "encType";

      public static final String TIME_CREATED = "timeCreated";

      public static final String TIME_LAST_USED = "timeLastUsed";

      public static final String TIME_PASSWORD_CHANGED = "timePasswordChanged";

      public static final String TIMES_USED = "timesUsed";
      
      public static final String[] PasswordColumns = new String[] {
        _ID,
        SyncColumns.GUID,
        //TIME_CREATED,
        //TIME_PASSWORD_CHANGED,
        SyncColumns.DATE_CREATED,
        SyncColumns.DATE_MODIFIED,
        SyncColumns.IS_DELETED,
        HOSTNAME,
        HTTP_REALM,
        FORM_SUBMIT_URL,
        USERNAME_FIELD,
        PASSWORD_FIELD,
        ENCRYPTED_USERNAME,
        ENCRYPTED_PASSWORD,
        ENC_TYPE,
        TIME_LAST_USED,
        TIMES_USED
      };
  }
}