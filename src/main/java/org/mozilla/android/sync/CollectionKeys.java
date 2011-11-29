/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Android Sync Client.
 *
 * The Initial Developer of the Original Code is
 * the Mozilla Foundation.
 * Portions created by the Initial Developer are Copyright (C) 2011
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *  Richard Newman <rnewman@mozilla.com>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

package org.mozilla.android.sync;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map.Entry;

import org.json.simple.JSONArray;
import org.json.simple.parser.ParseException;
import org.mozilla.android.sync.crypto.CryptoException;
import org.mozilla.android.sync.crypto.KeyBundle;

public class CollectionKeys {
  private KeyBundle                  defaultKeyBundle     = null;
  private HashMap<String, KeyBundle> collectionKeyBundles = new HashMap<String, KeyBundle>();

  public KeyBundle defaultKeyBundle() throws NoCollectionKeysSetException {
    if (this.defaultKeyBundle == null) {
      throw new NoCollectionKeysSetException();
    }
    return this.defaultKeyBundle;
  }

  public KeyBundle keyBundleForCollection(String collection)
                                                      throws NoCollectionKeysSetException {
    if (this.defaultKeyBundle == null) {
      throw new NoCollectionKeysSetException();
    }
    if (collectionKeyBundles.containsKey(collection)) {
      return collectionKeyBundles.get(collection);
    }
    return this.defaultKeyBundle;
  }

  /**
   * Take a pair of values in a JSON array, handing them off to KeyBundle to
   * produce a usable keypair.
   *
   * @param array
   * @return
   * @throws JSONException
   * @throws UnsupportedEncodingException
   */
  private static KeyBundle arrayToKeyBundle(JSONArray array) throws UnsupportedEncodingException {
    String encKeyStr = (String) array.get(0);
    String hmacKeyStr = (String) array.get(1);
    return KeyBundle.decodeKeyStrings(encKeyStr, hmacKeyStr);
  }

  /**
   * Take a downloaded record, and the Sync Key, decrypting the record and
   * setting our own keys accordingly.
   *
   * @param keys
   * @param syncKeyBundle
   * @throws CryptoException
   * @throws IOException
   * @throws ParseException
   * @throws NonObjectJSONException
   * @throws JSONException
   */
  public void setKeyPairsFromWBO(CryptoRecord keys, KeyBundle syncKeyBundle)
                                                                            throws CryptoException,
                                                                            IOException,
                                                                            ParseException,
                                                                            NonObjectJSONException {
    keys.keyBundle = syncKeyBundle;
    keys.decrypt();
    ExtendedJSONObject cleartext = keys.payload;
    KeyBundle defaultKey = arrayToKeyBundle((JSONArray) cleartext.get("default"));

    ExtendedJSONObject collections = cleartext.getObject("collections");
    HashMap<String, KeyBundle> collectionKeys = new HashMap<String, KeyBundle>();
    for (Entry<String, Object> pair : collections.entryIterable()) {
      KeyBundle bundle = arrayToKeyBundle((JSONArray) pair.getValue());
      collectionKeys.put(pair.getKey(), bundle);
    }

    this.collectionKeyBundles = collectionKeys;
    this.defaultKeyBundle     = defaultKey;
  }

  public void setKeyBundleForCollection(String collection, KeyBundle keys) {
    this.collectionKeyBundles.put(collection, keys);
  }

  public void setDefaultKeyBundle(KeyBundle keys) {
    this.defaultKeyBundle = keys;
  }
}
