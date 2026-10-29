#!/bin/bash
#
# This script is meant to be executed by runAllTests.sh.
# Usually, you should not run this script yourself.
#
# WARNING!
# To run these tests we depend on software Reinvent
# being available in the environment. See
# https://github.com/MolecularAI/REINVENT4
#

wrkDir=`pwd`
logFile="t34.log"
paramFile="t34.params"

if [[ "$(uname)" == CYGWIN* ]] || [[ "$(uname)" == MINGW* ]] || [[ "$(uname)" == MSYS* ]]
then
    echo "Test SKIPPED on Windows"
    exit 0
fi

mv data/* "$wrkDir"
rm -rf data

#Adjust path in scripts and parameter files
filesToModify=$(find . -type f | xargs grep -l "OTF")
for f in $filesToModify
do
    # Handle sedInPlace which may contain spaces (BSD sed: -i '')
    if [[ "$sedInPlace" == *"''"* ]]; then
        sed -i '' "s|OTF_WDIR|$wrkDir|g" "$f"
        sed -i '' "s|OTF_PROCS|$DENOPTIMslaveCores|g" "$f"
    else
        sed $sedInPlace "s|OTF_WDIR|$wrkDir|g" "$f"
        sed $sedInPlace "s|OTF_PROCS|$DENOPTIMslaveCores|g" "$f"
    fi
done

#Run it
exec 6>&1
exec > "$logFile"
exec 2>&1
"$javaDENOPTIM" -jar "$denoptimJar" -r GA "$paramFile"
exec 1>&6 6>&-

grep -q 'DENOPTIM EA run completed' "$wrkDir"/$runFolder.log
if [[ $? != 0 ]]
then
    echo " "
    echo "Test 't34' NOT PASSED (symptom: completion msg not found)"
    exit 1
else
    echo "Test 't34' PASSED"
fi
exit 0

