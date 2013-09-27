/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.background.healthreport.upload;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;

import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.gecko.background.bagheera.BagheeraClient;
import org.mozilla.gecko.background.bagheera.BagheeraRequestDelegate;
import org.mozilla.gecko.background.common.GlobalConstants;
import org.mozilla.gecko.background.common.log.Logger;
import org.mozilla.gecko.background.healthreport.EnvironmentBuilder;
import org.mozilla.gecko.background.healthreport.HealthReportConstants;
import org.mozilla.gecko.background.healthreport.HealthReportDatabaseStorage;
import org.mozilla.gecko.background.healthreport.HealthReportGenerator;
import org.mozilla.gecko.background.healthreport.HealthReportStorage;
import org.mozilla.gecko.background.healthreport.HealthReportStorage.Field;
import org.mozilla.gecko.background.healthreport.HealthReportStorage.MeasurementFields;
import org.mozilla.gecko.background.healthreport.HealthReportStorage.MeasurementFields.FieldSpec;
import org.mozilla.gecko.background.healthreport.ProfileInformationCache;
import org.mozilla.gecko.sync.net.BaseResource;

import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SharedPreferences;
import ch.boye.httpclientandroidlib.HttpResponse;

public class AndroidSubmissionClient implements SubmissionClient {
  protected static final String LOG_TAG = AndroidSubmissionClient.class.getSimpleName();

  private static final String MEASUREMENT_NAME_SUBMISSIONS = "org.mozilla.healthreport.submissions";
  private static final int MEASUREMENT_VERSION_SUBMISSIONS = 1;

  protected final Context context;
  protected final SharedPreferences sharedPreferences;
  protected final String profilePath;

  public AndroidSubmissionClient(Context context, SharedPreferences sharedPreferences, String profilePath) {
    this.context = context;
    this.sharedPreferences = sharedPreferences;
    this.profilePath = profilePath;
  }

  public SharedPreferences getSharedPreferences() {
    return sharedPreferences;
  }

  public String getDocumentServerURI() {
    return getSharedPreferences().getString(HealthReportConstants.PREF_DOCUMENT_SERVER_URI, HealthReportConstants.DEFAULT_DOCUMENT_SERVER_URI);
  }

  public String getDocumentServerNamespace() {
    return getSharedPreferences().getString(HealthReportConstants.PREF_DOCUMENT_SERVER_NAMESPACE, HealthReportConstants.DEFAULT_DOCUMENT_SERVER_NAMESPACE);
  }

  public long getLastUploadLocalTime() {
    return getSharedPreferences().getLong(HealthReportConstants.PREF_LAST_UPLOAD_LOCAL_TIME, 0L);
  }

  public String getLastUploadDocumentId() {
    return getSharedPreferences().getString(HealthReportConstants.PREF_LAST_UPLOAD_DOCUMENT_ID, null);
  }

  public boolean hasUploadBeenRequested() {
    return getSharedPreferences().contains(HealthReportConstants.PREF_LAST_UPLOAD_REQUESTED);
  }

  public void setLastUploadLocalTimeAndDocumentId(long localTime, String id) {
    getSharedPreferences().edit()
      .putLong(HealthReportConstants.PREF_LAST_UPLOAD_LOCAL_TIME, localTime)
      .putString(HealthReportConstants.PREF_LAST_UPLOAD_DOCUMENT_ID, id)
      .commit();
  }

  protected void incrementSubmissionAttemptCount(final SubmissionsStatusCounter statusCounter) {
    // TODO: Bug 910898 - Add errors from sharedPrefs to storage instance.
    if (!hasUploadBeenRequested()) {
      statusCounter.incrementFirstUploadAttemptCount();
    } else {
      statusCounter.incrementContinuationAttemptCount();
    }
  }

  protected HealthReportDatabaseStorage getStorage(final ContentProviderClient client,
      final String profilePath) {
    return EnvironmentBuilder.getStorage(client, profilePath);
  }

  protected ProfileInformationCache getProfileInformationCache(final String profilePath) {
    return new ProfileInformationCache(profilePath);
  }

  protected JSONObject generateDocument(final HealthReportStorage storage, final long localTime,
      final long last, final String profilePath) throws JSONException {
    final long since = localTime - GlobalConstants.MILLISECONDS_PER_SIX_MONTHS;
    final HealthReportGenerator generator = new HealthReportGenerator(storage);
    return generator.generateDocument(since, last, profilePath);
  }

  protected void uploadPayload(String id, String payload, Collection<String> oldIds, BagheeraRequestDelegate uploadDelegate) {
    final BagheeraClient client = new BagheeraClient(getDocumentServerURI());

    Logger.pii(LOG_TAG, "New health report has id " + id +
        "and obsoletes " + (oldIds != null ? Integer.toString(oldIds.size()) : "no") + " old ids.");

    try {
      client.uploadJSONDocument(getDocumentServerNamespace(),
          id,
          payload,
          oldIds,
          uploadDelegate);
    } catch (Exception e) {
      uploadDelegate.handleError(e);
    }
  }

  @Override
  public void upload(long localTime, String id, Collection<String> oldIds, Delegate delegate) {
    // We abuse the life-cycle of an Android ContentProvider slightly by holding
    // onto a ContentProviderClient while we generate a payload. This keeps our
    // database storage alive, and may also allow us to share a database
    // connection with a BrowserHealthRecorder from Fennec.  The ContentProvider
    // owns all underlying Storage instances, so we don't need to explicitly
    // close them.
    ContentProviderClient client = EnvironmentBuilder.getContentProviderClient(context);
    if (client == null) {
      // TODO: Bug 910898 - Store client failure in SharedPrefs so we can increment next time with storage.
      delegate.onHardFailure(localTime, null, "Could not fetch content provider client.", null);
      return;
    }

    try {
      // Storage instance is owned by HealthReportProvider, so we don't need to
      // close it. It's worth noting that this call will fail if called
      // out-of-process.
      final HealthReportDatabaseStorage storage = getStorage(client, profilePath);
      if (storage == null) {
        // TODO: Bug 910898 - Store error in SharedPrefs so we can increment next time with storage.
        delegate.onHardFailure(localTime, null, "No storage when generating report.", null);
        return;
      }

      long last = Math.max(getLastUploadLocalTime(), HealthReportConstants.EARLIEST_LAST_PING);
      if (!storage.hasEventSince(last)) {
        delegate.onHardFailure(localTime, null, "No new events in storage.", null);
        return;
      }

      initializeStorageForUploadProviders(storage);
      final ProfileInformationCache profileCache = getProfileInformationCache(profilePath);
      if (!profileCache.restoreUnlessInitialized()) {
        Logger.warn(LOG_TAG, "Not enough profile information to compute current environment.");
        return;
      }
      final int env = EnvironmentBuilder.registerCurrentEnvironment(storage, profileCache);
      final int day = storage.getDay(localTime);
      final SubmissionsStatusCounter statusCounter = new SubmissionsStatusCounter(env, day, storage);

      try {
        incrementSubmissionAttemptCount(statusCounter);
        final JSONObject document = generateDocument(storage, localTime, last, profilePath);
        if (document == null) {
          statusCounter.incrementUploadClientFailureCount();
          delegate.onHardFailure(localTime, null, "Generator returned null document.", null);
          return;
        }

        final BagheeraRequestDelegate uploadDelegate = new UploadRequestDelegate(delegate,
            localTime, true, id, statusCounter);
        this.uploadPayload(id, document.toString(), oldIds, uploadDelegate);
      } catch (Exception e) {
        // Incrementing the failure count here could potentially cause the failure count to be
        // incremented twice, but this helper class checks and prevents this.
        statusCounter.incrementUploadClientFailureCount();
        throw e;
      }
    } catch (Exception e) {
      // TODO: Bug 910898 - Store client failure in SharedPrefs so we can increment next time with storage.
      Logger.warn(LOG_TAG, "Got exception generating document.", e);
      delegate.onHardFailure(localTime, null, "Got exception uploading.", e);
      return;
    } finally {
      client.release();
    }
  }

  @Override
  public void delete(final long localTime, final String id, Delegate delegate) {
    final BagheeraClient client = new BagheeraClient(getDocumentServerURI());

    Logger.pii(LOG_TAG, "Deleting health report with id " + id + ".");

    BagheeraRequestDelegate deleteDelegate = new RequestDelegate(delegate, localTime, false, id);
    try {
      client.deleteDocument(getDocumentServerNamespace(), id, deleteDelegate);
    } catch (Exception e) {
      deleteDelegate.handleError(e);
    }
  }

  protected class RequestDelegate implements BagheeraRequestDelegate {
    protected final Delegate delegate;
    protected final boolean isUpload;
    protected final String methodString;
    protected final long localTime;
    protected final String id;

    public RequestDelegate(Delegate delegate, long localTime, boolean isUpload, String id) {
      this.delegate = delegate;
      this.localTime = localTime;
      this.isUpload = isUpload;
      this.methodString = this.isUpload ? "upload" : "delete";
      this.id = id;
    }

    @Override
    public void handleSuccess(int status, String namespace, String id, HttpResponse response) {
      BaseResource.consumeEntity(response);
      if (isUpload) {
        setLastUploadLocalTimeAndDocumentId(localTime, id);
      }
      Logger.debug(LOG_TAG, "Successful " + methodString + " at " + localTime + ".");
      delegate.onSuccess(localTime, id);
    }

    /**
     * Bagheera status codes:
     *
     * 403 Forbidden - Violated access restrictions. Most likely because of the method used.
     * 413 Request Too Large - Request payload was larger than the configured maximum.
     * 400 Bad Request - Returned if the POST/PUT failed validation in some manner.
     * 404 Not Found - Returned if the URI path doesn't exist or if the URI was not in the proper format.
     * 500 Server Error - General server error. Someone with access should look at the logs for more details.
     */
    @Override
    public void handleFailure(int status, String namespace, HttpResponse response) {
      BaseResource.consumeEntity(response);
      Logger.debug(LOG_TAG, "Failed " + methodString + " at " + localTime + ".");
      if (status >= 500) {
        delegate.onSoftFailure(localTime, id, "Got status " + status + " from server.", null);
        return;
      }
      // Things are either bad locally (bad payload format, too much data) or
      // bad remotely (badly configured server, temporarily unavailable). Try
      // again tomorrow.
      delegate.onHardFailure(localTime, id, "Got status " + status + " from server.", null);
    }

    @Override
    public void handleError(Exception e) {
      Logger.debug(LOG_TAG, "Exception during " + methodString + " at " + localTime + ".", e);
      if (e instanceof IOException) {
        // Let's assume IO exceptions are Android dropping the network.
        delegate.onSoftFailure(localTime, id, "Got exception during " + methodString + ".", e);
        return;
      }
      delegate.onHardFailure(localTime, id, "Got exception during " + methodString + ".", e);
    }
  };

  public class UploadRequestDelegate extends RequestDelegate {
    private final SubmissionsStatusCounter statusCounter;

    public UploadRequestDelegate(Delegate delegate, long localTime, boolean isUpload, String id,
        final SubmissionsStatusCounter statusCounter) {
      super(delegate, localTime, isUpload, id);
      this.statusCounter = statusCounter;
    }

    @Override
    public void handleSuccess(int status, String namespace, String id, HttpResponse response) {
      statusCounter.incrementUploadSuccessCount();
      super.handleSuccess(status, namespace, id, response);
    }

    @Override
    public void handleFailure(int status, String namespace, HttpResponse response) {
      switch (status) {
      case 200:
      case 201:
        throw new IllegalStateException("Did not expect HTTP success.");

      default:
        statusCounter.incrementUploadServerFailureCount();
      }
      super.handleFailure(status, namespace, response);
    }

    @Override
    public void handleError(Exception e) {
      if (e instanceof IllegalArgumentException ||
          e instanceof UnsupportedEncodingException ||
          e instanceof URISyntaxException) {
        statusCounter.incrementUploadClientFailureCount();
      } else {
        statusCounter.incrementUploadTransportFailureCount();
      }
      super.handleError(e);
    }
  }

  private void initializeStorageForUploadProviders(HealthReportDatabaseStorage storage) {
    storage.beginInitialization();
    try {
      initializeSubmissionsProvider(storage);
      storage.finishInitialization();
    } catch (Exception e) {
      // TODO: Bug 910898 - Store error in SharedPrefs so we can increment next time with storage.
      storage.abortInitialization();
    }
  }

  private void initializeSubmissionsProvider(HealthReportDatabaseStorage storage) {
    storage.ensureMeasurementInitialized(
        MEASUREMENT_NAME_SUBMISSIONS,
        MEASUREMENT_VERSION_SUBMISSIONS,
        new MeasurementFields() {
          @Override
          public Iterable<FieldSpec> getFields() {
            final ArrayList<FieldSpec> out = new ArrayList<FieldSpec>();
            for (SubmissionsFieldName fieldName : SubmissionsFieldName.values()) {
              FieldSpec lol = new FieldSpec(fieldName.getName(), Field.TYPE_INTEGER_COUNTER);
              out.add(lol);
            }
            return out;
          }
        });
  }

  public static enum SubmissionsFieldName {
    FIRST_ATTEMPT("firstDocumentUploadAttempt"),
    CONTINUATION_ATTEMPT("continuationDocumentUploadAttempt"),
    SUCCESS("uploadSuccess"),
    TRANSPORT_FAILURE("uploadTransportFailure"),
    SERVER_FAILURE("uploadServerFailure"),
    CLIENT_FAILURE("uploadClientFailure");

    private final String name;

    SubmissionsFieldName(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

    public int getID(HealthReportStorage storage) {
      final Field field = storage.getField(MEASUREMENT_NAME_SUBMISSIONS,
                                           MEASUREMENT_VERSION_SUBMISSIONS,
                                           name);
      return field.getID();
    }
  }

  /**
   * Encapsulates the counting mechanisms for submissions status counts. Ensures multiple failures
   * and successes are not recorded for a single instance.
   */
  public static class SubmissionsStatusCounter {
    private final int env;
    private final int day;
    private final HealthReportStorage storage;

    private boolean isUploadStatusCountIncremented;

    public SubmissionsStatusCounter(final int env, final int day, final HealthReportStorage storage) {
      this.env = env;
      this.day = day;
      this.storage = storage;

      this.isUploadStatusCountIncremented = false;
    }

    public void incrementFirstUploadAttemptCount() {
      Logger.debug(LOG_TAG, "Incrementing first upload attempt field.");
      storage.incrementDailyCount(env, day, SubmissionsFieldName.FIRST_ATTEMPT.getID(storage));
    }

    public void incrementContinuationAttemptCount() {
      // TODO: Worth protecting to ensure both of these methods are not called?
      Logger.debug(LOG_TAG, "Incrementing continuation upload attempt field.");
      storage.incrementDailyCount(env, day, SubmissionsFieldName.CONTINUATION_ATTEMPT.getID(storage));
    }

    public void incrementUploadSuccessCount() {
      incrementStatusCount(SubmissionsFieldName.SUCCESS.getID(storage), "success");
    }

    public void incrementUploadClientFailureCount() {
      incrementStatusCount(SubmissionsFieldName.CLIENT_FAILURE.getID(storage), "client failure");
    }

    public void incrementUploadTransportFailureCount() {
      incrementStatusCount(SubmissionsFieldName.TRANSPORT_FAILURE.getID(storage), "transport failure");
    }

    public void incrementUploadServerFailureCount() {
      incrementStatusCount(SubmissionsFieldName.SERVER_FAILURE.getID(storage), "server failure");
    }

    private void incrementStatusCount(final int fieldID, final String countType) {
      if (!isUploadStatusCountIncremented) {
        Logger.debug(LOG_TAG, "Incrementing upload attempt " + countType + " count.");
        storage.incrementDailyCount(env, day, fieldID);
        isUploadStatusCountIncremented = true;
      } else {
        Logger.warn(LOG_TAG, "Upload status count already incremented - not incrementing " +
            countType + " count.");
      }
    }
  }
}
