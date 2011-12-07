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
 * Jason Voll <jvoll@mozilla.com>
 * Richard Newman <rnewman@mozilla.com>
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

package org.mozilla.android.sync.repositories.domain;

import org.json.simple.JSONArray;
import org.mozilla.android.sync.CryptoRecord;
import org.mozilla.android.sync.ExtendedJSONObject;
import org.mozilla.android.sync.Utils;

/**
 * Covers the fields used by all bookmark objects.
 * @author rnewman
 *
 */
public class BookmarkRecord extends Record {
  
  public static final String COLLECTION_NAME = "bookmarks";
  
  public BookmarkRecord(String guid, String collection, long lastModified, boolean deleted) {
    super(guid, collection, lastModified, deleted);
  }
  public BookmarkRecord(String guid, String collection, long lastModified) {
    super(guid, collection, lastModified, false);
  }
  public BookmarkRecord(String guid, String collection) {
    super(guid, collection, 0, false);
  }
  public BookmarkRecord(String guid) {
    super(guid, COLLECTION_NAME, 0, false);
  }
  public BookmarkRecord() {
    super(Utils.generateGuid(), COLLECTION_NAME, 0, false);
  }

  // Note: redundant accessors are evil. We're all grownups; let's just use
  // public fields.
  public long    androidID;
  public boolean loadInSidebar;
  public String  title;
  public String  bookmarkURI;
  public String  description;
  public String  keyword;
  public String  parentID;
  public String  parentName;
  public String  type;
  public String  generatorURI;
  public String  staticTitle;
  public String  folderName;
  public String  queryID;
  public String  siteURI;
  public String  feedURI;
  public String  pos;

  public JSONArray children;
  public JSONArray tags;

  private static boolean getBooleanProperty(ExtendedJSONObject object, String property, boolean defaultValue) {
    Object val = object.get(property);
    if (val instanceof Boolean) {
      return ((Boolean) val).booleanValue();
    }
    return defaultValue;
  }

  @Override
  public void initFromPayload(CryptoRecord payload) {
    ExtendedJSONObject p = payload.payload;

    // All.
    this.type          = (String) p.get("type");
    this.title         = (String) p.get("title");
    this.description   = (String) p.get("description");
    this.parentID      = (String) p.get("parentid");
    this.parentName    = (String) p.get("parentName");
    this.loadInSidebar = getBooleanProperty(p, "loadInSidebar", false);

    // Bookmark.
    if (this.type == "bookmark") {
      this.bookmarkURI   = (String) p.get("bmkUri");
      this.keyword       = (String) p.get("keyword");
      this.tags          = (JSONArray) p.get("tags");
    }
    // Folder.
    if (this.type == "folder") {
      this.children      = (JSONArray) p.get("children");
    }

    // TODO: predecessor ID?
    // TODO: type-specific attributes:
    /*
      public String generatorURI;
      public String staticTitle;
      public String folderName;
      public String queryID;
      public String siteURI;
      public String feedURI;
      public String pos;
     */
  }

  @Override
  public CryptoRecord getPayload() {
    CryptoRecord rec = new CryptoRecord(this);
    rec.payload = new ExtendedJSONObject();
    rec.payload.put("type", this.type);
    rec.payload.put("title", this.title);
    rec.payload.put("description", this.description);
    rec.payload.put("parentid", this.parentID);
    rec.payload.put("parentName", this.parentName);
    rec.payload.put("loadInSidebar", new Boolean(this.loadInSidebar));
    if (this.type == "bookmark") {
      rec.payload.put("bmkUri", bookmarkURI);
      rec.payload.put("keyword", keyword);
      rec.payload.put("tags", this.tags);
    }
    if (this.type == "folder") {
      rec.payload.put("children", this.children);
    }
    return rec;
  }

}


/*
// Bookmark:
{cleartext:
  {id:            "l7p2xqOTMMXw",
   type:          "bookmark",
   title:         "Your Flight Status",
   parentName:    "mobile",
   bmkUri:        "http: //www.flightstats.com/go/Mobile/flightStatusByFlightProcess.do;jsessionid=13A6C8DCC9592AF141A43349040262CE.web3: 8009?utm_medium=cpc&utm_campaign=co-op&utm_source=airlineInformationAndStatus&id=212492593",
   tags:          [],
   keyword:       null,
   description:   null,
   loadInSidebar: false,
   parentid:      "mobile"},
 data: {payload: {ciphertext: null},
 id:         "l7p2xqOTMMXw",
 sortindex:  107},
 collection: "bookmarks"}

// Folder:
{cleartext:
  {id:          "mobile",
   type:        "folder",
   parentName:  "",
   title:       "mobile",
   description: null,
   children:    ["1ROdlTuIoddD", "3Z_bMIHPSZQ8", "4mSDUuOo2iVB", "8aEdE9IIrJVr",
                 "9DzPTmkkZRDb", "Qwwb99HtVKsD", "s8tM36aGPKbq", "JMTi61hOO3JV",
                 "JQUDk0wSvYip", "LmVH-J1r3HLz", "NhgQlC5ykYGW", "OVanevUUaqO2",
                 "OtQVX0PMiWQj", "_GP5cF595iie", "fkRssjXSZDL3", "k7K_NwIA1Ya0",
                 "raox_QGzvqh1", "vXYL-xHjK06k", "QKHKUN6Dm-xv", "pmN2dYWT2MJ_",
                 "EVeO_J1SQiwL", "7N-qkepS7bec", "NIGa3ha-HVOE", "2Phv1I25wbuH",
                 "TTSIAH1fV0VE", "WOmZ8PfH39Da", "gDTXNg4m1AJZ", "ayI30OZslHbO",
                 "zSEs4O3n6CzQ", "oWTDR0gO2aWf", "wWHUoFaInXi9", "F7QTuVJDpsTM",
                 "FIboggegplk-", "G4HWrT5nfRYS", "MHA7y9bupDdv", "T_Ldzmj0Ttte",
                 "U9eYu3SxsE_U", "bk463Kl9IO_m", "brUfrqJjFNSR", "ccpawfWsD-bY",
                 "l7p2xqOTMMXw", "o-nSDKtXYln7"],
   parentid: "places"},
 data:        {payload: {ciphertext: null},
 id:          "mobile",
 sortindex:   1000000},
 collection: "bookmarks"}
*/
