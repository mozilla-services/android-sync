package org.mozilla.gecko.sync.net;

import java.io.IOException;
import java.net.Socket;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;

import ch.boye.httpclientandroidlib.conn.ssl.SSLSocketFactory;
import ch.boye.httpclientandroidlib.params.HttpParams;

public class TLSSocketFactory extends SSLSocketFactory {
  public TLSSocketFactory(SSLContext sslContext) {
    super(sslContext);
  }

  @Override
  public Socket createSocket(HttpParams params) throws IOException {
    SSLSocket socket = (SSLSocket) super.createSocket(params);
    socket.setEnabledProtocols(new String[] { "SSLv3", "TLSv1" });
    socket.setEnabledCipherSuites(new String[] { "SSL_RSA_WITH_RC4_128_SHA" });
    return socket;
  }

  @Override
  public boolean isSecure(Socket sock) throws IllegalArgumentException {
    return true;
  }
}