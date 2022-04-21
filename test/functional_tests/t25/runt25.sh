#!/bin/bash

wrkDir=`pwd`
logFile="t25.log"
paramFile="t25.params"

wdToDenoptim="$wrkDir/"
if [[ "$(uname)" == CYGWIN* ]] || [[ "$(uname)" == MINGW* ]] || [[ "$(uname)" == MSYS* ]]
then
    wdToDenoptim="$(cd "$wrkDir" ; pwd -W | sed 's/\//\\\\/g')\\\\"
fi

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
"$javaDENOPTIM" -jar "$denoptimJar" -r FIT "$paramFile"
exec 1>&6 6>&- 


#Check outcome
if ! grep -q 'Completed FitnessRunner' "$wrkDir"/t25.log
then
    echo " "
    echo "Test 't25' NOT PASSED (symptom: completion msg not found)"
    exit 1
fi

if [ ! -f "$wrkDir"/t25_out.sdf ]
then
    echo " "
    echo "Test 't25' NOT PASSED (symptom: t25_out.sdf file not found)"
    exit 1
fi
if ! grep -q 'FITNESS' "$wrkDir"/t25_out.sdf
then
    echo " "
    echo "Test 't25' NOT PASSED (symptom: fitness tag not found)"
    exit 1
fi
if ! grep -A 1 'FITNESS' "$wrkDir"/t25_out.sdf | grep -q "^0\.9117"
then
    echo " "
    echo "Test 't25' NOT PASSED (symptom: wrong fitness value)"
    exit 1
fi

echo "Test 't25' PASSED"
exit 0
