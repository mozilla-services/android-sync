/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.background.healthreport.upload.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;

import org.mozilla.gecko.background.healthreport.test.HealthReportStorageStub;
import org.mozilla.gecko.background.healthreport.upload.AndroidSubmissionClient.SubmissionsFieldName;
import org.mozilla.gecko.background.healthreport.upload.AndroidSubmissionClient.SubmissionsStatusCounter;

public class TestSubmissionsStatusCounter {
  protected static class MockHealthReportStorage extends HealthReportStorageStub {
    private final HashSet<Integer> GUARDED_FIELDS = new HashSet<Integer>(Arrays.asList(new Integer[] {
      SubmissionsFieldName.SUCCESS.getID(this),
      SubmissionsFieldName.CLIENT_FAILURE.getID(this),
      SubmissionsFieldName.TRANSPORT_FAILURE.getID(this),
      SubmissionsFieldName.SERVER_FAILURE.getID(this)
    }));

    private boolean hasIncrementedGuardedFields = false;
    public boolean hasIncrementedDailyCount = false;

    @Override
    public void incrementDailyCount(int env, int day, int field) {
      hasIncrementedDailyCount = true;

      if (GUARDED_FIELDS.contains(field)) {
        if (hasIncrementedGuardedFields) {
          fail("incrementDailyCount called twice with the same guarded field.");
        }
        hasIncrementedGuardedFields = true;
      }
    }

    @Override
    public Field getField(String mName, int mVersion, String fieldName) {
      return new Field(mName, mVersion, fieldName, 0) {
        @Override
        public int getID() throws IllegalStateException {
          return fieldName.hashCode();
        }
      };
    }
  }

  private SubmissionsStatusCounter counter;
  private MockHealthReportStorage storage;

  @Before
  public void setUp() throws Exception {
    storage = new MockHealthReportStorage();
    counter = new SubmissionsStatusCounter(0, 0, storage);
  }

  @Test
  public void testIncrementFirstUploadAttemptCount() throws Exception {
    counter.incrementFirstUploadAttemptCount();
    assertTrue(storage.hasIncrementedDailyCount);
  }

  @Test
  public void testIncrementContinuationAttemptCount() throws Exception {
    counter.incrementContinuationAttemptCount();
    assertTrue(storage.hasIncrementedDailyCount);
  }

  @Test
  public void testIncrementUploadSuccessCount() throws Exception {
    counter.incrementUploadSuccessCount();
    assertTrue(storage.hasIncrementedDailyCount);
  }

  @Test
  public void testIncrementClientFailureCount() throws Exception {
    counter.incrementUploadClientFailureCount();
    assertTrue(storage.hasIncrementedDailyCount);
  }

  @Test
  public void testIncrementServerFailureCount() throws Exception {
    counter.incrementUploadServerFailureCount();
    assertTrue(storage.hasIncrementedDailyCount);
  }

  @Test
  public void testIncrementUploadTransportFailureCount() throws Exception {
    counter.incrementUploadTransportFailureCount();
    assertTrue(storage.hasIncrementedDailyCount);
  }

  @Test
  public void testIncrementCountGuardedFields() throws Exception {
    // Assertions take place in overridden HealthReportStorage methods.
    counter.incrementUploadSuccessCount();
    counter.incrementUploadClientFailureCount();
    counter.incrementUploadServerFailureCount();
    counter.incrementUploadTransportFailureCount();
  }
}
