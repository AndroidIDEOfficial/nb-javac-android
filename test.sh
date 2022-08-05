#!/bin/bash

set -e

source $(dirname $0)/setup_base.sh
source $(dirname $0)/setup.sh

ant -f $ant_dir -Dnb.internal.action.name=test -Dignore.failing.tests=true test