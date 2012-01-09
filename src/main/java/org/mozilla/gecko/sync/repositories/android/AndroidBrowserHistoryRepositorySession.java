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
 *   Jason Voll <jvoll@mozilla.com>
 *   Richard Newman <rnewman@mozilla.com>
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

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.mozilla.gecko.sync.repositories.NullCursorException;
import org.mozilla.gecko.sync.repositories.Repository;
import org.mozilla.gecko.sync.repositories.domain.HistoryRecord;
import org.mozilla.gecko.sync.repositories.domain.Record;

import android.content.Context;
import android.database.Cursor;

public class AndroidBrowserHistoryRepositorySession extends AndroidBrowserRepositorySession {
  
  public static final String KEY_DATE = "date";
  public static final String KEY_TYPE = "type";
  public static final long DEFAULT_VISIT_TYPE = 1;

  public AndroidBrowserHistoryRepositorySession(Repository repository, Context context) {
    super(repository);
    dbHelper = new AndroidBrowserHistoryDataAccessor(context);
  }

  @Override
  protected Record recordFromMirrorCursor(Cursor cur) {
    return RepoUtils.historyFromMirrorCursor(cur);
  }

  @Override
  protected String buildRecordString(Record record) {
    HistoryRecord hist = (HistoryRecord) record;
    return hist.title + hist.histURI;
  }

  @Override
  protected Record transformRecord(Record record) throws NullCursorException {
    return addVisitsToRecord(record);
  }
  
  @SuppressWarnings("unchecked")
  private Record addVisitsToRecord(Record record) throws NullCursorException {
    AndroidBrowserHistoryDataExtender dataExtender = ((AndroidBrowserHistoryDataAccessor) dbHelper).getHistoryDataExtender();
    HistoryRecord hist = (HistoryRecord) record;
    Cursor visits = dataExtender.fetch(hist.guid);
    visits.moveToFirst();
    JSONArray visitsArray = RepoUtils.getJSONArrayFromCursor(visits, AndroidBrowserHistoryDataExtender.COL_VISITS);
    long missingRecords = hist.fennecVisitCount - visitsArray.size();
      
    // Add missingRecords -1 fake visits.
    if (missingRecords >= 1) {
        
      if (missingRecords > 1) {
        for (int j = 0; j < missingRecords -1; j++) {
          JSONObject fake = new JSONObject();

          // Set fake visit timestamp to be just previous to
          // the real one we are about to add.
          fake.put(KEY_DATE, hist.fennecDateVisited - (1 + j));
          fake.put(KEY_TYPE, DEFAULT_VISIT_TYPE);
          visitsArray.add(fake);
        }
      }

      // Add the 1 actual record we have.
      // We still have to fake the visit type: Fennec doesn't track that/
      JSONObject real = new JSONObject();
      real.put(KEY_DATE, hist.fennecDateVisited);
      real.put(KEY_TYPE, DEFAULT_VISIT_TYPE);
      visitsArray.add(real);
    }
    hist.visits = visitsArray;
    return hist;
  }
}
