SOURCEDIR=$1
if [ -z "$SOURCEDIR" ]; then
  echo "No source directory specified."
  exit 1
fi

ANDROID=$SOURCEDIR/mobile/android
if [ ! -d $ANDROID ]; then
  echo "No android dir."
  exit 1
fi

cp $ANDROID/base/db/BrowserContract.java.in src/main/java/org/mozilla/gecko/db/
cp $ANDROID/base/locales/en-US/android_strings.dtd strings/

# Infrequently Fennec touches Android services strings.  Uncomment
# these lines to pull in Fennec's versions.
# cp $ANDROID/services/strings.xml.in strings/strings.xml.in 
# cp $ANDROID/base/locales/en-US/sync_strings.dtd strings/sync_strings.dtd.in

./preprocess.sh
