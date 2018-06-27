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
$javaDENOPTIM -jar $DENOPTIMJarFiles/DenoptimGA.jar $paramFile
exec 1>&6 6>&- 

#Check outcome
ls "$wrkDir/RUN*/*FIT.sdf" | while read f 
do 
    hasNull=$(grep -A1 "GraphENC" "$f" | grep -q "null") 
    if [[ $? == 0 ]] 
    then 
        echo " "
        echo "Test 't17' NOT PASSED (symptom: null in file $f)"
        exit 1
    fi
    hasNoSymmSet=$(grep -A1 "GraphENC" "$f" | grep -q "SymmetricSet")
    if [[ $? == 1 ]]
    then
        echo " "
        echo "Test 't17' NOT PASSED (symptom: no SymmetricSet in file $f)"
        exit 1
    fi
done 

grep -q 'DENOPTIM EA run completed' $wrkDir/RUN*.log
if [[ $? != 0 ]]
then
    echo " "
    echo "Test 't17' NOT PASSED (symptom: completion msg not found)"
    exit 1
else
    echo "Test 't17' PASSED"
fi
exit 0

