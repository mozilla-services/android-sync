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
 *  Chenxia Liu <liuche@mozilla.com>
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

package org.mozilla.android.sync.syncadapter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.mozilla.android.sync.crypto.KeyBundle;
import org.mozilla.android.sync.setup.Constants;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

public class SyncAdapter extends AbstractThreadedSyncAdapter {

  private static final String  TAG = "SyncAdapter";
  private final AccountManager mAccountManager;
  private final Context        mContext;

  public SyncAdapter(Context context, boolean autoInitialize) {
    super(context, autoInitialize);
    mContext = context;
    mAccountManager = AccountManager.get(context);
  }

  private void handleException(Exception e, SyncResult syncResult) {
    if (e instanceof OperationCanceledException) {
      Log.e("rnewman", "Operation canceled. Aborting sync.");
      e.printStackTrace();
      return;
    }
    if (e instanceof AuthenticatorException) {
      syncResult.stats.numParseExceptions++;
      Log.e("rnewman", "AuthenticatorException. Aborting sync.");
      e.printStackTrace();
      return;
    }
    if (e instanceof IOException) {
      syncResult.stats.numIoExceptions++;
      Log.e("rnewman", "IOException. Aborting sync.");
      e.printStackTrace();
      return;
    }
    syncResult.stats.numIoExceptions++;
    Log.e("rnewman", "Unknown exception. Aborting sync.");
    e.printStackTrace();
  }

  private String getAuthToken(String tokenName, Account account,
                              SyncResult syncResult) {
    try {
      return mAccountManager.blockingGetAuthToken(account, tokenName, true);
    } catch (Exception ex) {
      this.handleException(ex, syncResult);
    }
    return null;
  }

  @Override
  public void onPerformSync(final Account account,
                            final Bundle extras,
                            final String authority,
                            final ContentProviderClient provider,
                            final SyncResult syncResult) {

    Log.i("rnewman", "Got onPerformSync:");
    Log.i("rnewman", "Account name: " + account.name);
    final SyncAdapter self = this;
    AccountManagerCallback<Bundle> callback = new AccountManagerCallback<Bundle>() {
      @Override
      public void run(AccountManagerFuture<Bundle> future) {
        // TODO: N.B.: Future must not be used on the main thread.
        try {
          Bundle bundle   = future.getResult(60L, TimeUnit.SECONDS);
          String syncKey  = bundle.getString(Constants.OPTION_KEY);
          String password = bundle.getString(AccountManager.KEY_AUTHTOKEN);
          KeyBundle keyBundle = new KeyBundle(account.name, syncKey);
          self.performSync(account, extras, authority, provider, syncResult, keyBundle, password);
        } catch (Exception e) {
          self.handleException(e, syncResult);
          return;
        }
      }
    };
    Handler handler = null;
    mAccountManager.getAuthToken(account, Constants.AUTHTOKEN_TYPE_PLAIN, true, callback, handler);
    String profile = mAccountManager.getUserData(account, "profile");
    String password = this.getAuthToken("password", account, syncResult);
    if (password == null) {
      Log.e("rnewman", "No password: aborting sync.");
      return;
    }
    String syncKey = this.getAuthToken("syncKey", account, syncResult);
    if (syncKey == null) {
      Log.e("rnewman", "No Sync Key: aborting sync.");
      return;
    }
    Log.i("rnewman", password + ", " + syncKey);
  }

  /**
   * Now that we have a sync key and password, go ahead and do the work.
   */
  protected void performSync(Account account, Bundle extras, String authority,
                             ContentProviderClient provider,
                             SyncResult syncResult, KeyBundle keyBundle,
                             String password) {
    // TODO
  }
}

//        try {
//            // see if we already have a sync-state attached to this account. By handing
//            // This value to the server, we can just get the contacts that have
//            // been updated on the server-side since our last sync-up
//            long lastSyncMarker = getServerSyncMarker(account);
//
//            // By default, contacts from a 3rd party provider are hidden in the contacts
//            // list. So let's set the flag that causes them to be visible, so that users
//            // can actually see these contacts.
//            if (lastSyncMarker == 0) {
//                ContactManager.setAccountContactsVisibility(getContext(), account, true);
//            }
//
//            List<RawContact> dirtyContacts;
//            List<RawContact> updatedContacts;
//
//            // Use the account manager to request the AuthToken we'll need
//            // to talk to our sample server.  If we don't have an AuthToken
//            // yet, this could involve a round-trip to the server to request
//            // and AuthToken.
//            final String authtoken = mAccountManager.blockingGetAuthToken(account,
//                    Constants.AUTHTOKEN_TYPE, NOTIFY_AUTH_FAILURE);
//
//            // Make sure that the sample group exists
//            final long groupId = ContactManager.ensureSampleGroupExists(mContext, account);
//
//            // Find the local 'dirty' contacts that we need to tell the server about...
//            // Find the local users that need to be sync'd to the server...
//            dirtyContacts = ContactManager.getDirtyContacts(mContext, account);
//
//            // Send the dirty contacts to the server, and retrieve the server-side changes
//            updatedContacts = NetworkUtilities.syncContacts(account, authtoken,
//                    lastSyncMarker, dirtyContacts);
//
//            // Update the local contacts database with the changes. updateContacts()
//            // returns a syncState value that indicates the high-water-mark for
//            // the changes we received.
//            Log.d(TAG, "Calling contactManager's sync contacts");
//            long newSyncState = ContactManager.updateContacts(mContext,
//                    account.name,
//                    updatedContacts,
//                    groupId,
//                    lastSyncMarker);
//
//            // This is a demo of how you can update IM-style status messages
//            // for contacts on the client. This probably won't apply to
//            // 2-way contact sync providers - it's more likely that one-way
//            // sync providers (IM clients, social networking apps, etc) would
//            // use this feature.
//            ContactManager.updateStatusMessages(mContext, updatedContacts);
//
//            // Save off the new sync marker. On our next sync, we only want to receive
//            // contacts that have changed since this sync...
//            setServerSyncMarker(account, newSyncState);
//
//            if (dirtyContacts.size() > 0) {
//                ContactManager.clearSyncFlags(mContext, dirtyContacts);
//            }
//
//        } catch (final AuthenticatorException e) {
//            Log.e(TAG, "AuthenticatorException", e);
//            syncResult.stats.numParseExceptions++;
//        } catch (final OperationCanceledException e) {
//            Log.e(TAG, "OperationCanceledExcetpion", e);
//        } catch (final IOException e) {
//            Log.e(TAG, "IOException", e);
//            syncResult.stats.numIoExceptions++;
//        } catch (final AuthenticationException e) {
//            Log.e(TAG, "AuthenticationException", e);
//            syncResult.stats.numAuthExceptions++;
//        } catch (final ParseException e) {
//            Log.e(TAG, "ParseException", e);
//            syncResult.stats.numParseExceptions++;
//        } catch (final JSONException e) {
//            Log.e(TAG, "JSONException", e);
//            syncResult.stats.numParseExceptions++;
//        }
