#!/bin/sh

# This script will setup the java application automatically if you are on linux. 
LIB_DIR="lib"
JARS=$(find $LIB_DIR -name "*.jar" -exec printf :{} \;)

javac -cp ".:lib/*" -Xlint:unchecked FileServer.java

javac -cp ".${JARS}" utils/*.java


javac -cp ".${JARS}" -Xlint:unchecked FileServer.java

# if [ $# -eq 1 ]; then
#  sudo nohup java -cp ".${JARS}" FileServer "$1" > audit.log &
# else
#   sudo nohup java -cp ".${JARS}" FileServer 8000 > audit.log &
# fi
if [ $# -eq 1 ]; then
  sudo java -cp ".${JARS}" FileServer "$1"
else
  sudo  java -cp ".${JARS}" FileServer 8000
fi