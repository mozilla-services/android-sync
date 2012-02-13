/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import org.mozilla.gecko.sync.repositories.domain.FormHistoryRecord;

public class FormHistoryHelpers {

  public static FormHistoryRecord createFormHistory1() {
    FormHistoryRecord record = new FormHistoryRecord();
    record.fieldName      = "fieldName1";
    record.fieldValue     = "fieldValue1";
    record.fennecTimesUsed = 1;
    record.fennecFirstUsed = new Long("1330386168000").longValue();
    record.fennecLastUsed  = new Long("1330386168000").longValue();
    return record;
  }

  public static FormHistoryRecord createFormHistory2() {
    FormHistoryRecord record = new FormHistoryRecord();
    record.fieldName      = "fieldName2";
    record.fieldValue     = "fieldValue2";
    record.fennecTimesUsed = 2;
    record.fennecFirstUsed = new Long("1230386168000").longValue();
    record.fennecLastUsed  = new Long("1250320001000").longValue();
    return record;
  }

  public static FormHistoryRecord createFormHistory3() {
    FormHistoryRecord record = new FormHistoryRecord();
    record.fieldName      = "fieldName3";
    record.fieldValue     = "fieldValue3";
    record.fennecTimesUsed = 3;
    record.fennecFirstUsed = new Long("1230386168000").longValue();
    record.fennecLastUsed  = new Long("1260320001000").longValue();
    return record;
  }

  public static FormHistoryRecord createFormHistory4() {
    FormHistoryRecord record = new FormHistoryRecord();
    record.fieldName      = "fieldName4";
    record.fieldValue     = "fieldValue4";
    record.fennecTimesUsed = 4;
    record.fennecFirstUsed = new Long("1240386168000").longValue();
    record.fennecLastUsed  = new Long("1270320001000").longValue();
    return record;
  }

  public static FormHistoryRecord createFormHistory5() {
    FormHistoryRecord record = new FormHistoryRecord();
    record.fieldName      = "fieldName5";
    record.fieldValue     = "fieldValue5";
    record.fennecTimesUsed = 5;
    record.fennecFirstUsed = new Long("1230386168000").longValue();
    record.fennecLastUsed  = new Long("1260320001000").longValue();
    return record;
  }
}
