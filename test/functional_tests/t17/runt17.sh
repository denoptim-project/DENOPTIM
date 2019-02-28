#!/bin/bash

wrkDir=`pwd`
logFile="t17.log"
paramFile="t17.params"

mv data/* $wrkDir
rm -rf data

#Adjust path in scripts and parameter files
filesToModify=$(find . -type f | xargs grep -l "OTF")
for f in $filesToModify
do
    if [ "$sedSyntax" == "GNU" ]
    then
        sed -i "s|OTF_WDIR|$wrkDir|g" $f
        sed -i "s|OTF_DENOPTIMJARS|$DENOPTIMJarFiles|g" $f
        sed -i "s|OTF_JAVADIR|$javaDENOPTIM|g" $f
        sed -i "s|OTF_OBDIR|$obabelDENOPTIM|g" $f
        sed -i "s|OTF_PROCS|$DENOPTIMslaveCores|g" $f
        sed -i "s|OTF_SEDSYNTAX|$sedSyntax|g" $f
    elif [ "$sedSyntax" == "BSD" ]
    then
        sed -i '' "s|OTF_WDIR|$wrkDir|g" $f
        sed -i '' "s|OTF_DENOPTIMJARS|$DENOPTIMJarFiles|g" $f
        sed -i '' "s|OTF_JAVADIR|$javaDENOPTIM|g" $f
        sed -i '' "s|OTF_OBDIR|$obabelDENOPTIM|g" $f
        sed -i '' "s|OTF_PROCS|$DENOPTIMslaveCores|g" $f
        sed -i '' "s|OTF_SEDSYNTAX|$sedSyntax|g" $f
    fi
done

#Run it
exec 6>&1
exec > $logFile
exec 2>&1
$javaDENOPTIM -jar $DENOPTIMJarFiles/DenoptimRND.jar $paramFile
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

