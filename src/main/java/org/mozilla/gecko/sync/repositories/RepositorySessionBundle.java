package org.mozilla.gecko.sync.repositories;

import java.io.IOException;

import org.json.simple.parser.ParseException;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.NonObjectJSONException;

import android.util.Log;

public class RepositorySessionBundle extends ExtendedJSONObject {

  private static final String LOG_TAG = "RepositorySessionBundle";

  public RepositorySessionBundle() {
    super();
  }

  public RepositorySessionBundle(String jsonString) throws IOException, ParseException, NonObjectJSONException {
    super(jsonString);
  }

  public RepositorySessionBundle(long lastSyncTimestamp) {
    this();
    this.setTimestamp(lastSyncTimestamp);
  }

  public long getTimestamp() {
    if (this.containsKey("timestamp")) {
      return this.getLong("timestamp");
    }
    return -1;
  }

  public void setTimestamp(long timestamp) {
    Log.d(LOG_TAG, "Setting timestamp on RepositorySessionBundle to " + timestamp);
    this.put("timestamp", new Long(timestamp));
  }

  public void bumpTimestamp(long timestamp) {
    if (timestamp > this.getTimestamp()) {
      this.setTimestamp(timestamp);
    }
  }
}
