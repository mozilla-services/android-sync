/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import org.mozilla.gecko.sync.repositories.domain.FormHistoryRecord;

public class FormHistoryHelpers {

  public static FormHistoryRecord createFormHistory1() {
    FormHistoryRecord record = new FormHistoryRecord();
    record.fieldName      = "fieldName1";
    record.fieldValue     = "fieldValue1";
    return record;
  }

  public static FormHistoryRecord createFormHistory2() {
    FormHistoryRecord record = new FormHistoryRecord();
    record.fieldName      = "fieldName2";
    record.fieldValue     = "fieldValue2";
    return record;
  }

  public static FormHistoryRecord createFormHistory3() {
    FormHistoryRecord record = new FormHistoryRecord();
    record.fieldName      = "fieldName3";
    record.fieldValue     = "fieldValue3";
    return record;
  }

  public static FormHistoryRecord createFormHistory4() {
    FormHistoryRecord record = new FormHistoryRecord();
    record.fieldName      = "fieldName4";
    record.fieldValue     = "fieldValue4";
    return record;
  }

  public static FormHistoryRecord createFormHistory5() {
    FormHistoryRecord record = new FormHistoryRecord();
    record.fieldName      = "fieldName5";
    record.fieldValue     = "fieldValue5";
    return record;
  }

  public static FormHistoryRecord createDeletedFormHistory1() {
    FormHistoryRecord record = createFormHistory1();
    record.deleted = true;
    return record;
  }

  public static FormHistoryRecord createDeletedFormHistory2() {
    FormHistoryRecord record = createFormHistory2();
    record.deleted = true;
    return record;
  }
}
