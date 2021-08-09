#!/bin/bash

wrkDir=`pwd`
logFile="t25.log"
paramFile="t25.params"

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
"$javaDENOPTIM" -jar "$DENOPTIMJarFiles/FitnessRunner.jar" "$paramFile"
exec 1>&6 6>&- 


echo TODO checks
echo " "
cat "$logFile"
exit 0

#Check outcome
if ! grep -q 'FitnessRunner run completed' "$wrkDir"/t25.log
then
    echo " "
    echo "Test 't25' NOT PASSED (symptom: completion msg not found)"
    exit 1
fi

echo "Test 't25' PASSED"
exit 0
