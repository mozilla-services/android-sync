/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.background.healthreport.upload;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

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

  public void setLastUploadLocalTimeAndDocumentId(long localTime, String id) {
    getSharedPreferences().edit()
      .putLong(HealthReportConstants.PREF_LAST_UPLOAD_LOCAL_TIME, localTime)
      .putString(HealthReportConstants.PREF_LAST_UPLOAD_DOCUMENT_ID, id)
      .commit();
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
      delegate.onHardFailure(localTime, null, "Could not fetch content provider client.", null);
      return;
    }

    try {
      // Storage instance is owned by HealthReportProvider, so we don't need to
      // close it. It's worth noting that this call will fail if called
      // out-of-process.
      HealthReportDatabaseStorage storage = EnvironmentBuilder.getStorage(client, profilePath);
      if (storage == null) {
        delegate.onHardFailure(localTime, null, "No storage when generating report.", null);
        return;
      }

      long since = localTime - GlobalConstants.MILLISECONDS_PER_SIX_MONTHS;
      long last = Math.max(getLastUploadLocalTime(), HealthReportConstants.EARLIEST_LAST_PING);

      if (!storage.hasEventSince(last)) {
        delegate.onHardFailure(localTime, null, "No new events in storage.", null);
        return;
      }

      initializeStorageForUploadProviders(storage);
      final ProfileInformationCache profileCache = new ProfileInformationCache(profilePath);
      if (!profileCache.restoreUnlessInitialized()) {
        Logger.warn(LOG_TAG, "Not enough profile information to compute current environment.");
        return;
      }
      final int env = EnvironmentBuilder.registerCurrentEnvironment(storage, profileCache);
      final int day = storage.getDay(localTime);

      HealthReportGenerator generator = new HealthReportGenerator(storage);
      JSONObject document = generator.generateDocument(since, last, profilePath);
      if (document == null) {
        delegate.onHardFailure(localTime, null, "Generator returned null document.", null);
        return;
      }

      BagheeraRequestDelegate uploadDelegate = new UploadRequestDelegate(delegate, localTime,
          true, id, storage, env, day);
      this.uploadPayload(id, document.toString(), oldIds, uploadDelegate);
    } catch (Exception e) {
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

  protected class UploadRequestDelegate extends RequestDelegate {
    private final HealthReportDatabaseStorage storage;
    private final int env;
    private final int day;

    public UploadRequestDelegate(Delegate delegate, long localTime, boolean isUpload, String id,
        HealthReportDatabaseStorage storage, int env, int day) {
      super(delegate, localTime, isUpload, id);
      this.storage = storage;
      this.env = env;
      this.day = day;
    }

    @Override
    public void handleSuccess(int status, String namespace, String id, HttpResponse response) {
      storage.incrementDailyCount(env, day, SubmissionsFieldName.SUCCESS.getID(storage));
      super.handleSuccess(status, namespace, id, response);
    }

    @Override
    public void handleFailure(int status, String namespace, HttpResponse response) {
      // TODO: Check status code and increment.
      super.handleFailure(status, namespace, response);
    }

    @Override
    public void handleError(Exception e) {
      // TODO: Can catch specific errors to specify type; then increment.
      super.handleError(e);
    }
  }

  private void initializeStorageForUploadProviders(HealthReportDatabaseStorage storage) {
    storage.beginInitialization();
    try {
      initializeSubmissionsProvider(storage);
      storage.finishInitialization();
    } catch (Exception e) {
      // TODO: Store error count in sharedPrefs to increment next time?
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

  private enum SubmissionsFieldName {
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

    protected String getName() {
      return name;
    }

    protected int getID(HealthReportStorage storage) {
      final Field field = storage.getField(MEASUREMENT_NAME_SUBMISSIONS,
                                           MEASUREMENT_VERSION_SUBMISSIONS,
                                           name);
      return field.getID();
    }
  }
}
