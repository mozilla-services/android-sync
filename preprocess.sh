#!/bin/sh

# Or use the one in mozilla-central.
PREPROCESSOR=tools/Preprocessor.py

ANDROID_PACKAGE_NAME=$(cat package-name.txt)

echo "Using ANDROID_PACKAGE_NAME $ANDROID_PACKAGE_NAME."

AUTHORITIES=src/main/java/org/mozilla/gecko/sync/repositories/android/Authorities.java
MANIFEST=AndroidManifest.xml

DEFINITIONS="-DANDROID_PACKAGE_NAME=$ANDROID_PACKAGE_NAME"
python $PREPROCESSOR $DEFINITIONS $AUTHORITIES.in > $AUTHORITIES
python $PREPROCESSOR $DEFINITIONS $MANIFEST.in > $MANIFEST

python $PREPROCESSOR $DEFINITIONS strings.xml.template > res/values/strings.xml
python $PREPROCESSOR $DEFINITIONS sync_syncadapter.xml.template > res/xml/sync_syncadapter.xml
