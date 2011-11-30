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
 * Richard Newman <rnewman@mozilla.com>
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

package org.mozilla.android.sync.stage;

import java.net.URI;
import java.net.URISyntaxException;

import org.mozilla.android.sync.CollectionKeys;
import org.mozilla.android.sync.CryptoRecord;
import org.mozilla.android.sync.DelayedWorkTracker;
import org.mozilla.android.sync.GlobalSession;
import org.mozilla.android.sync.NoCollectionKeysSetException;
import org.mozilla.android.sync.crypto.KeyBundle;
import org.mozilla.android.sync.net.SyncStorageCollectionRequest;
import org.mozilla.android.sync.net.SyncStorageResponse;
import org.mozilla.android.sync.net.WBOCollectionRequestDelegate;
import org.mozilla.android.sync.repositories.RepositorySession;
import org.mozilla.android.sync.repositories.bookmarks.BookmarksRepository;
import org.mozilla.android.sync.repositories.bookmarks.BookmarksRepositorySession;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionBeginDelegate;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionCreationDelegate;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionFinishDelegate;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionStoreDelegate;
import org.mozilla.android.sync.repositories.domain.BookmarkRecord;
import org.mozilla.android.sync.repositories.domain.Record;

import android.util.Log;

public class TemporaryFetchBookmarksStage extends WBOCollectionRequestDelegate
    implements
    GlobalSyncStage,
    RepositorySessionCreationDelegate,
    RepositorySessionBeginDelegate,
    RepositorySessionFinishDelegate,
    RepositorySessionStoreDelegate {

  private GlobalSession session;
  private KeyBundle bookmarksKeyBundle;
  private BookmarksRepository bookmarksRepo;
  private BookmarksRepositorySession bookmarksSession;
  private SyncStorageCollectionRequest request;

  private DelayedWorkTracker workTracker = new DelayedWorkTracker();

  @Override
  public void execute(GlobalSession session) throws NoSuchStageException {
    this.session = session;
    URI bookmarksURI;
    try {
      // This will eventually be packaged up into a Server11Repository class,
      // rather than manually fetching.
      boolean full = true;
      bookmarksURI = session.collectionURI("bookmarks", full);
      Log.i("rnewman", "Bookmarks URI is " + bookmarksURI.toASCIIString());
      CollectionKeys collectionKeys = session.getCollectionKeys();
      if (collectionKeys == null) {
        session.abort(new Exception("No CollectionKeys!"), "No CollectionKeys!");
        return;
      }
      this.bookmarksKeyBundle = collectionKeys.keyBundleForCollection("bookmarks");
      request = new SyncStorageCollectionRequest(
          bookmarksURI);
      request.delegate = this;
      
      bookmarksRepo = new BookmarksRepository();
      long lastSyncTimestamp = 0;
      bookmarksRepo.createSession(session.getContext(), this, lastSyncTimestamp);

    } catch (URISyntaxException e) {
      session.abort(e, "Invalid URI.");
    } catch (NoCollectionKeysSetException e) {
      session.abort(e, "No CollectionKeys.");
    }
  }

  @Override
  public String credentials() {
    return session.credentials();
  }

  @Override
  public String ifUnmodifiedSince() {
    return null;
  }

  @Override
  public void handleRequestSuccess(SyncStorageResponse response) {
    Log.i("rnewman", "Bookmarks stage got handleSuccess! Session will finish as soon as store operations are complete.");
    // When we're done processing other events, finish the session.
    final RepositorySessionFinishDelegate delegate = this;
    workTracker.delayWorkItem(new Runnable() {
      @Override
      public void run() {
        bookmarksSession.finish(delegate);
      }
    });
  }

  @Override
  public void handleRequestFailure(SyncStorageResponse response) {
    Log.i("rnewman", "Bookmarks stage got handleFailure!");
    Log.i("rnewman", "Response: " + response.httpResponse().getStatusLine());
  }

  @Override
  public void handleRequestError(Exception ex) {
    Log.i("rnewman", "Bookmarks stage got handleError!");
  }

  @Override
  public void handleWBO(CryptoRecord record) {
    Log.i("rnewman", "Woo! Got a bookmark!");
    record.keyBundle = this.bookmarksKeyBundle;
    try {
      record.decrypt();
      Log.i("rnewman", "Decrypted.");
    } catch (Exception e) {
      Log.w("rnewman", "Exception decrypting record bookmarks/" + record.id, e);
    }

    // TODO: lastModified.
    BookmarkRecord b = new BookmarkRecord(record.id, record.collection);

    Log.i("rnewman", "Initing record.");
    b.initFromPayload(record);
    workTracker.incrementOutstanding();
    Log.i("rnewman", "Storing...");
    try {
      bookmarksSession.store(b, this);
    } catch (Exception ex) {
      Log.e("rnewman", "Exception in store", ex);
      throw new RuntimeException(ex);
    }
    Log.i("rnewman", "Store returned.");
  }

  @Override
  public KeyBundle keyBundle() {
    return session.syncKeyBundle;
  }


  @Override
  public void onSessionCreateFailed(Exception ex) {
    session.abort(ex, "BookmarksRepositorySession creation failed.");
  }

  @Override
  public void onSessionCreated(RepositorySession session) {
    Log.i("rnewman", "Beginning session.");
    bookmarksSession = (BookmarksRepositorySession) session;
    bookmarksSession.begin(this);
  }

  @Override
  public void onStoreFailed(Exception ex) {
    // TODO: more nuanced handling.
    workTracker.decrementOutstanding();
    session.abort(ex, "Storing record failed.");
  }

  @Override
  public void onStoreSucceeded(Record record) {
    // Great!
    Log.i("rnewman", "Storing " + record.guid + " succeeded.");
    workTracker.decrementOutstanding();
  }

  @Override
  public void onFinishFailed(Exception ex) {
    this.session.abort(ex, "Finish of BookmarksRepositorySession failed.");
  }

  @Override
  public void onFinishSucceeded() {
    try {
      this.session.advance();
    } catch (NoSuchStageException e) {
      this.session.abort(e, "No stage.");
    }
  }

  @Override
  public void onBeginFailed(Exception ex) {
    this.session.abort(ex, "Begin of BookmarksRepositorySession failed.");
  }

  @Override
  public void onBeginSucceeded() {
    this.request.get();
  }

}
