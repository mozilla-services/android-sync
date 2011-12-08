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
import java.util.HashMap;

import org.mozilla.android.sync.repositories.InvalidSessionTransitionException;
import org.mozilla.android.sync.repositories.NoGuidForIdException;
import org.mozilla.android.sync.repositories.Repository;
import org.mozilla.android.sync.repositories.domain.BookmarkRecord;
import org.mozilla.android.sync.repositories.domain.Record;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

public class AndroidBrowserBookmarksRepositorySession extends AndroidBrowserRepositorySession {

  private HashMap<String, Long> guidToID = new HashMap<String, Long>();
  private HashMap<Long, String> idToGuid = new HashMap<Long, String>();
  private HashMap<String, ArrayList<String>> missingParentToChildren = new HashMap<String, ArrayList<String>>();
  private AndroidBrowserBookmarksDataAccessor dataAccessor;
  
  public AndroidBrowserBookmarksRepositorySession(Repository repository, Context context) {
    super(repository);
    DBUtils.initialize(context);
    dbHelper = new AndroidBrowserBookmarksDataAccessor(context);
    dataAccessor = (AndroidBrowserBookmarksDataAccessor) dbHelper;
  }
  
  @Override
  protected Record recordFromMirrorCursor(Cursor cur) throws NoGuidForIdException {
    long androidParentId = DBUtils.getLongFromCursor(cur, BrowserContract.Bookmarks.PARENT);
    String guid = idToGuid.get(androidParentId);
    
    // Get parent name
    Cursor name = dataAccessor.fetch(new String[] { guid });
    name.moveToFirst();
    // TODO temp error string until we throw proper exception
    String parentName = "ERROR";
    if (!name.isAfterLast()) {
      parentName = DBUtils.getStringFromCursor(name, BrowserContract.Bookmarks.TITLE);
    }
    else {
      // TODO throw an exception
      Log.e(tag, "Couldn't find record with guid " + guid + " while looking for parent name");
    }
    return DBUtils.bookmarkFromMirrorCursor(cur, guid, parentName);
  }
  
  @Override
  protected boolean checkRecordType(Record record) {
    BookmarkRecord bmk = (BookmarkRecord) record;
    if (bmk.type.equalsIgnoreCase(AndroidBrowserBookmarksDataAccessor.TYPE_BOOKMARK) ||
        bmk.type.equalsIgnoreCase(AndroidBrowserBookmarksDataAccessor.TYPE_FOLDER)) {
      return true;
    }
    Log.i(tag, "Ignoring record with guid: " + record.guid + " and type: " + ((BookmarkRecord)record).type);
    return false;
  }
  
  @Override
  public void begin(org.mozilla.android.sync.repositories.delegates.RepositorySessionBeginDelegate delegate) {
    if (this.status == SessionStatus.UNSTARTED) {
      this.status = SessionStatus.ACTIVE;
      this.syncBeginTimestamp = System.currentTimeMillis();
    } else {
      Log.e(tag, "Tried to begin() an already active or finished session");
      delegate.onBeginFailed(new InvalidSessionTransitionException(null));
    }
    
    // To deal with parent mapping of bookmarks we have to do some
    // hairy stuff, here's the setup for it
    Cursor cur = dataAccessor.getGuidsIDsForFolders();
    cur.moveToFirst();
    while(!cur.isAfterLast()) {
      String guid = DBUtils.getStringFromCursor(cur, BrowserContract.SyncColumns.GUID);
      long id = DBUtils.getLongFromCursor(cur, BrowserContract.CommonColumns._ID);
      guidToID.put(guid, id);
      idToGuid.put(id, guid);
    }
    cur.close();
    
    delegate.onBeginSucceeded(this);
  }
  
  @Override
  protected void insert(Record record) {
    BookmarkRecord bmk = (BookmarkRecord) record;
    // Check if parent exists
    if (guidToID.containsKey(bmk.parentID)) {
      bmk.androidParentID = guidToID.get(bmk.parentID);
    }
    else {
      bmk.androidParentID = guidToID.get("unfiled");
      ArrayList<String> children;
      if (missingParentToChildren.containsKey(bmk.parentID)) {
        children = missingParentToChildren.get(bmk.parentID);
      } else {
        children = new ArrayList<String>();
      }
      children.add(bmk.guid);
      missingParentToChildren.put(bmk.parentID, children);
    }
    
    long id = DBUtils.getAndroidIdFromUri(dbHelper.insert(bmk));
    bmk.androidID = id;
    
    // If record is folder, update maps and re-parent children if necessary
    if(bmk.type.equalsIgnoreCase(AndroidBrowserBookmarksDataAccessor.TYPE_FOLDER)) {
      guidToID.put(bmk.guid, id);        
      idToGuid.put(id, bmk.guid);
      
      // re-parent
      if(missingParentToChildren.containsKey(bmk.guid)) {
        ArrayList<String> children = missingParentToChildren.get(bmk.guid);
        for (String child : children) {
          dataAccessor.updateParent(child, id);
        }
        missingParentToChildren.remove(bmk.guid);
      }
    }
  }

  @Override
  protected String buildRecordString(Record record) {
    BookmarkRecord bmk = (BookmarkRecord) record;
    return bmk.title + bmk.bookmarkURI + bmk.type + bmk.parentName;
  }
}
