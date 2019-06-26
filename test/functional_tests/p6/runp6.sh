#!/bin/bash

wrkDir=`pwd`
logFile="p6.log"
paramFile="p6.params"

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
"$javaDENOPTIM" -jar "$DENOPTIMJarFiles/FragSpaceExplorer.jar" "$paramFile"
exec 1>&6 6>&- 

refMsg="Stopped with converged checkpoint IDs:"
if ! grep -q "$refMsg" "$logFile"
then
    echo " "
    echo "Unsuccesful generation of checkpoint files for tests."
    exit -1
else
    echo "Preparation 'p6' done."
fi

# Move some of the generated files that are used by other tests
cp "$wrkDir"/FSE*.chk "$DENOPTIM_HOME/test/functional_tests/t6/data/oldCheckPoint.chk"
if [ $? != 0 ]
then
    echo " "
    echo "Error copying checkpoint file for t6."
    exit -1
fi
cp -r "$wrkDir"/FSE*/FSE-Level_1 "$wrkDir"/FSE*/FSE-Level_2 "$DENOPTIM_HOME/test/functional_tests/t6/data/prev_dbRoot"
if [ $? != 0 ]
then
    echo " "
    echo "Error copying base of serialized graphs for t6."
    exit -1
fi
cp -r "$wrkDir"/FSE*/FSE-Level_2/dg_96.ser "$DENOPTIM_HOME/test/functional_tests/t7/data/dg_96.ser"
if [ $? != 0 ]
then
    echo " "
    echo "Error copying serialized graph for t7."
    exit -1
fi


exit 0

