package org.mozilla.android.sync.net.test;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.PrintStream;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.core.Container;

public class TestServer implements Container {
  public String expectedBasicAuthHeader;
  protected void handle(Request request, Response response, int code, String body) {
    try {
      System.out.println("Handling request...");
      PrintStream bodyStream = response.getPrintStream();
      long time = System.currentTimeMillis();
      response.setCode(code);
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
      
      bodyStream.println(body);
      bodyStream.close();
    } catch (IOException e) {
      System.err.println("Oops.");
    }    
  }
  public void handle(Request request, Response response) {
    this.handle(request, response, 200, "Hello World");
  }
}