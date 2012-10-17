if [ -z "$ANDROID" ]
then
  echo "No ANDROID dir set. Aborting."
  exit 1;
fi

if [ -z "$SERVICES" ]
then
  echo "No SERVICES dir set. Aborting."
  exit 1;
fi
  
echo "Copying to $ANDROID ($SERVICES)..."

WARNING="These files are managed in the android-sync repo. Do not modify directly, or your changes will be lost."
echo "Creating README.txt."
echo $WARNING > $SERVICES/README.txt

echo "Copying manifests..."
rsync -a manifests $SERVICES/

echo "Copying sources. All use of R must be compiled with Fennec."
SOURCEROOT="src/main/java/org/mozilla/gecko"
SYNCSOURCEDIR="$SOURCEROOT/sync"
BACKGROUNDSOURCEDIR="$SOURCEROOT/background"
SOURCEFILES=$(find "$BACKGROUNDSOURCEDIR" "$SYNCSOURCEDIR" -name '*.java' -not -name 'GlobalConstants.java' -and -not -name 'BrowserContract.java' -and -not -name 'SyncConstants.java' | sed "s,$SOURCEROOT/,,")
rsync -C --exclude 'GlobalConstants.java' --exclude 'SyncConstants.java' --exclude 'BrowserContract.java' --exclude '*.in' -a $SYNCSOURCEDIR $ANDROID/base/
rsync -C --exclude '*.in' -a $BACKGROUNDSOURCEDIR $ANDROID/base/

echo "Copying preprocessed constants files."
PREPROCESS_FILES="sync/GlobalConstants.java sync/SyncConstants.java"
cp $SYNCSOURCEDIR/GlobalConstants.java.in $ANDROID/base/sync/
cp $SYNCSOURCEDIR/SyncConstants.java.in $ANDROID/base/sync/

echo "Copying preprocessed sync_authenticator.xml."
cp sync_authenticator.xml.template $ANDROID/base/resources/xml/sync_authenticator.xml.in

echo "Copying preprocessed sync_syncadapter.xml."
cp sync_syncadapter.xml.template $ANDROID/base/resources/xml/sync_syncadapter.xml.in

echo "Copying preprocessed sync_options.xml."
cp sync_options.xml.template $ANDROID/base/resources/xml/sync_options.xml.in

echo "Copying internal dependency sources."
APACHEDIR="src/main/java/org/mozilla/apache"
APACHEFILES=$(find "$APACHEDIR" -name '*.java' | sed "s,$APACHEDIR/,apache/,")
rsync -C -a $APACHEDIR $ANDROID/base/

echo "Copying external dependency sources."
JSONLIB=external/json-simple-1.1/src/org/json/simple
HTTPLIB=external/httpclientandroidlib/httpclientandroidlib/src/ch/boye/httpclientandroidlib
JSONLIBFILES=$(find "$JSONLIB" -name '*.java' | sed "s,$JSONLIB/,json-simple/,")
HTTPLIBFILES=$(find "$HTTPLIB" -name '*.java' | sed "s,$HTTPLIB/,httpclientandroidlib/,")
mkdir -p $ANDROID/base/json-simple/
rsync -C -a $HTTPLIB $ANDROID/base/
rsync -C -a $JSONLIB/ $ANDROID/base/json-simple/
cp external/json-simple-1.1/LICENSE.txt $ANDROID/base/json-simple/

# Creating Makefile for Mozilla.
MKFILE=$ANDROID/base/android-services-files.mk
echo "Creating makefile for including in the Mozilla build system at $MKFILE"
cat tools/makefile_mpl.txt > $MKFILE
echo "# $WARNING" >> $MKFILE
echo "SYNC_JAVA_FILES := $(echo $SOURCEFILES | xargs)" >> $MKFILE
echo "SYNC_PP_JAVA_FILES := $(echo $PREPROCESS_FILES | xargs)" >> $MKFILE
echo "SYNC_THIRDPARTY_JAVA_FILES := $(echo $HTTPLIBFILES $JSONLIBFILES $APACHEFILES | xargs)" >> $MKFILE
echo "SYNC_RES_DRAWABLE := $(find res/drawable       -not -name 'icon.png' \( -name '*.xml' -or -name '*.png' \) | sed 's,res/,mobile/android/base/resources/,' | xargs)" >> $MKFILE
echo "SYNC_RES_DRAWABLE_LDPI := $(find res/drawable-ldpi  -not -name 'icon.png' \( -name '*.xml' -or -name '*.png' \) | sed 's,res/,mobile/android/base/resources/,' | xargs)" >> $MKFILE
echo "SYNC_RES_DRAWABLE_MDPI := $(find res/drawable-mdpi  -not -name 'icon.png' \( -name '*.xml' -or -name '*.png' \) | sed 's,res/,mobile/android/base/resources/,' | xargs)" >> $MKFILE
echo "SYNC_RES_DRAWABLE_HDPI := $(find res/drawable-hdpi  -not -name 'icon.png' \( -name '*.xml' -or -name '*.png' \) | sed 's,res/,mobile/android/base/resources/,' | xargs)" >> $MKFILE
echo "SYNC_RES_LAYOUT := $(find res/layout -name '*.xml' | xargs)"  >> $MKFILE
echo "SYNC_RES_VALUES := res/values/sync_styles.xml" >> $MKFILE
echo "SYNC_RES_VALUES_LARGE_V11 := res/values-large-v11/sync_styles.xml" >> $MKFILE
# XML resources that do not need to be preprocessed.
echo "SYNC_RES_XML :=" >> $MKFILE
# XML resources that need to be preprocessed.
echo "SYNC_PP_RES_XML := res/xml/sync_syncadapter.xml res/xml/sync_options.xml res/xml/sync_authenticator.xml" >> $MKFILE

# Finished creating Makefile for Mozilla.

true > $SERVICES/preprocess-sources.mn
for f in $PREPROCESS_FILES ; do
    echo $f >> $SERVICES/preprocess-sources.mn
done

echo "Writing README."
echo $WARNING > $ANDROID/base/sync/README.txt
echo $WARNING > $ANDROID/base/httpclientandroidlib/README.txt
true > $SERVICES/java-sources.mn
for f in $SOURCEFILES ; do
    echo $f >> $SERVICES/java-sources.mn
done

true > $SERVICES/java-third-party-sources.mn
for f in $HTTPLIBFILES $JSONLIBFILES $APACHEFILES ; do
    echo $f >> $SERVICES/java-third-party-sources.mn
done

echo "Copying resources..."
# I'm guessing these go here.
rsync -a --exclude "icon.png" res/drawable $ANDROID/base/resources/
rsync -a --exclude "icon.png" res/drawable-hdpi $ANDROID/base/resources/
rsync -a --exclude "icon.png" res/drawable-mdpi $ANDROID/base/resources/
rsync -a --exclude "icon.png" res/drawable-ldpi $ANDROID/base/resources/
rsync -a res/layout/*.xml $ANDROID/base/resources/layout/
rsync -a res/values/sync_styles.xml $ANDROID/base/resources/values/
rsync -a res/values-large-v11/sync_styles.xml $ANDROID/base/resources/values-large-v11/
rsync -a res/xml/*.xml $ANDROID/base/resources/xml/
rsync -a strings/strings.xml.in $SERVICES/
rsync -a strings/sync_strings.dtd.in $ANDROID/base/locales/en-US/sync_strings.dtd

echo "res/values/sync_styles.xml " > $SERVICES/android-values-resources.mn
echo "res/values-large-v11/sync_styles.xml " > $SERVICES/android-values-resources.mn
find res/layout         -name '*.xml' > $SERVICES/android-layout-resources.mn
find res/drawable       -not -name 'icon.png' \( -name '*.xml' -or -name '*.png' \) | sed "s,res/,mobile/android/base/resources/," > $SERVICES/android-drawable-resources.mn
find res/drawable-ldpi  -not -name 'icon.png' \( -name '*.xml' -or -name '*.png' \) | sed "s,res/,mobile/android/base/resources/," > $SERVICES/android-drawable-ldpi-resources.mn
find res/drawable-mdpi  -not -name 'icon.png' \( -name '*.xml' -or -name '*.png' \) | sed "s,res/,mobile/android/base/resources/," > $SERVICES/android-drawable-mdpi-resources.mn
find res/drawable-hdpi  -not -name 'icon.png' \( -name '*.xml' -or -name '*.png' \) | sed "s,res/,mobile/android/base/resources/," > $SERVICES/android-drawable-hdpi-resources.mn
# We manually manage res/xml in the Fennec Makefile.

# These seem to get copied anyway.
rm $ANDROID/base/resources/xml/sync_authenticator.xml
rm $ANDROID/base/resources/xml/sync_syncadapter.xml
rm $ANDROID/base/resources/xml/sync_options.xml

echo "Done."
