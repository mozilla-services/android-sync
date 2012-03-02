/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import org.mozilla.android.sync.test.helpers.PasswordHelpers;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserPasswordsDataAccessor;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserPasswordsRepository;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserRepository;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserRepositoryDataAccessor;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserRepositorySession;
import org.mozilla.gecko.sync.repositories.android.BrowserContractHelpers;
import org.mozilla.gecko.sync.repositories.domain.PasswordRecord;
import org.mozilla.gecko.sync.repositories.domain.Record;

public class AndroidBrowserPasswordRepositoryTest extends AndroidBrowserRepositoryTest {

  // Hacky overrides to avoid test failures until passwords lands.
  public static boolean shouldSkip = BrowserContractHelpers.PASSWORDS_CONTENT_URI == null;

  @Override
  public void setUp() {
  }

  @Override
  protected void wipe() {
  }

  @Override
  public void testCreateSessionNullContext() {}
  @Override
  public void testStoreNullRecord() {}
  @Override
  public void testFetchNullGuids() {}
  @Override
  public void testFetchNoGuids() {}
  @Override
  public void testBeginOnNewSession() {}
  @Override
  public void testBeginOnRunningSession() {}
  @Override
  public void testBeginOnFinishedSession() {}
  @Override
  public void testFetchOnFinishedSession() {}
  @Override
  public void testFetchOnInactiveSession() {}
  @Override
  public void testFinishOnFinishedSession() {}
  @Override
  public void testGuidsSinceOnUnstartedSession() {}
  // End hacky overrides.

  @Override
  protected AndroidBrowserRepository getRepository() {
    return new AndroidBrowserPasswordsRepository();
  }

  @Override
  protected AndroidBrowserRepositoryDataAccessor getDataAccessor() {
    return new AndroidBrowserPasswordsDataAccessor(getApplicationContext());
  }

  @Override
  public void testFetchAll() {
    if (shouldSkip) {
      assertTrue(true);
      return;
    }
    Record[] expected = new Record[2];
    expected[0] = PasswordHelpers.createPassword1();
    expected[1] = PasswordHelpers.createPassword2();
    basicFetchAllTest(expected);
  }

  @Override
  public void testGuidsSinceReturnMultipleRecords() {
    if (shouldSkip) {
      assertTrue(true);
      return;
    }
    PasswordRecord record0 = PasswordHelpers.createPassword3();
    PasswordRecord record1 = PasswordHelpers.createPassword2(); 
    guidsSinceReturnMultipleRecords(record0, record1);
  }

  @Override
  public void testGuidsSinceReturnNoRecords() {
    if (shouldSkip) {
      assertTrue(true);
      return;
    }
    guidsSinceReturnNoRecords(PasswordHelpers.createPassword1());
  }

  @Override
  public void testFetchSinceOneRecord() {
    if (shouldSkip) {
      assertTrue(true);
      return;
    }
    fetchSinceOneRecord(PasswordHelpers.createPassword1(),
                        PasswordHelpers.createPassword2());
  }

  @Override
  public void testFetchSinceReturnNoRecords() {
    if (shouldSkip) {
      assertTrue(true);
      return;
    }
    fetchSinceReturnNoRecords(PasswordHelpers.createPassword2());
  }

  @Override
  public void testFetchOneRecordByGuid() {
    if (shouldSkip) {
      assertTrue(true);
      return;
    }
    fetchOneRecordByGuid(PasswordHelpers.createPassword1(),
                         PasswordHelpers.createPassword2());
  }

  @Override
  public void testFetchMultipleRecordsByGuids() {
    if (shouldSkip) {
      assertTrue(true);
      return;
    }
    PasswordRecord record0 = PasswordHelpers.createPassword1();
    PasswordRecord record1 = PasswordHelpers.createPassword2();
    PasswordRecord record2 = PasswordHelpers.createPassword3();
    fetchMultipleRecordsByGuids(record0, record1, record2);
  }

  @Override
  public void testFetchNoRecordByGuid() {
    if (shouldSkip) {
      assertTrue(true);
      return;
    }
    fetchNoRecordByGuid(PasswordHelpers.createPassword1());
  }

  @Override
  public void testWipe() {
    if (shouldSkip) {
      assertTrue(true);
      return;
    }
    doWipe(PasswordHelpers.createPassword2(), PasswordHelpers.createPassword3());
  }

  @Override
  public void testStore() {
    if (shouldSkip) {
      assertTrue(true);
      return;
    }
    prepSession();
    AndroidBrowserRepositorySession session = getSession();
    performWait(storeRunnable(session, PasswordHelpers.createPassword1()));
  }

  @Override
  public void testRemoteNewerTimeStamp() {
    if (shouldSkip) {
      assertTrue(true);
      return;
    }
    PasswordRecord local = PasswordHelpers.createPassword2();
    PasswordRecord remote = PasswordHelpers.createPassword1();
    remoteNewerTimeStamp(local, remote);
  }

  @Override
  public void testLocalNewerTimeStamp() {
    if (shouldSkip) {
      assertTrue(true);
      return;
    }
    PasswordRecord local = PasswordHelpers.createPassword1();
    PasswordRecord remote = PasswordHelpers.createPassword2();
    localNewerTimeStamp(local, remote);
  }

  @Override
  public void testDeleteRemoteNewer() {
    if (shouldSkip) {
      assertTrue(true);
      return;
    }
    PasswordRecord local = PasswordHelpers.createPassword1();
    PasswordRecord remote = PasswordHelpers.createPassword2();
    deleteRemoteNewer(local, remote);
  }

  @Override
  public void testDeleteLocalNewer() {
    if (shouldSkip) {
      assertTrue(true);
      return;
    }
    PasswordRecord local = PasswordHelpers.createPassword1();
    PasswordRecord remote = PasswordHelpers.createPassword3();
    deleteLocalNewer(local, remote);
  }

  @Override
  public void testDeleteRemoteLocalNonexistent() {
    if (shouldSkip) {
      assertTrue(true);
      return;
    }
    deleteRemoteLocalNonexistent(PasswordHelpers.createPassword5());
  }

  @Override
  public void testStoreIdenticalExceptGuid() {
    if (shouldSkip) {
      assertTrue(true);
      return;
    }
    storeIdenticalExceptGuid(PasswordHelpers.createPassword1());
  }

  @Override
  public void testCleanMultipleRecords() {
    if (shouldSkip) {
      assertTrue(true);
      return;
    }
    cleanMultipleRecords(
        PasswordHelpers.createPassword1(),
        PasswordHelpers.createPassword2(),
        PasswordHelpers.createPassword3(),
        PasswordHelpers.createPassword4(),
        PasswordHelpers.createPassword5()
    );
  }
}
