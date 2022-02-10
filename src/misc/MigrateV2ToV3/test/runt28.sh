#!/bin/bash
export DENOPTIM_HOME="$(cd ../../../.. ; pwd)"
export javaDENOPTIM="java"
export DENOPTIMJarFiles="$DENOPTIM_HOME/build"

wrkDir=`pwd`

"$javaDENOPTIM" -jar "$DENOPTIMJarFiles/MigrateV2ToV3.jar" t28.params

exit 0
