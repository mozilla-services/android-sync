package org.mozilla.android.sync.repositories.domain;

import org.mozilla.android.sync.CryptoRecord;
import org.mozilla.android.sync.repositories.Utils;

public class HistoryRecord extends Record implements SyncRecord {

  public HistoryRecord(String guid, String collection, long lastModified,
      boolean deleted) {
    super(guid, collection, lastModified, deleted);
  }
  public HistoryRecord(String guid, String collection, long lastModified) {
    super(guid, collection, lastModified, false);
  }
  public HistoryRecord(String guid, String collection) {
    super(guid, collection, 0, false);
  }
  public HistoryRecord(String guid) {
    super(guid, "history", 0, false);
  }
  public HistoryRecord() {
    super(Utils.generateGuid(), "history", 0, false);
  }

  public long     androidID;
  public String   title;
  public String   histURI;
  // TODO Change this once we start doing local sync and figuring out how to track visits 
  public String   visits; 
  public int      type;
  public long     dateVisited;
  
  @Override
  public void initFromPayload(CryptoRecord payload) {
    this.histURI = (String) payload.payload.get("histUri");
    this.title       = (String) payload.payload.get("title");
    // TODO add missing fields
  }
  @Override
  public CryptoRecord getPayload() {
    // TODO Auto-generated method stub
    return null;
  }

}
