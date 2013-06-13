/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.background.datareporting;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.json.JSONObject;

import android.util.Base64;
import android.util.Log;

/**
 * Writes telemetry ping to file.
 *
 * Also creates and updates a checksum for the payload to include in the ping
 * file.
 *
 * A saved telemetry ping file consists of JSON in the following format,
 *   {
 *     "slug": "<uuid-string>",
 *     "payload": "<escaped-json-data-string>",
 *     "checksum": "<base64-sha-256-string>"
 *   }
 *
 * The API provided by this class:
 * startPingFile() - opens stream to the File and writes the (filename) slug
 * appendPayload(String payloadContent) - append to the payload of the telemetry ping
 *                                        and update the checksum
 * finishPingFile() - writes the checksum and closes the stream
 */
public class TelemetryRecorder {
  protected String      LOGTAG;

  protected File        destFile;

  private OutputStream  outputStream;
  private MessageDigest checksum;
  private String        base64Checksum;

  // charset to use for writing pings; default is us-ascii.
  private String charset = "us-ascii";
  private int blockSize = 0;

  /**
   * Constructs a TelemetryRecorder for writing a ping file and opens the file
   * for writing.
   *
   * @param File
   *        destination file for writing telemetry ping
   */
  public TelemetryRecorder(File destFile) {
    this.destFile = destFile;
  }

  public TelemetryRecorder(File destFile, String charset) {
    this(destFile);
    this.charset = charset;
  }

  public TelemetryRecorder(File destFile, String charset, int blockSize) {
    this(destFile, charset);
    this.blockSize = blockSize;
  }

  /**
   * Start the ping file and write the "slug" header and payload key, of the
   * format: { "slug": "<uuid-string>", "payload":
   *
   * @throws Exception
   *           rethrown exception to caller
   */
  public void startPingFile() throws Exception {

    // Open stream for writing.
    try {
      if (blockSize > 0) {
        outputStream = new BufferedOutputStream(new FileOutputStream(destFile),
            blockSize);
      } else {
        outputStream = new BufferedOutputStream(new FileOutputStream(destFile));
      }
    } catch (FileNotFoundException e) {
      Log.e(LOGTAG, "File could not be found.", e);
    }

    try {
      // Create checksum for ping.
      checksum = MessageDigest.getInstance("SHA-256");

      // Write ping header.
      byte[] header = makePingHeader(destFile.getName());
      outputStream.write(header);
      Log.d(LOGTAG, "Wrote " + header.length + " header bytes.");

    } catch (NoSuchAlgorithmException e) {
      cleanUpAndRethrow("Error creating checksum digest", e);
    } catch (UnsupportedEncodingException e) {
      cleanUpAndRethrow("Error writing header", e);
    } catch (IOException e) {
      cleanUpAndRethrow("Error writing to stream", e);
    }
  }

  private byte[] makePingHeader(String slug)
      throws UnsupportedEncodingException {
    return ("{\"slug\":" + JSONObject.quote(slug) + "," + "\"payload\":\"")
        .getBytes(charset);
  }

  /**
   * Append payloadContent to ping file and update the checksum.
   *
   * @param payloadContent
   *          String content to be written
   * @return number of bytes written, or -1 if writing failed
   * @throws Exception
   *           rethrown Exception to caller
   */
  public int appendPayload(String payloadContent) throws Exception {
    try {
      byte[] payloadBytes = payloadContent.getBytes(charset);
      checksum.update(payloadBytes);

      byte[] quotedPayloadBytes = JSONObject.quote(payloadContent).getBytes(charset);
      // First and last bytes are quotes inserted by JSONObject.quote; discard
      // them.
      int numBytes = quotedPayloadBytes.length - 2;
      outputStream.write(quotedPayloadBytes, 1, numBytes);

      return numBytes;

    } catch (UnsupportedEncodingException e) {
      cleanUpAndRethrow("Error encoding payload", e);
      return -1;
    } catch (IOException e) {
      cleanUpAndRethrow("Error writing to stream", e);
      return -1;
    }
  }

  /**
   * Log message and error and clean up, then rethrow exception to caller.
   *
   * @param message
   *          Error message
   * @param e
   *          Exception
   *
   * @throws Exception
   *           rethrown exception to caller
   */
  private void cleanUpAndRethrow(String message, Exception e) throws Exception {
    Log.e(LOGTAG, message, e);
    if (destFile.exists()) {
      destFile.delete();
    }
    if (outputStream != null) {
      try {
        outputStream.close();
      } catch (IOException exception) {
        // Failed to close stream; nothing we can do.
      }
    }
    // Rethrow the exception.
    throw e;
  }

  /**
   * Add the checksum of the payload to the ping file and close the stream.
   *
   * @throws Exception
   *           rethrown exception to caller
   */
  public void finishPingFile() throws Exception {
    try {
      byte[] footer = makePingFooter(checksum);
      outputStream.write(footer);
      Log.d(LOGTAG, "Wrote " + footer.length + " footer bytes.");
      outputStream.close();
    } catch (UnsupportedEncodingException e) {
      cleanUpAndRethrow("Checksum encoding exception", e);
    } catch (IOException e) {
      cleanUpAndRethrow("Error writing footer to stream", e);
    }
  }

  private byte[] makePingFooter(MessageDigest checksum)
      throws UnsupportedEncodingException {
    base64Checksum = Base64.encodeToString(checksum.digest(), Base64.NO_WRAP);
    return ("\",\"checksum\":" + JSONObject.quote(base64Checksum) + "}")
        .getBytes(charset);
  }

  protected int getBlockSize() {
    return 0;
  }

  /**
   * Get final digested Base64 checksum.
   *
   * @return String checksum if it has been calculated, null otherwise.
   */
  protected String getFinalChecksum() {
    return base64Checksum;
  }
}
