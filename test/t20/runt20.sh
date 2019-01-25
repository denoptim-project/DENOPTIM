#!/bin/bash

wrkDir=`pwd`
logFile="t20.log"
paramFile="t20.params"

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
$javaDENOPTIM -jar $DENOPTIMJarFiles/GraphEditor.jar $paramFile
exec 1>&6 6>&- 

#Check outcome
nBr=$(grep Br $wrkDir/outGraph.sdf | wc -l | awk '{print $1}')
if [[ $nBr != 8 ]]
then
    echo " "
    echo "Test 't8' NOT PASSED (symptom: wrong number of Br-fragments, $nBr)"
    exit -1
fi
nF=$(grep " F " $wrkDir/outGraph.sdf | wc -l | awk '{print $1}')
if [[ $nF != 1 ]]
then
    echo " "
    echo "Test 't8' NOT PASSED (symptom: wrong number of F-fragments, $nF)"
    exit -1
fi
nCl=$(grep Cl $wrkDir/outGraph.sdf | wc -l | awk '{print $1}')
if [[ $nCl != 0 ]]
then
    echo " "
    echo "Test 't8' NOT PASSED (symptom: wrong number of Cl-fragments, $Cl)"
    exit -1
fi

grep -q 'GraphEditor run completed' $wrkDir/GraphEd.log
if [[ $? != 0 ]]
then
    echo " "
    echo "Test 't20' NOT PASSED (symptom: completion msg not found)"
    exit 1
else
    echo "Test 't20' PASSED"
fi
exit 0

