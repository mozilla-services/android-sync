/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.background.sync;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;

import junit.framework.Assert;

import org.mozilla.gecko.background.helpers.AndroidSyncTestCase;
import org.mozilla.gecko.fxa.FxAccountConstants;
import org.mozilla.gecko.sync.net.AuthHeaderProvider;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.sync.net.ResourceDelegate;

import ch.boye.httpclientandroidlib.HttpException;
import ch.boye.httpclientandroidlib.HttpHost;
import ch.boye.httpclientandroidlib.HttpRequest;
import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.client.ClientProtocolException;
import ch.boye.httpclientandroidlib.client.methods.HttpRequestBase;
import ch.boye.httpclientandroidlib.conn.routing.HttpRoute;
import ch.boye.httpclientandroidlib.conn.routing.HttpRoutePlanner;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;
import ch.boye.httpclientandroidlib.protocol.HttpContext;

public class TestLiveServerConnection extends AndroidSyncTestCase {
    public void testLiveConnection() throws Exception {
        final String url = FxAccountConstants.DEFAULT_AUTH_SERVER_ENDPOINT;
        //final String url = "https://account.services.mozilla.com/";
        //final String url = "https://google.com/";
        //final String A = "https://52.24.152.207/";
        //final String B = "https://54.200.178.199/";
        //final String C = "https://54.69.189.114/";
        final byte[] A = {52, 24, (byte) 152, (byte) 207};
        final byte[] B = {54, (byte) 200, (byte) 178, (byte) 199};
        final byte[] C = {54, 69, (byte) 189, (byte) 114};

        BaseResource resource = new BaseResource(url);
        resource.delegate = new ResourceDelegate() {
            
            @Override
            public int socketTimeout() {
                return 0;
            }
            
            @Override
            public void handleTransportException(GeneralSecurityException e) {
                Assert.fail("Noooo");
            }
            
            @Override
            public void handleHttpResponse(HttpResponse response) {
            }
            
            @Override
            public void handleHttpProtocolException(ClientProtocolException e) {
                Assert.fail("Noooo");
            }
            
            @Override
            public void handleHttpIOException(IOException e) {
                throw new RuntimeException(e);
            }
            
            @Override
            public String getUserAgent() {
                return FxAccountConstants.USER_AGENT;
            }
            
            @Override
            public AuthHeaderProvider getAuthHeaderProvider() {
                return null;
            }
            
            @Override
            public int connectionTimeout() {
                return 0;
            }
            
            @Override
            public void addHeaders(HttpRequestBase request, DefaultHttpClient client) {
            }
        };
        resource.getBlocking();
    }
}
