#!/bin/sh

# Or use the one in mozilla-central.
USERNAME=$(whoami)
PREPROCESSOR="python tools/Preprocessor.py"

ANDROID_PACKAGE_NAME=$($PREPROCESSOR -Fsubstitution -DUSERNAME=$USERNAME package-name.txt)

echo "Using ANDROID_PACKAGE_NAME $ANDROID_PACKAGE_NAME."

AUTHORITIES=src/main/java/org/mozilla/gecko/sync/repositories/android/Authorities.java
MANIFEST=AndroidManifest.xml
SYNC_PREFERENCES=res/xml/sync_preferences.xml

DEFINITIONS="-DANDROID_PACKAGE_NAME=$ANDROID_PACKAGE_NAME"
$PREPROCESSOR $DEFINITIONS $AUTHORITIES.in > $AUTHORITIES
$PREPROCESSOR $DEFINITIONS $MANIFEST.in > $MANIFEST

$PREPROCESSOR $DEFINITIONS strings.xml.template > res/values/strings.xml
$PREPROCESSOR $DEFINITIONS sync_syncadapter.xml.template > res/xml/sync_syncadapter.xml

# Now do the test project.
TEST_MANIFEST=test/AndroidManifest.xml
$PREPROCESSOR $DEFINITIONS $TEST_MANIFEST.in > $TEST_MANIFEST

# Now preprocess package name for stand-alone Android Sync.
# preprocess-fennec.sh will substitute for fennec-specific package naming.
DEFINITIONS="-DANDROID_PACKAGE_NAME=org.mozilla.gecko"
$PREPROCESSOR $DEFINITIONS sync_preferences.xml.template > $SYNC_PREFERENCES
