package org.mozilla.android.sync.test;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.mozilla.android.sync.test.helpers.SessionTestHelper;
import org.mozilla.android.sync.test.helpers.WaitHelper;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.repositories.NoStoreDelegateException;
import org.mozilla.gecko.sync.repositories.Repository;
import org.mozilla.gecko.sync.repositories.RepositorySession;
import org.mozilla.gecko.sync.repositories.android.PasswordsRepositorySession;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionCreationDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionFetchRecordsDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionStoreDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionWipeDelegate;
import org.mozilla.gecko.sync.repositories.domain.PasswordRecord;
import org.mozilla.gecko.sync.repositories.domain.Record;

import android.content.Context;

/**
 * Copy passwords to memory, wipe database, then recreate in database.
 * Then let's check if they appear in UI.
 *
 * (Create passwords in Fennec using UI before running this test. Then test autocomplete after test finishes.)
 * @author liuche
 *
 */
public class TestDatabaseReplace extends AndroidSyncTestCase {
  List<PasswordRecord> records = new LinkedList<PasswordRecord>();

  @Override
  public void setUp() {
    assertTrue(WaitHelper.getTestWaiter().isIdle());
  }

  // Don't wipe database before we test.
  public void testReplace() {
    final RepositorySession session = createAndBeginSession();

    // Runnable for fetching all the records.
    Runnable fetchRunnable = new Runnable() {
      @Override
      public void run() {
        session.fetchAll(new RepositorySessionFetchRecordsDelegate() {

          @Override
          public void onFetchFailed(Exception ex, Record record) {
            fail("Failed on fetch.");
          }

          @Override
          public void onFetchedRecord(Record record) {
            Logger.debug(LOG_TAG, "fetched " + record);
            if (!(record instanceof PasswordRecord)) {
              fail("Not a PasswordRecord.");
            }
            records.add((PasswordRecord) record);
          }

          @Override
          public void onFetchCompleted(long fetchEnd) {
            performNotify();
          }

          @Override
          public void onFetchSucceeded(Record[] records, long fetchEnd) {
            // Do nothing.
          }

          @Override
          public RepositorySessionFetchRecordsDelegate deferredFetchDelegate(
              ExecutorService executor) {
            // Do nothing.
            return null;
          }
        });
      }
    };
    // Fetch all the records.
    performWait(fetchRunnable);

    // Wipe database.
    performWait(new Runnable() {
      @Override
      public void run() {
        session.wipe(new RepositorySessionWipeDelegate() {

          @Override
          public void onWipeFailed(Exception ex) {
            fail("Database wipe failed.");
          }

          @Override
          public void onWipeSucceeded() {
            performNotify();
          }

          @Override
          public RepositorySessionWipeDelegate deferredWipeDelegate(
              ExecutorService executor) {
            // Do nothing.
            return null;
          }

        });
      }
    });

    // Runnable to store all the records back again.
    final int numRecords = records.size();
    Runnable storeRunnable = new Runnable() {
      int numStored = 0;

      @Override
      public void run() {
        session.setStoreDelegate(new RepositorySessionStoreDelegate() {

          @Override
          public void onRecordStoreFailed(Exception ex) {
            fail("Failed to store record.");
          }

          @Override
          public void onRecordStoreSucceeded(Record record) {
            Logger.debug(LOG_TAG, "stored " + record);
            if (++numStored == numRecords) {
              performNotify();
              Logger.debug(LOG_TAG, "performNotify for last test wait.");
            }
            Logger.debug(LOG_TAG, "# stored: " + numStored);
          }

          @Override
          public void onStoreCompleted(long storeEnd) {
            // Do nothing.
          }

          @Override
          public RepositorySessionStoreDelegate deferredStoreDelegate(
              ExecutorService executor) {
            // Do nothing.
            return null;
          }

        });
        for (PasswordRecord record : records) {
          try {
            session.store(record);
          } catch (NoStoreDelegateException e) {
            fail("No store delegate set.");
          }
        }
      }
    };
    performWait(storeRunnable);
  }

  @Override
  public void tearDown() {
    assertTrue(WaitHelper.getTestWaiter().isIdle());
  }

  private RepositorySession createAndBeginSession() {
    return SessionTestHelper.createAndBeginSession(
        getApplicationContext(),
        getRepository());
  }

  private Repository getRepository() {
    /**
     * Override this chain in order to avoid our test code having to create two
     * sessions all the time. Don't track records, so they filtering doesn't happen.
     */
    return new PasswordsRepositorySession.PasswordsRepository() {
      @Override
      public void createSession(RepositorySessionCreationDelegate delegate,
          Context context) {
        PasswordsRepositorySession session;
        session = new PasswordsRepositorySession(this, context) {
          @Override
          protected synchronized void trackGUID(String guid) {
            System.out.println("Ignoring trackGUID call: this is a test!");
          }
        };
        delegate.onSessionCreated(session);
      }
    };
  }
}
