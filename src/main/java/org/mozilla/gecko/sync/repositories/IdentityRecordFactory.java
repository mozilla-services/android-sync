package org.mozilla.gecko.sync.repositories;

import org.mozilla.gecko.sync.repositories.domain.Record;

public class IdentityRecordFactory extends RecordFactory {

  @Override
  public Record createRecord(Record record) {
    return record;
  }
}
