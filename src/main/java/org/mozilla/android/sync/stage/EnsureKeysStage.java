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
 *  Richard Newman <rnewman@mozilla.com>
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

import java.io.IOException;
import java.net.URISyntaxException;

import org.json.simple.parser.ParseException;
import org.mozilla.android.sync.CollectionKeys;
import org.mozilla.android.sync.CryptoRecord;
import org.mozilla.android.sync.ExtendedJSONObject;
import org.mozilla.android.sync.GlobalSession;
import org.mozilla.android.sync.NonObjectJSONException;
import org.mozilla.android.sync.crypto.CryptoException;
import org.mozilla.android.sync.net.SyncStorageRecordRequest;
import org.mozilla.android.sync.net.SyncStorageRequestDelegate;
import org.mozilla.android.sync.net.SyncStorageResponse;
import org.mozilla.android.sync.stage.GlobalSyncStage;
import org.mozilla.android.sync.stage.NoSuchStageException;

import android.util.Log;

public class EnsureKeysStage implements GlobalSyncStage, SyncStorageRequestDelegate {

  private GlobalSession session;

  @Override
  public void execute(GlobalSession session) throws NoSuchStageException {
    this.session = session;

    // TODO: decide whether we need to do this work.
    try {
      SyncStorageRecordRequest request = new SyncStorageRecordRequest(session.wboURI("crypto", "keys"));
      request.delegate = this;
      request.get();
    } catch (URISyntaxException e) {
      session.abort(e, "Invalid URI.");
    }
  }

  @Override
  public String credentials() {
    // TODO Auto-generated method stub
    return session.credentials();
  }

  @Override
  public String ifUnmodifiedSince() {
    // TODO: last key time!
    return null;
  }

  @Override
  public void handleSuccess(SyncStorageResponse response) {
    CollectionKeys k = new CollectionKeys();
    try {
      ExtendedJSONObject body = response.jsonObjectBody();
      Log.i("rnewman", "Fetched keys: " + body.toJSONString());
      k.setKeyPairsFromWBO(CryptoRecord.fromJSONRecord(body), session.syncKeyBundle);

    } catch (IllegalStateException e) {
      session.abort(e, "Invalid keys WBO.");
      return;
    } catch (ParseException e) {
      session.abort(e, "Invalid keys WBO.");
      return;
    } catch (NonObjectJSONException e) {
      session.abort(e, "Invalid keys WBO.");
      return;
    } catch (IOException e) {
      // Some kind of lower-level error.
      session.abort(e, "IOException fetching keys.");
      return;
    } catch (CryptoException e) {
      session.abort(e, "CryptoException handling keys WBO.");
      return;
    }

    Log.i("rnewman", "Setting keys. Yay!");
    session.setCollectionKeys(k);
    try {
      session.advance();
    } catch (NoSuchStageException e) {
      session.abort(e, "No stage.");
    }
  }

  @Override
  public void handleFailure(SyncStorageResponse response) {
    session.handleHTTPError(response, "Failure fetching keys.");
  }

  @Override
  public void handleError(Exception ex) {
    session.abort(ex, "Failure fetching keys.");
  }

}
