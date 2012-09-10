/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.net;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import ch.boye.httpclientandroidlib.entity.ContentProducer;
import ch.boye.httpclientandroidlib.entity.EntityTemplate;

/**
 * An HTTP entity that writes a list of byte arrays as a JSON array. Intended to
 * be the body of a multi-record POST to a Sync server.
 */
public class ByteArraysEntity extends EntityTemplate {
  private static final int PER_RECORD_OVERHEAD   = 2;              // Comma, newline.
  // {}, newlines, but we get to skip one record overhead.
  private static final int PER_BATCH_OVERHEAD    = 5 - PER_RECORD_OVERHEAD;

  private static byte[] recordsStart;
  private static byte[] recordSeparator;
  private static byte[] recordsEnd;

  static {
    try {
      recordsStart    = "[\n".getBytes("UTF-8");
      recordSeparator = ",\n".getBytes("UTF-8");
      recordsEnd      = "\n]\n".getBytes("UTF-8");
    } catch (UnsupportedEncodingException e) {
      // These won't fail.
    }
  }

  private long count;
  public ByteArraysEntity(ArrayList<byte[]> arrays, long totalBytes) {
    super(new ByteArraysContentProducer(arrays));
    this.count = totalBytes +
        (PER_RECORD_OVERHEAD * arrays.size()) +
        PER_BATCH_OVERHEAD;
    this.setContentType("application/json");
    // charset is set in BaseResource.
  }

  @Override
  public long getContentLength() {
    return count;
  }

  @Override
  public boolean isRepeatable() {
    return true;
  }

  public static class ByteArraysContentProducer implements ContentProducer {
    ArrayList<byte[]> outgoing;
    public ByteArraysContentProducer(ArrayList<byte[]> arrays) {
      outgoing = arrays;
    }

    @Override
    public void writeTo(OutputStream outstream) throws IOException {
      int count = outgoing.size();
      outstream.write(recordsStart);
      outstream.write(outgoing.get(0));
      for (int i = 1; i < count; ++i) {
        outstream.write(recordSeparator);
        outstream.write(outgoing.get(i));
      }
      outstream.write(recordsEnd);
    }
  }
}
