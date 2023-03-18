#!/bin/bash

wrkDir=`pwd`
logFile="t12e.log"
paramFile="t12e.params"

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
genSummaryFiles=()
while IFS=  read -r -d $'\0'; do
    genSummaryFiles+=("$REPLY")
done < <(find "$runFolder" -name "Gen*.txt" -print0)
for i in $(seq 0 $((${#genSummaryFiles[@]}-2)))
do
    membersGenJ=$(grep "M00" ${genSummaryFiles[$((i+1))]} | cut -c 1-17)
    for memberinGenI in $(grep "M00" ${genSummaryFiles[$i]} | cut -c 1-17); 
    do 
        if [[ "$membersGenJ" =~ "${membersGenI}" ]]; then
            echo "NOT PASSED (symptom: parent $membersGeni survived)"
            exit -1
        fi
    done 
done

grep -q 'DENOPTIM EA run completed' "$wrkDir"/$runFolder.log
if [[ $? != 0 ]]
then
    echo " "
    echo "Test 't12e' NOT PASSED (symptom: completion msg not found)"
    exit 1
else
    echo "Test 't12e' PASSED"
fi
exit 0
