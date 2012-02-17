/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.repositories.android;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.mozilla.gecko.sync.repositories.domain.HistoryRecord;
import org.mozilla.gecko.sync.repositories.domain.Record;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

public class AndroidBrowserHistoryDataAccessor extends AndroidBrowserRepositoryDataAccessor {

  private AndroidBrowserHistoryDataExtender dataExtender;

  public AndroidBrowserHistoryDataAccessor(Context context) {
    super(context);
    dataExtender = new AndroidBrowserHistoryDataExtender(context);
  }
  
  public AndroidBrowserHistoryDataExtender getHistoryDataExtender() {
    return dataExtender;
  }

  @Override
  protected Uri getUri() {
    return BrowserContract.History.CONTENT_URI;
  }

  @Override
  protected ContentValues getContentValues(Record record) {
    ContentValues cv = new ContentValues();
    HistoryRecord rec = (HistoryRecord) record;
    cv.put(BrowserContract.History.GUID,          rec.guid);
    cv.put(BrowserContract.History.DATE_MODIFIED, rec.lastModified);
    cv.put(BrowserContract.History.TITLE,         rec.title);
    cv.put(BrowserContract.History.URL,           rec.histURI);
    if (rec.visits != null) {
      JSONArray visits = rec.visits;
      long mostRecent = 0;
      for (int i = 0; i < visits.size(); i++) {
        JSONObject visit = (JSONObject) visits.get(i);
        long visitDate = (Long) visit.get(AndroidBrowserHistoryRepositorySession.KEY_DATE);
        if (visitDate > mostRecent) {
          mostRecent = visitDate;
        }
      }
      // Fennec stores milliseconds. The rest of Sync works in microseconds.
      cv.put(BrowserContract.History.DATE_LAST_VISITED, mostRecent / 1000);
      cv.put(BrowserContract.History.VISITS, Long.toString(visits.size()));
    }
    return cv;
  }

  @Override
  protected String[] getAllColumns() {
    return BrowserContract.History.HistoryColumns;
  }
  
  @Override
  public Uri insert(Record record) {
    HistoryRecord rec = (HistoryRecord) record;
    Log.d(LOG_TAG, "Storing visits for " + record.guid);
    dataExtender.store(record.guid, rec.visits);
    Log.d(LOG_TAG, "Storing record " + record.guid);
    return super.insert(record);
  }

  @Override
  public void update(String oldGUID, Record newRecord) {
    HistoryRecord rec = (HistoryRecord) newRecord;
    String newGUID = newRecord.guid;
    Log.d(LOG_TAG, "Storing visits for " + newGUID + ", replacing " + oldGUID);
    dataExtender.delete(oldGUID);
    dataExtender.store(newGUID, rec.visits);
    super.update(oldGUID, newRecord);
  }

  @Override
  protected void delete(String guid) {
    Log.d(LOG_TAG, "Deleting record " + guid);
    super.delete(guid);
    dataExtender.delete(guid);
  }

}