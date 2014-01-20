#!/bin/sh
#
# This will abort "git commit" and remove the trailing whitespaces from the files to be committed.
# Simply repeating the last "git commit" command will do the commit then.
#
# Put this into .git/hooks/pre-commit, and chmod +x it.

if git rev-parse --verify HEAD >/dev/null 2>&1; then
  against=HEAD
else
  # Initial commit: diff against an empty tree object
  against=4b825dc642cb6eb9a060e54bf8d69288fbee4904
fi

if test "$(git diff-index --check --cached $against --)"; then
  for FILE in `git diff-index --check --cached $against -- | sed '/^[+-]/d' | cut -d: -f1 | uniq`; do echo "* $FILE" ; sed -i "" 's/ *$//' "$FILE" ; done
  exit 1
fi
