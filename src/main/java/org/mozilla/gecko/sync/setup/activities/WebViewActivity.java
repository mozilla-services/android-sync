/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.setup.activities;

import org.mozilla.gecko.R;
import org.mozilla.gecko.sync.Logger;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Displays URI in an embedded WebView. Closes if there no URI is passed in.
 * @author liuche
 *
 */
public class WebViewActivity extends Activity {
  private final static String LOG_TAG = "WebViewActivity";

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.sync_setup_webview);
    // Extract URI to launch from Intent.
    Uri uri = this.getIntent().getData();
    if (uri == null) {
      Logger.debug(LOG_TAG, "No URI passed to display.");
      finish();
    }

    WebView wv = (WebView) findViewById(R.id.web_engine);
    wv.setWebViewClient(new WebViewClient() {
      
      // Handle url loading in this WebView, instead of asking the ActivityManager.
      @Override
      public boolean shouldOverrideUrlLoading(WebView view, String url) {
        view.loadUrl(url);
        return false;
      }
    });
    wv.loadUrl(uri.toString());
    
  }
}
