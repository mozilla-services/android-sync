/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.sync.net.BaseResourceDelegate;
import org.simpleframework.transport.connect.Connection;
import org.simpleframework.transport.connect.SocketConnection;

public class HTTPServerTestHelper {
  private static final String LOG_TAG = "HTTPServerTestHelper";
  private int port;

  public HTTPServerTestHelper(int port) {
    this.port = port;
  }

  public Connection connection;
  public MockServer server;

  public MockServer startHTTPServer(MockServer server) {
    BaseResource.rewriteLocalhost = false; // No sense rewriting when we're running the unit tests.
    BaseResourceDelegate.connectionTimeoutInMillis = 1000; // No sense waiting a long time for a local connection.

    try {
      Logger.info(LOG_TAG, "Starting HTTP server on port " + port + "...");
      this.server = server;
      connection = new SocketConnection(server);
      SocketAddress address = new InetSocketAddress(port);
      connection.connect(address);
      Logger.info(LOG_TAG, "Starting HTTP server on port " + port + "... DONE");
    } catch (IOException ex) {
      Logger.error(LOG_TAG, "Error starting HTTP server on port " + port + "... DONE", ex);
      fail(ex.toString());
    }
    return server;
  }

  public MockServer startHTTPServer() {
    return this.startHTTPServer(new MockServer());
  }

  public void stopHTTPServer() {
    Logger.info(LOG_TAG, "Stopping HTTP server on port " + port + "...");
    try {
      if (connection != null) {
        connection.close();
      }
      server = null;
      connection = null;
      Logger.info(LOG_TAG, "Closing connection pool...");
      BaseResource.shutdownConnectionManager();
      Logger.info(LOG_TAG, "Stopping HTTP server on port " + port + "... DONE");
    } catch (IOException ex) {
      Logger.error(LOG_TAG, "Error stopping HTTP server on port " + port + "... DONE", ex);
      fail(ex.toString());
    }
  }
}
