if [ -z "$ANDROID" ]
then
  echo "No ANDROID dir set. Aborting."
  exit 1;
fi
  
echo "Copying to $ANDROID ($SYNC)..."

WARNING="These files are managed in the android-sync repo. Do not modify directly, or your changes will be lost."
echo "Creating README.txt."
echo $WARNING > $SYNC/README.txt

echo "Copying manifests..."
rsync -a manifests $SYNC/

echo "Copying sources. All use of R must be compiled with Fennec."
SOURCEDIR="src/main/java/org/mozilla/gecko/sync"
SOURCEFILES=$(find "$SOURCEDIR" -name '*.java' -not -name 'GlobalConstants.java' -and -not -name 'BrowserContract.java' | sed "s,$SOURCEDIR/,sync/,")
rsync -C --exclude 'GlobalConstants.java' --exclude 'BrowserContract.java' --exclude '*.in' -a $SOURCEDIR $ANDROID/base/

echo "Copying preprocessed GlobalConstants file."
PREPROCESS_FILES="sync/GlobalConstants.java"
cp $SOURCEDIR/GlobalConstants.java.in $ANDROID/base/sync/

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

# Creating Makefile for Mozilla.
MKFILE=$ANDROID/base/android-sync-files.mk
echo "Creating makefile for including in the Mozilla build system at $MKFILE"
cat tools/makefile_mpl.txt > $MKFILE
echo "# $WARNING" >> $MKFILE
echo "SYNC_JAVA_FILES := $(echo $SOURCEFILES | xargs)" >> $MKFILE
echo "SYNC_PP_JAVA_FILES := $(echo $PREPROCESS_FILES | xargs)" >> $MKFILE
echo "SYNC_THIRDPARTY_JAVA_FILES := $(echo $HTTPLIBFILES $JSONLIBFILES $APACHEFILES | xargs)" >> $MKFILE
echo "SYNC_RES_DRAWABLE := $(find res/drawable       -name '*.xml' -or -name '*.png' | sed 's,res/,mobile/android/base/resources/,' | xargs)" >> $MKFILE
echo "SYNC_RES_DRAWABLE_LDPI := $(find res/drawable-ldpi  -name '*.xml' -or -name '*.png' | sed 's,res/,mobile/android/base/resources/,' | xargs)" >> $MKFILE
echo "SYNC_RES_DRAWABLE_MDPI := $(find res/drawable-mdpi  -name '*.xml' -or -name '*.png' | sed 's,res/,mobile/android/base/resources/,' | xargs)" >> $MKFILE
echo "SYNC_RES_DRAWABLE_HDPI := $(find res/drawable-hdpi  -name '*.xml' -or -name '*.png' | sed 's,res/,mobile/android/base/resources/,' | xargs)" >> $MKFILE
echo "SYNC_RES_LAYOUT := $(find res/layout -name '*.xml' | xargs)"  >> $MKFILE
echo "SYNC_RES_VALUES := res/values/sync_styles.xml" >> $MKFILE
# Finished creating Makefile for Mozilla.

true > $SYNC/preprocess-sources.mn
for f in $PREPROCESS_FILES ; do
    echo $f >> $SYNC/preprocess-sources.mn
done

echo "Writing README."
echo $WARNING > $ANDROID/base/sync/README.txt
echo $WARNING > $ANDROID/base/httpclientandroidlib/README.txt
true > $SYNC/java-sources.mn
for f in $SOURCEFILES ; do
    echo $f >> $SYNC/java-sources.mn
done

true > $SYNC/java-third-party-sources.mn
for f in $HTTPLIBFILES $JSONLIBFILES $APACHEFILES ; do
    echo $f >> $SYNC/java-third-party-sources.mn
done

echo "Copying resources..."
# I'm guessing these go here.
rsync -a res/drawable $ANDROID/base/resources/
rsync -a res/drawable-hdpi $ANDROID/base/resources/
rsync -a res/drawable-mdpi $ANDROID/base/resources/
rsync -a res/drawable-ldpi $ANDROID/base/resources/
rsync -a res/layout/*.xml $ANDROID/base/resources/layout/
rsync -a res/layout/*.xml $ANDROID/base/resources/layout/
rsync -a res/values/sync_styles.xml $ANDROID/base/resources/values/
rsync -a res/xml/*.xml $ANDROID/base/resources/xml/
rsync -a strings/strings.xml.in $SYNC/
rsync -a strings/sync_strings.dtd.in $ANDROID/base/locales/en-US/sync_strings.dtd

echo "res/values/sync_styles.xml " > $SYNC/android-values-resources.mn
find res/layout         -name '*.xml' > $SYNC/android-layout-resources.mn
find res/drawable       -name '*.xml' -or -name '*.png' | sed "s,res/,mobile/android/base/resources/," > $SYNC/android-drawable-resources.mn
find res/drawable-ldpi  -name '*.xml' -or -name '*.png' | sed "s,res/,mobile/android/base/resources/," > $SYNC/android-drawable-ldpi-resources.mn
find res/drawable-mdpi  -name '*.xml' -or -name '*.png' | sed "s,res/,mobile/android/base/resources/," > $SYNC/android-drawable-mdpi-resources.mn
find res/drawable-hdpi  -name '*.xml' -or -name '*.png' | sed "s,res/,mobile/android/base/resources/," > $SYNC/android-drawable-hdpi-resources.mn
# We manually manage res/xml in the Fennec Makefile.

# These seem to get copied anyway.
rm $ANDROID/base/resources/xml/sync_syncadapter.xml
rm $ANDROID/base/resources/xml/sync_options.xml

echo "Done."
