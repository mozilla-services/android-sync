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

import org.mozilla.android.sync.repositories.BookmarksRepository;
import org.mozilla.android.sync.repositories.BookmarksRepositorySession;
import org.mozilla.android.sync.repositories.RepoStatusCode;
import org.mozilla.android.sync.repositories.RepositoryCallbackReceiver;
import org.mozilla.android.sync.repositories.RepositorySession;
import org.mozilla.android.sync.repositories.RepositorySessionDelegate;
import org.mozilla.android.sync.repositories.domain.BookmarkRecord;
import org.mozilla.android.sync.repositories.domain.Record;
import org.mozilla.android.sync.test.CallbackResult.CallType;

import android.content.Context;

public class BookmarksSessionTestWrapper {
  /*
   * This class is basically used to turn async calls into synchronous
   * calls. This is used to make running JUnit tests easier.
   */

  private CallbackResult testResult;

  public CallbackResult doCreateSessionSync(BookmarksRepository repository, Context context, long lastSyncTimestamp) {

    repository.createSession(context, new CallbackReceiver(), lastSyncTimestamp);
    performWait();
    return testResult;
  }

  public CallbackResult doGuidsSinceSync(BookmarksRepositorySession session, long timestamp) {
    session.guidsSince(timestamp, new CallbackReceiver());
    performWait();
    return testResult;
  }

  public CallbackResult doStoreSync(BookmarksRepositorySession session, BookmarkRecord record) {
    session.store(record, new CallbackReceiver());
    performWait();
    return testResult;
  }

  public CallbackResult doFetchAllSync(BookmarksRepositorySession session) {
    session.fetchAll(new CallbackReceiver());
    performWait();
    return testResult;
  }

  public CallbackResult doFetchSync(BookmarksRepositorySession session, String[] guids) {
    session.fetch(guids, new CallbackReceiver());
    performWait();
    return testResult;
  }

  public CallbackResult doFetchSinceSync(BookmarksRepositorySession session, long timestamp) {
    session.fetchSince(timestamp, new CallbackReceiver());
    performWait();
    return testResult;
  }


  class CallbackReceiver implements RepositoryCallbackReceiver, RepositorySessionDelegate {

    public void guidsSinceCallback(RepoStatusCode status, String[] guids) {
      testResult = new CallbackResult(status, CallType.GUIDS_SINCE, guids);
      performNotify();
    }

    public void fetchSinceCallback(RepoStatusCode status, Record[] records) {
      testResult = new CallbackResult(status, CallType.FETCH_SINCE, records);
      performNotify();
    }

    public void fetchCallback(RepoStatusCode status, Record[] records) {
      testResult = new CallbackResult(status, CallType.FETCH, records);
      performNotify();
    }

    public void fetchAllCallback(RepoStatusCode status, Record[] records) {
      testResult = new CallbackResult(status, CallType.FETCH_ALL, records);
      performNotify();
    }

    public void storeCallback(RepoStatusCode status, long rowId) {
      testResult = new CallbackResult(status, CallType.STORE, rowId);
      performNotify();
    }

    public void wipeCallback(RepoStatusCode status) {
      // TODO Auto-generated method stub

    }

    public void beginCallback(RepoStatusCode status) {
      // TODO Auto-generated method stub

    }

    public void finishCallback(RepoStatusCode status) {
      // TODO Auto-generated method stub

    }

    public void sessionCallback(RepoStatusCode status, RepositorySession session) {
      testResult = new CallbackResult(status, CallType.CREATE_SESSION, session);
      performNotify();
    }

    public void storeCallback(RepoStatusCode status) {
      // TODO Auto-generated method stub

    }
  }

  // Helper to perform the wait
  private synchronized void performWait() {
      try {
        BookmarksSessionTestWrapper.this.wait();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
  }

  // Helper to perform notify
  private synchronized void performNotify() {
    BookmarksSessionTestWrapper.this.notify();
  }

}