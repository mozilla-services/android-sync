/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.repositories.android;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONArray;
import org.mozilla.gecko.R;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.repositories.NoGuidForIdException;
import org.mozilla.gecko.sync.repositories.NullCursorException;
import org.mozilla.gecko.sync.repositories.ParentNotFoundException;
import org.mozilla.gecko.sync.repositories.Repository;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionBeginDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionFinishDelegate;
import org.mozilla.gecko.sync.repositories.domain.BookmarkRecord;
import org.mozilla.gecko.sync.repositories.domain.Record;

import android.content.Context;
import android.database.Cursor;

public class AndroidBrowserBookmarksRepositorySession extends AndroidBrowserRepositorySession {

  // TODO: synchronization for these.
  private HashMap<String, Long> guidToID = new HashMap<String, Long>();
  private HashMap<Long, String> idToGuid = new HashMap<Long, String>();

  /**
   * Some notes on reparenting/reordering.
   *
   * Fennec stores new items with a high-negative position, because it doesn't care.
   * On the other hand, it also doesn't give us any help managing positions.
   *
   * We can process records and folders in any order, though we'll usually see folders
   * first because their sortindex is larger.
   *
   * We can also see folders that refer to children we haven't seen, and children we
   * won't see (perhaps due to a TTL, perhaps due to a limit on our fetch).
   *
   * And of course folders can refer to local children (including ones that might
   * be reconciled into oblivion!), or local children in other folders. And the local
   * version of a folder -- which might be a reconciling target, or might not -- can
   * have local additions or removals.
   *
   * We opt to leave records in a reasonable state as we go, applying reordering/
   * reparenting operations whenever possible. Typically this is after all incoming
   * records have been applied. As such, we need to track a bunch of stuff as we go:
   *
   * • For each downloaded folder, the array of children. These will be server GUIDs,
   *   but not necessarily identical to the remote list: if we download a record and
   *   it's been locally moved, it must be removed from this child array.
   *
   *   This mapping can be discarded when reordering has occurred.
   *
   * • A list of orphans: records whose parent folder does not yet exist. This can be
   *   trimmed as orphans are reparented.
   *
   * • Mappings from folder GUIDs to folder IDs, so that we can parent items without
   *   having to look in the DB. Of course, this must be kept up-to-date as we
   *   reconcile.
   *
   * Do we also need a list of "adopters", parents that are still waiting for children?
   * As items get picked out of the orphans list, we can do on-the-fly ordering, until
   * we're left with lonely records at the end.
   *
   * As we modify local folders, perhaps by moving children out of their purview, we
   * must bump their modification time so as to cause them to be uploaded on the next
   * stage of syncing. The same applies to simple reordering.
   */

  // TODO: can we guarantee serial access to these?
  private HashMap<String, ArrayList<String>> missingParentToChildren = new HashMap<String, ArrayList<String>>();
  private HashMap<String, JSONArray>         parentToChildArray      = new HashMap<String, JSONArray>();
  private int needsReparenting = 0;

  private AndroidBrowserBookmarksDataAccessor dataAccessor;

  /**
   * An array of known-special GUIDs.
   */
  public static String[] SPECIAL_GUIDS = new String[] {
    // Mobile and desktop places roots have to come first.
    "mobile",
    "places",
    "toolbar",
    "menu",
    "unfiled"
  };

  /**
   * = A note about folder mapping =
   *
   * Note that _none_ of Places's folders actually have a special GUID. They're all
   * randomly generated. Special folders are indicated by membership in the
   * moz_bookmarks_roots table, and by having the parent `1`.
   *
   * Additionally, the mobile root is annotated. In Firefox Sync, PlacesUtils is
   * used to find the IDs of these special folders.
   *
   * Sync skips over `places` and `tags` when finding IDs.
   *
   * We need to consume records with these various guids, producing a local
   * representation which we are able to stably map upstream.
   *
   * That is:
   *
   * * We should not upload a `places` record or a `tags` record.
   * * We can stably _store_ menu/toolbar/unfiled/mobile as special GUIDs, and set
     * their parent ID as appropriate on upload.
   *
   *
   * = Places folders =
   *
   * guid        root_name   folder_id   parent
   * ----------  ----------  ----------  ----------
   * ?           places      1           0
   * ?           menu        2           1
   * ?           toolbar     3           1
   * ?           tags        4           1
   * ?           unfiled     5           1
   *
   * ?           mobile*     474         1
   *
   *
   * = Fennec folders =
   *
   * guid        folder_id   parent
   * ----------  ----------  ----------
   * mobile      ?           0
   *
  */
  public static final Map<String, String> SPECIAL_GUID_PARENTS;
  static {
    HashMap<String, String> m = new HashMap<String, String>();
    m.put("places",  null);
    m.put("menu",    "places");
    m.put("toolbar", "places");
    m.put("tags",    "places");
    m.put("unfiled", "places");
    m.put("mobile",  "places");
    SPECIAL_GUID_PARENTS = Collections.unmodifiableMap(m);
  }

  /**
   * A map of guids to their localized name strings.
   */
  // Oh, if only we could make this final and initialize it in the static initializer.
  public static Map<String, String> SPECIAL_GUIDS_MAP;

  /**
   * Return true if the provided record GUID should be skipped
   * in child lists or fetch results.
   *
   * @param recordGUID
   * @return
   */
  public static boolean forbiddenGUID(String recordGUID) {
    return recordGUID == null ||
           "places".equals(recordGUID) ||
           "tags".equals(recordGUID);
  }

  public AndroidBrowserBookmarksRepositorySession(Repository repository, Context context) {
    super(repository);

    if (SPECIAL_GUIDS_MAP == null) {
      HashMap<String, String> m = new HashMap<String, String>();
      m.put("menu",    context.getString(R.string.bookmarks_folder_menu));
      m.put("places",  context.getString(R.string.bookmarks_folder_places));
      m.put("toolbar", context.getString(R.string.bookmarks_folder_toolbar));
      m.put("unfiled", context.getString(R.string.bookmarks_folder_unfiled));
      m.put("mobile",  context.getString(R.string.bookmarks_folder_mobile));
      SPECIAL_GUIDS_MAP = Collections.unmodifiableMap(m);
    }

    dbHelper = new AndroidBrowserBookmarksDataAccessor(context);
    dataAccessor = (AndroidBrowserBookmarksDataAccessor) dbHelper;
  }

  private boolean rowIsFolder(Cursor cur) {
    return RepoUtils.getLongFromCursor(cur, BrowserContract.Bookmarks.IS_FOLDER) == 1;
  }

  private String getGUIDForID(long androidID) {
    String guid = idToGuid.get(androidID);
    trace("  " + androidID + " => " + guid);
    return guid;
  }

  private long getIDForGUID(String guid) {
    Long id = guidToID.get(guid);
    if (id == null) {
      Logger.warn(LOG_TAG, "Couldn't find local ID for GUID " + guid);
      return -1;
    }
    return id.longValue();
  }

  private String getGUID(Cursor cur) {
    return RepoUtils.getStringFromCursor(cur, "guid");
  }

  private long getParentID(Cursor cur) {
    return RepoUtils.getLongFromCursor(cur, BrowserContract.Bookmarks.PARENT);
  }

  // More efficient for bulk operations.
  private long getPosition(Cursor cur, int positionIndex) {
    return cur.getLong(positionIndex);
  }
  private long getPosition(Cursor cur) {
    return RepoUtils.getLongFromCursor(cur, BrowserContract.Bookmarks.POSITION);
  }

  private String getParentName(String parentGUID) throws ParentNotFoundException, NullCursorException {
    if (parentGUID == null) {
      return "";
    }
    if (SPECIAL_GUIDS_MAP.containsKey(parentGUID)) {
      return SPECIAL_GUIDS_MAP.get(parentGUID);
    }

    // Get parent name from database.
    String parentName = "";
    Cursor name = dataAccessor.fetch(new String[] { parentGUID });
    try {
      name.moveToFirst();
      if (!name.isAfterLast()) {
        parentName = RepoUtils.getStringFromCursor(name, BrowserContract.Bookmarks.TITLE);
      }
      else {
        Logger.error(LOG_TAG, "Couldn't find record with guid '" + parentGUID + "' when looking for parent name.");
        throw new ParentNotFoundException(null);
      }
    } finally {
      name.close();
    }
    return parentName;
  }

  /**
   * Retrieve the child array for a record, repositioning and updating the database as necessary.
   *
   * @param folderID
   *        The database ID of the folder.
   * @param persist
   *        True if generated positions should be written to the database. The modified
   *        time of the parent folder is only bumped if this is true.
   * @return
   *        An array of GUIDs.
   * @throws NullCursorException
   */
  @SuppressWarnings("unchecked")
  private JSONArray getChildrenArray(long folderID, boolean persist) throws NullCursorException {
    trace("Calling getChildren for androidID " + folderID);
    JSONArray childArray = new JSONArray();
    Cursor children = dataAccessor.getChildren(folderID);
    try {
      if (!children.moveToFirst()) {
        trace("No children: empty cursor.");
        return childArray;
      }

      int count = children.getCount();
      String[] kids = new String[count];
      Logger.debug(LOG_TAG, "Expecting " + count + " children.");

      // Track badly positioned records.
      HashMap<String, Long> broken = new HashMap<String, Long>();   // TODO: sorted by value.

      int positionIndex = children.getColumnIndex(BrowserContract.Bookmarks.POSITION);

      // Get children into array in correct order.
      while (!children.isAfterLast()) {
        String childGuid   = getGUID(children);
        long parent        = getParentID(children);
        long childPosition = getPosition(children, positionIndex);
        trace("  Child GUID: " + childGuid + ", parent " + parent);
        trace("  Child position: " + childPosition);

        if (childPosition < 0 || childPosition >= count) {
          Logger.warn(LOG_TAG, "Child position " + childPosition + " out of range: [0, " + count + ")");
          broken.put(childGuid, childPosition);
        } else {
          String existing = kids[(int) childPosition];
          if (existing == null) {
            kids[(int) childPosition] = childGuid;
          } else {
            Logger.warn(LOG_TAG, "Child position " + childPosition + " already occupied! (" +
                                 childGuid + ", " + existing + ")");
            broken.put(childGuid, childPosition);
          }
        }
        children.moveToNext();
      }

      int moved = 0;
      boolean forbidden = false;
      if (broken.size() > 0) {
        // We've got some children with valid positions, and children
        // with useless ones. Pack the good ones to the front, then
        // fill the rest.
        moved = Utils.pack(kids);
        Logger.debug(LOG_TAG, "Packing moved " + moved + " records.");
        try {
          moved += Utils.fillArraySpaces(kids, broken);
          Logger.debug(LOG_TAG, "Filling and packing moved " + moved + " records.");
        } catch (Exception e) {
          Logger.error(LOG_TAG, "Unable to reposition children to yield a valid sequence. Data loss may result.", e);
        }
      }

      // Collect into a more friendly data structure.
      for (int i = 0; i < count; ++i) {
        String kid = kids[i];
        // Eliminate any special folders that crept in. TODO: why?
        if (forbiddenGUID(kid)) {
          forbidden = true;
          continue;
        }
        childArray.add(kid);
      }
      if (Logger.logVerbose(LOG_TAG)) {
        // Don't JSON-encode unless we're logging.
        Logger.trace(LOG_TAG, "Output child array: " + childArray.toJSONString());
      }

      if (!persist) {
        return childArray;
      }

      if (moved == 0 && !forbidden) {
        Logger.debug(LOG_TAG, "Nothing moved!");
        return childArray;
      }

      Logger.debug(LOG_TAG, "Generating child array required moving " + moved + " records. Updating DB.");
      final long time = now();
      if (0 < dataAccessor.updatePositions(childArray)) {
        Logger.debug(LOG_TAG, "Bumping parent time to " + time + ".");
        dataAccessor.bumpModified(folderID, time);
      }
    } finally {
      children.close();
    }

    return childArray;
  }

  protected static boolean isDeleted(Cursor cur) {
    return RepoUtils.getLongFromCursor(cur, BrowserContract.SyncColumns.IS_DELETED) != 0;
  }

  @Override
  protected Record retrieveDuringStore(Cursor cur) throws NoGuidForIdException, NullCursorException, ParentNotFoundException {
    return retrieveRecord(cur, false);
  }

  @Override
  protected Record retrieveDuringFetch(Cursor cur) throws NoGuidForIdException, NullCursorException, ParentNotFoundException {
    return retrieveRecord(cur, true);
  }

  /**
   * Build a record from a cursor, with a flag to dictate whether the
   * computed children array is written back into the database.
   */
  protected BookmarkRecord retrieveRecord(Cursor cur, boolean persist) throws NoGuidForIdException, NullCursorException, ParentNotFoundException {
    String recordGUID = getGUID(cur);
    Logger.trace(LOG_TAG, "Record from mirror cursor: " + recordGUID);

    if (forbiddenGUID(recordGUID)) {
      Logger.debug(LOG_TAG, "Ignoring " + recordGUID + " record in recordFromMirrorCursor.");
      return null;
    }

    // Short-cut for deleted items.
    if (isDeleted(cur)) {
      return AndroidBrowserBookmarksRepositorySession.bookmarkFromMirrorCursor(cur, null, null, null);
    }

    long androidParentID = getParentID(cur);

    // Ensure special folders stay in the right place.
    String androidParentGUID = SPECIAL_GUID_PARENTS.get(recordGUID);
    if (androidParentGUID == null) {
      androidParentGUID = getGUIDForID(androidParentID);
    }

    boolean needsReparenting = false;

    if (androidParentGUID == null) {
      Logger.debug(LOG_TAG, "No parent GUID for record " + recordGUID + " with parent " + androidParentID);
      // If the parent has been stored and somehow has a null GUID, throw an error.
      if (idToGuid.containsKey(androidParentID)) {
        Logger.error(LOG_TAG, "Have the parent android ID for the record but the parent's GUID wasn't found.");
        throw new NoGuidForIdException(null);
      }

      // We have a parent ID but it's wrong. If the record is deleted,
      // we'll just say that it was in the Unsorted Bookmarks folder.
      // If not, we'll move it into Mobile Bookmarks.
      needsReparenting = true;
    }

    // If record is a folder, build out the children array.
    JSONArray childArray = getChildrenArrayForRecordCursor(cur, recordGUID, persist);
    String parentName = getParentName(androidParentGUID);
    BookmarkRecord bookmark = AndroidBrowserBookmarksRepositorySession.bookmarkFromMirrorCursor(cur, androidParentGUID, parentName, childArray);

    if (needsReparenting) {
      Logger.warn(LOG_TAG, "Bookmark record " + recordGUID + " has a bad parent pointer. Reparenting now.");

      String destination = bookmark.deleted ? "unfiled" : "mobile";
      bookmark.androidParentID = getIDForGUID(destination);
      bookmark.androidPosition = getPosition(cur);
      bookmark.parentID        = destination;
      bookmark.parentName      = getParentName(destination);
      if (!bookmark.deleted) {
        // Actually move it.
        // TODO: compute position. Persist.
        relocateBookmark(bookmark);
      }
    }

    return bookmark;
  }

  /**
   * Ensure that the local database row for the provided bookmark
   * reflects this record's parent information.
   *
   * @param bookmark
   */
  private void relocateBookmark(BookmarkRecord bookmark) {
    dataAccessor.updateParentAndPosition(bookmark.guid, bookmark.androidParentID, bookmark.androidPosition);
  }

  protected JSONArray getChildrenArrayForRecordCursor(Cursor cur, String recordGUID, boolean persist) throws NullCursorException {
    boolean isFolder = rowIsFolder(cur);
    if (!isFolder) {
      return null;
    }

    long androidID = guidToID.get(recordGUID);
    JSONArray childArray = getChildrenArray(androidID, persist);
    if (childArray == null) {
      return null;
    }

    Logger.debug(LOG_TAG, "Fetched " + childArray.size() + " children for " + recordGUID);
    return childArray;
  }

  @Override
  protected boolean checkRecordType(Record record) {
    BookmarkRecord bmk = (BookmarkRecord) record;
    if (bmk.type.equalsIgnoreCase(AndroidBrowserBookmarksDataAccessor.TYPE_BOOKMARK) ||
        bmk.type.equalsIgnoreCase(AndroidBrowserBookmarksDataAccessor.TYPE_FOLDER)) {
      return true;
    }
    Logger.info(LOG_TAG, "Ignoring record with guid: " + record.guid + " and type: " + ((BookmarkRecord)record).type);
    return false;
  }
  
  @Override
  public void begin(RepositorySessionBeginDelegate delegate) {
    // Check for the existence of special folders
    // and insert them if they don't exist.
    Cursor cur;
    try {
      Logger.debug(LOG_TAG, "Check and build special GUIDs.");
      dataAccessor.checkAndBuildSpecialGuids();
      cur = dataAccessor.getGuidsIDsForFolders();
      Logger.debug(LOG_TAG, "Got GUIDs for folders.");
    } catch (android.database.sqlite.SQLiteConstraintException e) {
      Logger.error(LOG_TAG, "Got sqlite constraint exception working with Fennec bookmark DB.", e);
      delegate.onBeginFailed(e);
      return;
    } catch (NullCursorException e) {
      delegate.onBeginFailed(e);
      return;
    } catch (Exception e) {
      delegate.onBeginFailed(e);
      return;
    }
    
    // To deal with parent mapping of bookmarks we have to do some
    // hairy stuff. Here's the setup for it.

    Logger.debug(LOG_TAG, "Preparing folder ID mappings.");
    idToGuid.put(0L, "places");       // Fake our root.
    try {
      cur.moveToFirst();
      while (!cur.isAfterLast()) {
        String guid = getGUID(cur);
        long id = RepoUtils.getLongFromCursor(cur, BrowserContract.Bookmarks._ID);
        guidToID.put(guid, id);
        idToGuid.put(id, guid);
        Logger.debug(LOG_TAG, "GUID " + guid + " maps to " + id);
        cur.moveToNext();
      }
    } finally {
      cur.close();
    }
    Logger.debug(LOG_TAG, "Done with initial setup of bookmarks session.");
    super.begin(delegate);
  }

  @Override
  public void finish(RepositorySessionFinishDelegate delegate) {
    // Override finish to do this check; make sure all records
    // needing re-parenting have been re-parented.
    if (needsReparenting != 0) {
      Logger.error(LOG_TAG, "Finish called but " + needsReparenting +
                            " bookmark(s) have been placed in unsorted bookmarks and not been reparented.");

      // TODO: handling of failed reparenting.
      // E.g., delegate.onFinishFailed(new BookmarkNeedsReparentingException(null));
    }
    super.finish(delegate);
  };

  @Override
  @SuppressWarnings("unchecked")
  protected Record prepareRecord(Record record) {
    BookmarkRecord bmk = (BookmarkRecord) record;
    
    // Check if parent exists.
    // TODO: synchronization!
    // TODO: you're modifying a child array in-place! Don't do that!
    if (guidToID.containsKey(bmk.parentID)) {
      bmk.androidParentID = guidToID.get(bmk.parentID);
      JSONArray children = parentToChildArray.get(bmk.parentID);
      if (children != null) {
        if (!children.contains(bmk.guid)) {
          children.add(bmk.guid);
          parentToChildArray.put(bmk.parentID, children);
        }
        bmk.androidPosition = children.indexOf(bmk.guid);
      }
    }
    else {
      bmk.androidParentID = guidToID.get("unfiled");
      ArrayList<String> children;
      if (missingParentToChildren.containsKey(bmk.parentID)) {
        children = missingParentToChildren.get(bmk.parentID);
      } else {
        children = new ArrayList<String>();
      }
      children.add(bmk.guid);
      needsReparenting++;
      missingParentToChildren.put(bmk.parentID, children);
    }

    if (Logger.LOG_PERSONAL_INFORMATION) {
      if (bmk.isFolder()) {
        Logger.pii(LOG_TAG, "Inserting folder " + bmk.guid + ", " + bmk.title +
                            " with parent " + bmk.androidParentID +
                            " (" + bmk.parentID + ", " + bmk.parentName +
                            ", " + bmk.pos + ")");
      } else {
        Logger.pii(LOG_TAG, "Inserting bookmark " + bmk.guid + ", " + bmk.title + ", " +
                            bmk.bookmarkURI + " with parent " + bmk.androidParentID +
                            " (" + bmk.parentID + ", " + bmk.parentName +
                            ", " + bmk.pos + ")");
      }
    } else {
      if (bmk.isFolder()) {
        Logger.debug(LOG_TAG, "Inserting folder " + bmk.guid +  ", parent " +
                              bmk.androidParentID +
                              " (" + bmk.parentID + ", " + bmk.pos + ")");
      } else {
        Logger.debug(LOG_TAG, "Inserting bookmark " + bmk.guid + " with parent " +
                              bmk.androidParentID +
                              " (" + bmk.parentID + ", " + ", " + bmk.pos + ")");
      }
    }
    return bmk;
  }

  @Override
  @SuppressWarnings("unchecked")
  protected void updateBookkeeping(Record record) throws NoGuidForIdException,
                                                         NullCursorException,
                                                         ParentNotFoundException {
    super.updateBookkeeping(record);
    BookmarkRecord bmk = (BookmarkRecord) record;

    // If record is folder, update maps and re-parent children if necessary.
    if (!bmk.type.equalsIgnoreCase(AndroidBrowserBookmarksDataAccessor.TYPE_FOLDER)) {
      Logger.debug(LOG_TAG, "Not a folder. No bookkeeping.");
      return;
    }

    Logger.debug(LOG_TAG, "Updating bookkeeping for folder " + record.guid);

    // Mappings between ID and GUID.
    // TODO: update our persisted children arrays!
    // TODO: if our Android ID just changed, replace parents for all of our children.
    guidToID.put(bmk.guid,      bmk.androidID);
    idToGuid.put(bmk.androidID, bmk.guid);

    JSONArray childArray = bmk.children;

    Logger.debug(LOG_TAG, record.guid + " has children " + childArray.toJSONString());
    parentToChildArray.put(bmk.guid, childArray);

    // Re-parent.
    if (missingParentToChildren.containsKey(bmk.guid)) {
      for (String child : missingParentToChildren.get(bmk.guid)) {
        long position;
        if (!bmk.children.contains(child)) {
          childArray.add(child);
        }
        position = childArray.indexOf(child);
        dataAccessor.updateParentAndPosition(child, bmk.androidID, position);
        needsReparenting--;
      }
      missingParentToChildren.remove(bmk.guid);
    }
  }

  @Override
  protected void storeRecordDeletion(final Record record) {
    if (SPECIAL_GUIDS_MAP.containsKey(record.guid)) {
      Logger.debug(LOG_TAG, "Told to delete record " + record.guid + ". Ignoring.");
      return;
    }
    final BookmarkRecord bookmarkRecord = (BookmarkRecord) record;
    if (bookmarkRecord.isFolder()) {
      Logger.debug(LOG_TAG, "Deleting folder. Ensuring consistency of children.");
      handleFolderDeletion(bookmarkRecord);
      return;
    }
    super.storeRecordDeletion(record);
  }

  /**
   * When a folder deletion is received, we must ensure -- for database
   * consistency -- that its children are placed somewhere sane.
   *
   * Note that its children might also be deleted, but we'll process
   * folders first. For that reason we might want to queue up these
   * folder deletions and handle them in onStoreDone.
   *
   * See Bug 724739.
   *
   * @param folder
   */
  protected void handleFolderDeletion(final BookmarkRecord folder) {
    // TODO: reparent children. Bug 724740.
    // For now we'll trust that we'll process the item deletions, too.
    super.storeRecordDeletion(folder);
  }

  @Override
  public void storeDone() {
    // TODO: queue up a Runnable to do final reparenting/repositioning work.
    super.storeDone();
  }

  @Override
  protected String buildRecordString(Record record) {
    BookmarkRecord bmk = (BookmarkRecord) record;
    return bmk.title + bmk.bookmarkURI + bmk.type + bmk.parentName;
  }

  public static BookmarkRecord computeParentFields(BookmarkRecord rec, String suggestedParentGUID, String suggestedParentName) {
    final String guid = rec.guid;
    if (guid == null) {
      // Oh dear.
      Logger.error(LOG_TAG, "No guid in computeParentFields!");
      return null;
    }

    String realParent = SPECIAL_GUID_PARENTS.get(guid);
    if (realParent == null) {
      // No magic parent. Use whatever the caller suggests.
      realParent = suggestedParentGUID;
    } else {
      Logger.debug(LOG_TAG, "Ignoring suggested parent ID " + suggestedParentGUID +
                            " for " + guid + "; using " + realParent);
    }

    if (realParent == null) {
      // Oh dear.
      Logger.error(LOG_TAG, "No parent for record " + guid);
      return null;
    }

    // Always set the parent name for special folders back to default.
    String parentName = SPECIAL_GUIDS_MAP.get(realParent);
    if (parentName == null) {
      parentName = suggestedParentName;
    }

    rec.parentID = realParent;
    rec.parentName = parentName;
    return rec;
  }

  private static BookmarkRecord logBookmark(BookmarkRecord rec) {
    try {
      Logger.debug(LOG_TAG, "Returning " + (rec.deleted ? "deleted " : "") +
                            "bookmark record " + rec.guid + " (" + rec.androidID +
                           ", parent " + rec.parentID + ")");
      if (!rec.deleted && Logger.LOG_PERSONAL_INFORMATION) {
        Logger.pii(LOG_TAG, "> Parent name:      " + rec.parentName);
        Logger.pii(LOG_TAG, "> Title:            " + rec.title);
        Logger.pii(LOG_TAG, "> Type:             " + rec.type);
        Logger.pii(LOG_TAG, "> URI:              " + rec.bookmarkURI);
        Logger.pii(LOG_TAG, "> Android position: " + rec.androidPosition);
        Logger.pii(LOG_TAG, "> Position:         " + rec.pos);
        if (rec.isFolder()) {
          Logger.pii(LOG_TAG, "FOLDER: Children are " +
                             (rec.children == null ?
                                 "null" :
                                 rec.children.toJSONString()));
        }
      }
    } catch (Exception e) {
      Logger.debug(LOG_TAG, "Exception logging bookmark record " + rec, e);
    }
    return rec;
  }

  // Create a BookmarkRecord object from a cursor on a row containing a Fennec bookmark.
  public static BookmarkRecord bookmarkFromMirrorCursor(Cursor cur, String parentGUID, String parentName, JSONArray children) {
    final String collection = "bookmarks";
    final String guid       = RepoUtils.getStringFromCursor(cur, BrowserContract.SyncColumns.GUID);
    final long lastModified = RepoUtils.getLongFromCursor(cur,   BrowserContract.SyncColumns.DATE_MODIFIED);
    final boolean deleted   = isDeleted(cur);
    BookmarkRecord rec = new BookmarkRecord(guid, collection, lastModified, deleted);

    // No point in populating it.
    if (deleted) {
      return logBookmark(rec);
    }

    boolean isFolder  = RepoUtils.getIntFromCursor(cur, BrowserContract.Bookmarks.IS_FOLDER) == 1;

    rec.title = RepoUtils.getStringFromCursor(cur, BrowserContract.Bookmarks.TITLE);
    rec.bookmarkURI = RepoUtils.getStringFromCursor(cur, BrowserContract.Bookmarks.URL);
    rec.description = RepoUtils.getStringFromCursor(cur, BrowserContract.Bookmarks.DESCRIPTION);
    rec.tags = RepoUtils.getJSONArrayFromCursor(cur, BrowserContract.Bookmarks.TAGS);
    rec.keyword = RepoUtils.getStringFromCursor(cur, BrowserContract.Bookmarks.KEYWORD);
    rec.type = isFolder ? AndroidBrowserBookmarksDataAccessor.TYPE_FOLDER :
                          AndroidBrowserBookmarksDataAccessor.TYPE_BOOKMARK;

    rec.androidID = RepoUtils.getLongFromCursor(cur, BrowserContract.Bookmarks._ID);
    rec.androidPosition = RepoUtils.getLongFromCursor(cur, BrowserContract.Bookmarks.POSITION);
    rec.children = children;

    // Need to restore the parentId since it isn't stored in content provider.
    // We also take this opportunity to fix up parents for special folders,
    // allowing us to map between the hierarchies used by Fennec and Places.
    BookmarkRecord withParentFields = computeParentFields(rec, parentGUID, parentName);
    if (withParentFields == null) {
      // Oh dear. Something went wrong.
      return null;
    }
    return logBookmark(withParentFields);
  }
}
