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
 * Jason Voll <jvoll@mozilla.com>
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

package org.mozilla.android.sync.repositories.android;

import java.util.ArrayList;

import org.mozilla.android.sync.repositories.Repository;
import org.mozilla.android.sync.repositories.RepositorySession;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionCreationDelegate;
import org.mozilla.android.sync.repositories.domain.BookmarkRecord;
import org.mozilla.android.sync.repositories.domain.Record;

import android.content.Context;
import android.database.Cursor;

public class AndroidBrowserBookmarksRepositorySession extends RepositorySession {

  public AndroidBrowserBookmarksRepositorySession(Repository repository,
      RepositorySessionCreationDelegate callbackReciever, Context context, long lastSyncTimestamp) {
    super(repository, callbackReciever, lastSyncTimestamp);
    dbHelper = new AndroidBrowserBookmarksDatabaseHelper(context);
  }
  
  @Override
  protected Record[] compileIntoRecordsArray(Cursor cur) {
    ArrayList<BookmarkRecord> records = new ArrayList<BookmarkRecord>();
    cur.moveToFirst();
    while (!cur.isAfterLast()) {
      records.add(DBUtils.bookmarkFromMirrorCursor(cur));
      cur.moveToNext();
    }
    cur.close();
  
    Record[] recordArray = new Record[records.size()];
    records.toArray(recordArray);
    return recordArray;
  }
  
  @Override
  protected Record reconcileRecords(Record local, Record remote) {
    // Do modifications on local since we always want to keep guid and androidId from local
    
    BookmarkRecord localBookmark = (BookmarkRecord) local;
    BookmarkRecord remoteBookmark = (BookmarkRecord) remote;

    // Determine which record is newer since this is the one we will take in case of conflict
    BookmarkRecord newer;
    if (local.lastModified > remote.lastModified) {
      newer = localBookmark;
    } else {
      newer = remoteBookmark;
    }

    // Do dumb resolution for now and just return the newer one with the android id added if it wasn't the local one
    // Need to track changes (not implemented yet) in order to merge two changed bookmarks nicely
    newer.androidID = localBookmark.androidID;

    /*
    // Title
    if (!local.title.equals(remote.title)) {
      local.title = newer.title;
    }

    // URI
    if (!local.bookmarkURI.equals(remote.bookmarkURI)) {
      local.bookmarkURI = newer.bookmarkURI;
    }

    // Description
    if (!local.description.equals(remote.description)) {
      local.description = newer.description;
    }

    // Load in sidebar.
    if (local.loadInSidebar != remote.loadInSidebar) {
    }
    */

    return newer;
  }
}
