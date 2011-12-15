/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import org.mozilla.gecko.sync.repositories.domain.HistoryRecord;

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
  
  public static HistoryRecord createHistory4() {
    HistoryRecord record = new HistoryRecord();
    record.title          = "History 4";
    record.histURI        = "http://history.page4.com";
    record.visits         = "visits4, visits4, visits4";
    record.transitionType = 4;
    record.dateVisited    = DATE_VISITED + 400;
    return record;
  }
  
  public static HistoryRecord createHistory5() {
    HistoryRecord record = new HistoryRecord();
    record.title          = "History 5";
    record.histURI        = "http://history.page5.com";
    record.visits         = "visits5, visits5, visits5";
    record.transitionType = 5;
    record.dateVisited    = DATE_VISITED + 500;
    return record;
  }
  
}