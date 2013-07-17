Landing code: getting your changes committed to android-sync and mozilla-inbound
================================================================================

Since Android services are developed in a repository external to the
main Mozilla repositories, landing code is a two-step process.  We
first land on the branch ``develop`` of git repository
``android-sync``, and then we land on ``mozilla-inbound`` (or any
other Mozilla repository).

Merging to develop
------------------

We use a gitflow_-like development process.  All new work is developed
on a branch that is continually rebased to ``develop``.  We prefer to
name branches like ``username/bug-NUMBER-description``, e.g.,
``nalexander/bug-844347-logger``.  We always open a GitHub pull request
to get review before merging.

We always rebase our branches onto ``develop`` to keep our history easy
to read, and so that GitHub will automatically close our pull requests
after merge.  We include bug numbers at the start of every commit
message (this helps when parsing ``git blame``).  After rebasing, your
git log should look something like: ::

  2babb1b * nalexander/bug-844347-logger Bug 844347: move Logger and log writers to org.mozilla.gecko.background.common.log package.
  d868215 * Bug 844347: move org.mozilla.gecko.sync.GlobalConstants to org.mozilla.gecko.background.common.GlobalConstants.
  1c24220 * Bug 844347: fold BackgroundConstants.java into GlobalConstants.java.in.
  319879b * Bug 844347: separate Sync-specific from common pieces in {SyncConstants,GlobalConstants}.java.in.
  e19f136 * origin/develop develop Bug 845080 - Extract BackgroundService superclass. r=rnewman

Commit ``e19f136`` is ``develop``; the other four commits are the commits
to be merged into ``develop``.  To merge: ::

  $ git checkout develop
  $ git merge --no-ff -m "Bug 844347 - Factor logging code that is not Sync-specific out of org.mozilla.gecko.sync. r=rnewman" nalexander/bug-844347-logger

Note the ``--no-ff`` flag; we always want merge commits.  This is
partly because we only put the ``r=reviewer`` tag on the merge
commits.  By rebasing and merging in this way, it is easy to tell who
did what and how development proceeded.  Most of the time, all of the
changes you just merged to ``develop`` will be landed as a single
patch on ``mozilla-inbound``.  Therefore, your merge commit should say
what you did (not how you did it) and should reference the product you
modified (in this case, Sync).  At this point, your git log should
look something like: ::

  f1f41af *   develop Bug 844347 - Factor logging code that is not Sync-specific out of org.mozilla.gecko.sync. r=rnewman
          |\
  2babb1b | * origin/nalexander/bug-844347-logger nalexander/bug-844347-logger Bug 844347: move Logger and log writers to org.mozilla.gecko.background.common.log package.
  d868215 | * Bug 844347: move org.mozilla.gecko.sync.GlobalConstants to org.mozilla.gecko.background.common.GlobalConstants.
  1c24220 | * Bug 844347: fold BackgroundConstants.java into GlobalConstants.java.in.
  319879b | * Bug 844347: separate Sync-specific from common pieces in {SyncConstants,GlobalConstants}.java.in.
          |/
  e19f136 *   origin/develop Bug 845080 - Extract BackgroundService superclass. r=rnewman

Finally, you need to push your changes back upstream: ::

  $ git push origin develop

.. _gitflow: http://nvie.com/posts/a-successful-git-branching-model/

Merging to mozilla-inbound
--------------------------

Let's assume your working directories look like ::

  $ ls
  android-sync
  mozilla-inbound

First, create a new mq_ patch.  Ensure that you're correctly carrying
over the author from the commits merged into ``develop``. ::

  $ cd mozilla-inbound
  $ hg qnew -m "Bug 844347 - Factor logging code that is not Sync-specific out of org.mozilla.gecko.sync. r=rnewman" --user "Nick Alexander <nalexander@mozilla.com>" 844347.patch

Run the Android services code-drop script, targeting the correct
Mozilla repository.  (You can also use copy-code, which does not
verify that the code builds and the unit test suite passes.) ::

  $ cd android-sync
  $ ./fennec-code-drop.sh ../mozilla-inbound

These scripts copy pieces of the ``android-sync`` repository into
``mobile-inbound/mobile/android/``.  Now you need to refresh the
patch.  Be sure to add and remove files, and be aware that renamed
files require special care [#hgaddremove]_: ::

  $ cd mozilla-inbound
  $ hg status
  $ hg add any-missing-files.java
  $ hg rm anything-removed.java
  $ hg qref

Check that the patch is what you want to commit.  You are responsible
for anything that you land in the tree, so it behooves you to make
sure you get this right. ::

  $ less .hg/patches/844347.patch

Finally, ensure that everything builds and runs.  Assuming your object
directory is ``objdir-droid``: ::

  $ make -C objdir-droid
  $ make -C objdir-droid package install

You can now finish your patch, verify what you're going to send, and
push it upstream: ::

  $ hg qfinish tip
  $ hg outgoing
  $ hg push

.. _mq: http://mercurial.selenic.com/wiki/MqExtension

.. [#hgaddremove] See
   http://hgtip.com/tips/advanced/2009-09-30-detecting-renames-automatically/.
   Consider using the argument ``--similarity 95`` (not 100, since
   moving Java code often changes at least the package name).

Updating Bugzilla
-----------------

This is not Android services specific, but we'll call it out anyway.
You need to:

1. set the Bugzilla ticket status as ASSIGNED to the author of the commits;
1. add the changeset URL that ``hg push`` reports to the Bugzilla
   ticket;
1. and set the target milestone.
