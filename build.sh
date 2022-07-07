#!/bin/bash

set -e

JDK_8="/usr/lib/jvm/java-8-openjdk-amd64"
if [ -d "$JDK_8" ]; then
  export JAVA_HOME=$JDK_8
fi

ant -f ./make/langtools/netbeans/nb-javac "$@"
