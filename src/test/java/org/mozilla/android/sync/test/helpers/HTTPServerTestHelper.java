/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.IdentityHashMap;
import java.util.Map;

import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.sync.net.SyncResourceDelegate;
import org.simpleframework.transport.connect.Connection;
import org.simpleframework.transport.connect.SocketConnection;

/**
 * Test helper code to bind <code>MockServer</code> instances to ports.
 * <p>
 * Maintains a collection of running servers and (by default) throws helpful
 * errors if two servers are started "on top" of each other. The
 * <b>unchecked</b> exception thrown contains a stack trace pointing to where
 * the new server is being created and where the pre-existing server was
 * created.
 */
public class HTTPServerTestHelper {
  private static final String LOG_TAG = "HTTPServerTestHelper";

  public int port;
  public Connection connection;
  public MockServer server;

  public HTTPServerTestHelper(int port) {
    this.port = port;
  }

  /**
   * Used to maintain a stack trace pointing to where a server was started.
   */
  public static class HTTPServerStartedError extends Error {
    private static final long serialVersionUID = -6778447718799087274L;

    public final HTTPServerTestHelper httpServer;

    public HTTPServerStartedError(HTTPServerTestHelper httpServer) {
      this.httpServer = httpServer;
    }
  }

  /**
   * Thrown when a server is started "on top" of another server. The cause error
   * will be an <code>HTTPServerStartedError</code> with a stack trace pointing
   * to where the pre-existing server was started.
   */
  public static class HTTPServerAlreadyRunningError extends Error {
    private static final long serialVersionUID = -6778447718799087275L;

    public HTTPServerAlreadyRunningError(Throwable e) {
      super(e);
    }
  }

  /**
   * Maintain a hash of running servers. Each value is an error with a stack
   * traces pointing to where that server was started.
   * <p>
   * We don't key on the server itself because each server is a <it>helper</it>
   * that may be started many times with different <code>MockServer</code>
   * instances.
   * <p>
   * Synchronize access on the class.
   */
  protected static Map<Connection, HTTPServerStartedError> runningServers =
      new IdentityHashMap<Connection, HTTPServerStartedError>();

  protected synchronized static void throwIfServerAlreadyRunning() {
    for (HTTPServerStartedError value : runningServers.values()) {
      throw new HTTPServerAlreadyRunningError(value);
    }
  }

  protected synchronized static void registerServerAsRunning(HTTPServerTestHelper httpServer) {
    if (httpServer == null || httpServer.connection == null) {
      throw new IllegalArgumentException("HTTPServerTestHelper or connection was null; perhaps server has not been started?");
    }

    HTTPServerStartedError old = runningServers.put(httpServer.connection, new HTTPServerStartedError(httpServer));
    if (old != null) {
      // Should never happen.
      throw old;
    }
  }

  protected synchronized static void unregisterServerAsRunning(HTTPServerTestHelper httpServer) {
    if (httpServer == null || httpServer.connection == null) {
      throw new IllegalArgumentException("HTTPServerTestHelper or connection was null; perhaps server has not been started?");
    }

    runningServers.remove(httpServer.connection);
  }

  public MockServer startHTTPServer(MockServer server, boolean allowMultipleServers) {
    BaseResource.rewriteLocalhost = false; // No sense rewriting when we're running the unit tests.
    SyncResourceDelegate.connectionTimeoutInMillis = 1000; // No sense waiting a long time for a local connection.

    if (!allowMultipleServers) {
      throwIfServerAlreadyRunning();
    }

    try {
      Logger.info(LOG_TAG, "Starting HTTP server on port " + port + "...");
      this.server = server;
      connection = new SocketConnection(server);
      SocketAddress address = new InetSocketAddress(port);
      connection.connect(address);

      registerServerAsRunning(this);

      Logger.info(LOG_TAG, "Starting HTTP server on port " + port + "... DONE");
    } catch (IOException ex) {
      Logger.error(LOG_TAG, "Error starting HTTP server on port " + port + "... DONE", ex);
      fail(ex.toString());
    }

    return server;
  }

  public MockServer startHTTPServer(MockServer server) {
    return startHTTPServer(server, false);
  }

  public MockServer startHTTPServer() {
    return startHTTPServer(new MockServer());
  }

  public void stopHTTPServer() {
    Logger.info(LOG_TAG, "Stopping HTTP server on port " + port + "...");
    try {
      if (connection != null) {
        unregisterServerAsRunning(this);

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
