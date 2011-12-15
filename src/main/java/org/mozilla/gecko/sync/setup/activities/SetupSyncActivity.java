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

package org.mozilla.gecko.sync.setup.activities;


import org.json.simple.JSONObject;
import org.mozilla.gecko.R;
import org.mozilla.gecko.sync.setup.Constants;
import org.mozilla.gecko.sync.setup.jpake.JPakeClient;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class SetupSyncActivity extends Activity {
  private final static String LOG_TAG = "SetupSync";
  private TextView setupTitleView;
  private TextView setupNoDeviceLinkTitleView;
  private TextView setupSubtitleView;
  private TextView pinTextView;

  public SetupSyncActivity() {
    super();
    Log.i(LOG_TAG, "SetupSyncActivity constructor called.");
  }

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    Log.i(LOG_TAG, "Called SetupSyncActivity.onCreate.");
    super.onCreate(savedInstanceState);
    Log.i(LOG_TAG, "Loading content view " + R.layout.sync_setup);
    setContentView(R.layout.sync_setup);

    setupTitleView             = ((TextView) findViewById(R.id.setup_title));
    setupSubtitleView          = (TextView) findViewById(R.id.setup_subtitle);
    setupNoDeviceLinkTitleView = (TextView) findViewById(R.id.link_nodevice);
    pinTextView                = ((TextView) findViewById(R.id.text_pin));
    if (setupTitleView == null) {
      Log.e(LOG_TAG, "No title view.");
    }
    if (setupSubtitleView == null) {
      Log.e(LOG_TAG, "No subtitle view.");
    }
    if (setupNoDeviceLinkTitleView == null) {
      Log.e(LOG_TAG, "No 'no device' link view.");
    }
  }

  @Override
  public void onResume() {
    Log.i(LOG_TAG, "Called SetupSyncActivity.onResume.");
    super.onResume();

    // Check whether Sync accounts exist; if so, display Pair text
    AccountManager mAccountManager = AccountManager.get(this);
    Account[] accts = mAccountManager.getAccountsByType(Constants.ACCOUNTTYPE_SYNC);
    Log.d(LOG_TAG, "number: " + accts.length);
    if (accts.length > 0) {
      setupTitleView.setText(getString(R.string.sync_title_pair));
      setupSubtitleView.setText(getString(R.string.sync_subtitle_pair));
      setupNoDeviceLinkTitleView.setVisibility(View.INVISIBLE);
    }
    // Start J-PAKE
    final JPakeClient jClient = new JPakeClient(this);
    jClient.receiveNoPin();
  }

  /* Click Handlers */
  public void manualClickHandler(View target) {
    Intent accountIntent = new Intent(this, AccountActivity.class);
    accountIntent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
    startActivity(accountIntent);
    overridePendingTransition(0, 0);
  }
  public void cancelClickHandler(View target) {
    finish();
  }

  // Controller methods
  public void displayPin(String pin) {
    if (pin == null) {
      Log.w(LOG_TAG, "Asked to display null pin.");
      return;
    }
    // format PIN
    int charPerLine = pin.length() / 3;
    String prettyPin  = pin.substring(0, charPerLine) + "\n";
    prettyPin        += pin.substring(charPerLine, 2 * charPerLine) + "\n";
    prettyPin        += pin.substring(2 * charPerLine, pin.length());
    final String toDisplay = prettyPin;
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        TextView view = pinTextView;
        if (view == null) {
          Log.w(LOG_TAG, "Couldn't find view to display PIN.");
          return;
        }
        view.setText(toDisplay);
      }
    });
  }

  public void displayAbort(String error) {
    // TODO: display abort error or something
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        // TODO
      }
    });
  }

  public void onPaired() {
    // TODO Auto-generated method stub
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        // TODO
      }
    });
  }

  public void onComplete(JSONObject newData) {
    // TODO Auto-generated method stub
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        // TODO
      }
    });
  }

  public void onPairingStart() {
    // TODO Auto-generated method stub
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        // TODO
      }
    });
  }
}
