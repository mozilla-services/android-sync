DESTDIR=$1
ANDROID=$DESTDIR/mobile/android

if [ ! -d $ANDROID ]; then
  echo "No android dir."
  exit 1
fi

SYNC=$DESTDIR/mobile/android/sync
WARNING="These files are managed in the android-sync repo. Do not modify directly, or your changes will be lost."

if [ -d $SYNC ]; then
  echo "Sync directory already exists. Updating."
else
  echo "No Sync directory. Creating directory structure."
  mkdir -p $SYNC
fi

echo "Building deps."
./deps.sh

echo "Copying resources..."
# I'm guessing these go here.
rsync -av res/drawable $ANDROID/base/resources/
rsync -av res/drawable-hdpi $ANDROID/base/resources/
rsync -av res/drawable-mdpi $ANDROID/base/resources/
rsync -av res/drawable-ldpi $ANDROID/base/resources/
rsync -av res/layout/*.xml $ANDROID/base/resources/layout/
rsync -av res/layout/*.xml $ANDROID/base/resources/layout/
rsync -av res/values/sync_styles.xml $ANDROID/base/resources/values/
rsync -av res/xml/*.xml $ANDROID/base/resources/xml/
rsync -av strings.xml.in $SYNC/
rsync -av sync_strings.dtd.in $ANDROID/base/locales/en-US/sync_strings.dtd

echo "res/values/sync_styles.xml " > $SYNC/android-values-resources.mn
find res/layout         -name '*.xml' > $SYNC/android-layout-resources.mn
find res/drawable       -name '*.png' | sed "s,res/,mobile/android/base/resources/," > $SYNC/android-drawable-resources.mn
find res/drawable-ldpi  -name '*.png' | sed "s,res/,mobile/android/base/resources/," > $SYNC/android-drawable-ldpi-resources.mn
find res/drawable-mdpi  -name '*.png' | sed "s,res/,mobile/android/base/resources/," > $SYNC/android-drawable-mdpi-resources.mn
find res/drawable-hdpi  -name '*.png' | sed "s,res/,mobile/android/base/resources/," > $SYNC/android-drawable-hdpi-resources.mn
find res/xml            -name '*.xml' | sed "s,res/,mobile/android/base/resources/," > $SYNC/android-xml-resources.mn

echo "Creating README.txt."
echo $WARNING > $SYNC/README.txt

echo "Copying manifests..."
rsync -a manifests $SYNC/

echo "Copying sources. All use of R must be compiled with Fennec."
SOURCEDIR="src/main/java/org/mozilla/gecko/sync"
SOURCEFILES=$(find "$SOURCEDIR" -name '*.java' -not -name 'Authorities.java' | sed "s,$SOURCEDIR/,sync/,")
echo "Source files: \n  $SOURCEFILES"
rsync --include "*.java" -C --exclude 'Authorities.java' -a $SOURCEDIR $ANDROID/base/

echo "Copying preprocessor Authorities file."
PREPROCESS_FILES="sync/repositories/android/Authorities.java"
cp $SOURCEDIR/repositories/android/Authorities.java.in $ANDROID/base/sync/repositories/android/

echo "Copying preprocessed sync_syncadapter.xml."
cp sync_syncadapter.xml.template $ANDROID/base/resources/xml/sync_syncadapter.xml.in

echo "Copying internal dependency sources."
APACHEDIR="src/main/java/org/mozilla/apache"
APACHEFILES=$(find "$APACHEDIR" -name '*.java' | sed "s,$APACHEDIR/,apache/,")
echo "Source files: \n  $APACHEFILES"
rsync --include "*.java" -C -a $APACHEDIR $ANDROID/base/

echo "Copying external dependency sources."
JSONLIB=external/json-simple-1.1/src/org/json/simple
HTTPLIB=external/httpclientandroidlib/httpclientandroidlib/src/ch/boye/httpclientandroidlib
JSONLIBFILES=$(find "$JSONLIB" -name '*.java' | sed "s,$JSONLIB/,json-simple/,")
HTTPLIBFILES=$(find "$HTTPLIB" -name '*.java' | sed "s,$HTTPLIB/,httpclientandroidlib/,")
echo "httpclientandroidlib files: \n  $HTTPLIBFILES"
echo "json-simple files: \n  $JSONLIBFILES"
mkdir -p $ANDROID/base/json-simple/
rsync --include "*.java" -C -a $HTTPLIB $ANDROID/base/
rsync --include "*.java" -C -a $JSONLIB/ $ANDROID/base/json-simple/

# These seem to get copied anyway.
rm $ANDROID/base/sync/repositories/android/Authorities.java
rm $ANDROID/base/resources/xml/sync_syncadapter.xml

echo $PREPROCESS_FILES > $SYNC/preprocess-sources.mn
echo $WARNING > $ANDROID/base/sync/README.txt
echo $WARNING > $ANDROID/base/httpclientandroidlib/README.txt
echo $SOURCEFILES > $SYNC/java-sources.mn
echo $HTTPLIBFILES $JSONLIBFILES $APACHEFILES > $SYNC/java-third-party-sources.mn
