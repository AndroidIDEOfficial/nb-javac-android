#!/bin/bash

set -e

source $(dirname $0)/setup_base.sh

# Build langtools
cd make/langtools
ant build
cd -

source $(dirname $0)/setup.sh

ant -f $ant_dir -Dmaven.groupId=io.github.itsaky "$@"
