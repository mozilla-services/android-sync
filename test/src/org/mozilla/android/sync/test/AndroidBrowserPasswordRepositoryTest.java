/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import org.mozilla.android.sync.test.helpers.PasswordHelpers;
import org.mozilla.gecko.sync.repositories.RepositorySession;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserPasswordsDataAccessor;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserPasswordsRepository;
import org.mozilla.gecko.sync.repositories.android.PasswordsRepositorySession;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserRepository;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserRepositoryDataAccessor;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionCreationDelegate;
import org.mozilla.gecko.sync.repositories.domain.PasswordRecord;
import org.mozilla.gecko.sync.repositories.domain.Record;

import android.content.Context;
import android.util.Log;

public class AndroidBrowserPasswordRepositoryTest extends AndroidBrowserRepositoryTest {
  private final String NEW_PASSWORD = "password";

  @Override
  protected AndroidBrowserRepository getRepository() {

    /**
     * Override this chain in order to avoid our test code having to create two
     * sessions all the time.
     */
    return new AndroidBrowserPasswordsRepository() {
      @Override
      protected void sessionCreator(RepositorySessionCreationDelegate delegate, Context context) {
        PasswordsRepositorySession session;
        session = new PasswordsRepositorySession(this, context) {
          @Override
          protected synchronized void trackRecord(Record record) {
            System.out.println("Ignoring trackRecord call: this is a test!");
          }
        };
        delegate.onSessionCreated(session);
      }
    };
  }

  @Override
  protected AndroidBrowserRepositoryDataAccessor getDataAccessor() {
    return new AndroidBrowserPasswordsDataAccessor(getApplicationContext());
  }

  @Override
  public void testFetchAll() {
    Record[] expected = new Record[2];
    expected[0] = PasswordHelpers.createPassword1();
    expected[1] = PasswordHelpers.createPassword2();
    basicFetchAllTest(expected);
  }

  @Override
  public void testGuidsSinceReturnMultipleRecords() {
    RepositorySession session = createAndBeginSession();

    PasswordRecord record1 = PasswordHelpers.createPassword1();
    PasswordRecord record2 = PasswordHelpers.createPassword2();

    updatePassword(NEW_PASSWORD, record1);
    long timestamp = updatePassword(NEW_PASSWORD, record2);

    String[] expected = new String[2];
    expected[0] = record1.guid;
    expected[1] = record2.guid;

    Log.i(getName(), "Storing two records...");
    performWait(storeManyRunnable(session, new Record[] { record1, record2 }));
    Log.i(getName(), "Getting guids since " + timestamp + "; expecting " + expected.length);
    performWait(guidsSinceRunnable(session, timestamp, expected));
    dispose(session);
  }

  @Override
  public void testGuidsSinceReturnNoRecords() {
    guidsSinceReturnNoRecords(PasswordHelpers.createPassword1());
  }

  @Override
  public void testFetchSinceOneRecord() {
    RepositorySession session = createAndBeginSession();

    // Passwords fetchSince checks timePasswordChanged, not insertion time.
    PasswordRecord record1 = PasswordHelpers.createPassword1();
    long timeModified1 = updatePassword(NEW_PASSWORD, record1);
    Log.i("fetchSinceOneRecord", "Storing record1.");
    performWait(storeRunnable(session, record1));

    PasswordRecord record2 = PasswordHelpers.createPassword2();
    long timeModified2 = updatePassword(NEW_PASSWORD, record2);
    Log.i("fetchSinceOneRecord", "Storing record2.");
    performWait(storeRunnable(session, record2));

    Log.i("fetchSinceOneRecord", "Fetching record 1.");
    String[] expectedOne = new String[] { record2.guid };
    performWait(fetchSinceRunnable(session, timeModified2 - 10, expectedOne));

    Log.i("fetchSinceOneRecord", "Fetching both, relying on inclusiveness.");
    String[] expectedBoth = new String[] { record1.guid, record2.guid };
    performWait(fetchSinceRunnable(session, timeModified1 - 10, expectedBoth));

    Log.i("fetchSinceOneRecord", "Done.");
    dispose(session);
  }

  @Override
  public void testFetchSinceReturnNoRecords() {
    fetchSinceReturnNoRecords(PasswordHelpers.createPassword2());
  }

  @Override
  public void testFetchOneRecordByGuid() {
    // failing
    fetchOneRecordByGuid(PasswordHelpers.createPassword1(),
                         PasswordHelpers.createPassword2());
  }

  @Override
  public void testFetchMultipleRecordsByGuids() {
    PasswordRecord record0 = PasswordHelpers.createPassword1();
    PasswordRecord record1 = PasswordHelpers.createPassword2();
    PasswordRecord record2 = PasswordHelpers.createPassword3();
    fetchMultipleRecordsByGuids(record0, record1, record2);
  }

  @Override
  public void testFetchNoRecordByGuid() {
    fetchNoRecordByGuid(PasswordHelpers.createPassword1());
  }

  @Override
  public void testWipe() {
    doWipe(PasswordHelpers.createPassword2(), PasswordHelpers.createPassword3());
  }

  @Override
  public void testStore() {
    final RepositorySession session = createAndBeginSession();
    performWait(storeRunnable(session, PasswordHelpers.createPassword1()));
  }

  @Override
  public void testRemoteNewerTimeStamp() {
    PasswordRecord local = PasswordHelpers.createPassword2();
    PasswordRecord remote = PasswordHelpers.createPassword1();
    remoteNewerTimeStamp(local, remote);
  }

  @Override
  public void testLocalNewerTimeStamp() {
    PasswordRecord local = PasswordHelpers.createPassword1();
    PasswordRecord remote = PasswordHelpers.createPassword2();
    localNewerTimeStamp(local, remote);
  }

  @Override
  public void testDeleteRemoteNewer() {
    PasswordRecord local = PasswordHelpers.createPassword1();
    PasswordRecord remote = PasswordHelpers.createPassword2();
    deleteRemoteNewer(local, remote);
  }

  @Override
  public void testDeleteLocalNewer() {
    PasswordRecord local = PasswordHelpers.createPassword1();
    PasswordRecord remote = PasswordHelpers.createPassword3();
    deleteLocalNewer(local, remote);
  }

  @Override
  public void testDeleteRemoteLocalNonexistent() {
    deleteRemoteLocalNonexistent(PasswordHelpers.createPassword5());
  }

  @Override
  public void testStoreIdenticalExceptGuid() {
    storeIdenticalExceptGuid(PasswordHelpers.createPassword1());
  }

  @Override
  public void testCleanMultipleRecords() {
    //failing
    cleanMultipleRecords(
        PasswordHelpers.createPassword1(),
        PasswordHelpers.createPassword2(),
        PasswordHelpers.createPassword3(),
        PasswordHelpers.createPassword4(),
        PasswordHelpers.createPassword5()
    );
  }

  private long updatePassword(String password, PasswordRecord record) {
    record.encryptedPassword = password;
    long modifiedTime = System.currentTimeMillis();
    record.timePasswordChanged = record.lastModified = modifiedTime;
    return modifiedTime;
  }
}
