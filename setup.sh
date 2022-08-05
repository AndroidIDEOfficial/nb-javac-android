#!/bin/bash

export ant_dir=./make/langtools/netbeans/nb-javac

# Build nb-javac-android
JDK_8="/usr/lib/jvm/java-8-openjdk-amd64"

if [[ -z "$CI" ]]; then
  JDK_8=$JAVA_8
fi

if [ -d "$JDK_8" ]; then
  export JAVA_HOME=$JDK_8
fi

echo "Using JAVA_HOME: $JAVA_HOME"