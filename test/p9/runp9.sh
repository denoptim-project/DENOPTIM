#!/bin/bash

wrkDir=`pwd`
logFile="p9.log"
paramFile="p9.params"

mv data/* "$wrkDir"
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
exec > "$logFile"
exec 2>&1
"$javaDENOPTIM" -jar "$DENOPTIMJarFiles/FragSpaceExplorer.jar" "$paramFile"
exec 1>&6 6>&- 

refMsg="Stopped with converged checkpoint IDs:"
if ! grep -q "$refMsg" "$logFile"
then
    echo " "
    echo "Unsuccesful generation of checkpoint files for tests."
    exit -1
else
    echo "Test 'p9' PASSED"
fi

# Move some of the generated files that are used by other tests
cp "$wrkDir"/FSE*.chk "$DENOPTIMHomeDir/test/t9/data/oldCheckPoint.chk"
if [ $? != 0 ]
then
    echo " "
    echo "Error copying checkpoint file for t9."
    exit -1
fi
cp -r "$wrkDir"/FSE*/FSE-Level_1 "$wrkDir"/FSE*/FSE-Level_2 "$DENOPTIMHomeDir/test/t9/data/prev_dbRoot"
if [ $? != 0 ]
then
    echo " "
    echo "Error copying base of serialized graphs for t9."
    exit -1
fi
cp -r "$wrkDir"/FSE*/FSE-Level_2/dg_96.ser "$DENOPTIMHomeDir/test/t11/data/dg_96.ser"
if [ $? != 0 ]
then
    echo " "
    echo "Error copying serialized graph for t11."
    exit -1
fi


exit 0

