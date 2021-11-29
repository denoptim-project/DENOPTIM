#!/bin/bash

wrkDir=`pwd`
logFile="t27.log"
paramFile="t27.params"

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
"$javaDENOPTIM" -jar "$DENOPTIMJarFiles/DenoptimGA.jar" "$paramFile"
exec 1>&6 6>&-

#Check outcome
runFolder=$(basename $(ls -lrtd "$wrkDir"/RUN*/ | tail -n 1 | awk '{print $NF}'))
n28=$(grep -l M00000028 "$wrkDir/$runFolder/"*/Gen*.txt | wc -l | awk '{print $1}')
n31=$(grep -l M00000031 "$wrkDir/$runFolder/"*/Gen*.txt | wc -l | awk '{print $1}')
nMAX28=$(grep -l "MAX: *28.000" "$wrkDir/$runFolder/"*/Gen*.txt | wc -l | awk '{print $1}')
if [ "$n28" -lt 7 ] || [ "$n28" -gt 10 ]; then
    echo " "
    echo "Test 't27' NOT PASSED (Lack of reproducibility - symptom: wrong number of populations including M00000028: $n28)"
    exit 1
fi
if [ "$n31" -lt 7 ] || [ "$n31" -gt 10 ]; then
    echo " "
    echo "Test 't27' NOT PASSED (Lack of reproducibility - symptom: wrong number of populations including M00000031: $n31)"
    exit 1
fi
if [ "$nMAX28" -lt 6 ] || [ "$nMAX28" -gt 9 ]; then
    echo " "
    echo "Test 't27' NOT PASSED (Lack of reproducibility - symptom: wrong number of generation with max fitness from manually added molecule: $nMAX28)"
    exit 1
fi

if ! grep -q "Removing 'M00000028 M00000031' upon external request" "$wrkDir"/$runFolder.log
then
    echo " "
    echo "Test 't27' NOT PASSED (symptom: log reports no removal task)"
    exit 1
fi

if ! grep -q 'GA run will be stopped upon external request' "$wrkDir"/$runFolder.log
then
    echo " "
    echo "Test 't27' NOT PASSED (symptom: log reports no stop task)"
    exit 1
fi

if ! grep -q 'DENOPTIM EA run stopped' "$wrkDir"/$runFolder.log
then
    echo " "
    echo "Test 't27' NOT PASSED (symptom: completion msg not found)"
    exit 1
else
    echo "Test 't27' PASSED"
fi
exit 0
