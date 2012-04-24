/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.PrintStream;

import org.mozilla.gecko.sync.Utils;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.core.Container;

public class MockServer implements Container {
  public int statusCode = 200;
  public String body = "Hello World";

  public MockServer() {
  }

  public MockServer(int statusCode, String body) {
    this.statusCode = statusCode;
    this.body = body;
  }

  public String expectedBasicAuthHeader;

  protected PrintStream handleBasicHeaders(Request request, Response response, int code, String contentType) throws IOException {
    return this.handleBasicHeaders(request, response, code, contentType, System.currentTimeMillis());
  }

  protected PrintStream handleBasicHeaders(Request request, Response response, int code, String contentType, long time) throws IOException {
    System.out.println("< Auth header: " + request.getValue("Authorization"));

    PrintStream bodyStream = response.getPrintStream();
    response.setCode(code);
    response.set("Content-Type", contentType);
    response.set("Server", "HelloWorld/1.0 (Simple 4.0)");
    response.setDate("Date", time);
    response.setDate("Last-Modified", time);

    final String timestampHeader = Utils.millisecondsToDecimalSecondsString(time);
    response.set("X-Weave-Timestamp", timestampHeader);
    System.out.println("> X-Weave-Timestamp header: " + timestampHeader);
    return bodyStream;
  }

  protected void handle(Request request, Response response, int code, String body) {
    try {
      System.out.println("Handling request...");
      PrintStream bodyStream = this.handleBasicHeaders(request, response, code, "application/json");

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
    this.handle(request, response, statusCode, body);
  }
}
