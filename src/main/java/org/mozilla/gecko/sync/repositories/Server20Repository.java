/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.repositories;

import java.net.URISyntaxException;

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
}
