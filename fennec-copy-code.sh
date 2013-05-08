DIR=$(dirname "$0")
if [[ ! -d "$DIR" ]]; then DIR="$PWD"; fi
. "$DIR/fennec-paths.sh"
  
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
SOURCEFILES=$(find "$BACKGROUNDSOURCEDIR" "$SYNCSOURCEDIR" \
  -name '*.java' \
  -and -not -name 'AnnouncementsConstants.java' \
  -and -not -name 'GlobalConstants.java' \
  -and -not -name 'BrowserContract.java' \
  -and -not -name 'AppConstants.java' \
  -and -not -name 'SyncConstants.java' \
  | sed "s,$SOURCEROOT/,,")

rsync -C \
  --exclude 'AppConstants.java' \
  --exclude 'SyncConstants.java' \
  --exclude 'BrowserContract.java' \
  --exclude '*.in' \
  -a $SYNCSOURCEDIR $ANDROID/base/

rsync -C \
  --exclude 'AppConstants.java' \
  --exclude 'GlobalConstants.java' \
  --exclude 'AnnouncementsConstants.java' \
  --exclude '*.in' \
  -a $BACKGROUNDSOURCEDIR $ANDROID/base/

echo "Copying preprocessed constants files."
PREPROCESS_FILES="background/common/GlobalConstants.java sync/SyncConstants.java background/announcements/AnnouncementsConstants.java"
cp $BACKGROUNDSOURCEDIR/common/GlobalConstants.java.in $ANDROID/base/background/common/
cp $SYNCSOURCEDIR/SyncConstants.java.in $ANDROID/base/sync/
cp $BACKGROUNDSOURCEDIR/announcements/AnnouncementsConstants.java.in $ANDROID/base/background/announcements/

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

# Write a list of files to a Makefile variable.
# turn
# VAR:=1.java 2.java
# into
# VAR:=\
#   1.java \
#   2.java \
#   $(NULL)
function dump_mkfile_variable {
    output_file=$MKFILE
    variable_name=$1
    shift

    echo "$variable_name := \\" >> $output_file
    for var in "$@" ; do
        for f in $var ; do
            echo "  $f \\" >> $output_file
        done
    done
    echo "  \$(NULL)" >> $output_file
    echo "" >> $output_file
}

# Prefer PNGs in drawable-*: Android lint complains about PNG files in drawable.
SYNC_RES_DRAWABLE=$(find res/drawable -not -name 'icon.png' -not -name 'ic_status_logo.png' \( -name '*.xml' \) | sed 's,res/,mobile/android/base/resources/,')

SYNC_RES_DRAWABLE_LDPI=$(find res/drawable-ldpi  -not -name 'icon.png' -not -name 'ic_status_logo.png' \( -name '*.xml' -or -name '*.png' \) | sed 's,res/,mobile/android/base/resources/,')
SYNC_RES_DRAWABLE_MDPI=$(find res/drawable-mdpi  -not -name 'icon.png' -not -name 'ic_status_logo.png' \( -name '*.xml' -or -name '*.png' \) | sed 's,res/,mobile/android/base/resources/,')
SYNC_RES_DRAWABLE_HDPI=$(find res/drawable-hdpi  -not -name 'icon.png' -not -name 'ic_status_logo.png' \( -name '*.xml' -or -name '*.png' \) | sed 's,res/,mobile/android/base/resources/,')

SYNC_RES_LAYOUT=$(find res/layout -name '*.xml')
SYNC_RES_VALUES="res/values/sync_styles.xml"
SYNC_RES_VALUES_V11="res/values-v11/sync_styles.xml"
SYNC_RES_VALUES_LARGE_V11="res/values-large-v11/sync_styles.xml"
# XML resources that do not need to be preprocessed.
SYNC_RES_XML=""
# XML resources that need to be preprocessed.
SYNC_PP_RES_XML="res/xml/sync_syncadapter.xml res/xml/sync_options.xml res/xml/sync_authenticator.xml"

dump_mkfile_variable "SYNC_PP_JAVA_FILES" "$PREPROCESS_FILES"
dump_mkfile_variable "SYNC_JAVA_FILES" "$SOURCEFILES"

dump_mkfile_variable "SYNC_RES_DRAWABLE" "$SYNC_RES_DRAWABLE"
dump_mkfile_variable "SYNC_RES_DRAWABLE_LDPI" "$SYNC_RES_DRAWABLE_LDPI"
dump_mkfile_variable "SYNC_RES_DRAWABLE_MDPI" "$SYNC_RES_DRAWABLE_MDPI"
dump_mkfile_variable "SYNC_RES_DRAWABLE_HDPI" "$SYNC_RES_DRAWABLE_HDPI"

dump_mkfile_variable "SYNC_RES_LAYOUT" "$SYNC_RES_LAYOUT"
dump_mkfile_variable "SYNC_RES_VALUES" "$SYNC_RES_VALUES"
dump_mkfile_variable "SYNC_RES_VALUES_V11" "$SYNC_RES_VALUES_V11"
dump_mkfile_variable "SYNC_RES_VALUES_LARGE_V11" "$SYNC_RES_VALUES_LARGE_V11"
dump_mkfile_variable "SYNC_RES_XML" "$SYNC_RES_XML"
dump_mkfile_variable "SYNC_PP_RES_XML" "$SYNC_PP_RES_XML"

dump_mkfile_variable "SYNC_THIRDPARTY_JAVA_FILES" "$HTTPLIBFILES" "$JSONLIBFILES" "$APACHEFILES"

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
rsync -a --exclude 'icon.png' --exclude 'ic_status_logo.png' res/drawable $ANDROID/base/resources/
rsync -a --exclude 'icon.png' --exclude 'ic_status_logo.png' res/drawable-hdpi $ANDROID/base/resources/
rsync -a --exclude 'icon.png' --exclude 'ic_status_logo.png' res/drawable-mdpi $ANDROID/base/resources/
rsync -a --exclude 'icon.png' --exclude 'ic_status_logo.png' res/drawable-ldpi $ANDROID/base/resources/
rsync -a res/layout/*.xml $ANDROID/base/resources/layout/
rsync -a res/values/sync_styles.xml $ANDROID/base/resources/values/
rsync -a res/values-v11/sync_styles.xml $ANDROID/base/resources/values-v11/
rsync -a res/values-large-v11/sync_styles.xml $ANDROID/base/resources/values-large-v11/
rsync -a res/xml/*.xml $ANDROID/base/resources/xml/
rsync -a strings/strings.xml.in $SERVICES/
rsync -a strings/sync_strings.dtd.in $ANDROID/base/locales/en-US/sync_strings.dtd

echo "res/values/sync_styles.xml " > $SERVICES/android-values-resources.mn
echo "res/values-large-v11/sync_styles.xml " > $SERVICES/android-values-resources.mn
find res/layout         -name '*.xml' > $SERVICES/android-layout-resources.mn
find res/drawable       -not -name 'icon.png' -not -name 'ic_status_logo.png' \( -name '*.xml' -or -name '*.png' \) | sed "s,res/,mobile/android/base/resources/," > $SERVICES/android-drawable-resources.mn
find res/drawable-ldpi  -not -name 'icon.png' -not -name 'ic_status_logo.png' \( -name '*.xml' -or -name '*.png' \) | sed "s,res/,mobile/android/base/resources/," > $SERVICES/android-drawable-ldpi-resources.mn
find res/drawable-mdpi  -not -name 'icon.png' -not -name 'ic_status_logo.png' \( -name '*.xml' -or -name '*.png' \) | sed "s,res/,mobile/android/base/resources/," > $SERVICES/android-drawable-mdpi-resources.mn
find res/drawable-hdpi  -not -name 'icon.png' -not -name 'ic_status_logo.png' \( -name '*.xml' -or -name '*.png' \) | sed "s,res/,mobile/android/base/resources/," > $SERVICES/android-drawable-hdpi-resources.mn
# We manually manage res/xml in the Fennec Makefile.

# These seem to get copied anyway.
rm $ANDROID/base/resources/xml/sync_authenticator.xml
rm $ANDROID/base/resources/xml/sync_syncadapter.xml
rm $ANDROID/base/resources/xml/sync_options.xml

echo "Done."
