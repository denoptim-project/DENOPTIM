#!/bin/bash

wrkDir=`pwd`
logFile="t12f.log"
paramFile="t12f.params"

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

grep "Replacing .* with its sibling " "$runFolder.log" | awk '{print $3" "$7}' | while read pair 
do
    molI=$(echo $pair | awk '{print $1}')
    molJ=$(echo $pair | awk '{print $2}')
    fileI=$(find "$runFolder"/Gen* -name "${molI}_out.sdf")
    fileJ=$(find "$runFolder"/Gen* -name "${molJ}_out.sdf")
    fitI=$(grep -A 1 FITNESS "$fileI" | tail -n 1 | awk -F'.' '{print $1}')
    fitJ=$(grep -A 1 FITNESS "$fileJ" | tail -n 1 | awk -F'.' '{print $1}')
    if [ "$fitI" -ge "$fitJ" ]; then
        echo "NOT PASSED (sympton: should not replace $molI with $molJ because finess values are $fitI and $fitJ, respectively)"
        exit -1
    fi
done

counter=$(grep -c "Replacing .* with its sibling " "$runFolder.log")
if [ 3 -gt $counter ]; then
    echo "NOT PASSED (sympton: too few sibling replacements ($counter)"
    exit -1
fi

grep -q 'DENOPTIM EA run completed' "$wrkDir"/$runFolder.log
if [[ $? != 0 ]]
then
    echo " "
    echo "Test 't12f' NOT PASSED (symptom: completion msg not found)"
    exit 1
else
    echo "Test 't12f' PASSED"
fi
exit 0
