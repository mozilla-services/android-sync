# Shift $DESTDIR directory from arguments list;
# set $ANDROID sub-directory;
# set and if necessary create $SERVICES sub-directory.

DESTDIR=$1
if [ -z "$DESTDIR" ]; then
  echo "No destination directory specified."
  exit 1
fi
shift

ANDROID=$DESTDIR/mobile/android
if [ ! -d $ANDROID ]; then
  echo "No Android dir."
  exit 1
fi

SERVICES=$DESTDIR/mobile/android/services
if [ -d $SERVICES ]; then
  echo "Services directory already exists."
else
  echo "No Services directory. Creating directory structure."
  mkdir -p $SERVICES
fi
