#!/bin/bash

wrkDir=`pwd`
logFile="t12i.log"
paramFile="t12i.params"

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
    sed "$sedInPlace" "s|OTF_WDIR|$wrkDir|g" "$f"
    sed "$sedInPlace" "s|OTF_PROCS|$DENOPTIMslaveCores|g" "$f"
done

#Run it
exec 6>&1
exec > "$logFile"
exec 2>&1
"$javaDENOPTIM" -jar "$denoptimJar" -r GA "$paramFile"
exec 1>&6 6>&-

#Check outcome

runFolder=$(basename $(ls -lrtd "$wrkDir"/RUN*/ | tail -n 1 | awk '{print $NF}'))

if ! grep -q "\"gRings\": \[$" "$wrkDir/$runFolder"/*/*sdf ; then
    echo " "
    echo "Test 't12i' NOT PASSED (symptom: no cyclic graph)"
    exit 1
fi

if ! grep -q "ADDFUSEDRING.*: done" "$wrkDir"/$runFolder.log
then
    echo " "
    echo "Test 't12i' NOT PASSED (symptom: no ADDFUSEDRING mutation)"
    exit 1
fi

if ! grep -q 'DENOPTIM EA run completed' "$wrkDir"/$runFolder.log
then
    echo " "
    echo "Test 't12i' NOT PASSED (symptom: completion msg not found)"
    exit 1
else
    echo "Test 't12i' PASSED"
fi
exit 0
