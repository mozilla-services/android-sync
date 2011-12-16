./build-authorities.sh
./build-manifest.sh
./build-strings.sh
mvn clean
mvn install
mvn assembly:assembly
