#!/bin/bash

if [ -d "$JDK_8" ]; then
  export JAVA_HOME=$JDK_8
fi

echo "Using JAVA_HOME: $JAVA_HOME"