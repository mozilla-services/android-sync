package org.mozilla.android.sync.test.helpers;

import org.mozilla.gecko.sync.CryptoRecord;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.repositories.domain.Record;

public class MockRecord extends Record {
  public MockRecord(String guid, String collection, long lastModified,
      boolean deleted) {
    super(guid, collection, lastModified, deleted);
  }

  @Override
  public void initFromEnvelope(CryptoRecord payload) {
  }

  @Override
  public CryptoRecord getEnvelope() {
    return null;
  }

  @Override
  protected void populatePayload(ExtendedJSONObject payload) {
  }

  @Override
  protected void initFromPayload(ExtendedJSONObject payload) {
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
