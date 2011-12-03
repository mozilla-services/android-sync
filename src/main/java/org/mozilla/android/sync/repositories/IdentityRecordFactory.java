package org.mozilla.android.sync.repositories;

import org.mozilla.android.sync.repositories.domain.Record;

public class IdentityRecordFactory extends RecordFactory {

  @Override
  public Record createRecord(Record record) {
    return record;
  }
}
