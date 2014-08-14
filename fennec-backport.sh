#!/bin/sh

# Backport (most) changes from a Gecko source tree.
#
# How to use:
#
# 1. ./fennec-copy-code.sh ../gecko
#
# This "undoes" changes from the Gecko source tree.
#
# 2. ./fennec-backport ../gecko
#
# This lists the "undone" changes from the Gecko source tree, and copies the
# updated file (from Gecko's source control) to android-sync.
#
# A subsequent |./fennec-copy-code.sh ../gecko| should not change ../gecko.
# You'll need to commit the changes in android-sync and push upstream.

export LC_ALL=C # Ensure consistent sort order across platforms.
SORT_CMD="sort -f"

DIR=$(dirname "$0")
if [[ ! -d "$DIR" ]]; then DIR="$PWD"; fi
. "$DIR/fennec-paths.sh"

echo "Porting from $DESTDIR ..."

FILES=`hg --cwd $DESTDIR status -n`
for f in $FILES ; do
    g=`basename $f`
    h=`git ls-files "*/$g"`
    if [ -e "$h" ]; then
        hg --cwd $DESTDIR cat $f > $h
    fi
done
