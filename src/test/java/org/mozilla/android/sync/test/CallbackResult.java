package org.mozilla.android.sync.test;

import org.mozilla.android.sync.repositories.RepoStatusCode;
import org.mozilla.android.sync.repositories.domain.Record;

/*
 * This is a class used in tests only. It is used to pass
 * around the results of callback methods. It is a helper
 * class that supports waiting for asynchronous functions
 * in our JUnit tests.
 */
public class CallbackResult {

  public enum CallType {
    FETCH,
    FETCH_SINCE,
    FETCH_ALL,
    STORE,
  }

  private RepoStatusCode statusCode;
  private CallType callType;
  private String[] guids;
  private Record[] records;
  private long rowId;

  private CallbackResult(RepoStatusCode statusCode, CallType callType, String[] guids, Record[] records, long rowId) {
   this.setStatusCode(statusCode);
   this.setCallType(callType);
   this.setGuids(guids);
   this.setRecords(records);
   this.setRowId(rowId);
  }

  public CallbackResult(RepoStatusCode statusCode, CallType callType, String[] guids) {
    this(statusCode, callType, guids, null, -1);
  }

  public CallbackResult(RepoStatusCode statusCode, CallType callType, long rowId) {
    this(statusCode, callType, null, null, rowId);
  }

  public CallbackResult(RepoStatusCode statusCode, CallType callType, Record[] records) {
    this(statusCode, callType, null, records, -1);
  }

  public RepoStatusCode getStatusCode() {
    return statusCode;
  }

  public void setStatusCode(RepoStatusCode statusCode) {
    this.statusCode = statusCode;
  }

  public String[] getGuids() {
    return guids;
  }

  public void setGuids(String[] guids) {
    this.guids = guids;
  }

  public long getRowId() {
    return rowId;
  }

  public void setRowId(long rowId) {
    this.rowId = rowId;
  }

  public CallType getCallType() {
    return callType;
  }

  public void setCallType(CallType callType) {
    this.callType = callType;
  }

  public Record[] getRecords() {
    return records;
  }

  public void setRecords(Record[] records) {
    this.records = records;
  }

}
