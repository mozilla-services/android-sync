package org.mozilla.gecko.sync;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import org.json.simple.parser.ParseException;
import org.mozilla.gecko.sync.repositories.RepositorySessionBundle;

import android.os.Bundle;

public class SynchronizerConfigurations {
  private static final int CONFIGURATION_VERSION = 1;
  private HashMap<String, SynchronizerConfiguration> engines;

  protected HashMap<String, SynchronizerConfiguration> enginesMapFromBundleV1(Bundle engineBundle) throws IOException, ParseException, NonObjectJSONException {
    HashMap<String, SynchronizerConfiguration> engines = new HashMap<String, SynchronizerConfiguration>();
    Set<String> keySet = engineBundle.keySet();
    for (String engine : keySet) {
      String[] values = engineBundle.getStringArray(engine);
      String syncID                        = values[0];
      RepositorySessionBundle remoteBundle = new RepositorySessionBundle(values[1]);
      RepositorySessionBundle localBundle  = new RepositorySessionBundle(values[2]);
      engines.put(engine, new SynchronizerConfiguration(syncID, remoteBundle, localBundle));
    }
    return engines;
  }

  public SynchronizerConfigurations(Bundle bundle) throws IOException, ParseException, NonObjectJSONException, UnknownSynchronizerConfigurationVersionException {
    Bundle engineBundle = bundle.getBundle("engines");
    if (engineBundle == null) {
      // No saved state.
      // TODO
      return;
    }
    int version = engineBundle.getInt("version");
    if (version == 0) {
      // No data in the bundle.
      engines = new HashMap<String, SynchronizerConfiguration>();
      return;
    }
    if (version == 1) {
      engines = enginesMapFromBundleV1(engineBundle);
      return;
    }
    throw new UnknownSynchronizerConfigurationVersionException(version);
  }

  public void fillBundle(Bundle bundle) {
    Bundle contents = new Bundle();
    for (Entry<String, SynchronizerConfiguration> entry : engines.entrySet()) {
      contents.putStringArray(entry.getKey(), entry.getValue().toStringValues());
    }
    contents.putInt("version", CONFIGURATION_VERSION);
    bundle.putBundle("engines", contents);
  }

  public SynchronizerConfiguration forEngine(String engineName) {
    return engines.get(engineName);
  }
}
