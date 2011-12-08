/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.net.test;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.simpleframework.transport.connect.Connection;
import org.simpleframework.transport.connect.SocketConnection;

public class HTTPServerTestHelper {
  public Connection connection;
  public MockServer server;

  public MockServer startHTTPServer(MockServer server) {
    try {
      System.out.println("Starting HTTP server...");
      this.server = server;
      connection = new SocketConnection(server);
      SocketAddress address = new InetSocketAddress(8080);
      connection.connect(address);
      System.out.println("Done starting.");
    } catch (IOException ex) {
      System.err.println("Error starting test HTTP server.");
      ex.printStackTrace();
      fail(ex.toString());
    }
    return server;
  }

  public MockServer startHTTPServer() {
    return this.startHTTPServer(new MockServer());
  }

  public void stopHTTPServer() {
    System.out.println("Stopping HTTP server.");
    try {
      connection.close();
      server = null;
      connection = null;
    } catch (IOException ex) {
      System.err.println("Error stopping test HTTP server.");
      fail(ex.toString());
    }
  }

  public HTTPServerTestHelper() {
  }
}
