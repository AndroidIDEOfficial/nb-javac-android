#!/bin/bash

set -e

# Build langtools
cd make/langtools
ant build
cd -

source $(dirname $0)/setup.sh

ant -f $ant_dir -Dmaven.groupId=io.github.itsaky "$@"
