#!/bin/sh

export LC_ALL=C # Ensure consistent sort order across platforms.
SORT_CMD="sort -f"

DIR=$(dirname "$0")
if [[ ! -d "$DIR" ]]; then DIR="$PWD"; fi
. "$DIR/fennec-paths.sh"
  
echo "Copying to $ANDROID ($SERVICES)..."

WARNING="These files are managed in the android-sync repo. Do not modify directly, or your changes will be lost."
echo "Creating README.txt."
echo $WARNING > $SERVICES/README.txt

echo "Copying background tests..."
BACKGROUND_TESTS_DIR=$ANDROID/tests/background/junit3
mkdir -p $BACKGROUND_TESTS_DIR

BACKGROUND_SRC_DIR="test/src/org/mozilla/gecko/background"
BACKGROUND_TESTHELPERS_SRC_DIR="src/main/java/org/mozilla/gecko/background/testhelpers"

BACKGROUND_TESTS_JAVA_FILES=$(find \
  $BACKGROUND_SRC_DIR/* \
  $BACKGROUND_TESTHELPERS_SRC_DIR/* \
  -name '*.java' \
  | sed "s,^$BACKGROUND_SRC_DIR,src," \
  | sed "s,^$BACKGROUND_TESTHELPERS_SRC_DIR,src/testhelpers," \
  | $SORT_CMD)

BACKGROUND_TESTS_RES_FILES=$(find \
  "test/res" \
  -type f \
  | sed "s,^test/res,res," \
  | $SORT_CMD)

mkdir -p $BACKGROUND_TESTS_DIR/src
rsync -C -a \
  $BACKGROUND_SRC_DIR/* \
  $BACKGROUND_TESTS_DIR/src

mkdir -p $BACKGROUND_TESTS_DIR/src/testhelpers
rsync -C -a \
  $BACKGROUND_TESTHELPERS_SRC_DIR/* \
  $BACKGROUND_TESTS_DIR/src/testhelpers

rsync -C -a \
  test/res \
  $BACKGROUND_TESTS_DIR

rsync -C -a \
  test/AndroidManifest.xml.in \
  $BACKGROUND_TESTS_DIR
echo "Copying background tests... done."

echo "Copying manifests..."
rsync -a manifests $SERVICES/

echo "Copying sources. All use of R must be compiled with Fennec."
SOURCEROOT="src/main/java/org/mozilla/gecko"
BACKGROUNDSOURCEDIR="$SOURCEROOT/background"
BROWSERIDSOURCEDIR="$SOURCEROOT/browserid"
FXASOURCEDIR="$SOURCEROOT/fxa"
SYNCSOURCEDIR="$SOURCEROOT/sync"
TOKENSERVERSOURCEDIR="$SOURCEROOT/tokenserver"
SOURCEFILES=$(find \
  "$BACKGROUNDSOURCEDIR" \
  "$BROWSERIDSOURCEDIR" \
  "$FXASOURCEDIR" \
  "$SYNCSOURCEDIR" \
  "$TOKENSERVERSOURCEDIR" \
  -name '*.java' \
  -and -not -name 'AppConstants.java' \
  -and -not -name 'SysInfo.java' \
  -and -not -name 'HardwareUtils.java' \
  -and -not -name 'RobocopTarget.java' \
  -and -not -name 'BrowserLocaleManager.java' \
  -and -not -name 'Locales.java' \
  -and -not -name 'LocaleManager.java' \
  -and -not -path '*testhelpers*' \
  | sed "s,$SOURCEROOT/,," | $SORT_CMD)

rsync --archive --cvs-exclude --delete \
  --exclude 'AppConstants.java' \
  --exclude 'SysInfo.java' \
  --exclude 'HardwareUtils.java' \
  --exclude 'RobocopTarget.java' \
  --exclude 'BrowserLocaleManager.java' \
  --exclude 'Locales.java' \
  --exclude 'LocaleManager.java' \
  --exclude '*.in' \
  --exclude '*testhelper*' \
  $BACKGROUNDSOURCEDIR $ANDROID/base/

rsync --archive --cvs-exclude --delete \
  --exclude '*.in' \
  $BROWSERIDSOURCEDIR $ANDROID/base/

rsync --archive --cvs-exclude --delete \
  --exclude '*.in' \
  $FXASOURCEDIR $ANDROID/base/

rsync --archive --cvs-exclude --delete \
  --exclude 'BrowserContract.java' \
  --exclude '*.in' \
  --exclude '*testhelper*' \
  $SYNCSOURCEDIR $ANDROID/base/

rsync --archive --cvs-exclude --delete \
  --exclude '*.in' \
  $TOKENSERVERSOURCEDIR $ANDROID/base/

echo "Copying preprocessed constants files."

# The grep line removes files in the root: those are provided by
# Fennec itself.
PREPROCESS_FILES=$(find \
  "$SOURCEROOT" \
  -name '*.java.in' \
  | grep "$SOURCEROOT/.*/" \
  | sed "s,.java.in,.java," \
  | sed "s,$SOURCEROOT/,," | $SORT_CMD)
for i in $PREPROCESS_FILES; do
# Just in case, delete the processed version.
  rm -f "$ANDROID/base/$i";
  cp "$SOURCEROOT/$i.in" "$ANDROID/base/$i.in";
done

echo "Copying internal dependency sources."
mkdir -p $ANDROID/thirdparty/ch/boye/httpclientandroidlib/
mkdir -p $ANDROID/thirdparty/org/json/simple/
mkdir -p $ANDROID/thirdparty/org/mozilla/apache/

APACHEDIR="src/main/java/org/mozilla/apache"
APACHEFILES=$(find "$APACHEDIR" -name '*.java' | sed "s,$APACHEDIR,org/mozilla/apache," | $SORT_CMD)
rsync -C -a "$APACHEDIR/" "$ANDROID/thirdparty/org/mozilla/apache"

echo "Copying external dependency sources."
JSONLIB=external/json-simple-1.1/src/org/json/simple
HTTPLIB=external/httpclientandroidlib/httpclientandroidlib/src/ch/boye/httpclientandroidlib
JSONLIBFILES=$(find "$JSONLIB" -name '*.java' | sed "s,$JSONLIB,org/json/simple," | $SORT_CMD)
HTTPLIBFILES=$(find "$HTTPLIB" -name '*.java' | sed "s,$HTTPLIB,ch/boye/httpclientandroidlib," | $SORT_CMD)
rsync -C -a "$HTTPLIB/" "$ANDROID/thirdparty/ch/boye/httpclientandroidlib/"
rsync -C -a "$JSONLIB/" "$ANDROID/thirdparty/org/json/simple/"
cp external/json-simple-1.1/LICENSE.txt $ANDROID/thirdparty/org/json/simple/

# Write a list of files to a mozbuild variable.
# turn
# VAR:=1.java 2.java
# into
# VAR += [\
#   '1.java',
#   '2.java',
# ]
function dump_mozbuild_variable {
    output_file=$1
    variable_name=$2
    shift
    shift

    echo "$variable_name [" >> $output_file
    for var in "$@" ; do
        for f in $var ; do
            echo "    '$f'," >> $output_file
        done
    done
    echo "]" >> $output_file
}

# Prefer PNGs in drawable-*: Android lint complains about PNG files in drawable.
SYNC_RES=$(find res \( -name 'sync*' -or -name 'fxa*' \) \( -name '*.xml' -or -name '*.png' \) | sed 's,res/,resources/,' | $SORT_CMD)

# Creating moz.build file for Mozilla.
MOZBUILDFILE=$ANDROID/base/android-services.mozbuild
echo "Creating moz.build file for including in the Mozilla build system at $MOZBUILDFILE"
cat tools/mozbuild_mpl.txt > $MOZBUILDFILE

dump_mozbuild_variable $MOZBUILDFILE "sync_thirdparty_java_files =" "$HTTPLIBFILES" "$JSONLIBFILES" "$APACHEFILES"
echo >> $MOZBUILDFILE
dump_mozbuild_variable $MOZBUILDFILE "sync_java_files =" "$SOURCEFILES"

# Creating moz.build for Mozilla.
MOZBUILDFILE=$ANDROID/tests/background/junit3/background_junit3_sources.mozbuild
echo "Creating background tests moz.build file for including in the Mozilla build system at $MOZBUILDFILE"
cat tools/mozbuild_mpl.txt > $MOZBUILDFILE
dump_mozbuild_variable $MOZBUILDFILE "background_junit3_sources =" "$BACKGROUND_TESTS_JAVA_FILES"

# Finished creating Makefile for Mozilla.

echo "Writing README."
echo $WARNING > $ANDROID/base/sync/README.txt
echo $WARNING > $ANDROID/thirdparty/ch/boye/httpclientandroidlib/README.txt

echo "Copying resources..."
# I'm guessing these go here.
rsync -a --exclude 'icon.png' --exclude 'ic_status_logo.png' res/drawable $ANDROID/base/resources/
rsync -a --exclude 'icon.png' --exclude 'ic_status_logo.png' res/drawable-xxhdpi $ANDROID/base/resources/
rsync -a --exclude 'icon.png' --exclude 'ic_status_logo.png' res/drawable-xhdpi $ANDROID/base/resources/
rsync -a --exclude 'icon.png' --exclude 'ic_status_logo.png' res/drawable-hdpi $ANDROID/base/resources/
rsync -a --exclude 'icon.png' --exclude 'ic_status_logo.png' res/drawable-mdpi $ANDROID/base/resources/
rsync -a --exclude 'icon.png' --exclude 'ic_status_logo.png' res/drawable-ldpi $ANDROID/base/resources/
rsync -a --exclude 'icon.png' --exclude 'ic_status_logo.png' res/drawable-v12 $ANDROID/base/resources/
rsync -a res/layout/*.xml $ANDROID/base/resources/layout/

# We use shell globbing to update all fxaccount and sync owned values.
rm -f $ANDROID/base/resources/{menu*,values*}/{fxaccount_,sync_}*
for f in res/{menu*,values*}/{fxaccount_,sync_}*; do
    g=$(echo $f | sed "s,^res/,$ANDROID/base/resources/,")
    rsync --archive $f $g
done

rsync -a res/xml/*.xml $ANDROID/base/resources/xml/
rsync -a strings/strings.xml.in $SERVICES/
rsync -a strings/sync_strings.dtd $ANDROID/base/locales/en-US/sync_strings.dtd

echo "Done."
