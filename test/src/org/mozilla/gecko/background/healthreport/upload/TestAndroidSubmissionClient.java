/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.background.healthreport.upload;

import java.util.Collection;

import org.mozilla.gecko.background.bagheera.BagheeraRequestDelegate;
import org.mozilla.gecko.background.healthreport.HealthReportDatabaseStorage;
import org.mozilla.gecko.background.healthreport.MockHealthReportDatabaseStorage.PrepopulatedMockHealthReportDatabaseStorage;
import org.mozilla.gecko.background.healthreport.MockProfileInformationCache;
import org.mozilla.gecko.background.healthreport.ProfileInformationCache;
import org.mozilla.gecko.background.healthreport.testhelpers.StubDelegate;
import org.mozilla.gecko.background.healthreport.upload.AndroidSubmissionClient;
import org.mozilla.gecko.background.healthreport.upload.AndroidSubmissionClient.SubmissionsFieldName;
import org.mozilla.gecko.background.helpers.FakeProfileTestCase;

import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SharedPreferences;

public class TestAndroidSubmissionClient extends FakeProfileTestCase {
  public static class MockAndroidSubmissionClient extends AndroidSubmissionClient {
    protected final PrepopulatedMockHealthReportDatabaseStorage storage;

    public boolean hasUploadBeenRequested = true;

    public MockAndroidSubmissionClient(final Context context, final SharedPreferences sharedPrefs,
        final PrepopulatedMockHealthReportDatabaseStorage storage) {
      super(context, sharedPrefs, "profilePath");
      this.storage = storage;
    }

    @Override
    public HealthReportDatabaseStorage getStorage(final ContentProviderClient client,
        final String profilePath) {
      return storage;
    }

    @Override
    public ProfileInformationCache getProfileInformationCache(final String profilePath) {
      final MockProfileInformationCache cache = new MockProfileInformationCache(profilePath);
      cache.setInitialized(true); // Will throw errors otherwise.
      return cache;
    }

    @Override
    public boolean hasUploadBeenRequested() {
      return hasUploadBeenRequested;
    }

    @Override
    protected void uploadPayload(String id, String payload, Collection<String> oldIds,
        BagheeraRequestDelegate delegate) {
      // Do nothing so we don't connect to the network.
    }
  }

  public StubDelegate stubDelegate;
  public PrepopulatedMockHealthReportDatabaseStorage storage;
  public MockAndroidSubmissionClient client;

  public void setUp() throws Exception {
    super.setUp();
    stubDelegate = new StubDelegate();
    storage = new PrepopulatedMockHealthReportDatabaseStorage(context, fakeProfileDirectory);
    client = new MockAndroidSubmissionClient(context, getSharedPreferences(), storage);
  }

  public int getSubmissionsCount(final SubmissionsFieldName fieldName) {
    final int id = fieldName.getID(storage);
    return storage.getIntFromQuery("SELECT COUNT(*) FROM events WHERE field = " + id, null);
  }

  public void testUploadSubmissionsFirstAttemptCount() throws Exception {
    client.hasUploadBeenRequested = false;
    client.upload(storage.now, null, null, stubDelegate);
    assertEquals(1, getSubmissionsCount(SubmissionsFieldName.FIRST_ATTEMPT));
    assertEquals(0, getSubmissionsCount(SubmissionsFieldName.CONTINUATION_ATTEMPT));
  }

  public void testUploadSubmissionsContinuationAttemptCount() throws Exception {
    client.upload(storage.now, null, null, stubDelegate);
    assertEquals(0, getSubmissionsCount(SubmissionsFieldName.FIRST_ATTEMPT));
    assertEquals(1, getSubmissionsCount(SubmissionsFieldName.CONTINUATION_ATTEMPT));
  }
}
