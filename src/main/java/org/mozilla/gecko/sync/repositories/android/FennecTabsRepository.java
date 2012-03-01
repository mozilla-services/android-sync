/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.repositories.android;

import org.mozilla.android.sync.repositories.domain.TabsRecord;
import org.mozilla.gecko.db.BrowserContract;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.repositories.InactiveSessionException;
import org.mozilla.gecko.sync.repositories.NoStoreDelegateException;
import org.mozilla.gecko.sync.repositories.Repository;
import org.mozilla.gecko.sync.repositories.RepositorySession;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionCreationDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionFetchRecordsDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionGuidsSinceDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionWipeDelegate;
import org.mozilla.gecko.sync.repositories.domain.Record;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;

public class FennecTabsRepository extends Repository {

  private Context context;
  public class FennecTabsRepositorySession extends RepositorySession {

    private static final String LOG_TAG = "FennecTabsRepositorySession";

    /**
     * Note that this will fetch Fennec's tabs, and store tabs from other clients.
     * It will never retrieve tabs from other clients, or store tabs for Fennec,
     * unless you specify an explicit GUID.
     *
     * @param repository
     */
    public FennecTabsRepositorySession(Repository repository) {
      super(repository);
    }

    @Override
    public void guidsSince(long timestamp,
                           RepositorySessionGuidsSinceDelegate delegate) {
      // Empty until Bug 730039 lands.
      delegate.onGuidsSinceSucceeded(new String[] {});
    }

    @Override
    public void fetchSince(long timestamp,
                           RepositorySessionFetchRecordsDelegate delegate) {
      // Empty until Bug 730039 lands.
      delegate.onFetchCompleted(now());
    }

    @Override
    public void fetch(String[] guids,
                      RepositorySessionFetchRecordsDelegate delegate) {
      // Incomplete until Bug 730039 lands.
      // TODO
      delegate.onFetchCompleted(now());
    }

    @Override
    public void fetchAll(RepositorySessionFetchRecordsDelegate delegate) {
      // Incomplete until Bug 730039 lands.
      // TODO
      delegate.onFetchCompleted(now());
    }

    private static final String TABS_CLIENT_GUID_IS = BrowserContract.Tabs.CLIENT_GUID + " = ?";
    private static final String CLIENT_GUID_IS = BrowserContract.Clients.GUID + " = ?";
    @Override
    public void store(final Record record) throws NoStoreDelegateException {
      if (delegate == null) {
        throw new NoStoreDelegateException();
      }
      if (record == null) {
        Logger.error(LOG_TAG, "Record sent to store was null");
        throw new IllegalArgumentException("Null record passed to FennecTabsRepositorySession.store().");
      }
      if (!(record instanceof TabsRecord)) {
        Logger.error(LOG_TAG, "Can't store anything but a TabsRecord");
        throw new IllegalArgumentException("Non-TabsRecord passed to FennecTabsRepositorySession.store().");
      }
      final TabsRecord tabsRecord = (TabsRecord) record;
      Runnable command = new Runnable() {
        @Override
        public void run() {
          if (!isActive()) {
            delegate.onRecordStoreFailed(new InactiveSessionException(null));
            return;
          }
          if (tabsRecord.guid == null) {
            delegate.onRecordStoreFailed(new RuntimeException("Can't store record with null GUID."));
            return;
          }

          try {
            // This is nice and easy: we *always* store.
            final String[] selectionArgs = new String[] { record.guid };
            if (tabsRecord.deleted) {
              try {
                context.getContentResolver().delete(BrowserContract.Clients.CONTENT_URI,
                    CLIENT_GUID_IS,
                    selectionArgs);
                delegate.onRecordStoreSucceeded(record);
              } catch (Exception e) {
                delegate.onRecordStoreFailed(e);
              }
              return;
            }

            // If it exists, update the client record; otherwise insert.
            final ContentValues clientsCV = tabsRecord.getClientsContentValues();
            final int updated = context.getContentResolver().update(BrowserContract.Clients.CONTENT_URI,
                clientsCV,
                CLIENT_GUID_IS,
                selectionArgs);
            if (0 == updated) {
              context.getContentResolver().insert(BrowserContract.Clients.CONTENT_URI, clientsCV);
            }

            // Now insert tabs.
            ContentResolver cr = context.getContentResolver();

            cr.delete(BrowserContract.Tabs.CONTENT_URI, TABS_CLIENT_GUID_IS, selectionArgs);
            cr.bulkInsert(BrowserContract.Tabs.CONTENT_URI, tabsRecord.getTabsContentValues());
            delegate.onRecordStoreSucceeded(tabsRecord);
          } catch (Exception e) {
            Logger.warn(LOG_TAG, "Error storing tabs.", e);
            delegate.onRecordStoreFailed(e);
          }
        }
      };

      storeWorkQueue.execute(command);
    }

    @Override
    public void wipe(RepositorySessionWipeDelegate delegate) {
      ContentResolver cr = context.getContentResolver();
      cr.delete(BrowserContract.Tabs.CONTENT_URI, null, null);
      cr.delete(BrowserContract.Clients.CONTENT_URI, null, null);
      delegate.onWipeSucceeded();
    }

  }

  @Override
  public void createSession(RepositorySessionCreationDelegate delegate,
                            Context context) {
    this.context = context;
    try {
      final FennecTabsRepositorySession session = new FennecTabsRepositorySession(this);
      delegate.onSessionCreated(session);
    } catch (Exception e) {
      delegate.onSessionCreateFailed(e);
    }
  }

}
