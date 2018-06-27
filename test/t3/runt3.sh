#!/bin/bash

wrkDir=`pwd`
logFile="t3.log"
paramFile="t3.params"

mv data/* $wrkDir

#Adjust path in scripts and parameter files
filesToModify=$(find . -type f | xargs grep -l "OTF")
for f in $filesToModify
do
    if [ "$sedSyntax" == "GNU" ]
    then
        sed -i "s|OTF_WDIR|$wrkDir|g" $f
        sed -i "s|OTF_DENOPTIMJARS|$DENOPTIMJarFiles|g" $f
        sed -i "s|OTF_JAVADIR|$javaDENOPTIM|g" $f
        sed -i "s|OTF_TINKERDIR|$tinkerPathDENOPTIM|g" $f
        sed -i "s|OTF_OBDIR|$obabelDENOPTIM|g" $f
        sed -i "s|OTF_PROCS|$DENOPTIMslaveCores|g" $f
        sed -i "s|OTF_SEDSYNTAX|$sedSyntax|g" $f
    elif [ "$sedSyntax" == "BSD" ]
    then
        sed -i '' "s|OTF_WDIR|$wrkDir|g" $f
        sed -i '' "s|OTF_DENOPTIMJARS|$DENOPTIMJarFiles|g" $f
        sed -i '' "s|OTF_JAVADIR|$javaDENOPTIM|g" $f
        sed -i '' "s|OTF_TINKERDIR|$tinkerPathDENOPTIM|g" $f
        sed -i '' "s|OTF_OBDIR|$obabelDENOPTIM|g" $f
        sed -i '' "s|OTF_PROCS|$DENOPTIMslaveCores|g" $f
        sed -i '' "s|OTF_SEDSYNTAX|$sedSyntax|g" $f
    fi
done

#Run it
echo " "
echo "----------------------- Running DENOPTIM ----------------------------"
exec > $logFile
exec 2>&1
$javaDENOPTIM -jar $DENOPTIMJarFiles/DenoptimGA.jar $paramFile > $logFile
#$javaDENOPTIM -jar $DENOPTIMJarFiles/DenoptimGA.jar $paramFile
echo "--------------------- Execution completed ---------------------------"
echo " "
exit 0

