/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.background.healthreport.upload.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;
import org.mozilla.android.sync.test.helpers.HTTPServerTestHelper;
import org.mozilla.android.sync.test.helpers.MockResourceDelegate;
import org.mozilla.gecko.background.healthreport.HealthReportStorage;
import org.mozilla.gecko.background.healthreport.upload.AndroidSubmissionClient.SubmissionsTracker.TrackingRequestDelegate;
import org.mozilla.gecko.background.healthreport.upload.test.MockAndroidSubmissionClient.MockHealthReportStorage;
import org.mozilla.gecko.background.testhelpers.StubDelegate;
import org.mozilla.gecko.background.testhelpers.WaitHelper;
import org.mozilla.gecko.sync.net.BaseResource;

import ch.boye.httpclientandroidlib.HttpResponse;

public class TestTrackingRequestDelegate {
  public static class MockAndroidSubmissionClient2 extends MockAndroidSubmissionClient {
    public MockAndroidSubmissionClient2() {
      super(null, null, null);
    }

    @Override
    public void setLastUploadLocalTimeAndDocumentId(long localTime, String id) { /* Do nothing. */ }

    public class MockSubmissionsTracker2 extends MockSubmissionsTracker {
      public HashSet<InvocationResult> invocationResults;

      public MockSubmissionsTracker2(final HealthReportStorage storage) {
        super(storage, 1, false);
        reset();
      }

      public void reset() {
        invocationResults = new HashSet<InvocationResult>(InvocationResult.values().length);
      }

      @Override
      public void incrementFirstUploadAttemptCount() { /* Do nothing. */ }

      @Override
      public void incrementContinuationAttemptCount() { /* Do nothing. */ }

      @Override
      public void incrementUploadSuccessCount() {
        invocationResults.add(InvocationResult.SUCCESS);
      }

      @Override
      public void incrementUploadClientFailureCount() {
        invocationResults.add(InvocationResult.CLIENT_FAILURE);
      }

      @Override
      public void incrementUploadTransportFailureCount() {
        invocationResults.add(InvocationResult.TRANSPORT_FAILURE);
      }

      @Override
      public void incrementUploadServerFailureCount() {
        invocationResults.add(InvocationResult.SERVER_FAILURE);
      }

      public boolean gotResult(final InvocationResult resultToCheck) {
        return invocationResults.contains(resultToCheck);
      }

      @Override
      protected void setLastSkew(final HttpResponse response) {
        // By default, this writes to storage, which causes a stub failure.
      }

      // Public for testing.
      @Override
      public int getCappedSkewInSeconds(final String serverURI, final long now, final HttpResponse response) {
        return super.getCappedSkewInSeconds(serverURI, now, response);
      }

      /**
       * Asserts that the given result has been received and all others have not been. Passing null
       * will assert that no results have been received.
       */
      public void assertResult(final InvocationResult expectedResult) {
        for (InvocationResult compareResult : InvocationResult.values()) {
          if (compareResult == expectedResult) {
            assertTrue(gotResult(compareResult));
          } else {
            assertFalse(gotResult(compareResult));
          }
        }
      }
    }
  }

  public static enum InvocationResult { SUCCESS, CLIENT_FAILURE, TRANSPORT_FAILURE, SERVER_FAILURE }

  public MockAndroidSubmissionClient2.MockSubmissionsTracker2 tracker;
  public TrackingRequestDelegate delegate;

  @Before
  public void setUp() throws Exception {
    final MockAndroidSubmissionClient2 client = new MockAndroidSubmissionClient2();
    final MockHealthReportStorage storage = new MockHealthReportStorage();
    tracker = client.new MockSubmissionsTracker2(storage);
    delegate = tracker.new TrackingRequestDelegate(new StubDelegate(), 1, true, null);
  }

  @Test
  public void testHandleSuccess() throws Exception {
    delegate.handleSuccess(200, null, null, null);
    tracker.assertResult(InvocationResult.SUCCESS);
  }

  @Test
  public void testHandleFailure() throws Exception {
    delegate.handleFailure(404, null, null);
    tracker.assertResult(InvocationResult.SERVER_FAILURE);
  }

  @Test
  public void testHandleError() throws Exception {
    delegate.handleError(new IllegalStateException());
    tracker.assertResult(InvocationResult.TRANSPORT_FAILURE);
    tracker.reset();

    final Exception[] clientExceptions = new Exception[] {
        new IllegalArgumentException(),
        new UnsupportedEncodingException(),
        new URISyntaxException("input", "a good raisin")
    };
    for (Exception e : clientExceptions) {
      delegate.handleError(e);
      tracker.assertResult(InvocationResult.CLIENT_FAILURE);
      tracker.reset();
    }
  }

  @Test
  public void testGetCappedSkewInSeconds() throws Exception {
    // Here's an inefficient but somewhat realistic way to create an HttpResponse.
    final MockResourceDelegate mockResourceDelegate = new MockResourceDelegate(WaitHelper.getTestWaiter());
    BaseResource.rewriteLocalhost = false;
    final int TEST_PORT = HTTPServerTestHelper.getTestPort();
    final String TEST_SERVER = "http://localhost:" + TEST_PORT;
    final HTTPServerTestHelper data = new HTTPServerTestHelper();
    data.startHTTPServer();
    try {
        final BaseResource r = new BaseResource(TEST_SERVER);
        r.delegate = mockResourceDelegate;
        WaitHelper.getTestWaiter().performWait(new Runnable() {
          @Override
          public void run() {
            r.get();
          }
        });
    } finally {
      data.stopHTTPServer();
    }

    final HttpResponse response = mockResourceDelegate.httpResponse;
    assertNotNull(response);

    // On the same machine, the timestamp from the response should be in the
    // past. This is in seconds, so we expect to actually get 0 seconds skew.
    final long now = System.currentTimeMillis();
    assertTrue(tracker.getCappedSkewInSeconds(TEST_SERVER, now, response) <= 0);
    assertTrue(tracker.getCappedSkewInSeconds(TEST_SERVER, now + 1000, response) < 0);

    // A simple verification that the skew is monotonically decreasing as local
    // time advances. That is, we need to shift local time backwards more in
    // order to match the remote clock as local time advances.
    assertTrue(
        tracker.getCappedSkewInSeconds(TEST_SERVER, now + 1000, response) <
        tracker.getCappedSkewInSeconds(TEST_SERVER, now, response));
  }
}
