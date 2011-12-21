#!/bin/sh
SOURCEDIR=external/commons-codec-1.5/src/java/org/apache/commons/codec
DESTDIR=src/main/java/org/mozilla/apache/commons

echo "Clearing $DESTDIR..."
rm -r $DESTDIR
mkdir -p $DESTDIR

echo "Copying source from $SOURCEDIR..."
rsync -av --exclude=".svn" $SOURCEDIR $DESTDIR/

# Would be nice if we could rely on sed -i.
echo "Rewriting source."

find $DESTDIR -name '*.java' -exec perl -pi -e 's/org\.apache\.commons\.codec/org.mozilla.apache.commons.codec/g' {} \;

# This is very inefficient, but it works.
echo "Including license block."
LICENSE=commons-codec-license.txt
TEMPFILE=`mktemp -t mozilla-license`
for file in $(find $DESTDIR -name '*.java'); do
  cat $LICENSE "$file" > $TEMPFILE
  cp $TEMPFILE "$file"
done
rm $TEMPFILE
