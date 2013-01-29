DIR=$(dirname "$0")
if [[ ! -d "$DIR" ]]; then DIR="$PWD"; fi
. "$DIR/fennec-paths.sh"

echo "Preprocessing."
./preprocess.sh

mvn clean compile || { echo 'mvn clean compile failed'; exit 1; }

if [ "$1" = "notests" ]
then
  echo "Not running tests."
else
  echo "Running tests."
  mvn test || { echo 'mvn test failed'; exit 1; }
fi
shift

echo "Copying."
./fennec-copy-code.sh $DESTDIR
