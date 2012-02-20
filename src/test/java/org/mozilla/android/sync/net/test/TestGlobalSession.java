/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.net.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;

import junit.framework.AssertionFailedError;

import org.json.simple.parser.ParseException;
import org.junit.Test;
import org.mozilla.android.sync.test.helpers.MockGlobalSession;
import org.mozilla.android.sync.test.helpers.MockGlobalSessionCallback;
import org.mozilla.android.sync.test.helpers.MockSharedPreferences;
import org.mozilla.android.sync.test.helpers.WaitHelper;
import org.mozilla.gecko.sync.GlobalSession;
import org.mozilla.gecko.sync.HTTPFailureException;
import org.mozilla.gecko.sync.NonObjectJSONException;
import org.mozilla.gecko.sync.SyncConfigurationException;
import org.mozilla.gecko.sync.crypto.CryptoException;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.delegates.GlobalSessionCallback;
import org.mozilla.gecko.sync.net.SyncStorageResponse;
import org.mozilla.gecko.sync.stage.FetchInfoCollectionsStage;
import org.mozilla.gecko.sync.stage.GlobalSyncStage.Stage;

import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.ProtocolVersion;
import ch.boye.httpclientandroidlib.message.BasicHttpResponse;
import ch.boye.httpclientandroidlib.message.BasicStatusLine;

public class TestGlobalSession {
  private final String TEST_CLUSTER_URL         = "http://localhost:8080/";
  private final String TEST_USERNAME            = "johndoe";
  private final String TEST_PASSWORD            = "password";
  private final String TEST_SYNC_KEY            = "abcdeabcdeabcdeabcdeabcdea";
  private final long   TEST_BACKOFF_IN_SECONDS  = 2401;

  public WaitHelper getTestWaiter() {
    return WaitHelper.getTestWaiter();
  }

  /**
   * A mock GlobalSession that fakes the info/collections stage.
   */
  public class MockGlobalSessionWithInfoCollectionsStage extends MockGlobalSession {

    public MockSharedPreferences prefs;

    public MockGlobalSessionWithInfoCollectionsStage(GlobalSessionCallback callback)
            throws SyncConfigurationException, IllegalArgumentException, IOException, ParseException, NonObjectJSONException, CryptoException {
      super(TEST_CLUSTER_URL, TEST_USERNAME, TEST_PASSWORD, new KeyBundle(TEST_USERNAME, TEST_SYNC_KEY), callback);
    }

    public void infoCollectionsStage(GlobalSession session) {
      session.advance();
    }

    public class MockInfoCollectionsStage extends FetchInfoCollectionsStage {
      @Override
      public void execute(GlobalSession session) {
        infoCollectionsStage(session);
      }
    }

    @Override
    protected void prepareStages() {
      super.prepareStages();
      stages.put(Stage.fetchInfoCollections, new MockInfoCollectionsStage());
    }
  }

  /**
   * Test that interpretHTTPFailure calls requestBackoff if
   * X-Weave-Backoff is present.
   */
  @Test
  public void testBackoffCalledIfBackoffHeaderPresent() {
    try {
      final MockGlobalSessionCallback callback = new MockGlobalSessionCallback();
      final GlobalSession session = new MockGlobalSession(TEST_CLUSTER_URL, TEST_USERNAME, TEST_PASSWORD,
        new KeyBundle(TEST_USERNAME, TEST_SYNC_KEY), callback);

      final HttpResponse response = new BasicHttpResponse(
        new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 200, "OK"));
      response.addHeader("X-Weave-Backoff", Long.toString(TEST_BACKOFF_IN_SECONDS)); // Backoff given in seconds.

      session.interpretHTTPFailure(new SyncStorageResponse(response)); // This is synchronous...

      assertEquals(false, callback.calledSuccess); // ... so we can test immediately.
      assertEquals(false, callback.calledError);
      assertEquals(false, callback.calledAborted);
      assertEquals(true,  callback.calledRequestBackoff);
      assertEquals(TEST_BACKOFF_IN_SECONDS * 1000, callback.weaveBackoff); // Backoff returned in milliseconds.
    } catch (Exception e) {
      e.printStackTrace();
      fail("Got exception.");
    }
  }

  /**
   * Test that interpretHTTPFailure does not call requestBackoff if
   * X-Weave-Backoff is not present.
   */
  @Test
  public void testBackoffNotCalledIfBackoffHeaderNotPresent() {
    try {
      final MockGlobalSessionCallback callback = new MockGlobalSessionCallback();
      final GlobalSession session = new MockGlobalSession(TEST_CLUSTER_URL, TEST_USERNAME, TEST_PASSWORD,
        new KeyBundle(TEST_USERNAME, TEST_SYNC_KEY), callback);

      final HttpResponse response = new BasicHttpResponse(
  new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 200, "OK"));

      session.interpretHTTPFailure(new SyncStorageResponse(response)); // This is synchronous...

      assertEquals(false, callback.calledSuccess); // ... so we can test immediately.
      assertEquals(false, callback.calledError);
      assertEquals(false, callback.calledAborted);
      assertEquals(false, callback.calledRequestBackoff);
    } catch (Exception e) {
      e.printStackTrace();
      fail("Got exception.");
    }
  }

  /**
   * Test that interpretHTTPFailure calls requestBackoff with the
   * largest specified value if X-Weave-Backoff and Retry-After are
   * present.
   */
  @Test
  public void testBackoffCalledIfMultipleBackoffHeadersPresent() {
    try {
      final MockGlobalSessionCallback callback = new MockGlobalSessionCallback();
      final GlobalSession session = new MockGlobalSession(TEST_CLUSTER_URL, TEST_USERNAME, TEST_PASSWORD,
        new KeyBundle(TEST_USERNAME, TEST_SYNC_KEY), callback);

      final HttpResponse response = new BasicHttpResponse(
        new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 200, "OK"));
      response.addHeader("Retry-After", Long.toString(TEST_BACKOFF_IN_SECONDS)); // Backoff given in seconds.
      response.addHeader("X-Weave-Backoff", Long.toString(TEST_BACKOFF_IN_SECONDS + 1)); // If we now add a second header, the larger should be returned.

      session.interpretHTTPFailure(new SyncStorageResponse(response)); // This is synchronous...

      assertEquals(false, callback.calledSuccess); // ... so we can test immediately.
      assertEquals(false, callback.calledError);
      assertEquals(false, callback.calledAborted);
      assertEquals(true,  callback.calledRequestBackoff);
      assertEquals((TEST_BACKOFF_IN_SECONDS + 1) * 1000, callback.weaveBackoff); // Backoff returned in milliseconds.
    } catch (Exception e) {
      e.printStackTrace();
      fail("Got exception.");
    }
  }

  /**
   * Test that abort does in fact backoff.
   */
  @Test
  public void testBackoffCalledByHandleHTTPError() {
    try {
      final MockGlobalSessionCallback callback = new MockGlobalSessionCallback();
      final GlobalSession session = new MockGlobalSession(TEST_CLUSTER_URL, TEST_USERNAME, TEST_PASSWORD,
        new KeyBundle(TEST_USERNAME, TEST_SYNC_KEY), callback);

      final HttpResponse response = new BasicHttpResponse(
        new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 503, "Illegal method/protocol"));
      response.addHeader("X-Weave-Backoff", Long.toString(TEST_BACKOFF_IN_SECONDS)); // Backoff given in seconds.

      getTestWaiter().performWait(WaitHelper.onThreadRunnable(new Runnable() {
        public void run() {
          session.abort(new HTTPFailureException(new SyncStorageResponse(response)), "Illegal method/protocol");
        }
      }));

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


  /**
   * Test that a trivially successful GlobalSession does not fail or backoff.
   */
  @Test
  public void testSuccessCalledAfterStages() {
    try {
      final MockGlobalSessionCallback callback = new MockGlobalSessionCallback();
      final GlobalSession session = new MockGlobalSession(TEST_CLUSTER_URL, TEST_USERNAME, TEST_PASSWORD,
        new KeyBundle(TEST_USERNAME, TEST_SYNC_KEY), callback);

      getTestWaiter().performWait(WaitHelper.onThreadRunnable(new Runnable() {
        public void run() {
          try {
            session.start();
          } catch (Exception e) {
            final AssertionFailedError error = new AssertionFailedError();
            error.initCause(e);
            getTestWaiter().performNotify(error);
          }
        }
      }));

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
      final GlobalSession session = new MockGlobalSessionWithInfoCollectionsStage(callback) {
        @Override
        public void infoCollectionsStage(GlobalSession session) {
          final HttpResponse response = new BasicHttpResponse(
              new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 503, "Illegal method/protocol"));

          response.addHeader("X-Weave-Backoff", Long.toString(TEST_BACKOFF_IN_SECONDS)); // Backoff given in seconds.
          session.abort(new HTTPFailureException(new SyncStorageResponse(response)), "Failure fetching info/collections.");
        }
      };

      getTestWaiter().performWait(WaitHelper.onThreadRunnable(new Runnable() {
        public void run() {
          try {
            session.start();
          } catch (Exception e) {
            final AssertionFailedError error = new AssertionFailedError();
            error.initCause(e);
            getTestWaiter().performNotify(error);
          }
        }
      }));

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

  /**
   * Test that a failling session calls requestNewNodeAssignment on 401 HTTP status code.
   */
  @Test
  public void testNewNodeAssignmentCalledOn401() throws SyncConfigurationException, IllegalArgumentException, NonObjectJSONException, IOException, ParseException, CryptoException {
    final MockGlobalSessionCallback callback = new MockGlobalSessionCallback();
    final GlobalSession session = new MockGlobalSessionWithInfoCollectionsStage(callback) {
      @Override
      public void infoCollectionsStage(GlobalSession session) {
        final HttpResponse response = new BasicHttpResponse(
            new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 401, "User not found"));

        session.abort(new HTTPFailureException(new SyncStorageResponse(response)), "Failure fetching info/collections.");
      }
    };

    getTestWaiter().performWait(WaitHelper.onThreadRunnable(new Runnable() {
      public void run() {
        try {
          session.start();
        } catch (Exception e) {
          final AssertionFailedError error = new AssertionFailedError();
          error.initCause(e);
          getTestWaiter().performNotify(error);
        }
      }
    }));

    assertEquals(false, callback.calledSuccess);
    assertEquals(true,  callback.calledError);
    assertEquals(false, callback.calledAborted);
    assertEquals(false, callback.calledRequestBackoff);
    assertEquals(true,  callback.calledRequestNewNodeAssignment);
  }
}
