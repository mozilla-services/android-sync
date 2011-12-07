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

import org.mozilla.android.sync.repositories.domain.HistoryRecord;
import org.mozilla.android.sync.repositories.domain.Record;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

public class AndroidBrowserHistoryDataAccessor extends AndroidBrowserRepositoryDataAccessor {

  private static final Uri PROVIDER_URI = Uri.parse("content://org.mozilla.gecko.providers.browser/history");
  
  public AndroidBrowserHistoryDataAccessor(Context context) {
    super(context);
  }

  @Override
  protected Uri getUri() {
    return PROVIDER_URI;
  }  

  @Override
  protected ContentValues getContentValues(Record record) {
    ContentValues cv = new ContentValues();
    HistoryRecord rec = (HistoryRecord) record;
    cv.put(BrowserContract.SyncColumns.GUID,            rec.guid);
    cv.put(BrowserContract.SyncColumns.DATE_MODIFIED,        rec.lastModified);
    cv.put(BrowserContract.CommonColumns.TITLE,           rec.title);
    cv.put(BrowserContract.CommonColumns.URL,        rec.histURI);
    //cv.put(BrowserContract.History.VISITS,          rec.visits);
    //cv.put(COL_TRANS_TYPE,      rec.transitionType);
    cv.put(BrowserContract.History.DATE_LAST_VISITED,    rec.dateVisited);
    return cv;
  }
  
  // Missing columns
  //visits- they seem to store a count...which is not the same as us
  //transition type

}