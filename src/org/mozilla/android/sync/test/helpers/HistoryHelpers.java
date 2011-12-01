package org.mozilla.android.sync.test.helpers;

import org.mozilla.android.sync.repositories.domain.HistoryRecord;

public class HistoryHelpers {
  
  private static final long DATE_VISITED = 10000000;
  
  public static HistoryRecord createHistory1() {
    HistoryRecord record = new HistoryRecord();
    record.title          = "History 1";
    record.histURI        = "http://history.page1.com";
    record.visits         = "visits1, visits1, visits1";
    record.transitionType = 1;
    record.dateVisited    = DATE_VISITED + 100;
    return record;
  }
  
  public static HistoryRecord createHistory2() {
    HistoryRecord record = new HistoryRecord();
    record.title          = "History 2";
    record.histURI        = "http://history.page2.com";
    record.visits         = "visits2, visits2, visits2";
    record.transitionType = 2;
    record.dateVisited    = DATE_VISITED + 200;
    return record;
  }
  
  public static HistoryRecord createHistory3() {
    HistoryRecord record = new HistoryRecord();
    record.title          = "History 3";
    record.histURI        = "http://history.page3.com";
    record.visits         = "visits3, visits3, visits3";
    record.transitionType = 3;
    record.dateVisited    = DATE_VISITED + 300;
    return record;
  }
  
}