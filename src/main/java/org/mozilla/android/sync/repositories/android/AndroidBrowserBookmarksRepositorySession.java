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

import org.mozilla.android.sync.repositories.Repository;
import org.mozilla.android.sync.repositories.domain.BookmarkRecord;
import org.mozilla.android.sync.repositories.domain.Record;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

public class AndroidBrowserBookmarksRepositorySession extends AndroidBrowserRepositorySession {

  private static final String LOG_TAG = "AndroidBrowserBookmarksRepositorySession";

  public AndroidBrowserBookmarksRepositorySession(Repository repository, Context context) {
    super(repository);
    dbHelper = new AndroidBrowserBookmarksDataAccessor(context);
  }
  
  @Override
  protected Record recordFromMirrorCursor(Cursor cur) {
    return DBUtils.bookmarkFromMirrorCursor(cur);
  }
  
  @Override
  protected Record reconcileRecords(Record local, Record remote) {
    Log.i(LOG_TAG, "Reconciling " + local.guid + " against " + remote.guid);
    
    // Do modifications on local since we always want to keep guid and androidId from local.
    BookmarkRecord localBookmark = (BookmarkRecord) local;
    BookmarkRecord remoteBookmark = (BookmarkRecord) remote;

    // Determine which record is newer since this is the one we will take in case of conflict.
    // Yes, clock drift. *sigh*
    BookmarkRecord newer;
    if (local.lastModified > remote.lastModified) {
      newer = localBookmark;
    } else {
      newer = remoteBookmark;
    }

    // TODO Do this smarter (will differ between types of records which is why this isn't pulled up to super class)
    // Do dumb resolution for now and just return the newer one with the android id added if it wasn't the local one
    // Need to track changes (not implemented yet) in order to merge two changed bookmarks nicely
    newer.androidID = localBookmark.androidID;

    return newer;
  }
}
