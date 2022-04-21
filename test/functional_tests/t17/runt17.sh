#!/bin/bash

wrkDir=`pwd`
logFile="t17.log"
paramFile="t17.params"

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
"$javaDENOPTIM" -jar "$DENOPTIMJarFiles"/DenoptimRND.jar "$paramFile"
exec 1>&6 6>&- 

#Check outcome
if [ ! -f "$wrkDir"/RUN*.log ]
then
    echo "Test 't17' NOT PASSED (symptom: log file not found)"
    exit 1
fi
nUIDs=$(grep "INFO: Selecting graph" "$wrkDir"/RUN*.log | awk '{print $5}' | sort -u | wc -l)
if [ "$nUIDs" -ne 26 ]
then
    echo "Test 't17' NOT PASSED (symptom: wrong number of UIDs)"
    exit 1
fi
# In this test we intend to trigger an exception
if ! grep -q "ERROR! Not enough graphs in the collection of evaluated graphs." "$wrkDir"/RUN*.log
then
    echo "Test 't17' NOT PASSED (symptom: did not run out of graphs)"
    exit 1
fi

echo "Test 't17' PASSED"
exit 0

