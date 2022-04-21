#!/bin/bash

wrkDir=`pwd`
logFile="t14.log"
paramFile="t14.params"

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
"$javaDENOPTIM" -jar "$denoptimJar" -r GO "$paramFile"
exec 1>&6 6>&- 

#Check outcome
if \[ ! -f "$wrkDir/male_xo.sdf" \] || \[ ! -f "$wrkDir/female_xo.sdf" \]
then
    echo " "
    echo "Test 't14' NOT PASSED (symptom: output file/s not found)"
    exit 1
fi

exec 6>&1
exec > "${logFile}_check-1"
exec 2>&1
"$javaDENOPTIM" -jar "$denoptimJar" -r CLG t14-check-1.params
exec 1>&6 6>&-

if ! grep -q "#Matches in list A: 1/1" "${logFile}_check-1"
then
    echo " "
    echo "Test 't14' NOT PASSED (symptom: Graph differs in male_xo.sdf)"
    exit 1
fi
if ! grep -q "#Matches in list B: 1/1" "${logFile}_check-1"
then
    echo " "
    echo "Test 't14' NOT PASSED (symptom: Graph differs in male_xo.sdf)"
    exit 1
fi

exec 6>&1
exec > "${logFile}_check-2"
exec 2>&1
"$javaDENOPTIM" -jar "$denoptimJar" -r CLG t14-check-2.params
exec 1>&6 6>&-

if ! grep -q "#Matches in list A: 1/1" "${logFile}_check-2"
then
    echo " "
    echo "Test 't14' NOT PASSED (symptom: Graph differs in female_xo.sdf)"
    exit 1
fi
if ! grep -q "#Matches in list B: 1/1" "${logFile}_check-2"
then
    echo " "
    echo "Test 't14' NOT PASSED (symptom: Graph differs in female_xo.sdf)"
    exit 1
fi

if ! grep -q 'Completed GeneOpsRunner' "$wrkDir"/t14.log
then
    echo " "
    echo "Test 't14' NOT PASSED (symptom: completion msg not found)"
    exit 1
else
    echo "Test 't14' PASSED"
fi
exit 0

