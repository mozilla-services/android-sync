/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Android Sync Client.
 *
 * The Initial Developer of the Original Code is
 * the Mozilla Foundation.
 * Portions created by the Initial Developer are Copyright (C) 2011
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *  Richard Newman <rnewman@mozilla.com>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

package org.mozilla.android.sync.repositories;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import org.json.simple.parser.ParseException;
import org.mozilla.android.sync.CryptoRecord;
import org.mozilla.android.sync.NonObjectJSONException;
import org.mozilla.android.sync.crypto.CryptoException;
import org.mozilla.android.sync.crypto.KeyBundle;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionFetchRecordsDelegate;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionGuidsSinceDelegate;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionStoreDelegate;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionWipeDelegate;
import org.mozilla.android.sync.repositories.domain.Record;

/**
 * It's a RepositorySession that accepts Records as input, producing CryptoRecords
 * for submission to a remote service.
 * Takes a RecordFactory as a parameter. This is in charge of taking decrypted CryptoRecords
 * as input and producing some expected kind of Record as output for local use.
 *
 *



                 +------------------------------------+
                 |    Server11RepositorySession       |
                 +-------------------------+----------+
                           ^               |
                           |               |
                        Encrypted CryptoRecords
                           |               |
                           |               v
                 +---------+--------------------------+
                 | Crypto5MiddlewareRepositorySession |
                 +------------------------------------+
                           ^               |
                           |               |  Decrypted CryptoRecords
                           |               |
                           |            +---------------+
                           |            | RecordFactory |
                           |            +--+------------+
                           |               |
                          Local Record instances
                           |               |
                           |               v
                 +---------+--------------------------+
                 |  Local RepositorySession instance  |
                 +------------------------------------+


 * @author rnewman
 *
 */
public class Crypto5MiddlewareRepositorySession extends RepositorySession {
  private KeyBundle keyBundle;
  private RepositorySession inner;
  private RecordFactory recordFactory;

  public Crypto5MiddlewareRepositorySession(RepositorySession session, Crypto5MiddlewareRepository repository, long lastSyncTimestamp, RecordFactory recordFactory) {
    super(repository, lastSyncTimestamp);
    this.keyBundle = repository.keyBundle;
    this.recordFactory = recordFactory;
  }

  public class DecryptingTransformingFetchDelegate implements RepositorySessionFetchRecordsDelegate {
    private RepositorySessionFetchRecordsDelegate next;
    private KeyBundle keyBundle;

    DecryptingTransformingFetchDelegate(RepositorySessionFetchRecordsDelegate next, KeyBundle bundle) {
      this.next = next;
      this.keyBundle = bundle;
    }

    @Override
    public void onFetchFailed(Exception ex) {
      next.onFetchFailed(ex);
    }

    @Override
    public void onFetchSucceeded(Record[] records) {
      CryptoRecord[] cryptoRecords = (CryptoRecord[]) records;

      // Partition the input so that we can handle decryption errors.
      ArrayList<CryptoRecord> failed = new ArrayList<CryptoRecord>();
      ArrayList<Record> succeeded = new ArrayList<Record>();
      for (CryptoRecord record : cryptoRecords) {
        record.keyBundle = keyBundle;
        try {
          record.decrypt();
        } catch (Exception e) {
          failed.add(record);
          break;
        }
        Record transformed;
        try {
          transformed = recordFactory.createRecord(record);
        } catch (Exception e) {
          failed.add(record);
          break;
        }
        succeeded.add(transformed);
      }
      if (failed.size() > 0) {
        next.onFetchFailed(failed.toArray(null));
      }
    }

  }


  @Override
  public void guidsSince(long timestamp,
                         RepositorySessionGuidsSinceDelegate delegate) {
    // TODO: need to do anything here?
    inner.guidsSince(timestamp, delegate);
  }

  @Override
  public void fetchSince(long timestamp,
                         RepositorySessionFetchRecordsDelegate delegate) {
    // TODO: unwrap.
    inner.fetchSince(timestamp, delegate);
  }

  @Override
  public void fetch(String[] guids,
                    RepositorySessionFetchRecordsDelegate delegate) {
    // TODO: unwrap.
    inner.fetch(guids, delegate);
  }

  @Override
  public void fetchAll(RepositorySessionFetchRecordsDelegate delegate) {
    // TODO: unwrap.
    inner.fetchAll(delegate);
  }

  @Override
  public void store(Record record, RepositorySessionStoreDelegate delegate) {
    CryptoRecord rec = record.getPayload();
    rec.keyBundle = this.keyBundle;
    try {
      rec.encrypt();
    } catch (UnsupportedEncodingException e) {
      delegate.onStoreFailed(e);
      return;
    } catch (CryptoException e) {
      delegate.onStoreFailed(e);
      return;
    }
    // TODO: it remains to be seen how this will work.
    inner.store(rec, delegate);
  }

  @Override
  public void wipe(RepositorySessionWipeDelegate delegate) {
    inner.wipe(delegate);
  }
}
