#!/bin/bash

wrkDir=`pwd`
logFile="p7.log"
paramFile="p7.params"

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

echo "Preparation 'p7' done"

# Move some of the generated files that are used by other tests
cp -r "$wrkDir"/FSE*/FSE-Level_5/dg_37.ser "$DENOPTIM_HOME/test/functional_tests/t7/data/"
if [ $? != 0 ]
then
    echo " "
    echo "Error copying serialized graph for t7."
    exit -1
fi


exit 0

