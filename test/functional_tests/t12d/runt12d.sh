#!/bin/bash

wrkDir=`pwd`
logFile="t12d.log"
paramFile="t12d.params"

wdToDenoptim="$wrkDir/"
if [[ "$(uname)" == CYGWIN* ]] || [[ "$(uname)" == MINGW* ]] || [[ "$(uname)" == MSYS* ]]
then
    wdToDenoptim="$(cd "$wrkDir" ; pwd -W | sed 's/\//\\\\/g')\\\\"
fi

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

runFolder=$(basename $(ls -lrtd "$wrkDir"/RUN*/ | tail -n 1 | awk '{print $NF}'))

numFitFiles=$(find "$runFolder" -type f -name "*_out.sdf" | wc -l)
if [ 5 -ne "$numFitFiles" ]; then
    echo " "
    echo "Test 't12d' NOT PASSED (symptom: expecting 5 fitness file, found $numFitFiles)"
    exit 1
fi

numFitFiles=$(find "$runFolder/Final" -type f -name "*_out.sdf" | wc -l)
if [ 5 -ne "$numFitFiles" ]; then
    echo " "
    echo "Test 't12d' NOT PASSED (symptom: expecting 5 fitness file, found $numFitFiles)"
    exit 1
fi

grep -q 'DENOPTIM EA run completed' "$wrkDir"/$runFolder.log
if [[ $? != 0 ]]
then
    echo " "
    echo "Test 't12d' NOT PASSED (symptom: completion msg not found)"
    exit 1
else
    echo "Test 't12d' PASSED"
fi
exit 0
