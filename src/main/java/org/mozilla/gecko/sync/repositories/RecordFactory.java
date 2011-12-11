package org.mozilla.gecko.sync.repositories;

import org.mozilla.gecko.sync.repositories.domain.Record;

// Take a record retrieved from some middleware, producing
// some concrete record type for application to some local repository.
public abstract class RecordFactory {
  public abstract Record createRecord(Record record);
}
