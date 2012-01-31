#!/bin/sh

# Substitute package names for bundling with Fennec.
USERNAME=$(whoami)
PREPROCESSOR="python tools/Preprocessor.py"

ANDROID_PACKAGE_NAME=$($PREPROCESSOR -Fsubstitution -DUSERNAME=$USERNAME package-name.txt)
DEFINITIONS="-DANDROID_PACKAGE_NAME=$ANDROID_PACKAGE_NAME"

SYNC_PREFERENCES=res/xml/sync_preferences.xml

$PREPROCESSOR $DEFINITIONS sync_preferences.xml.template > $SYNC_PREFERENCES
