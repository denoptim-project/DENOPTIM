#!/bin/bash

wrkDir=`pwd`

mv data/* "$wrkDir"
rm -rf data

#Adjust path in scripts and parameter files
filesToModify=$(find . -type f | xargs grep -l "OTF")
for f in $filesToModify
do
    sed "$sedInPlace" "s|OTF_WDIR|$wrkDir|g" "$f"
    sed "$sedInPlace" "s|OTF_PROCS|$DENOPTIMslaveCores|g" "$f"
done

#Run first step
exec 6>&1
exec > "t19.log_1"
exec 2>&1
"$javaDENOPTIM" -jar "$DENOPTIMJarFiles/StringConverter.jar" t19.params_1
exec 1>&6 6>&- 

#Check outcome
if ! grep -q 'StringConverter run completed' "$wrkDir"/t19.log_1
then
    echo " "
    echo "Test 't19' NOT PASSED (symptom: completion msg not found - step 1)"
    exit 1
fi
if [ ! -f "intermediate.txt" ]
then
    echo " "
    echo "Test 't19' NOT PASSED (symptom: missing intermediate file, i.e., output of 1st step)"
    exit -1
fi

exec 6>&1
exec > "t19.log_2"
exec 2>&1
"$javaDENOPTIM" -jar "$DENOPTIMJarFiles/StringConverter.jar" t19.params_2
exec 1>&6 6>&-

#Check outcome
if ! grep -q 'StringConverter run completed' "$wrkDir"/t19.log_2
then
    echo " "
    echo "Test 't19' NOT PASSED (symptom: completion msg not found - step 2)"
    exit 1
fi
if [ ! -f "final.sdf" ]
then
    echo " "
    echo "Test 't19' NOT PASSED (symptom: missing final sdf file)"
    exit -1
fi

naref=$(head -n 4 reference.sdf | tail -n 1 | awk '{print $1}')
nbref=$(head -n 4 reference.sdf | tail -n 1 | awk '{print $1}')
na=$(head -n 4 final.sdf | tail -n 1 | awk '{print $1}')
nb=$(head -n 4 final.sdf | tail -n 1 | awk '{print $1}')

if [[ "$na" != "$naref" ]] ; then
    echo "Test 't19' NOT PASSED (symptom: wrong number of atoms)."
    exit -1
fi
if [[ "$nb" != "$nbref" ]] ; then
    echo "Test 't19' NOT PASSED (symptom: wrong number of bonds)."
    exit -1
fi


nref=$(grep -n "^M *END" reference.sdf | awk -F':' '{print $1}')
n=$(grep -n "^M *END" final.sdf | awk -F':' '{print $1}')

head -n "$nref" reference.sdf | tail -n +4 > cnRef
head -n "$n" final.sdf | tail -n +4 > cn

differences=$(diff cn cnref)
if [ "$differences" != "" ]; then
    echo "Test 't19' NOT PASSED (symptom: different atom list or connection table)."
    echo "$differences"
    exit -1
else 
    echo "Test 't19' PASSED"
fi

exit 0
