/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.background.healthreport.upload.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.mozilla.android.sync.test.helpers.MockSharedPreferences;
import org.mozilla.gecko.background.healthreport.HealthReportConstants;
import org.mozilla.gecko.background.healthreport.upload.SubmissionClient;
import org.mozilla.gecko.background.healthreport.upload.SubmissionPolicy;
import org.mozilla.gecko.background.healthreport.upload.test.TestSubmissionPolicy.MockSubmissionClient.Response;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.Utils;

import android.content.SharedPreferences;

public class TestSubmissionPolicy {
  public static class MockSubmissionClient implements SubmissionClient {
    public enum Response { SUCCESS, SOFT_FAILURE, HARD_FAILURE };
    public Response upload = Response.SUCCESS;
    public Response delete = Response.SUCCESS;

    protected void response(long localTime, String id, Delegate delegate, Response response) {
      switch (response) {
      case SOFT_FAILURE:
        delegate.onSoftFailure(localTime, id, "Soft failure.", null);
        break;
      case HARD_FAILURE:
        delegate.onHardFailure(localTime, id, "Hard failure.", null);
        break;
      default:
        delegate.onSuccess(localTime, id);
      }
    }

    @Override
    public void upload(long localTime, Delegate delegate) {
      response(localTime, Utils.generateGuid(), delegate, upload);
    }

    @Override
    public void delete(long localTime, String id, Delegate delegate) {
      response(localTime, id, delegate, delete);
    }
  }

  public MockSubmissionClient client;
  public SubmissionPolicy policy;
  public SharedPreferences sharedPrefs;


  public void setMinimumTimeBetweenUploads(long time) {
    sharedPrefs.edit().putLong(HealthReportConstants.PREF_MINIMUM_TIME_BETWEEN_UPLOADS, time).commit();
  }

  public void setMinimumTimeBeforeFirstSubmission(long time) {
    sharedPrefs.edit().putLong(HealthReportConstants.PREF_MINIMUM_TIME_BEFORE_FIRST_SUBMISSION, time).commit();
  }

  public void setCurrentDayFailureCount(long count) {
    sharedPrefs.edit().putLong(HealthReportConstants.PREF_CURRENT_DAY_FAILURE_COUNT, count).commit();
  }

  @Before
  public void setUp() throws Exception {
    sharedPrefs = new MockSharedPreferences();
    client = new MockSubmissionClient();
    policy = new SubmissionPolicy(sharedPrefs, client, true);
    setMinimumTimeBeforeFirstSubmission(0);
  }

  @Test
  public void testNoMinimumTimeBeforeFirstSubmission() {
    assertTrue(policy.tick(0));
  }

  @Test
  public void testMinimumTimeBeforeFirstSubmission() {
    setMinimumTimeBeforeFirstSubmission(10);
    assertFalse(policy.tick(0));
    assertEquals(policy.getMinimumTimeBeforeFirstSubmission(), policy.getNextSubmission());
    assertFalse(policy.tick(policy.getMinimumTimeBeforeFirstSubmission() - 1));
    assertTrue(policy.tick(policy.getMinimumTimeBeforeFirstSubmission()));
  }

  @Test
  public void testNextUpload() {
    assertTrue(policy.tick(0));
    assertEquals(policy.getMinimumTimeBetweenUploads(), policy.getNextSubmission());
    assertFalse(policy.tick(policy.getMinimumTimeBetweenUploads() - 1));
    assertTrue(policy.tick(policy.getMinimumTimeBetweenUploads()));
  }

  @Test
  public void testLastUploadRequested() {
    assertTrue(policy.tick(0));
    assertEquals(0, policy.getLastUploadRequested());
    assertFalse(policy.tick(1));
    assertEquals(0, policy.getLastUploadRequested());
    assertTrue(policy.tick(2*policy.getMinimumTimeBetweenUploads()));
    assertEquals(2*policy.getMinimumTimeBetweenUploads(), policy.getLastUploadRequested());
  }

  @Test
  public void testUploadSuccess() throws Exception {
    assertTrue(policy.tick(0));
    setCurrentDayFailureCount(1);
    client.upload = Response.SUCCESS;
    assertTrue(policy.tick(2*policy.getMinimumTimeBetweenUploads()));
    assertEquals(2*policy.getMinimumTimeBetweenUploads(), policy.getLastUploadRequested());
    assertEquals(2*policy.getMinimumTimeBetweenUploads(), policy.getLastUploadSucceeded());
    assertTrue(policy.getNextSubmission() > policy.getLastUploadSucceeded());
    assertEquals(0, policy.getCurrentDayFailureCount());
  }

  @Test
  public void testUploadSoftFailure() throws Exception {
    assertTrue(policy.tick(0));
    client.upload = Response.SOFT_FAILURE;

    assertTrue(policy.tick(2*policy.getMinimumTimeBetweenUploads()));
    assertEquals(2*policy.getMinimumTimeBetweenUploads(), policy.getLastUploadRequested());
    assertEquals(2*policy.getMinimumTimeBetweenUploads(), policy.getLastUploadFailed());
    assertEquals(1, policy.getCurrentDayFailureCount());
    assertEquals(policy.getLastUploadFailed() + policy.getMinimumTimeAfterFailure(), policy.getNextSubmission());

    assertTrue(policy.tick(3*policy.getMinimumTimeBetweenUploads()));
    assertEquals(3*policy.getMinimumTimeBetweenUploads(), policy.getLastUploadRequested());
    assertEquals(3*policy.getMinimumTimeBetweenUploads(), policy.getLastUploadFailed());
    assertEquals(2, policy.getCurrentDayFailureCount());
    assertEquals(policy.getLastUploadFailed() + policy.getMinimumTimeAfterFailure(), policy.getNextSubmission());

    assertTrue(policy.tick(4*policy.getMinimumTimeBetweenUploads()));
    assertEquals(4*policy.getMinimumTimeBetweenUploads(), policy.getLastUploadRequested());
    assertEquals(4*policy.getMinimumTimeBetweenUploads(), policy.getLastUploadFailed());
    assertEquals(0, policy.getCurrentDayFailureCount());
    assertEquals(policy.getLastUploadFailed() + policy.getMinimumTimeBetweenUploads(), policy.getNextSubmission());
  }

  @Test
  public void testUploadHardFailure() throws Exception {
    assertTrue(policy.tick(0));
    client.upload = Response.HARD_FAILURE;

    assertTrue(policy.tick(2*policy.getMinimumTimeBetweenUploads()));
    assertEquals(2*policy.getMinimumTimeBetweenUploads(), policy.getLastUploadRequested());
    assertEquals(2*policy.getMinimumTimeBetweenUploads(), policy.getLastUploadFailed());
    assertEquals(0, policy.getCurrentDayFailureCount());
    assertEquals(policy.getLastUploadFailed() + policy.getMinimumTimeBetweenUploads(), policy.getNextSubmission());
  }

  @Test
  public void testDisabledNoObsoleteDocuments() throws Exception {
    policy = new SubmissionPolicy(new MockSharedPreferences(), client, false);
    assertFalse(policy.tick(0));
  }

  @Test
  public void testDecrement() {
    MockSharedPreferences tempPrefs = new MockSharedPreferences();
    policy = new SubmissionPolicy(tempPrefs, client, false);
    ExtendedJSONObject ids = new ExtendedJSONObject();
    ids.put("id1", 5L);
    ids.put("id2", 5L);
    policy.setObsoleteIds(ids);
    assertEquals(ids, policy.getObsoleteIds());

    policy.decrementObsoleteIdAttempts("id1");
    ids.put("id1", 4L);
    assertEquals(ids, policy.getObsoleteIds());

    policy.decrementObsoleteIdAttempts("id1"); // 3
    policy.decrementObsoleteIdAttempts("id1"); // 2
    policy.decrementObsoleteIdAttempts("id1"); // 1
    policy.decrementObsoleteIdAttempts("id1"); // 0 (should be gone).
    ids.remove("id1");
    assertEquals(ids, policy.getObsoleteIds());

    policy.removeObsoleteId("id2");
    ids.remove("id2");
    assertEquals(ids, policy.getObsoleteIds());
  }

  @Test
  public void testDisabledObsoleteDocumentsSuccess() throws Exception {
    policy = new SubmissionPolicy(new MockSharedPreferences(), client, false);
    setMinimumTimeBetweenUploads(policy.getMinimumTimeBetweenUploads() - 1);
    ExtendedJSONObject ids = new ExtendedJSONObject();
    ids.put("id1", 5L);
    ids.put("id2", 5L);
    policy.setObsoleteIds(ids);

    assertTrue(policy.tick(3));
    assertEquals(1, policy.getObsoleteIds().size());

    // Forensic timestamps.
    assertEquals(3 + policy.getMinimumTimeBetweenDeletes(), policy.getNextSubmission());
    assertEquals(3, policy.getLastDeleteRequested());
    assertEquals(-1, policy.getLastDeleteFailed());
    assertEquals(3, policy.getLastDeleteSucceeded());

    assertTrue(policy.tick(2*policy.getMinimumTimeBetweenDeletes()));
    assertEquals(0, policy.getObsoleteIds().size());

    assertFalse(policy.tick(4*policy.getMinimumTimeBetweenDeletes()));
  }

  @Test
  public void testDisabledObsoleteDocumentsSoftFailure() throws Exception {
    client.delete = Response.SOFT_FAILURE;
    policy = new SubmissionPolicy(new MockSharedPreferences(), client, false);
    setMinimumTimeBetweenUploads(policy.getMinimumTimeBetweenUploads() - 2);
    ExtendedJSONObject ids = new ExtendedJSONObject();
    ids.put("id1", 5L);
    ids.put("id2", 5L);
    policy.setObsoleteIds(ids);

    assertTrue(policy.tick(3));
    ids.put("id1", 4L); // policy's choice is deterministic.
    assertEquals(ids, policy.getObsoleteIds());

    // Forensic timestamps.
    assertEquals(3 + policy.getMinimumTimeBetweenDeletes(), policy.getNextSubmission());
    assertEquals(3, policy.getLastDeleteRequested());
    assertEquals(3, policy.getLastDeleteFailed());
    assertEquals(-1, policy.getLastDeleteSucceeded());

    assertTrue(policy.tick(2*policy.getMinimumTimeBetweenDeletes())); // 3.
    assertTrue(policy.tick(4*policy.getMinimumTimeBetweenDeletes())); // 2.
    assertTrue(policy.tick(6*policy.getMinimumTimeBetweenDeletes())); // 1.
    assertTrue(policy.tick(8*policy.getMinimumTimeBetweenDeletes())); // 0 (should be gone).
    ids.remove("id1");
    assertEquals(ids, policy.getObsoleteIds());

    assertTrue(policy.tick(10*policy.getMinimumTimeBetweenDeletes()));
    ids.put("id2", 4L);
    assertEquals(ids, policy.getObsoleteIds());
  }

  @Test
  public void testDisabledObsoleteDocumentsHardFailure() throws Exception {
    client.delete = Response.HARD_FAILURE;
    policy = new SubmissionPolicy(new MockSharedPreferences(), client, false);
    setMinimumTimeBetweenUploads(policy.getMinimumTimeBetweenUploads() - 3);
    ExtendedJSONObject ids = new ExtendedJSONObject();
    ids.put("id1", 5L);
    ids.put("id2", 5L);
    policy.setObsoleteIds(ids);

    assertTrue(policy.tick(3));
    ids.remove("id1"); // policy's choice is deterministic.
    assertEquals(ids, policy.getObsoleteIds());

    // Forensic timestamps.
    assertEquals(3 + policy.getMinimumTimeBetweenDeletes(), policy.getNextSubmission());
    assertEquals(3, policy.getLastDeleteRequested());
    assertEquals(3, policy.getLastDeleteFailed());
    assertEquals(-1, policy.getLastDeleteSucceeded());

    assertTrue(policy.tick(2*policy.getMinimumTimeBetweenDeletes())); // 3.
    ids.remove("id2");
    assertEquals(ids, policy.getObsoleteIds());
  }
}
