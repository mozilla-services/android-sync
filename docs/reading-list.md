# How This Works

Let's start top-down.

An Android `SyncAdapter` is told to sync. This is boilerplate: we go through the various FxA callbacks, before eventually we have a working assertion, a set of `SharedPreferences`, etc. etc.

At that point we hand over to the `ReadingListSynchronizer`.

The synchronizer is composed of three things:

*   A local storage class, `LocalReadingListStorage`, implements
    `ReadingListStorage` and wraps a `ContentProviderClient`.
    This class knows how to fetch records for each phase: status changes, modifications, and new records.

    Storage also exposes a kind of ‘builder’ — a `ReadingListChangeAccumulator`.

    Each reading list API operation results in an updated record or a specific action to take (like “this record was deleted, so delete it locally”). The accumulator accumulates these updates, and encapsulates the flushing of them back to storage.

    For example, the synchronizer might upload some status changes, which will push new deltas into the accumulator (new timestamps, mainly); upload some more meaningful changes; and upload some new records, which will push both the complete GUID-ed server-built versions of those records, and perhaps some deletions (on conflict) into the accumulator.

*   A scratchpad: prefs. This stores enough state to recreate the synchronizer for a second run from the same position.

*   A `ReadingListClient`, which exposes the server API.

These various parts communicate in the form of `ReadingListRecords`. The server version incorporates some `ServerMetadata`, and a JSON bundle. The client version has a similar JSON bundle (with keys matching the API, not local columns), and both client and server metadata.

During success response processing, this arrangement allows us to unify some metadata from the uploaded record (*e.g.*, the local numeric ID for an item) with the unified data and metadata from the server, so that we can update the DB.

The `ReadingListClient` works with a collection of delegate interfaces that on one side talk to `BaseResource` and on the other speak a domain-level language. This allows callers of the client to be written in terms of conflicts, failures, and uploads, and for the client itself to be expressed concisely (once all the Java boilerplate is stripped away, that is!).

Two factories are used: one to turn `ClientReadingListRecords` into `ContentValues` for application to the database, and one to go the other direction, turning cursors into `ClientReadingListRecords`. `ServerReadingListRecord` is its own factory, because the mapping is trivial.
