#!/bin/bash

# Script that runs a GA experiment with interaction with "the user"...well, in
# this test the "user" is a programmed sequence of events. 
# See data/fitness_provider.sh. 
# NB: you might need to adapt such script  as a consequence of a change in
# the algorithms that makes the exact sequence of candidates not reproducible
# anymore. For example, this will indeed happen when adding a new
# type of genetic operator. In such case, be aware that some candidates never
# reach this point of the fitness evaluation script. This because they are
# rejected due to duplicate UID.

wrkDir=`pwd`
logFile="t27.log"
paramFile="t27.params"

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
# NB: these values must be consistent with the fitness provider script!
candIdTo50="M00000027"
candIdTo40="M00000025"
runFolder=$(basename $(ls -lrtd "$wrkDir"/RUN*/ | tail -n 1 | awk '{print $NF}'))
n50=$(grep -l "$candIdTo50" "$wrkDir/$runFolder/"*/Gen*.txt | wc -l | awk '{print $1}')
n40=$(grep -l "$candIdTo40" "$wrkDir/$runFolder/"*/Gen*.txt | wc -l | awk '{print $1}')
nMAX28=$(grep -l "MAX: *28.000" "$wrkDir/$runFolder/"*/Gen*.txt | wc -l | awk '{print $1}')
# Difficult to get the execution of independent threads. The following checks may fail because of an eccessive loading on the cpus which retard the execution of some tasks resulting in widely different results. Usually, re-running the test leads to its successfull completion.
if [ "$n50" -lt 5 ] || [ "$n50" -gt 15 ]; then
    echo " "
    echo "Test 't27' WARNING: unreproducibile behavior: wrong number of populations including $candIdTo50 ($n50). Try re-running t27"
fi
if [ "$n40" -lt 5 ] || [ "$n40" -gt 15 ]; then
    echo " "
    echo "Test 't27' WARNING: unreproducibile behavior: wrong number of populations including "$candIdTo40" ($n40). Try re-running t27"
fi
if [ "$nMAX28" -lt 4 ]; then
    echo " "
    echo "Test 't27' WARNING: unreproducibile behavior: wrong number of generation with max fitness from manually added molecule ($nMAX28). Try re-running t27."
fi

if ! grep -q "Removing '$candIdTo50 $candIdTo40' upon external request" "$wrkDir"/$runFolder.log
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
