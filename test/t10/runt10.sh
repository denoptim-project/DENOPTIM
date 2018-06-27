#!/bin/bash

wrkDir=`pwd`
logFile="t10.log"
paramFile="t10.params"

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
$javaDENOPTIM -jar $DENOPTIMJarFiles/FragSpaceExplorer.jar $paramFile
exec 1>&6 6>&- 

#Check outcome
nGraphs=$(cat $wrkDir/*/FSE-Level_*/F*.txt | wc -l | tr -d '[[:space:]]')
if [[ $nGraphs != 250 ]]
then
    echo " "
    echo "Test 't10' NOT PASSED (symptom: wrong number of graphs $nGraphs)"
    exit -1
fi

grep -q 'FragSpaceExplorer run completed' $wrkDir/FSE*.log
if [[ $? != 0 ]]
then
    echo " "
    echo "Test 't10' NOT PASSED (symptom: completion msg not found)"
    exit -1
else
    echo "Test 't10' PASSED"
fi


exit 0

