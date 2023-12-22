LIB_DIR="lib"
JARS=$(find $LIB_DIR -name "*.jar" -exec printf :{} \;)

javac -cp ".:lib/*" -Xlint:unchecked FileServer.java

javac -cp ".${JARS}" utils/*.java


javac -cp ".${JARS}" -Xlint:unchecked FileServer.java

if [ $# -eq 1 ]; then
  java -cp ".${JARS}" FileServer "$1"
else
  java -cp ".${JARS}" FileServer 8000
fi
