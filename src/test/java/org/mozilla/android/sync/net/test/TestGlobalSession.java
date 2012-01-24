/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.net.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.json.simple.parser.ParseException;
import org.junit.Test;
import org.mozilla.gecko.sync.GlobalSession;
import org.mozilla.gecko.sync.NonObjectJSONException;
import org.mozilla.gecko.sync.SyncConfigurationException;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.delegates.GlobalSessionCallback;
import org.mozilla.gecko.sync.stage.FetchInfoCollectionsStage;
import org.mozilla.gecko.sync.stage.GlobalSyncStage.Stage;
import org.mozilla.gecko.sync.net.SyncStorageResponse;

import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.ProtocolVersion;
import ch.boye.httpclientandroidlib.message.BasicHttpResponse;
import ch.boye.httpclientandroidlib.message.BasicStatusLine;

import org.mozilla.android.sync.test.helpers.MockSharedPreferences;
import org.mozilla.android.sync.test.helpers.MockGlobalSession;
import org.mozilla.android.sync.test.helpers.MockGlobalSessionCallback;
import org.mozilla.android.sync.test.helpers.WaitHelper;

public class TestGlobalSession {
  private final String TEST_CLUSTER_URL		= "http://localhost:8080/";
  private final String TEST_USERNAME		= "johndoe";
  private final String TEST_PASSWORD		= "password";
  private final String TEST_SYNC_KEY		= "abcdeabcdeabcdeabcdeabcdea";
  private final long   TEST_BACKOFF_IN_SECONDS	= 2401;
  
  /**
   * A mock GlobalSession that fakes a 503 on info/collections and
   * sets X-Weave-Backoff header to the specified number of seconds.
   */
  public class MockBackoffGlobalSession extends MockGlobalSession {

    public MockSharedPreferences prefs;
    public long backoffInSeconds = -1;
    
    public MockBackoffGlobalSession(long backoffInSeconds,
				    String clusterURL, String username, String password,
				    KeyBundle syncKeyBundle, GlobalSessionCallback callback)
      throws SyncConfigurationException, IllegalArgumentException, IOException, ParseException, NonObjectJSONException {
      super(clusterURL, username, password, syncKeyBundle, callback);
      this.backoffInSeconds = backoffInSeconds;
    }

    public class MockBackoffFetchInfoCollectionsStage extends FetchInfoCollectionsStage {
      @Override
      public void execute(GlobalSession session) {
        HttpResponse response;
        response = new BasicHttpResponse(
            new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 503, "Illegal method/protocol"));       

        response.addHeader("X-Weave-Backoff", Long.toString(backoffInSeconds)); // Backoff given in seconds.        
        session.handleHTTPError(new SyncStorageResponse(response), "Failure fetching info/collections.");
      }
    }

    @Override
    protected void prepareStages() {
      super.prepareStages();
      stages.put(Stage.fetchInfoCollections, new MockBackoffFetchInfoCollectionsStage());
    }
  }
  
  /**
   * Test that handleHTTPError does in fact backoff.
   */
  @Test
  public void testBackoffCalledByHandleHTTPError() {
    try {
      final MockGlobalSessionCallback callback = new MockGlobalSessionCallback();
      final GlobalSession session = new MockGlobalSession(TEST_CLUSTER_URL, TEST_USERNAME, TEST_PASSWORD,
        new KeyBundle(TEST_USERNAME, TEST_SYNC_KEY), callback);
      
      final HttpResponse response;
      response = new BasicHttpResponse(
          new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 503, "Illegal method/protocol"));             
      response.addHeader("X-Weave-Backoff", Long.toString(TEST_BACKOFF_IN_SECONDS)); // Backoff given in seconds.
      
      WaitHelper.getTestWaiter().performWaitAfterSpawningThread(new Runnable() {
        public void run() {
          session.handleHTTPError(new SyncStorageResponse(response), "Illegal method/protocol");
        }
      });
      
      assertEquals(false, callback.calledSuccess);
      assertEquals(true,  callback.calledError);
      assertEquals(false, callback.calledAborted);
      assertEquals(true, callback.calledRequestBackoff);
      assertEquals(TEST_BACKOFF_IN_SECONDS * 1000, callback.weaveBackoff); // Backoff returned in milliseconds.
    } catch (Exception e) {
      e.printStackTrace();
      fail("Got exception.");
    }
  }
  

  /**
   * Test that a trivially successful GlobalSession does not fail or backoff.
   */
  @Test
  public void testSuccessCalledAfterStages() {
    try {
      final MockGlobalSessionCallback callback = new MockGlobalSessionCallback();
      final GlobalSession session = new MockGlobalSession(TEST_CLUSTER_URL, TEST_USERNAME, TEST_PASSWORD,
        new KeyBundle(TEST_USERNAME, TEST_SYNC_KEY), callback);

      WaitHelper.getTestWaiter().performWaitAfterSpawningThread(new Runnable() {
        public void run() {
          try {
            session.start();
          } catch(Exception e) {
            WaitHelper.getTestWaiter().performNotify(new AssertionError(e));
          }
        }
      });      
      
      assertEquals(true,  callback.calledSuccess);
      assertEquals(false, callback.calledError);
      assertEquals(false, callback.calledAborted);
      assertEquals(false, callback.calledRequestBackoff);
    } catch (Exception e) {
      e.printStackTrace();
      fail("Got exception.");
    }
  }
  
  /**
   * Test that a failing GlobalSession does in fact fail and back off.
   */
  @Test
  public void testBackoffCalledInStages() {
    try {
      final MockGlobalSessionCallback callback = new MockGlobalSessionCallback();
      final GlobalSession session = new MockBackoffGlobalSession(TEST_BACKOFF_IN_SECONDS, TEST_CLUSTER_URL, TEST_USERNAME, TEST_PASSWORD,
        new KeyBundle(TEST_USERNAME, TEST_SYNC_KEY), callback);

      WaitHelper.getTestWaiter().performWaitAfterSpawningThread(new Runnable() {
        public void run() {
          try {
            session.start();
          } catch(Exception e) {
            WaitHelper.getTestWaiter().performNotify(new AssertionError(e));
          }
        }
      });
      
      assertEquals(false, callback.calledSuccess);
      assertEquals(true,  callback.calledError);
      assertEquals(false, callback.calledAborted);
      assertEquals(true,  callback.calledRequestBackoff);
      assertEquals(TEST_BACKOFF_IN_SECONDS * 1000, callback.weaveBackoff); // Backoff returned in milliseconds.
    } catch (Exception e) {
      e.printStackTrace();
      fail("Got exception.");
    }
  }
}
