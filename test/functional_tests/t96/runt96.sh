#!/bin/bash

wrkDir=`pwd`
logFile="t96.log"
paramFile="t96.params"

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
echo " "
echo "WARNING: We do not yet check the outcome of this test run."
echo " "

exit 0
