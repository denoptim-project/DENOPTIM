#!/bin/bash

wrkDir=`pwd`
logFile="t96.log"
paramFile="t96.params"

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
"$javaDENOPTIM" -jar "$DENOPTIMJarFiles/TestOperator.jar" "$paramFile"
exec 1>&6 6>&-


# Checking outcome
actualMale=$(head -n 12 male_xo.sdf | tail -n 9)
expectedMale=$(cat expected_output/male_sdf.txt)
isDifferentMale=$(cmp -s $expectedMale $actualMale)

actualFemale=$(head -n 12 female_xo.sdf | tail -n 9)
expectedFemale=$(cat expected_output/female_sdf.txt)
isDifferentFemale=$(cmp -s $expectedFemale $actualFemale)

if cmp -s "$expectedMale" "$actualMale" ; then
  echo " "
  echo "Test 't96' NOT PASSED (symptom: unexpected result from crossover)"
  echo "Expected male:"
  echo $expectedMale
  echo " "
  echo "But was:"
  echo $actualMale
  exit -1
elif cmp -s "$expectedFemale" "$actualFemale" ; then 
  echo " "
  echo "Test 't96' NOT PASSED (symptom: unexpected result from crossover)"
  echo "Expected female:"
  echo $expectedFemale
  echo " "
  echo "But was:"
  echo $actualFemale
  exit -1
fi

echo "Test 't96' PASSED"

exit 0
