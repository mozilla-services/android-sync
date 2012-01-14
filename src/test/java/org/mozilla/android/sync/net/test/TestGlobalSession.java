/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.net.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Map;

import org.json.simple.parser.ParseException;
import org.junit.Test;
import org.mozilla.gecko.sync.GlobalSession;
import org.mozilla.gecko.sync.NonObjectJSONException;
import org.mozilla.gecko.sync.SyncConfiguration;
import org.mozilla.gecko.sync.SyncConfigurationException;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.delegates.GlobalSessionCallback;
import org.mozilla.gecko.sync.repositories.RecordFactory;
import org.mozilla.gecko.sync.repositories.Repository;
import org.mozilla.gecko.sync.stage.EnsureClusterURLStage;
import org.mozilla.gecko.sync.stage.EnsureKeysStage;
import org.mozilla.gecko.sync.stage.FetchInfoCollectionsStage;
import org.mozilla.gecko.sync.stage.FetchMetaGlobalStage;
import org.mozilla.gecko.sync.stage.ServerSyncStage;
import org.mozilla.gecko.sync.stage.GlobalSyncStage.Stage;
import org.mozilla.gecko.sync.net.SyncStorageResponse;

import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.ProtocolVersion;
import ch.boye.httpclientandroidlib.message.BasicHttpResponse;
import ch.boye.httpclientandroidlib.message.BasicStatusLine;

import android.content.Context;
import android.content.SharedPreferences;

public class TestGlobalSession {

  public class MockSuccessCallback implements GlobalSessionCallback {
    int stageCounter = Stage.values().length - 1;    // Exclude starting state.
    public boolean calledSuccess = false;
    
    public void handleError(GlobalSession globalSession, Exception ex) {
      ex.printStackTrace();
      fail("No error should occur.");
    }

    public void handleSuccess(GlobalSession globalSession) {
      assertEquals(0, stageCounter);
      calledSuccess = true;
    }

    public void handleStageCompleted(Stage currentState,
        GlobalSession globalSession) {
      stageCounter--;
    }

    @Override
    public void requestBackoff(long backoff) {
      fail("Not expecting backoff.");
    }

    @Override
    public void handleAborted(GlobalSession globalSession, String reason) {
      fail("Not expecting abort.");
    }

    @Override
    public boolean shouldBackOff() {
      return false;
    }
  }

  public class MockBackoffCallback extends MockSuccessCallback {
    public boolean calledBackoff = false;
    public long weaveBackoff = -1;
    
    @Override
    public void requestBackoff(long backoff) {
      this.calledBackoff = true;
      this.weaveBackoff = backoff;
    }
    
    @Override
    public void handleSuccess(GlobalSession globalSession) {
      fail("No success should occur.");
    }
  }
  
  public class MockAbortCallback extends MockBackoffCallback {
    public boolean calledError = false;
    
    @Override
    public void handleError(GlobalSession globalSession, Exception ex) {
      calledError = true;
    }
  }
  
  public class MockSharedPreferences implements SharedPreferences {

    @Override
    public boolean contains(String key) {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public Editor edit() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Map<String, ?> getAll() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public float getFloat(String key, float defValue) {
      // TODO Auto-generated method stub
      return 0;
    }

    @Override
    public int getInt(String key, int defValue) {
      // TODO Auto-generated method stub
      return 0;
    }

    @Override
    public long getLong(String key, long defValue) {
      // TODO Auto-generated method stub
      return 0;
    }

    @Override
    public String getString(String key, String defValue) {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(
        OnSharedPreferenceChangeListener listener) {
      // TODO Auto-generated method stub
      
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(
        OnSharedPreferenceChangeListener listener) {
      // TODO Auto-generated method stub
      
    }
  }
  
  //Mock this out so our tests continue to pass as we hack.
  public class MockSuccessGlobalSession extends GlobalSession {

    public MockSharedPreferences prefs; 
    
    // TODO: mock prefs.
    public MockSuccessGlobalSession(String clusterURL, String username, String password,
        KeyBundle syncKeyBundle, GlobalSessionCallback callback)
            throws SyncConfigurationException, IllegalArgumentException, IOException, ParseException, NonObjectJSONException {
      super(SyncConfiguration.DEFAULT_USER_API, clusterURL, username, password, null, syncKeyBundle, callback, /* context */ null, null);
    }
    
    /*
     * PrefsSource methods.
     */
    @Override
    public SharedPreferences getPrefs(String name, int mode) {
      if (prefs == null)
        prefs = new MockSharedPreferences();
      return prefs;
    }

    @Override
    public Context getContext() {
      return null;
    }

    
    public class MockServerSyncStage extends ServerSyncStage {
      @Override
      public boolean isEnabled() {
        return false;
      }

      @Override
      protected String getCollection() {
        return null;
      }

      @Override
      protected Repository getLocalRepository() {
        return null;
      }

      @Override
      protected String getEngineName() {
        return null;
      }

      @Override
      protected RecordFactory getRecordFactory() {
        return null;
      }
    }

    public class MockFetchInfoCollectionsStage extends FetchInfoCollectionsStage {
      @Override
      public void execute(GlobalSession session) {
        session.advance();
      }
    }

    public class MockFetchMetaGlobalStage extends FetchMetaGlobalStage {
      @Override
      public void execute(GlobalSession session) {
        session.advance();
      }
    }

    public class MockEnsureKeysStage extends EnsureKeysStage {
      @Override
      public void execute(GlobalSession session) {
        session.advance();
      }
    }

    public class MockEnsureClusterURLStage extends EnsureClusterURLStage {
      @Override
      public void execute(GlobalSession session) {
        session.advance();
      }
    }

    @Override
    protected void prepareStages() {
      super.prepareStages();
      // Fake whatever stages we don't want to run.      
      stages.put(Stage.syncBookmarks,           new MockServerSyncStage());
      stages.put(Stage.syncHistory,             new MockServerSyncStage());
      stages.put(Stage.fetchInfoCollections,    new MockFetchInfoCollectionsStage());
      stages.put(Stage.fetchMetaGlobal,         new MockFetchMetaGlobalStage());
      stages.put(Stage.ensureKeysStage,         new MockFetchInfoCollectionsStage());
      stages.put(Stage.ensureClusterURL,        new MockEnsureClusterURLStage());
    }
  }
  
  //Mock this out so our tests continue to pass as we hack.
  public class MockBackoffGlobalSession extends MockSuccessGlobalSession {

    public MockSharedPreferences prefs; 
    
    // TODO: mock prefs.
    public MockBackoffGlobalSession(String clusterURL, String username, String password,
        KeyBundle syncKeyBundle, GlobalSessionCallback callback)
            throws SyncConfigurationException, IllegalArgumentException, IOException, ParseException, NonObjectJSONException {
      super(clusterURL, username, password, syncKeyBundle, callback);
    }

    public class MockBackoffFetchInfoCollectionsStage extends FetchInfoCollectionsStage {
      @Override
      public void execute(GlobalSession session) {
        HttpResponse response;
        response = new BasicHttpResponse(
            new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 503, "Illegal method/protocol"));       
        
        response.addHeader("X-Weave-Backoff", "2401.0"); // Backoff given in seconds.        
        session.handleHTTPError(new SyncStorageResponse(response), "Failure fetching info/collections.");
      }
    }

    @Override
    protected void prepareStages() {
      super.prepareStages();
      stages.put(Stage.fetchInfoCollections, new MockBackoffFetchInfoCollectionsStage());
    }
  }

  @Test
  public void testBackoffCalledInStages() {
    String clusterURL = "http://localhost:8080/";
    String username   = "johndoe";
    String password   = "password";
    String syncKey    = "abcdeabcdeabcdeabcdeabcdea";
    KeyBundle syncKeyBundle = new KeyBundle(username, syncKey);
    
    try {
      MockAbortCallback callback = new MockAbortCallback();
      GlobalSession session = new MockBackoffGlobalSession(clusterURL, username, password, syncKeyBundle, callback);

      session.start();
      assertEquals(true, callback.calledBackoff);
      assertEquals(true, callback.calledError);      
      assertEquals(2401*1000, callback.weaveBackoff); // Backoff returned in milliseconds.
    } catch (Exception e) {
      e.printStackTrace();
      fail("Got exception.");
    }
  }
  
  @Test
  public void testSuccessCalledAfterStages() {
    String clusterURL = "http://localhost:8080/";
    String username   = "johndoe";
    String password   = "password";
    String syncKey    = "abcdeabcdeabcdeabcdeabcdea";
    KeyBundle syncKeyBundle = new KeyBundle(username, syncKey);
    
    try {
      MockSuccessCallback callback = new MockSuccessCallback();
      GlobalSession session = new MockSuccessGlobalSession(clusterURL, username, password, syncKeyBundle, callback);

      session.start();
      assertEquals(true, callback.calledSuccess);
    } catch (Exception e) {
      e.printStackTrace();
      fail("Got exception.");
    }
  }

  @Test
  public void testBackoffCalledIfBackoffHeaderPresent() {
    String clusterURL = "http://localhost:8080/";
    String username   = "johndoe";
    String password   = "password";
    String syncKey    = "abcdeabcdeabcdeabcdeabcdea";
    KeyBundle syncKeyBundle = new KeyBundle(username, syncKey);
    
    try {
      MockBackoffCallback callback = new MockBackoffCallback();
      GlobalSession session = new MockSuccessGlobalSession(clusterURL, username, password, syncKeyBundle, callback);
      HttpResponse response;
      response = new BasicHttpResponse(
          new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 200, "OK"));       

      response.addHeader("X-Weave-Backoff", "1801"); // Backoff given in seconds.
      session.interpretHTTPFailure(response);
      assertEquals(true, callback.calledBackoff);
      assertEquals(1801*1000, callback.weaveBackoff); // Backoff returned in milliseconds.
    } catch (Exception e) {
      e.printStackTrace();
      fail("Got exception.");
    }
  }
  
  @Test
  public void testBackoffNotCalledIfBackoffHeaderNotPresent() {
    String clusterURL = "http://localhost:8080/";
    String username   = "johndoe";
    String password   = "password";
    String syncKey    = "abcdeabcdeabcdeabcdeabcdea";
    KeyBundle syncKeyBundle = new KeyBundle(username, syncKey);
    
    try {
      MockBackoffCallback callback = new MockBackoffCallback();
      GlobalSession session = new MockSuccessGlobalSession(clusterURL, username, password, syncKeyBundle, callback);
      HttpResponse response;
      response = new BasicHttpResponse(
          new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 200, "OK"));       

      session.interpretHTTPFailure(response);
      assertEquals(false, callback.calledBackoff);
      assertEquals(-1, callback.weaveBackoff);
    } catch (Exception e) {
      e.printStackTrace();
      fail("Got exception.");
    }
  }
  
  @Test
  public void testBackoffCalledIfMultipleBackoffHeadersPresent() {
    String clusterURL = "http://localhost:8080/";
    String username   = "johndoe";
    String password   = "password";
    String syncKey    = "abcdeabcdeabcdeabcdeabcdea";
    KeyBundle syncKeyBundle = new KeyBundle(username, syncKey);
    
    try {
      MockBackoffCallback callback = new MockBackoffCallback();
      GlobalSession session = new MockSuccessGlobalSession(clusterURL, username, password, syncKeyBundle, callback);
      HttpResponse response;
      response = new BasicHttpResponse(
          new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 200, "OK"));       

      response.addHeader("Retry-After", "3001"); // Backoff given in seconds.
      response.addHeader("X-Weave-Backoff", "3001.5"); // If we now add a second header, the larger should be returned.
      session.interpretHTTPFailure(response);
      assertEquals(true, callback.calledBackoff);
      assertEquals(3001*1000 + 500, callback.weaveBackoff); // Backoff returned in milliseconds.
    } catch (Exception e) {
      e.printStackTrace();
      fail("Got exception.");
    }
  }
  
  @Test
  public void testBackoffCalledByHandleHTTPError() {
    String clusterURL = "http://localhost:8080/";
    String username   = "johndoe";
    String password   = "password";
    String syncKey    = "abcdeabcdeabcdeabcdeabcdea";
    KeyBundle syncKeyBundle = new KeyBundle(username, syncKey);

    try {
      MockAbortCallback callback = new MockAbortCallback();
      GlobalSession session = new MockSuccessGlobalSession(clusterURL, username, password, syncKeyBundle, callback);
      
      HttpResponse response;
      response = new BasicHttpResponse(
          new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 503, "Illegal method/protocol"));       
      
      response.addHeader("X-Weave-Backoff", "1801.5"); // Backoff given in seconds.
      session.handleHTTPError(new SyncStorageResponse(response), "Illegal method/protocol");
      assertEquals(true, callback.calledBackoff);
      assertEquals(true, callback.calledError);      
      assertEquals(1801*1000 + 500, callback.weaveBackoff); // Backoff returned in milliseconds.
    } catch (Exception e) {
      e.printStackTrace();
      fail("Got exception.");
    }
  }
}
