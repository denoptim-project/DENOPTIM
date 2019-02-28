#!/bin/bash

wrkDir=`pwd`
logFile="t7.log"
paramFile="t7.params"

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
"$javaDENOPTIM" -jar "$DENOPTIMJarFiles"/SerConverter.jar "$paramFile"
exec 1>&6 6>&- 

#Check outcome
grep -q ' 16 15  0  0  .  0  0  0  0 ' "$wrkDir"/dg_96.sdf
if [[ $? != 0 ]]
then
    echo " "
    echo "Test 't7' NOT PASSED (symptom: SDF not correct or not found)"
    exit -1
else
    echo "Test 't7' PASSED"
fi

exit 0

