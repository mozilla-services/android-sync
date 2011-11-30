package org.mozilla.android.sync.repositories.domain;

public class HistoryRecord extends Record {

  public HistoryRecord(String guid, String collection, long lastModified,
      boolean deleted) {
    super(guid, collection, lastModified, deleted);
    // TODO Auto-generated constructor stub
  }
  
  

  public long     androidID;
  public String   title;
  public String   histURI;
  // TODO Change this once we start doing local sync and figuring out how to track visits 
  public String   visits; 
  public int      type;
  public long     dateVisited;

}
