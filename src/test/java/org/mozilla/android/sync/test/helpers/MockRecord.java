package org.mozilla.android.sync.test.helpers;

import org.mozilla.gecko.sync.CryptoRecord;
import org.mozilla.gecko.sync.repositories.domain.Record;

public class MockRecord extends Record {
  public MockRecord(String guid, String collection, long lastModified,
      boolean deleted) {
    super(guid, collection, lastModified, deleted);
  }

  @Override
  public void initFromPayload(CryptoRecord payload) {
  }

  @Override
  public CryptoRecord getPayload() {
    return null;
  }

  @Override
  public String toJSONString() {
    return "{\"id\":\"" + guid + "\", \"payload\": \"foo\"}";
  }

  @Override
  public Record copyWithIDs(String guid, long androidID) {
    return new MockRecord(guid, this.collection, this.lastModified, this.deleted);
  }
}
