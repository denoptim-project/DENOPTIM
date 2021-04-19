#!/bin/bash

wrkDir=`pwd`
logFile="t13.log"
paramFile="t13.params"

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
"$javaDENOPTIM" -jar "$DENOPTIMJarFiles"/DenoptimGA.jar "$paramFile"
exec 1>&6 6>&- 

#Check outcome
ls "$wrkDir/RUN*/Gen*/*out.sdf" | while read f 
do 
    hasNull=$(grep -A1 "GraphENC" "$f" | grep -q "null") 
    if [[ $? == 0 ]] 
    then 
        echo " "
        echo "Test 't13' NOT PASSED (symptom: null in file $f)"
        exit 1
    fi
    hasNoSymmSet=$(grep -A1 "GraphENC" "$f" | grep -q "SymmetricSet")
    if [[ $? == 1 ]]
    then
        echo " "
        echo "Test 't13' NOT PASSED (symptom: no SymmetricSet in file $f)"
        exit 1
    fi
done 

grep -q 'DENOPTIM EA run completed' "$wrkDir"/RUN*.log
if [[ $? != 0 ]]
then
    echo " "
    echo "Test 't13' NOT PASSED (symptom: completion msg not found)"
    exit 1
else
    echo "Test 't13' PASSED"
fi
exit 0

