package org.mozilla.persona.client;

import android.app.Activity;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;

public class ProviderActivity extends Activity {

  private static final String TAG = "ProviderActivity";

  private LinearLayout    root;
  private ProviderWebView webView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    getWindow().requestFeature(Window.FEATURE_NO_TITLE);

    // XXX: Use LinearLayoutSoftKeyboardDetect to avoid issues with keyboard
    root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setLayoutParams(new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT,
        0.0F));

    setContentView(root);

    // Setup the hardware volume controls to handle volume control
    setVolumeControlStream(AudioManager.STREAM_MUSIC);

    // Initialize webView and all its components
    webView = new ProviderWebView(getApplicationContext());

    webView.beginProvisioning();
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {

    // If back key
    if (keyCode == KeyEvent.KEYCODE_BACK) {
      // If not bound
      // Go to previous page in webview if it is possible to go back
      if (webView.canGoBack()) {
        webView.goBack();
        return true;
      }

      return super.onKeyDown(keyCode, event);
    }

    Log.d(TAG, "KeyUp has been triggered on the view");
    return false;
  }

}
