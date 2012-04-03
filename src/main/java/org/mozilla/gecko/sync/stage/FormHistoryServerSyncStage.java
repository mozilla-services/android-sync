/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.stage;

import java.net.URISyntaxException;

import org.mozilla.gecko.sync.CryptoRecord;
import org.mozilla.gecko.sync.repositories.ConstrainedServer11Repository;
import org.mozilla.gecko.sync.repositories.RecordFactory;
import org.mozilla.gecko.sync.repositories.Repository;
import org.mozilla.gecko.sync.repositories.android.FormHistoryRepositorySession;
import org.mozilla.gecko.sync.repositories.domain.FormHistoryRecord;
import org.mozilla.gecko.sync.repositories.domain.Record;

public class FormHistoryServerSyncStage extends ServerSyncStage {

  // Eventually this kind of sync stage will be data-driven,
  // and all this hard-coding can go away.
  private static final String FORM_HISTORY_SORT          = "index";
  private static final long   FORM_HISTORY_REQUEST_LIMIT = 5000;         // Sanity limit.

  @Override
  public void execute(org.mozilla.gecko.sync.GlobalSession session) throws NoSuchStageException {
    super.execute(session);
  }

  @Override
  protected String getCollection() {
    return "forms";
  }
  @Override
  protected String getEngineName() {
    return "forms";
  }

  @Override
  protected Repository getRemoteRepository() throws URISyntaxException {
    return new ConstrainedServer11Repository(session.config.getClusterURLString(),
                                             session.config.username,
                                             getCollection(),
                                             session,
                                             FORM_HISTORY_REQUEST_LIMIT,
                                             FORM_HISTORY_SORT);
  }

  @Override
  protected Repository getLocalRepository() {
    return new FormHistoryRepositorySession.FormHistoryRepository();
  }

  public class FormHistoryRecordFactory extends RecordFactory {

    @Override
    public Record createRecord(Record record) {
      FormHistoryRecord r = new FormHistoryRecord();
      r.initFromEnvelope((CryptoRecord) record);
      return r;
    }
  }

  @Override
  protected RecordFactory getRecordFactory() {
    return new FormHistoryRecordFactory();
  }

  @Override
  public boolean isEnabled() {
    return false;
  }
}
