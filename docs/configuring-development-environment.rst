Configuring your development environment
========================================

First, clone the git repository: ::

  archo:temp ncalexan$ git clone https://github.com/mozilla-services/android-sync
  Cloning into 'android-sync'...
  remote: Counting objects: 39508, done.
  remote: Compressing objects: 100% (11663/11663), done.
  remote: Total 39508 (delta 18085), reused 39220 (delta 17827)
  Receiving objects: 100% (39508/39508), 7.48 MiB | 374 KiB/s, done.
  Resolving deltas: 100% (18085/18085), done.

Licensing
---------

Android Services code that is shipped as part of Firefox for Mobile
must be licensed `Mozilla Public License 2.0`_.  Code that is not
shipped (for example, test code, command line scripts, and utilities)
is usually explicitly stated as being in the public domain.

.. _`Mozilla Public License 2.0`: http://www.mozilla.org/MPL

The top-level scripts ``./check_headers.sh`` and
``./check_head_headers.sh`` check, respectively, that all source and
test files, and uncommitted source and test files, have correct headers.
(Internally they both use ``./tools/check_headers.py``.)

git hooks
---------

The top-level script ``./pre-commit.sh`` is a git pre-commit hook that
will stop you committing files with incorrect headers.  To install it,
run ::

  archo:android-sync ncalexan$ ln ./pre-commit.sh .git/hooks/pre-commit

.. note:

  If the pre-commit hook is failing, check that
  ``.git/hooks/pre-commit`` exists and is executable.

To temporarily ignore this pre-commit hook, use ``git commit --no-verify``.
