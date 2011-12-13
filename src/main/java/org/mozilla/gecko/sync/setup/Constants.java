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
 *  Chenxia Liu <liuche@mozilla.com>
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

package org.mozilla.gecko.sync.setup;

public class Constants {
  // Constants for Firefox Sync SyncAdapter Accounts
  public static final String ACCOUNTTYPE_SYNC = "account.type.sync";
  public static final String OPTION_SYNCKEY = "option.synckey";
  public static final String OPTION_USERNAME = "option.username";
  public static final String AUTHTOKEN_TYPE_PLAIN = "auth.plain";

  // Constants for JSON payload

  // private
  public static final String B1 = "b1";
  public static final String B2 = "b2";

// protocol
  public static final String KEY_PAYLOAD = "payload";
  public static final String KEY_CIPHERTEXT = "ciphertext";
  public static final String KEY_HMAC = "hmac";
  public static final String KEY_IV = "IV";
  public static final String KEY_TYPE = "type";
  public static final String KEY_VERSION = "version";

  public static final String ETAG = "etag";

  public static final String X1 = "x1";
  public static final String X2 = "x2";

  public static final String GX1 = "gx1";
  public static final String GX2 = "gx2";

  public static final String ZKP_X1 = "zkp_x1";
  public static final String ZKP_X2 = "zkp_x2";
  public static final String B = "b";
  public static final String GR = "gr";
  public static final String ID = "id";

  public static final String A = "A";
  public static final String ZKP_A = "zkp_A";

  // JPAKE Errors
  public static final String JPAKE_ERROR_CHANNEL = "jpake.error.channel";
  public static final String JPAKE_ERROR_NETWORK = "jpake.error.network";
  public static final String JPAKE_ERROR_SERVER = "jpake.error.server";
  public static final String JPAKE_ERROR_TIMEOUT = "jpake.error.timeout";
  public static final String JPAKE_ERROR_INTERNAL = "jpake.error.internal";
  public static final String JPAKE_ERROR_INVALID = "jpake.error.invalid";
  public static final String JPAKE_ERROR_NODATA = "jpake.error.nodata";
  public static final String JPAKE_ERROR_KEYMISMATCH = "jpake.error.keymismatch";
  public static final String JPAKE_ERROR_WRONGMESSAGE = "jpake.error.wrongmessage";
  public static final String JPAKE_ERROR_USERABORT = "jpake.error.userabort";
  public static final String JPAKE_ERROR_DELAYUNSUPPORTED = "jpake.error.delayunsupported";
}
