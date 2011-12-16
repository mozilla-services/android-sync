/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.mozilla.gecko.sync.repositories.domain.HistoryRecord;

public class HistoryHelpers {
  
  @SuppressWarnings("unchecked")
  private static JSONArray getDefaultVisits() {
    JSONArray json = new JSONArray();
    JSONObject obj = new JSONObject();
    obj.put("date", 1320087601465600L);
    obj.put("type", 2L);
    json.add(obj);
    obj = new JSONObject();
    obj.put("date", 1320084970724990L);
    obj.put("type", 1L);
    json.add(obj);
    obj = new JSONObject();
    obj.put("date", 1319764134412287L);
    obj.put("type", 1L);
    json.add(obj);
    obj = new JSONObject();
    obj.put("date", 1319681306455594L);
    obj.put("type", 2L);
    json.add(obj);
    return json;
  }
  
  public static HistoryRecord createHistory1() {
    HistoryRecord record = new HistoryRecord();
    record.title          = "History 1";
    record.histURI        = "http://history.page1.com";
    record.visits = getDefaultVisits();
    return record;
  }
  
  
  public static HistoryRecord createHistory2() {
    HistoryRecord record = new HistoryRecord();
    record.title          = "History 2";
    record.histURI        = "http://history.page2.com";
    record.visits         = getDefaultVisits();
    return record;
  }
  
  public static HistoryRecord createHistory3() {
    HistoryRecord record = new HistoryRecord();
    record.title          = "History 3";
    record.histURI        = "http://history.page3.com";
    record.visits         = getDefaultVisits();
    return record;
  }
  
  public static HistoryRecord createHistory4() {
    HistoryRecord record = new HistoryRecord();
    record.title          = "History 4";
    record.histURI        = "http://history.page4.com";
    record.visits         = getDefaultVisits();
    return record;
  }
  
  public static HistoryRecord createHistory5() {
    HistoryRecord record = new HistoryRecord();
    record.title          = "History 5";
    record.histURI        = "http://history.page5.com";
    record.visits         = getDefaultVisits();
    return record;
  }
  
}