#!/bin/bash

wrkDir=`pwd`
logFile="t18.log"
paramFile="t18.params"

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
"$javaDENOPTIM" -jar "$DENOPTIMJarFiles"/GraphListsHandler.jar "$paramFile"
exec 1>&6 6>&- 

#Check outcome
if [ ! -f "$wrkDir"/t18.log ]
then
    echo "Test 't18' NOT PASSED (symptom: log file not found)"
    exit 1
fi

#TODO

echo "Test 't18' PASSED"
exit 0

