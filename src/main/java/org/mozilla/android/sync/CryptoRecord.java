package org.mozilla.android.sync;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.json.simple.parser.ParseException;
import org.mozilla.android.sync.domain.KeyBundle;

public interface CryptoRecord {
  void setKeyBundle(KeyBundle bundle);
  void decrypt() throws CryptoException, IOException, ParseException, NonObjectJSONException;
  void encrypt() throws CryptoException, UnsupportedEncodingException;
}
