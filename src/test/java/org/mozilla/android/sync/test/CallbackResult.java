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
