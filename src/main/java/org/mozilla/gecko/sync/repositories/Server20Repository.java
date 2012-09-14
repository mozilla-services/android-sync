/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.repositories;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import org.mozilla.gecko.sync.CredentialsSource;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionCreationDelegate;

import android.content.Context;

/**
 * Fetch and store against the Sync 2.0 HTTP API, documented at
 * <a href="http://docs.services.mozilla.com/storage/apis-2.0.html">http://docs.services.mozilla.com/storage/apis-2.0.html</a>.
 * <p>
 * It doesn't do crypto: that's the job of the middleware.
 */
public class Server20Repository extends ServerRepository {
  public static final String API_VERSION = "2.0";

  /**
   * @param serverURI
   *        URI of the Sync 2.0 server (string)
   * @param username
   *        Username on the server (string)
   * @param collection
   *        Name of the collection (string)
   * @throws URISyntaxException
   */
  public Server20Repository(String serverURI, String username, String collection, CredentialsSource credentialsSource) throws URISyntaxException {
    super(serverURI, API_VERSION, username, collection, credentialsSource);
  }

  @Override
  public void createSession(final RepositorySessionCreationDelegate delegate, final Context context) {
    delegate.onSessionCreated(new Server20RepositorySession(this));
  }

  public URI collectionURI(boolean full, long newer, long limit, String sort, String ids) throws URISyntaxException {
    ArrayList<String> params = new ArrayList<String>();
    if (full) {
      params.add("full=1");
    }
    if (newer >= 0) {
      params.add("newer=" + newer);
    }
    if (limit > 0) {
      params.add("limit=" + limit);
    }
    if (sort != null) {
      params.add("sort=" + sort);       // We trust these values.
    }
    if (ids != null) {
      params.add("ids=" + ids);         // We trust these values.
    }

    if (params.size() == 0) {
      return this.collectionPathURI;
    }

    StringBuilder out = new StringBuilder();
    char indicator = '?';
    for (String param : params) {
      out.append(indicator);
      indicator = '&';
      out.append(param);
    }
    String uri = this.collectionPath + out.toString();
    return new URI(uri);
  }
}
