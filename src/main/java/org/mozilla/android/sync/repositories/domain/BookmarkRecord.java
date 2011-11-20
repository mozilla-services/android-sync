package org.mozilla.android.sync.repositories.domain;

public class BookmarkRecord extends Record {
  // Covers the fields used by all bookmark objects.
  // TODO Consider using Google's gson for parsing JSON into
  // these domain objects.

  // Note: redundant accessors are evil. We're all grownups; let's just use
  // public fields.

  // TODO I don't think there is a benefit to storing this, nor do we ever use
  // it. Leave it for now.
  public long    id;
  public long    androidID;
  public boolean loadInSidebar;
  public String  title;
  public String  bookmarkURI;
  public String  description;
  public String  tags;
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
  public String  children;
}
