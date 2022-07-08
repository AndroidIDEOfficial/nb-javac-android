#!/bin/bash

set -e

JDK_8="/usr/lib/jvm/java-8-openjdk-amd64"
if [ -d "$JDK_8" ]; then
  export JAVA_HOME=$JDK_8
fi

echo "Using JAVA_HOME: $JAVA_HOME"

ant -f ./make/langtools/netbeans/nb-javac "$@"
