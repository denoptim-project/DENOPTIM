#!/bin/bash

wrkDir=`pwd`
logFile="t12h.log"
paramFile="t12h.params"

wdToDenoptim="$wrkDir/"

mv data/* "$wrkDir"
rm -rf data

#Adjust path in scripts and parameter files
filesToModify=$(find . -type f | xargs grep -l "OTF")
for f in $filesToModify
do
    sed "$sedInPlace" "s|OTF_WDIR\/|$wdToDenoptim|g" "$f"
    sed "$sedInPlace" "s|OTF_WDIR|$wdToDenoptim|g" "$f"
    sed "$sedInPlace" "s|OTF_PROCS|$DENOPTIMslaveCores|g" "$f"
done

#Run it
exec 6>&1
exec > "$logFile"
exec 2>&1
"$javaDENOPTIM" -jar "$denoptimJar" -r GA "$paramFile"
exec 1>&6 6>&- 

#Check outcome
nFromFragmentation=$(grep -l "INITIAL_MOL_FRAGMENTED" "$wrkDir/"RUN*/Gen*/*.sdf | wc -l)

if [[ 7 -ne "$nFromFragmentation" ]]; then
   echo " "
   echo "Test 't12h' NOT PASSED (Found $nFromFragmentation instead of 7 candidates build from fragmentation)"
   exit 1
fi

nTemplated=$(grep -l "Template" "$wrkDir/"RUN*/Gen0/*.sdf | wc -l)
if [[ 0 -ne "$nTemplated" ]]; then
   echo " "
   echo "Test 't12h' NOT PASSED (Found $nTemplated instead of 0 candidates build from fragmentation)"
   exit 1
fi

grep -q 'DENOPTIM EA run completed' "$wrkDir"/RUN*.log
if [[ $? != 0 ]]
then
    echo " "
    echo "Test 't12h' NOT PASSED (symptom: completion msg not found)"
    exit 1
else
    echo "Test 't12h' PASSED"
fi
exit 0

