package org.mozilla.android.sync.repositories.domain;


public class BookmarkRecord implements Record {
  // Covers the fields used by all bookmark related objects
  // TODO Consider using Google's gson for parsing json into
  // these domain objects

  private long id;
  private String guid;
  private String androidId;
  private String title;
  private String bmkUri;
  private String description;
  private boolean loadInSidebar;
  private String tags;
  private String keyword;
  private String parentId;
  private String parentName;
  private String type;
  private String generatorUri;
  private String staticTitle;
  private String folderName;
  private String queryId;
  private String siteUri;
  private String feedUri;
  private String pos;
  private String children;

  public String getGuid() {
    return guid;
  }
  public void setGuid(String guid) {
    this.guid = guid;
  }
  public String getAndroidId() {
    return androidId;
  }
  public void setAndroidId(String androidId) {
    this.androidId = androidId;
  }
  public String getTitle() {
    return title;
  }
  public void setTitle(String title) {
    this.title = title;
  }
  public String getBmkUri() {
    return bmkUri;
  }
  public void setBmkUri(String bmkUri) {
    this.bmkUri = bmkUri;
  }
  public String getDescription() {
    return description;
  }
  public void setDescription(String description) {
    this.description = description;
  }
  public boolean getLoadInSidebar() {
    return loadInSidebar;
  }
  public void setLoadInSidebar(boolean loadInSidebar) {
    this.loadInSidebar = loadInSidebar;
  }
  public String getTags() {
    return tags;
  }
  public void setTags(String tags) {
    this.tags = tags;
  }
  public String getKeyword() {
    return keyword;
  }
  public void setKeyword(String keyword) {
    this.keyword = keyword;
  }
  public String getParentName() {
    return parentName;
  }
  public void setParentName(String parentName) {
    this.parentName = parentName;
  }
  public String getType() {
    return type;
  }
  public void setType(String type) {
    this.type = type;
  }
  public String getGeneratorUri() {
    return generatorUri;
  }
  public void setGeneratorUri(String generatorUri) {
    this.generatorUri = generatorUri;
  }
  public String getStaticTitle() {
    return staticTitle;
  }
  public void setStaticTitle(String staticTitle) {
    this.staticTitle = staticTitle;
  }
  public String getFolderName() {
    return folderName;
  }
  public void setFolderName(String folderName) {
    this.folderName = folderName;
  }
  public String getQueryId() {
    return queryId;
  }
  public void setQueryId(String queryId) {
    this.queryId = queryId;
  }
  public String getSiteUri() {
    return siteUri;
  }
  public void setSiteUri(String siteUri) {
    this.siteUri = siteUri;
  }
  public String getFeedUri() {
    return feedUri;
  }
  public void setFeedUri(String feedUri) {
    this.feedUri = feedUri;
  }
  public String getPos() {
    return pos;
  }
  public void setPos(String pos) {
    this.pos = pos;
  }
  public String getChildren() {
    return children;
  }
  public void setChildren(String children) {
    this.children = children;
  }
  public long getId() {
    return id;
  }
  public void setId(long id) {
    this.id = id;
  }
  public String getParentId() {
    return parentId;
  }
  public void setParentId(String parentId) {
    this.parentId = parentId;
  }

}
