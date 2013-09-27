/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.background.healthreport.upload.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;
import org.mozilla.gecko.background.healthreport.testhelpers.StubDelegate;
import org.mozilla.gecko.background.healthreport.upload.AndroidSubmissionClient;
import org.mozilla.gecko.background.healthreport.upload.AndroidSubmissionClient.SubmissionsStatusCounter;
import org.mozilla.gecko.background.healthreport.upload.AndroidSubmissionClient.UploadRequestDelegate;

import android.content.Context;
import android.content.SharedPreferences;

public class TestUploadRequestDelegate {
  public static class MockSubmissionsStatusCounter extends SubmissionsStatusCounter {
    public HashMap<InvocationResult, Boolean> invocationResults;

    public MockSubmissionsStatusCounter() {
      super(0, 0, null);
      invocationResults = new HashMap<InvocationResult, Boolean>();
    }

    @Override
    public void incrementUploadSuccessCount() {
      invocationResults.put(InvocationResult.SUCCESS, true);
    }

    @Override
    public void incrementUploadClientFailureCount() {
      invocationResults.put(InvocationResult.CLIENT_FAILURE, true);
    }

    @Override
    public void incrementUploadTransportFailureCount() {
      invocationResults.put(InvocationResult.TRANSPORT_FAILURE, true);
    }

    @Override
    public void incrementUploadServerFailureCount() {
      invocationResults.put(InvocationResult.SERVER_FAILURE, true);
    }

    public boolean gotResult(final InvocationResult resultToCheck) {
      final Boolean gotResult = invocationResults.get(resultToCheck);
      return gotResult == null ? false : gotResult;
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

  public static class MockAndroidSubmissionClient extends AndroidSubmissionClient {
    public MockAndroidSubmissionClient() {
      super(null, null, null);
    }

    @Override
    public void setLastUploadLocalTimeAndDocumentId(long localTime, String id) { /* Do nothing. */ }
  }

  public static enum InvocationResult { SUCCESS, CLIENT_FAILURE, TRANSPORT_FAILURE, SERVER_FAILURE }

  public MockSubmissionsStatusCounter statusCounter;
  public AndroidSubmissionClient.UploadRequestDelegate delegate;

  @Before
  public void setUp() throws Exception {
    final MockAndroidSubmissionClient client = new MockAndroidSubmissionClient();
    statusCounter = new MockSubmissionsStatusCounter();
    delegate = client.new UploadRequestDelegate(new StubDelegate(), 0, true, null, statusCounter);
  }

  @Test
  public void testHandleSuccess() throws Exception {
    delegate.handleSuccess(0, null, null, null);
    statusCounter.assertResult(InvocationResult.SUCCESS);
  }
}
