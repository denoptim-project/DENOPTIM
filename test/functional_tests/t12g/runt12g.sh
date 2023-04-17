#!/bin/bash

wrkDir=`pwd`
logFile="t12g.log"
paramFile="t12g.params"

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
nFromFragmentation=$(grep -l "INITIAL_MOL_FRAGMENTED" "$wrkDir/"RUN*/Gen0/*.sdf | wc -l)
if [[ 34 -ne "$nFromFragmentation" ]]; then
   echo " "
   echo "Test 't12g' NOT PASSED (Found $nFromFragmentation instead of 34 candidates build from fragmentation)"
   exit 1
fi

maxFitness=$(grep "^MAX:" "$wrkDir/"RUN*/Gen0/Gen0.txt | awk '{print $2}')
if [[ "$maxFitness" != "59"* ]]; then
   echo " "
   echo "Test 't12g' NOT PASSED (Max fitness in Gen0 is $maxFitness instead of 58*)"
   exit 1
fi

minFitness=$(grep "^MIN:" "$wrkDir/"RUN*/Gen0/Gen0.txt | awk '{print $2}')
if [[ "$minFitness" != "43"* ]]; then
   echo " "
   echo "Test 't12g' NOT PASSED (Min fitness in Gen0 is $minFitness instead of 43*)"
   exit 1
fi

maxFitnessMember=$(head -n 2 "$wrkDir/"RUN*/Gen0/Gen0.txt | tail -n 1 | awk '{print $1}')
if [[ "M00000028_out.sdf" -ne "$maxFitnessMember" ]]; then
   echo " "
   echo "Test 't12g' NOT PASSED (Max fitness member in Gen0 is $maxFitnessMember instead of M00000028_out.sdf)"
   exit 1
fi

minFitnessMember=$(head -n 11 "$wrkDir/"RUN*/Gen0/Gen0.txt | tail -n 1 | awk '{print $1}')
if [[ "M00000014_out.sdf" -ne "$maxFitnessMember" ]]; then
   echo " "
   echo "Test 't12g' NOT PASSED (Min fitness member in Gen0 is $minFitnessMember instead of M00000014_out.sdf)"
   exit 1
fi

nTemplated=$(grep -l "Template" "$wrkDir/"RUN*/Gen0/*.sdf | wc -l)
if "$nTemplated" -gt 1 ; then
   echo " "
   echo "Test 't12g' NOT PASSED (Found $nTemplated candidates with Templates)"
   exit 1
fi

nFixedStruct=$(grep -l "FIXED_STRUCT" "$wrkDir/"RUN*/Gen0/*.sdf | wc -l)
if "$nFixedStruct" -gt 1 ; then
   echo " "
   echo "Test 't12g' NOT PASSED (Found $nFixedStruct candidated with fixed-structure Templates)"
   exit 1
fi

grep -q 'DENOPTIM EA run completed' "$wrkDir"/RUN*.log
if [[ $? != 0 ]]
then
    echo " "
    echo "Test 't12g' NOT PASSED (symptom: completion msg not found)"
    exit 1
else
    echo "Test 't12g' PASSED"
fi
exit 0

