package org.mozilla.android.sync.test.integration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.security.GeneralSecurityException;

import org.json.simple.JSONArray;
import org.mozilla.android.sync.test.helpers.WaitHelper;
import org.mozilla.gecko.sync.CryptoRecord;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.crypto.CryptoException;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.sync.net.BaseResourceDelegate;
import org.mozilla.gecko.sync.net.Resource;
import org.mozilla.gecko.sync.repositories.Server20Repository;
import org.mozilla.gecko.sync.repositories.domain.Record;

import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.client.ClientProtocolException;

public class Server20Client {
  public static final String LOG_TAG = "Server20Client";

  protected final Server20Repository repository;
  protected final KeyBundle keyBundle;

  public Server20Client(final Server20Repository repository, final KeyBundle keyBundle) throws CryptoException {
    this.repository = repository;
    this.keyBundle = keyBundle;
  }

  protected class MockServer20ResourceDelegate extends BaseResourceDelegate {
    public MockServer20ResourceDelegate(Resource resource) {
      super(resource);
    }

    public HttpResponse response = null;
    public int statusCode;
    public String responseBody = null;
    public ExtendedJSONObject responseObject = null;

    @Override
    public String getCredentials() {
      return repository.credentials();
    }

    @Override
    public void handleHttpResponse(HttpResponse response) {
      this.response = response;
      statusCode = response.getStatusLine().getStatusCode();
      try {
        InputStream content = response.getEntity().getContent();
        BufferedReader reader = new BufferedReader(new InputStreamReader(content, "UTF-8"), 1024);
        responseBody = reader.readLine();
        BaseResource.consumeReader(reader);
        reader.close();
      } catch (Throwable e) {
        // Do nothing.
      }

      if (responseBody != null) {
        try {
          responseObject = ExtendedJSONObject.parseJSONObject(responseBody);
        } catch (Throwable e) {
          // Do nothing.
        }
      }

      WaitHelper.getTestWaiter().performNotify();
    }

    @Override
    public void handleHttpProtocolException(ClientProtocolException e) {
      WaitHelper.getTestWaiter().performNotify(e);
    }

    @Override
    public void handleHttpIOException(IOException e) {
      WaitHelper.getTestWaiter().performNotify(e);
    }

    @Override
    public void handleTransportException(GeneralSecurityException e) {
      WaitHelper.getTestWaiter().performNotify(e);
    }
  }

  public MockServer20ResourceDelegate delete(final URI uri) throws Exception {
    final BaseResource res = new BaseResource(uri);

    res.delegate = new MockServer20ResourceDelegate(res);

    WaitHelper.getTestWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        res.delete();
      }
    });

    return (MockServer20ResourceDelegate) res.delegate;
  }

  public MockServer20ResourceDelegate delete() throws Exception {
    return delete(repository.collectionURI());
  }

  public MockServer20ResourceDelegate delete(final String guid) throws Exception {
    return delete(repository.wboURI(guid));
  }

  protected MockServer20ResourceDelegate get(final URI uri) throws Exception {
    final BaseResource res = new BaseResource(uri);

    res.delegate = new MockServer20ResourceDelegate(res);

    WaitHelper.getTestWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        res.get();
      }
    });

    return (MockServer20ResourceDelegate) res.delegate;
  }

  public MockServer20ResourceDelegate get() throws Exception {
    return get(repository.collectionURI());
  }

  public MockServer20ResourceDelegate get(final String guid) throws Exception {
    return get(repository.wboURI(guid));
  }

  public MockServer20ResourceDelegate put(final Record record) throws Exception {
    final CryptoRecord c = record.getEnvelope();
    c.keyBundle = keyBundle;
    c.encrypt();

    final BaseResource res = new BaseResource(repository.wboURI(record.guid));

    res.delegate = new MockServer20ResourceDelegate(res);

    WaitHelper.getTestWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        res.put(c.toJSONObject());
      }
    });

    return (MockServer20ResourceDelegate) res.delegate;
  }

  @SuppressWarnings("unchecked")
  public MockServer20ResourceDelegate post(final Record[] records) throws Exception {
    final JSONArray array = new JSONArray();
    for (Record record : records) {
      final CryptoRecord c = record.getEnvelope();
      c.keyBundle = keyBundle;
      c.encrypt();
      array.add(c.toJSONObject());
    }

    final BaseResource res = new BaseResource(repository.collectionURI());

    res.delegate = new MockServer20ResourceDelegate(res);

    WaitHelper.getTestWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        res.post(array);
      }
    });

    return (MockServer20ResourceDelegate) res.delegate;
  }
}
