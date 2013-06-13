/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.background.bagheera.test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;

import junit.framework.Assert;

import org.junit.Test;
import org.mozilla.android.sync.test.helpers.HTTPServerTestHelper;
import org.mozilla.android.sync.test.helpers.WaitHelper;
import org.mozilla.gecko.background.bagheera.BagheeraClient;
import org.mozilla.gecko.background.bagheera.BagheeraRequestDelegate;
import org.mozilla.gecko.background.common.log.Logger;

import ch.boye.httpclientandroidlib.HttpResponse;

import com.mozilla.bagheera.BagheeraProto;
import com.mozilla.bagheera.BagheeraProto.BagheeraMessage;
import com.mozilla.bagheera.http.Bagheera;
import com.mozilla.bagheera.http.Bagheera.BagheeraServerState;
import com.mozilla.bagheera.metrics.MetricsManager;
import com.mozilla.bagheera.producer.Producer;
import com.mozilla.bagheera.util.WildcardProperties;

public class TestBagheeraClient {
  public static final String LOG_TAG = TestBagheeraClient.class.getSimpleName();

  private static final int TEST_PORT = HTTPServerTestHelper.getTestPort();

  private static BagheeraServerState initBagheeraServer() throws Exception {
    final String namespace = "test";
    final String channelGroup = "TestGroup";

    final Producer producer = new TestProducer();
    final MetricsManager m = new MetricsManager(new Properties(), "");

    final WildcardProperties props = new WildcardProperties();
    props.put("valid.namespaces", namespace);
    props.put(namespace + ".allow.delete.access", "true");

    return Bagheera.startServer(TEST_PORT,
                                true,
                                props,
                                producer,
                                Bagheera.getChannelFactory(),
                                channelGroup,
                                m);
  }

  private static void upload(final BagheeraClient client, final BagheeraRequestDelegate delegate, final String id, final String document) {
    WaitHelper.getTestWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        try {
          client.uploadJSONDocument("test", id, document, null, delegate);
        } catch (URISyntaxException e) {
          WaitHelper.getTestWaiter().performNotify(e);
        }
      }
    });
  }

  private static void deleteDocument(final BagheeraClient client, final BagheeraRequestDelegate delegate, final String id) {
    WaitHelper.getTestWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        try {
          client.deleteDocument("test", id, delegate);
        } catch (URISyntaxException e) {
          WaitHelper.getTestWaiter().performNotify(e);
        }
      }
    });
  }

  @Test
  public final void testURIComponent() throws Exception {
    Assert.assertFalse(BagheeraClient.isValidURIComponent(""));
    Assert.assertFalse(BagheeraClient.isValidURIComponent("+"));
    Assert.assertFalse(BagheeraClient.isValidURIComponent(" "));
    Assert.assertFalse(BagheeraClient.isValidURIComponent("abc "));
    Assert.assertFalse(BagheeraClient.isValidURIComponent("{}"));
    Assert.assertTrue(BagheeraClient.isValidURIComponent("_abc"));
    Assert.assertTrue(BagheeraClient.isValidURIComponent("_ab-c"));
    Assert.assertTrue(BagheeraClient.isValidURIComponent("-_"));
    Assert.assertTrue(BagheeraClient.isValidURIComponent("-_999"));
    Assert.assertTrue(BagheeraClient.isValidURIComponent("abcdefghijklmNOPQRST12345-"));
  }

  @Test
  public final void testInit() throws Exception {
    BagheeraServerState server = initBagheeraServer();
    server.close();
  }

  @Test
  public final void testUpload() throws Exception {
    BagheeraServerState server = null;
    try {
      server = initBagheeraServer();
      TestProducer producer = (TestProducer) server.producer;
      final BagheeraClient client = new BagheeraClient("http://127.0.0.1:" + TEST_PORT);
      final TestBagheeraRequestDelegate delegate = new TestBagheeraRequestDelegate();

      final String document = "{\"abc\": 1}";
      final String badID = "foo";
      final String goodID = "032cc07e-2b0e-d14b-a23d-55901ad3529a";

      upload(client, delegate, badID, document);
      Assert.assertEquals(delegate.statuses.get(0).intValue(), 404);
      delegate.clear();

      upload(client, delegate, goodID, document);
      Assert.assertEquals(delegate.statuses.get(0).intValue(), 201);
      Assert.assertEquals(producer.messages.size(), 1);
      final BagheeraMessage message = producer.messages.get(0);
      final String body = message.getPayload().toStringUtf8();
      Logger.info(LOG_TAG, "Body was " + body);
      Assert.assertEquals(message.getId(), goodID);
      Assert.assertEquals(body, document);
      delegate.clear();
    } finally {
      server.close();
    }
  }


  @Test
  public final void testDelete() throws Exception {
    BagheeraServerState server = null;
    try {
      server = initBagheeraServer();
      TestProducer producer = (TestProducer) server.producer;
      Assert.assertEquals(0, producer.messages.size());

      final BagheeraClient client = new BagheeraClient("http://127.0.0.1:" + TEST_PORT);
      final TestBagheeraRequestDelegate delegate = new TestBagheeraRequestDelegate();

      final String document = "{\"abc\": 1}";
      final String badID = "foo";
      final String goodID = "032cc07e-2b0e-d14b-a23d-55901ad3529a";

      deleteDocument(client, delegate, badID);
      Assert.assertEquals(404, delegate.statuses.get(0).intValue());
      delegate.clear();
      Assert.assertEquals(0, producer.messages.size());

      // It doesn't matter if the document doesn't exist.
      deleteDocument(client, delegate, goodID);
      Assert.assertEquals(200, delegate.statuses.get(0).intValue());
      Assert.assertEquals(1, producer.messages.size());
      final BagheeraMessage deleteMessage = producer.messages.get(0);
      Assert.assertEquals(BagheeraProto.BagheeraMessage.Operation.DELETE,
                          deleteMessage.getOperation());
      Assert.assertEquals(goodID, deleteMessage.getId());
      delegate.clear();

      upload(client, delegate, goodID, document);
      Assert.assertEquals(2, producer.messages.size());
      final BagheeraMessage uploadMessage = producer.messages.get(1);
      final String body = uploadMessage.getPayload().toStringUtf8();

      Assert.assertEquals(goodID, uploadMessage.getId());
      Assert.assertEquals(document, body);
      Assert.assertEquals(BagheeraProto.BagheeraMessage.Operation.CREATE_UPDATE,
                          uploadMessage.getOperation());
      deleteDocument(client, delegate, goodID);
      Assert.assertEquals(201, delegate.statuses.get(0).intValue());
      Assert.assertEquals(200, delegate.statuses.get(1).intValue());
      delegate.clear();
    } finally {
      server.close();
    }
  }


  public static final class TestBagheeraRequestDelegate implements
      BagheeraRequestDelegate {
    public ArrayList<Integer> statuses = new ArrayList<Integer>();

    public void clear() {
      statuses.clear();
    }

    @Override
    public void handleSuccess(int status, String namespace, String id, HttpResponse response) {
      Logger.info(LOG_TAG, "Got success: " + status + " for id: " + id);
      statuses.add(status);
      WaitHelper.getTestWaiter().performNotify();
    }

    @Override
    public void handleFailure(int status, String namespace, HttpResponse response) {
      Logger.info(LOG_TAG, "Got failure: " + status);
      statuses.add(status);
      WaitHelper.getTestWaiter().performNotify();
    }

    @Override
    public void handleError(Exception e) {
      Logger.info(LOG_TAG, "Got error.", e);
      WaitHelper.getTestWaiter().performNotify(e);
    }
  }


  public static class TestProducer implements Producer {
    public final CopyOnWriteArrayList<BagheeraMessage> messages = new CopyOnWriteArrayList<BagheeraMessage>();

    @Override
    public void close() throws IOException {
    }

    @Override
    public void send(BagheeraMessage message) {
      messages.add(message);
    }
  }
}
