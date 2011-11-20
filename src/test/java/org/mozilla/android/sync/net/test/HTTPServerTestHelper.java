package org.mozilla.android.sync.net.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.core.Container;
import org.simpleframework.transport.connect.Connection;
import org.simpleframework.transport.connect.SocketConnection;

public class HTTPServerTestHelper {
  public Connection connection;
  public TestServer server;

  public class TestServer implements Container {
    public String expectedBasicAuthHeader;
    public void handle(Request request, Response response) {
      try {
        System.out.println("Handling request...");
        PrintStream body = response.getPrintStream();
        long time = System.currentTimeMillis();
        response.set("Content-Type", "application/json");
        response.set("Server", "HelloWorld/1.0 (Simple 4.0)");
        response.setDate("Date", time);
        response.setDate("Last-Modified", time);
        response.set("X-Weave-Timestamp", Long.toString(time));
        System.out.println("Auth header: " + request.getValue("Authorization"));
        
        if (expectedBasicAuthHeader != null) {
          System.out.println("Expecting auth header " + expectedBasicAuthHeader);
          assertEquals(request.getValue("Authorization"), expectedBasicAuthHeader);
        }
        
        body.println("Hello World");
        body.close();
      } catch (IOException e) {
        System.err.println("Oops.");
      }
    }
  }

  public TestServer startHTTPServer() {
    try {
      System.out.println("Starting HTTP server...");
      server = new TestServer();
      connection = new SocketConnection(server);
      SocketAddress address = new InetSocketAddress(8080);
      connection.connect(address);
      System.out.println("Done starting.");
    } catch (IOException ex) {
      System.err.println("Error starting test HTTP server.");
      fail(ex.toString());
    }
    return server;
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

  public HTTPServerTestHelper() {
  }
}
