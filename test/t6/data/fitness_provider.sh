#!/bin/bash
#
# This is a fitness evaluation script for testing purposes
#


###############################################################################
#
# Parameters
#
###############################################################################

# Platform dependencies
sedSyntax="OTF_SEDSYNTAX"

# Parameters for molecular builder
scaffoldLib="OTF_WDIR/lib_scaff.sdf"
fragmentLib="OTF_WDIR/lib_frags.sdf"
cappingLib="OTF_WDIR/lib_cap.sdf"
cpm="OTF_WDIR/CPMap.par"

#Setting for the execution of DENOPTIM tools
java="OTF_JAVADIR"
pathToJarFiles="OTF_DENOPTIMJARS"

#Openbabel
obabel="OTF_OBDIR/obabel"
obprop="OTF_OBDIR/obprop"

#TINKER - PSSROT and TinkerLFMM
pathToTinkerBin="OTF_TINKERDIR"
tinkerForceField="OTF_WDIR/uff_vdw.prm"
rotSpaceDef="OTF_WDIR/rotatableBonds-1.2"
# Ring Search PSSROT
tinkerRSKeyFile="OTF_WDIR/build_uff_RingClosing.key"
tinkerRSSubmitFile="OTF_WDIR/submit_RingClosing"
# Conformational Search PSSROT
tinkerKeyFile="OTF_WDIR/build_uff_ConfSearch.key"
tinkerSubmitFile="OTF_WDIR/submit_ConfSearch"



#Exit code for uncomplete evaluation of fitness
# -> set to 0 to return *FIT.sdf file with MOL_ERROR field
# -> set to anything else to make DENOPTIM stop in case of uncomplete evaluation of fitness
E_OPTERROR=0
# Exit code for fatal errors
E_FATAL=1


###############################################################################
###############################################################################
#
#                  No need to change things below this line
#
###############################################################################
###############################################################################

#
# Cleanup function: used to remove temporary files
#
function cleanup() {
    FILE=$1
    if [ -f $FILE ];
    then
        rm $FILE
    fi
}


###############################################################################
# Main Starts Here
###############################################################################

if [ "$#" -lt 5 ]
then
    echo " "
    echo "Usage: `basename $0` required number of arguments not supplied"       
    echo "5 parameters must be supplied (in this order):"
    echo " <inputFileName.sdf>  <outputFileName.sdf> <workingDirectory> <taskID> <UIDFile>"
    echo " "
    exit 1
fi

#
# Define command line arguments
#
# Input: Graph representation
inpSDF=$1
# Output: 3D geometries of Low and High spin states (with Fitness/Error)
outSDF=$2
# Working directory
wrkDir=$3
# Task ID
taskId=$4
# Location of the UID file
UIDFILE=$5

locDir=$(pwd)

molName=`basename $inpSDF .sdf`
molNum=`basename $inpSDF _inp.sdf`

#
# Redirect log of this script
#
log=$wrkDir/$molName"_FProvider.log"
exec > $log
exec 2>&1

#
# Create UID
#
echo "Evaluation of UID..."
molUniqueFile=$wrkDir/$molName".uid"
molNoRCA=$wrkDir/$molName"_noRCA.sdf"
cp $inpSDF $molNoRCA
if [ "$sedSyntax" == "GNU" ]
then
    sed -i "s/ATP/H  /g" $molNoRCA
    sed -i "s/ATM/H  /g" $molNoRCA
elif [ "$sedSyntax" == "BSD" ]
then
    sed -i '' "s/ATP/H  /g" $molNoRCA
    sed -i '' "s/ATM/H  /g" $molNoRCA
fi
$obabel -isdf $molNoRCA -oinchikey > $molUniqueFile
uid=$(cat $molUniqueFile) 
echo $uid >> $UIDFILE

#
# Prepare final SDF file
#
echo "All done!"
$obabel -isdf $molNoRCA -osdf -O $outSDF --property "UID" "$uid"

exit 0
