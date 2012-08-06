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
 * Displays Uri in an embedded WebView. Closes if there no Uri is passed in.
 * @author liuche
 *
 */
public class WebViewActivity extends Activity {
  private final String LOG_TAG = "WebViewActivity";

  public WebViewActivity() {
    super();
    Logger.info(LOG_TAG, "WebViewActivty constructor called.");
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.sync_setup_webview);
    // Extract uri to launch from Intent.
    Uri uri = this.getIntent().getData();
    if (uri == null) {
      Logger.debug(LOG_TAG, "No Uri passed to display.");
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
