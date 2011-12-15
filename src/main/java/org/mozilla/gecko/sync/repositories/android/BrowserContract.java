/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * ***** BEGIN LICENSE BLOCK *****
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
 * The Original Code is Mozilla Android code.
 *
 * The Initial Developer of the Original Code is Mozilla Foundation.
 * Portions created by the Initial Developer are Copyright (C) 2011
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *   Lucas Rocha <lucasr@mozilla.com>
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

package org.mozilla.gecko.sync.repositories.android;

import android.net.Uri;

/*
 * IMPORTANT NOTE
 * This file is a copy of mobile/android/base/db/BrowserContract.java
 * and is included here to avoid creating a compile-time dependency on
 * Fennec.
 */

public class BrowserContract {
    public static final String AUTHORITY = "org.mozilla.gecko.providers.browser";

    public static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);

    public static final String DEFAULT_PROFILE = "default";

    public static final String PARAM_PROFILE = "profile";

    public static final String PARAM_LIMIT = "limit";

    // TODO waiting on mobile team to use these column names and add
    // deleted for both content providers
    /*interface SyncColumns {
        public static final String GUID = "guid";

        public static final String DATE_CREATED = "created";

        public static final String DATE_MODIFIED = "modified";

        // NOTE: This column doesn't exist in fennec's code yet!!!
        public static final String DELETED = "deleted";
    }*/

    /*
    interface CommonColumns {
        public static final String _ID = "_id";

        public static final String URL = "url";

        public static final String TITLE = "title";
    }
    */

    /*
    interface ImageColumns {
        public static final String FAVICON = "favicon";

        public static final String THUMBNAIL = "thumbnail";
    }
    */

    /*public static final class Images implements ImageColumns, SyncColumns {
        private Images() {}

        public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, "images");

        public static final String URL = "url_key";

        public static final String FAVICON_URL = "favicon_url";
    }*/

    public static final class Bookmarks /*implements CommonColumns, ImageColumns, SyncColumns*/ {
        private Bookmarks() {}

        public static final String GUID = "bookmarks.guid";

        public static final String DATE_CREATED = "created";

        public static final String DATE_MODIFIED = "bookmarks.modified";

        // this column doesn't exist yet!
        //public static final String DELETED = "deleted";

        public static final String _ID = "_id";

        public static final String URL = "url";

        public static final String TITLE = "title";
        public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, "bookmarks");

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/bookmark";

        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/bookmark";

        public static final String IS_FOLDER = "folder";

        public static final String PARENT = "parent";

        public static final String POSITION = "position";
    }

    public static final class History /*implements CommonColumns, ImageColumns, SyncColumns*/ {
        private History() {}

        public static final String _ID = "history._id";

        public static final String URL = "history.url";

        public static final String TITLE = "history.title";

        public static final String GUID = "history.guid";

        public static final String DATE_CREATED = "history.created";

        public static final String DATE_MODIFIED = "history.modified";

        public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, "history");

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/browser-history";

        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/browser-history";

        public static final String DATE_LAST_VISITED = "date";

        public static final String VISITS = "visits";
    }
}
