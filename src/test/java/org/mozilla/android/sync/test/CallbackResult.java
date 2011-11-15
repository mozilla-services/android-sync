package org.mozilla.android.sync.test;

import org.mozilla.android.sync.repositories.RepoStatusCode;
import org.mozilla.android.sync.repositories.RepositorySession;
import org.mozilla.android.sync.repositories.domain.Record;

/*
 * This is a class used in tests only. It is used to pass
 * around the results of callback methods. It is a helper
 * class that supports waiting for asynchronous functions
 * in our JUnit tests.
 */
public class CallbackResult {

  public static long DEFAULT_ROW_ID = -1;

  public enum CallType {
    FETCH,
    FETCH_SINCE,
    FETCH_ALL,
    GUIDS_SINCE,
    STORE,
    CREATE_SESSION,
  }

  private RepoStatusCode statusCode;
  private CallType callType;
  private String[] guids;
  private Record[] records;
  private long rowId;
  private RepositorySession session;

  private CallbackResult(RepoStatusCode statusCode, CallType callType, String[] guids, Record[] records, long rowId, RepositorySession session) {
   this.setStatusCode(statusCode);
   this.setCallType(callType);
   this.setGuids(guids);
   this.setRecords(records);
   this.setRowId(rowId);
   this.setSession(session);
  }

  public CallbackResult(RepoStatusCode statusCode, CallType callType, String[] guids) {
    this(statusCode, callType, guids, null, DEFAULT_ROW_ID, null);
  }

  public CallbackResult(RepoStatusCode statusCode, CallType callType, long rowId) {
    this(statusCode, callType, null, null, rowId, null);
  }

  public CallbackResult(RepoStatusCode statusCode, CallType callType, Record[] records) {
    this(statusCode, callType, null, records, DEFAULT_ROW_ID, null);
  }

  public CallbackResult(RepoStatusCode statusCode, CallType callType, RepositorySession session) {
    this(statusCode, callType, null, null, DEFAULT_ROW_ID, session);
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

  public RepositorySession getSession() {
    return session;
  }

  public void setSession(RepositorySession session) {
    this.session = session;
  }

}
