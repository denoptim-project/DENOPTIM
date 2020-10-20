#!/bin/bash

wrkDir=`pwd`
logFile="t98.log"
paramFile="t98.params"

mv data/* "$wrkDir"
rm -rf data

#Adjust path in scripts and parameter files
filesToModify=$(find . -type f | xargs grep -l "OTF")
for f in $filesToModify
do
    sed "$sedInPlace" "s|OTF_WDIR|$wrkDir|g" "$f"
    sed "$sedInPlace" "s|OTF_PROCS|$DENOPTIMslaveCores|g" "$f"
done

#Run it
exec 6>&1
exec > "$logFile"
exec 2>&1
"$javaDENOPTIM" -jar "$DENOPTIMJarFiles/TestOperator.jar" "$paramFile"
exec 1>&6 6>&- 

#Check outcome
if ! grep -q 'TestOperator run completed' "$wrkDir"/t98.log
then
    echo " "
    echo "Test 't98' NOT PASSED (symptom: completion msg not found)"
    exit 1
else
    echo " "
    echo "WARNING: We do not yet check the outcome of this test run."
    echo " "
fi
exit 0
