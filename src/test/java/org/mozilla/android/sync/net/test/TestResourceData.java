package org.mozilla.android.sync.net.test;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.simpleframework.transport.connect.Connection;
import org.simpleframework.transport.connect.SocketConnection;

public class TestResourceData {
  public Connection connection;
  public TestServer server;

  public TestServer startHTTPServer(TestServer server) {
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

  public TestServer startHTTPServer() {
    return this.startHTTPServer(new TestServer());
  }

  public void stopHTTPServer() {
    try {
      connection.close();
      server = null;
      connection = null;
    } catch (IOException ex) {
      System.err.println("Error stopping test HTTP server.");
      fail(ex.toString());
    }
  }

  public TestResourceData() {
  }
}