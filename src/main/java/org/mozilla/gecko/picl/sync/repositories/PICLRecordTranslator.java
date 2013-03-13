package org.mozilla.gecko.picl.sync.repositories;

import java.io.IOException;

import org.json.simple.parser.ParseException;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.NonObjectJSONException;
import org.mozilla.gecko.sync.repositories.domain.Record;

public interface PICLRecordTranslator {
  public ExtendedJSONObject fromRecord(Record record);
  public Record toRecord(ExtendedJSONObject json) throws NonObjectJSONException, IOException, ParseException;
}
