/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.picl.sync.repositories;

import org.json.simple.JSONObject;
import org.mozilla.gecko.background.common.log.Logger;
import org.mozilla.gecko.picl.sync.net.PICLServer0Client.PICLServer0ClientDelegate;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.repositories.InactiveSessionException;
import org.mozilla.gecko.sync.repositories.NoStoreDelegateException;
import org.mozilla.gecko.sync.repositories.Repository;
import org.mozilla.gecko.sync.repositories.RepositorySession;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionFetchRecordsDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionGuidsSinceDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionWipeDelegate;
import org.mozilla.gecko.sync.repositories.domain.Record;
import org.mozilla.gecko.sync.repositories.domain.TabsRecord;

import ch.boye.httpclientandroidlib.HttpResponse;

public class PICLServer0RepositorySession extends RepositorySession {

  private static final String LOG_TAG = "PICL0RepoSession";

  public static final String TABS = "tabs";

  protected PICLRecordTranslator translator;

  protected PICLServer0Repository serverRepository;

  public PICLServer0RepositorySession(Repository repository) {
    super(repository);
    serverRepository = (PICLServer0Repository) repository;
  }

  @Override
  public void guidsSince(long timestamp, RepositorySessionGuidsSinceDelegate delegate) {
    throw new IllegalArgumentException("not implemented yet");
  }

  @Override
  public void fetchSince(long timestamp, final RepositorySessionFetchRecordsDelegate delegate) {
    final long now = System.currentTimeMillis();
    delegateQueue.execute(new Runnable() {

      @Override
      public void run() {
        serverRepository.client.get(new PICLServer0ClientDelegate() {

          @Override
          public void handleSuccess(ExtendedJSONObject json) {
            Logger.warn(LOG_TAG, "Fetched: " + json.toJSONString());
            try {
              for (Object itemJson : json.getArray("items")) {
                delegate.onFetchedRecord(serverRepository.translator.toRecord((ExtendedJSONObject) itemJson));
              }

            } catch (Exception e) {
              handleError(e);
            }


            delegate.onFetchCompleted(now);
          }

          @Override
          public void handleFailure(HttpResponse response, Exception e) {
            delegate.onFetchFailed(e, null);
          }

          @Override
          public void handleError(Exception e) {
            delegate.onFetchFailed(e, null);
          }

        });



      }

    });

  }

  @Override
  public void fetch(String[] guids,
      RepositorySessionFetchRecordsDelegate delegate)
      throws InactiveSessionException {
    throw new IllegalArgumentException("not implemented yet");
  }

  @Override
  public void fetchAll(RepositorySessionFetchRecordsDelegate delegate) {
    throw new IllegalArgumentException("not implemented yet");
  }

  @Override
  public void store(final Record record) throws NoStoreDelegateException {
    storeWorkQueue.execute(new Runnable() {

      @Override
      public void run() {
        Logger.warn(LOG_TAG, "Calling store with record " + record);

        serverRepository.client.post(serverRepository.translator.fromRecord(record), new PICLServer0ClientDelegate() {

          @Override
          public void handleSuccess(ExtendedJSONObject json) {

            delegate.onRecordStoreSucceeded(record.guid);
          }

          @Override
          public void handleFailure(HttpResponse response, Exception e) {
            delegate.onRecordStoreFailed(e, record.guid);
          }

          @Override
          public void handleError(Exception e) {
            delegate.onRecordStoreFailed(e, record.guid);
          }

        });
      }

    });
  }

  @Override
  public void storeDone() {
    Logger.debug(LOG_TAG, "storeDone()");
    storeWorkQueue.execute(new Runnable() {

      @Override
      public void run() {
        final long end = System.currentTimeMillis();
        Logger.warn(LOG_TAG, "Calling store with " + end);
        storeDone(end);
      }

    });
  }

  @Override
  public void wipe(RepositorySessionWipeDelegate delegate) {
    throw new IllegalArgumentException("not implemented yet");
  }

}
