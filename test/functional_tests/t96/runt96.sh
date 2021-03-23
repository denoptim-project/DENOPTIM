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

# Check outcome
# echo " "
# echo "WARNING: We do not yet check the outcome of this test run."
# echo " "

# Adapted from https://unix.stackexchange.com/questions/112132/how-can-i-grep-patterns-across-multiple-lines
# Original code: sed -n '/foo/{:start /bar/!{N;b start};/your_regex/p}' your_file
# sed -n /`<file2`/p file1 > /tmp/testing.txt

# expectedMale="$wrkDir/t96/expected_output/male.sdf"
# expectedFemail="$wrkDir/t96/expected_output/female.sdf"
# cd /tmp/dnTestTemplate/t96
# actualMale="/tmp/dnTestTemplate/t96/male_xo.sdf"
# actualFemale="/tmp/dnTestTemplate/t96/female_xo.sdf"
# cmp --silent $expectedMale $actualMale || echo "expected male "

# Check if /tmp/dnTestTemplate/male_xo.sdf contains string in expected_output/male.txt



exit 0
