#!/bin/sh

# Or use the one in mozilla-central.
USERNAME=$(whoami)
PREPROCESSOR="python tools/Preprocessor.py"

ANDROID_PACKAGE_NAME=$($PREPROCESSOR -Fsubstitution -DUSERNAME=$USERNAME package-name.txt)
MOZ_APP_DISPLAYNAME="Firefox_Sync_(Standalone)"

echo "Using ANDROID_PACKAGE_NAME $ANDROID_PACKAGE_NAME."
echo "Using MOZ_APP_DISPLAYNAME $MOZ_APP_DISPLAYNAME."

AUTHORITIES=src/main/java/org/mozilla/gecko/sync/repositories/android/Authorities.java
CLIENT_NAME=src/main/java/org/mozilla/gecko/sync/setup/ClientName.java
MANIFEST=AndroidManifest.xml

DEFINITIONS="-DANDROID_PACKAGE_NAME=$ANDROID_PACKAGE_NAME"
DISPLAYNAME_DEF="-DMOZ_APP_DISPLAYNAME=$MOZ_APP_DISPLAYNAME"
$PREPROCESSOR $DEFINITIONS $AUTHORITIES.in > $AUTHORITIES
$PREPROCESSOR $DEFINITIONS $MANIFEST.in > $MANIFEST
$PREPROCESSOR $DISPLAYNAME_DEF $CLIENT_NAME.in > $CLIENT_NAME

$PREPROCESSOR $DEFINITIONS strings.xml.template > res/values/strings.xml
$PREPROCESSOR $DEFINITIONS sync_syncadapter.xml.template > res/xml/sync_syncadapter.xml
$PREPROCESSOR $DEFINITIONS sync_options.xml.template > res/xml/sync_options.xml

# Now do the test project.
TEST_MANIFEST=test/AndroidManifest.xml
$PREPROCESSOR $DEFINITIONS $TEST_MANIFEST.in > $TEST_MANIFEST
