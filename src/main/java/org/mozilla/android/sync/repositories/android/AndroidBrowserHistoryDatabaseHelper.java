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

public class AndroidBrowserHistoryDatabaseHelper extends AndroidBrowserRepositoryDatabaseHelper {

  private static String[] HISTORY_COLUMNS;
  
  public AndroidBrowserHistoryDatabaseHelper(Context context) {
    super(context);
    HISTORY_COLUMNS = new String[] {
        COL_ID, COL_GUID, COL_LAST_MOD,
        COL_DELETED, COL_ANDROID_ID,
        COL_TITLE, COL_HIST_URI, COL_VISITS,
        COL_TRANS_TYPE, COL_DATE_VISITED
    };
  }

  @Override
  protected String getTable() {
    return TBL_HISTORY;
  }
  
  @Override
  protected String[] getAllColumns() {
    return HISTORY_COLUMNS;
  }  

  @Override
  protected ContentValues getContentValues(Record record) {
    ContentValues cv = new ContentValues();
    HistoryRecord rec = (HistoryRecord) record;
    cv.put(COL_GUID,            rec.guid);
    cv.put(COL_ANDROID_ID,      rec.androidID);
    cv.put(COL_LAST_MOD,        rec.lastModified);
    cv.put(COL_DELETED,         rec.deleted);
    cv.put(COL_TITLE,           rec.title);
    cv.put(COL_HIST_URI,        rec.histURI);
    cv.put(COL_VISITS,          rec.visits);
    cv.put(COL_TRANS_TYPE,      rec.transitionType);
    cv.put(COL_DATE_VISITED,    rec.dateVisited);
    return cv;
  }

}