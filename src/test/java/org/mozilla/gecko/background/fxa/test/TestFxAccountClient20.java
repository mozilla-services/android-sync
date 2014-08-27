/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.background.fxa.test;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import junit.framework.Assert;

import org.junit.Test;
import org.mozilla.gecko.background.fxa.FxAccountClient20;

public class TestFxAccountClient20 {
  protected static class MockFxAccountClient20 extends FxAccountClient20 {
    public MockFxAccountClient20(String serverURI, Executor executor) {
      super(serverURI, executor);
    }

    // Public for testing.
    @Override
    public URI getCreateAccountURI(final boolean getKeys, final String service) throws UnsupportedEncodingException, URISyntaxException {
      return super.getCreateAccountURI(getKeys, service);
    }
  }

  @Test
  public void testGetCreateAccountURI() throws Exception {
    final String TEST_SERVER = "https://test.com:4430/inner/v1/";
    final MockFxAccountClient20 client = new MockFxAccountClient20(TEST_SERVER, Executors.newSingleThreadExecutor());
    Assert.assertEquals(TEST_SERVER + "account/create?service=sync&keys=true", client.getCreateAccountURI(true, "sync").toASCIIString());
    Assert.assertEquals(TEST_SERVER + "account/create?service=two+words", client.getCreateAccountURI(false, "two words").toASCIIString());
    Assert.assertEquals(TEST_SERVER + "account/create?service=symbols%2F%3A%3F%2B", client.getCreateAccountURI(false, "symbols/:?+").toASCIIString());
  }
}
