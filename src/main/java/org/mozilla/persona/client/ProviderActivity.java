package org.mozilla.persona.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.ConsoleMessage;
import android.webkit.GeolocationPermissions.Callback;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebSettings.LayoutAlgorithm;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.LinearLayout;

public class ProviderActivity extends Activity {

  private static final String TAG = "ProviderActivity";

  public class ProviderWebChromeClient extends WebChromeClient {

    private static final long   MAX_QUOTA      = 100 * 1024 * 1024;

    private static final String CHANNEL_PREFIX = "__channel__:";

    /**
     * Tell the client to display a javascript alert dialog.
     * 
     * @param view
     * @param url
     * @param message
     * @param result
     */
    @Override
    public boolean onJsAlert(WebView view, String url, String message,
        final JsResult result) {
      AlertDialog.Builder dlg = new AlertDialog.Builder(ProviderActivity.this);
      dlg.setMessage(message);
      dlg.setTitle(android.R.string.dialog_alert_title);

      dlg.setCancelable(true);
      dlg.setPositiveButton(android.R.string.ok,
          new AlertDialog.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
              result.confirm();
            }
          });
      dlg.setOnCancelListener(new DialogInterface.OnCancelListener() {
        public void onCancel(DialogInterface dialog) {
          result.cancel();
        }
      });
      dlg.setOnKeyListener(new DialogInterface.OnKeyListener() {
        // DO NOTHING
        public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
          if (keyCode == KeyEvent.KEYCODE_BACK) {
            result.confirm();
            return false;
          } else
            return true;
        }
      });
      dlg.create();
      dlg.show();
      return true;
    }

    /**
     * Tell the client to display a confirm dialog to the user.
     * 
     * @param view
     * @param url
     * @param message
     * @param result
     */
    @Override
    public boolean onJsConfirm(WebView view, String url, String message,
        final JsResult result) {
      AlertDialog.Builder dlg = new AlertDialog.Builder(ProviderActivity.this);
      dlg.setMessage(message);
      dlg.setTitle("Confirm");
      dlg.setCancelable(true);
      dlg.setPositiveButton(android.R.string.ok,
          new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
              result.confirm();
            }
          });
      dlg.setNegativeButton(android.R.string.cancel,
          new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
              result.cancel();
            }
          });
      dlg.setOnCancelListener(new DialogInterface.OnCancelListener() {
        public void onCancel(DialogInterface dialog) {
          result.cancel();
        }
      });
      dlg.setOnKeyListener(new DialogInterface.OnKeyListener() {
        // DO NOTHING
        public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
          if (keyCode == KeyEvent.KEYCODE_BACK) {
            result.cancel();
            return false;
          } else
            return true;
        }
      });
      dlg.create();
      dlg.show();
      return true;
    }

    /**
     * Tell the client to display a prompt dialog to the user. If the client
     * returns true, WebView will assume that the client will handle the prompt
     * dialog and call the appropriate JsPromptResult method.
     * 
     * Since we are hacking prompts for our own purposes, we should not be using
     * them for this purpose, perhaps we should hack console.log to do this
     * instead!
     * 
     * @param view
     * @param url
     * @param message
     * @param defaultValue
     * @param result
     */
    @Override
    public boolean onJsPrompt(WebView view, final String url,
        final String message, final String defaultValue,
        final JsPromptResult result) {

      Log.d(TAG, "onJsPrompt: " + message);

      if (message.startsWith(CHANNEL_PREFIX)) {
        JSONObject body = null;
        try {
          body = new JSONObject(message.substring(CHANNEL_PREFIX.length()));
        } catch (JSONException e) {
          Log.e(TAG, e.getMessage(), e);
        }

        if (body != null && onChannelMessage(body, result)) {
          return true;
        }

        result.cancel();
      }

      AlertDialog.Builder dlg = new AlertDialog.Builder(ProviderActivity.this);
      dlg.setMessage(message);
      final EditText input = new EditText(ProviderActivity.this);
      if (defaultValue != null) {
        input.setText(defaultValue);
      }
      dlg.setView(input);
      dlg.setCancelable(false);
      dlg.setPositiveButton(android.R.string.ok,
          new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
              String usertext = input.getText().toString();
              result.confirm(usertext);
            }
          });
      dlg.setNegativeButton(android.R.string.cancel,
          new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
              result.cancel();
            }
          });
      dlg.create();
      dlg.show();

      return true;
    }

    private boolean onChannelMessage(final JSONObject message,
        final JsPromptResult result) {

      Log.d(TAG, "onChannelMessage " + message.toString());

      String method = null;
      try {
        method = message.getString("method");
      } catch (JSONException e) {
        Log.e(TAG, e.getMessage(), e);
      }
      JSONObject params = new JSONObject();
      try {
        if (!message.isNull("params")) {
          params = message.getJSONObject("params");
        }
      } catch (JSONException e) {
        Log.e(TAG, e.getMessage(), e);
      }

      if (method == null) {
        result.cancel();
        return true;
      }

      if (method.equals("beginProvisioning")) {

        JSONObject response = new JSONObject();
        JSONObject results = new JSONObject();

        try {
          results.put("identity", "harald@eyedee.me");
          results.put("certValidityDuration", 60 * 60 * 24 * 14); // 2 weeks
          response.put("result", results);
        } catch (JSONException e) {
          Log.e(TAG, e.getMessage(), e);
        }
        result.confirm(response.toString());
        return true;

      } else if (method.equals("genKeyPair")) {

        JSONObject response = new JSONObject();
        JSONObject results = new JSONObject();

        try {

          JSONObject fakePublicKey = new JSONObject();
          fakePublicKey.put("version", "2012.08.15");
          fakePublicKey.put("algorithm", "2012.08.15");
          fakePublicKey
              .put(
                  "modulus",
                  "0vx7agoebGcQSuuPiLJXZptN9nndrQmbXEps2aiAFbWhM78LhWx4cbbfAAtVT86zwu1RK7aPFFxuhDR1L6tSoc_BJECPebWKRXjBZCiFV4n3oknjhMstn64tZ_2W-5JsGY4Hc5n9yBXArwl93lqt7_RN5w6Cf0h4QyQ5v-65YGjQR0_FDW2QvzqY368QQMicAtaSqzs8KJZgnYb9c7d0zgdAZHzu6qMQvRL5hajrn1n91CbOpbISD08qNLyrdkt-bFTWhAI4vMQFh6WeZu0fM4lFd2NcRwr3XPksINHaQ-G_xBniIqbw0Ls1jF44-csFCur-kEgU8awapJzKnqDKgw");
          fakePublicKey.put("exponent", "AQAB");
          fakePublicKey.put("kid", "key-2011-04-29");

          results.put("publicKey", fakePublicKey);
          response.put("result", results);
        } catch (JSONException e) {
          Log.e(TAG, e.getMessage(), e);
        }
        result.confirm(response.toString());
        return true;

      } else if (method.equals("registerCertificate")) {

        result.cancel();
        try {
          endProvisioning(params.getString("certificate"), null);
        } catch (JSONException e) {
          Log.e(TAG, e.getMessage(), e);
        }
        return true;

      } else if (method.equals("raiseProvisioningFailure")) {

        result.cancel();
        try {
          endProvisioning(null, params.getString("reason"));
        } catch (JSONException e) {
          Log.e(TAG, e.getMessage(), e);
        }
        return true;

      } else if (method.equals("beginAuthentication")) {

        JSONObject response = new JSONObject();
        JSONObject results = new JSONObject();

        try {
          results.put("identity", "harald@eyedee.me");
          response.put("result", results);
        } catch (JSONException e) {
          Log.e(TAG, e.getMessage(), e);
        }
        result.confirm(response.toString());
        return true;

      } else if (method.equals("completeAuthentication")) {

        result.cancel();
        endAuthentication(null);
        return true;

      } else if (method.equals("raiseAuthenticationFailure")) {

        result.cancel();
        try {
          endAuthentication(params.getString("reason"));
        } catch (JSONException e) {
          Log.e(TAG, e.getMessage(), e);
        }
        return true;

      }

      result.cancel();
      return true;
    }

    /**
     * Handle database quota exceeded notification.
     * 
     * @param url
     * @param databaseIdentifier
     * @param currentQuota
     * @param estimatedSize
     * @param totalUsedQuota
     * @param quotaUpdater
     */
    @Override
    public void onExceededDatabaseQuota(String url, String databaseIdentifier,
        long currentQuota, long estimatedSize, long totalUsedQuota,
        WebStorage.QuotaUpdater quotaUpdater) {
      Log.d(TAG, "onExceededDatabaseQuota estimatedSize: " + estimatedSize
          + "  currentQuota: " + currentQuota + "  totalUsedQuota: "
          + totalUsedQuota);

      if (estimatedSize < MAX_QUOTA) {
        // increase for 1Mb
        long newQuota = estimatedSize;
        Log.d(TAG, "calling quotaUpdater.updateQuota newQuota: " + newQuota);
        quotaUpdater.updateQuota(newQuota);
      } else {
        // Set the quota to whatever it is and force an error
        // TODO: get docs on how to handle this properly
        quotaUpdater.updateQuota(currentQuota);
      }
    }

    // console.log in api level 7:
    // http://developer.android.com/guide/developing/debug-tasks.html
    @Override
    public void onConsoleMessage(String message, int lineNumber, String sourceID) {
      Log.d(TAG, sourceID + ": Line " + lineNumber + " : " + message);
      super.onConsoleMessage(message, lineNumber, sourceID);
    }

    @Override
    public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
      if (consoleMessage.message() != null)
        Log.d(TAG, consoleMessage.message());
      return super.onConsoleMessage(consoleMessage);
    }

    @Override
    /**
     * Instructs the client to show a prompt to ask the user to set the Geolocation permission state for the specified origin.
     *
     * @param origin
     * @param callback
     */
    public void onGeolocationPermissionsShowPrompt(String origin,
        Callback callback) {
      super.onGeolocationPermissionsShowPrompt(origin, callback);
      callback.invoke(origin, true, false);
    }

  }

  protected class ProviderWebViewClient extends WebViewClient {

    /**
     * Give the host application a chance to take over the control when a new
     * url is about to be loaded in the current WebView.
     * 
     * @param view
     *          The WebView that is initiating the callback.
     * @param url
     *          The url to be loaded.
     * @return true to override, false for default behavior
     */
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {

      // XXX: Allow all URLs to be loaded?

      return true;
    }

    // XXX: onReceivedSslError
    // XXX: onReceivedError
    // TODO: onReceivedHttpAuthRequest

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {

      HashMap<String, String> interceptions = new HashMap<String, String>();
      interceptions.put("//browserid.org/provisioning_api.js",
          "provisioning_api.js");
      interceptions.put("//browserid.org/authentication_api.js",
          "authentication_api.js");

      Log.d(TAG, "shouldInterceptRequest: " + url);

      String replacing = null;
      for (String test : interceptions.keySet()) {
        if (url.endsWith(test)) {
          replacing = test;
          break;
        }
      }

      if (replacing != null) {

        String local = interceptions.get(replacing);

        String mimetype = null;
        if (local.endsWith(".js")) {
          mimetype = "application/javascript";
        }

        try {
          AssetManager assets = getAssets();
          InputStream stream = assets.open("browserid_api/" + local,
              AssetManager.ACCESS_STREAMING);
          WebResourceResponse response = new WebResourceResponse(mimetype,
              "UTF-8", stream);

          Log.d(TAG, "shouldInterceptRequest SERVED: " + local);

          return response;
        } catch (IOException e) {
          Log.e(TAG, e.getMessage(), e);
        }
      }

      return super.shouldInterceptRequest(view, url);
    }
  }

  protected class ProviderWebView extends WebView {

    public ProviderWebView(Context context) {
      super(context);

      ProviderWebViewClient webViewCLient = new ProviderWebViewClient();
      ProviderWebChromeClient webChromeCLient = new ProviderWebChromeClient();

      setWebViewClient(webViewCLient);
      setWebChromeClient(webChromeCLient);

      setInitialScale(0);
      setVerticalScrollBarEnabled(false);
      requestFocusFromTouch();

      // Enable JavaScript
      WebSettings settings = this.getSettings();
      settings.setJavaScriptEnabled(true);
      settings.setJavaScriptCanOpenWindowsAutomatically(true);
      settings.setLayoutAlgorithm(LayoutAlgorithm.NORMAL);

      // Set the nav dump for HTC 2.x devices (disabling for ICS/Jellybean)
      if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB)
        settings.setNavDump(true);

      // Enable database
      settings.setDatabaseEnabled(true);
      String databasePath = getApplicationContext().getDir("database",
          Context.MODE_PRIVATE).getPath();
      settings.setDatabasePath(databasePath);

      // Enable DOM storage
      settings.setDomStorageEnabled(true);

      // Enable built-in geolocation
      settings.setGeolocationEnabled(true);

    }

  }

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
    webView = new ProviderWebView(this);

    webView.setLayoutParams(new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT,
        1.0F));

    webView.setVisibility(View.VISIBLE);

    root.addView(webView);

    beginProvisioning();
  }

  private void beginBrowsing(String url, boolean visible) {
    webView.clearHistory();
    webView.clearView();

    webView.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
    // XXX: Show progress?

    webView.loadUrl(url);
  }

  private void beginProvisioning() {
    beginBrowsing("https://eyedee.me/browserid/provision.html", false);
  }

  private void beginAuthentication() {
    beginBrowsing("https://eyedee.me/browserid/sign_in.html", true);
  }

  private void endProvisioning(String certificate, String error) {
    Log.d(TAG, "endProvisioning " + certificate);

    if (error != null) {
      beginAuthentication();
    } else {
      // TODO: Return result
    }
  }

  private void endAuthentication(String error) {
    // TODO: Store certificate
    beginProvisioning();
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
