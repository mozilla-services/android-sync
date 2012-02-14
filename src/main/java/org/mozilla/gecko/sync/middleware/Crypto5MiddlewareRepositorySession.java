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

package org.mozilla.gecko.sync.middleware;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.ExecutorService;

import org.mozilla.gecko.sync.crypto.CryptoException;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.CryptoRecord;
import org.mozilla.gecko.sync.repositories.NoStoreDelegateException;
import org.mozilla.gecko.sync.repositories.RecordFactory;
import org.mozilla.gecko.sync.repositories.RepositorySession;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionFetchRecordsDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionGuidsSinceDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionStoreDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionWipeDelegate;
import org.mozilla.gecko.sync.repositories.domain.Record;

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

  public Crypto5MiddlewareRepositorySession(RepositorySession session, Crypto5MiddlewareRepository repository, RecordFactory recordFactory) {
    super(repository);
    this.inner = session;
    this.keyBundle = repository.keyBundle;
    this.recordFactory = recordFactory;
  }

  public class DecryptingTransformingFetchDelegate implements RepositorySessionFetchRecordsDelegate {
    private RepositorySessionFetchRecordsDelegate next;
    private KeyBundle keyBundle;
    private RecordFactory recordFactory;

    DecryptingTransformingFetchDelegate(RepositorySessionFetchRecordsDelegate next, KeyBundle bundle, RecordFactory recordFactory) {
      this.next = next;
      this.keyBundle = bundle;
      this.recordFactory = recordFactory;
    }

    @Override
    public void onFetchFailed(Exception ex, Record record) {
      next.onFetchFailed(ex, record);
    }

    @Override
    public void onFetchedRecord(Record record) {
      CryptoRecord r;
      try {
        r = (CryptoRecord) record;
      } catch (ClassCastException e) {
        next.onFetchFailed(e, record);
        return;
      }
      r.keyBundle = keyBundle;
      try {
        r.decrypt();
      } catch (Exception e) {
        next.onFetchFailed(e, r);
        return;
      }
      Record transformed;
      try {
        transformed = this.recordFactory.createRecord(r);
      } catch (Exception e) {
        next.onFetchFailed(e, r);
        return;
      }
      next.onFetchedRecord(transformed);
    }

    @Override
    public void onFetchSucceeded(Record[] records, long end) {
      for (Record record : records) {
        try {
          this.onFetchedRecord(record);
        } catch (Exception e) {
          this.onFetchFailed(e, record);
        }
      }
      this.onFetchCompleted(end);
    }

    @Override
    public void onFetchCompleted(long end) {
      next.onFetchCompleted(end);
    }

    @Override
    public RepositorySessionFetchRecordsDelegate deferredFetchDelegate(ExecutorService executor) {
      // Synchronously perform *our* work, passing through appropriately.
      RepositorySessionFetchRecordsDelegate deferredNext = next.deferredFetchDelegate(executor);
      return new DecryptingTransformingFetchDelegate(deferredNext, keyBundle, recordFactory);
    }
  }

  private DecryptingTransformingFetchDelegate makeUnwrappingDelegate(RepositorySessionFetchRecordsDelegate inner) {
    if (inner == null) {
      throw new IllegalArgumentException("Inner delegate cannot be null!");
    }
    return new DecryptingTransformingFetchDelegate(inner, this.keyBundle, this.recordFactory);
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
    inner.fetchSince(timestamp, makeUnwrappingDelegate(delegate));
  }

  @Override
  public void fetch(String[] guids,
                    RepositorySessionFetchRecordsDelegate delegate) {
    inner.fetch(guids, makeUnwrappingDelegate(delegate));
  }

  @Override
  public void fetchAll(RepositorySessionFetchRecordsDelegate delegate) {
    inner.fetchAll(makeUnwrappingDelegate(delegate));
  }

  @Override
  public void setStoreDelegate(RepositorySessionStoreDelegate delegate) {
    // TODO: it remains to be seen how this will work.
    inner.setStoreDelegate(delegate);
    this.delegate = delegate;             // So we can handle errors without involving inner.
  }

  @Override
  public void store(Record record) throws NoStoreDelegateException {
    if (delegate == null) {
      throw new NoStoreDelegateException();
    }
    CryptoRecord rec = record.getPayload();
    rec.keyBundle = this.keyBundle;
    try {
      rec.encrypt();
    } catch (UnsupportedEncodingException e) {
      delegate.onRecordStoreFailed(e);
      return;
    } catch (CryptoException e) {
      delegate.onRecordStoreFailed(e);
      return;
    }
    // Allow the inner session to do delegate handling.
    inner.store(rec);
  }

  @Override
  public void wipe(RepositorySessionWipeDelegate delegate) {
    inner.wipe(delegate);
  }

  @Override
  public void storeDone() {
    inner.storeDone();
  }

  @Override
  public void storeDone(long end) {
    inner.storeDone(end);
  }
}
