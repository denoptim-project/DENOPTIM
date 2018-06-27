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
molNum=`basename $inpSDF _I.sdf`

#
# Log of this script
#
log=$wrkDir/$molName"_FProvider.log"
# From here redirect stdout and stderr to log file
exec > $log
exec 2>&1

#
# Create UID from input mol
#
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
$java -jar $pathToJarFiles/UpdateUID.jar -m $molUniqueFile -s $inpSDF -k $UIDFILE
if grep -q "MOL_ERROR" $inpSDF
then
    cat $inpSDF > $outSDF
    exit $E_OPTERROR
fi

#
# Build 3D model
#
DenoptimCG3Dout=$wrkDir/$molName"_3D-CG.sdf"
DenoptimCGParFile=$wrkDir/$molName"_DenoptimCG.par"

echo "Starting DenoptimCG..."
# prepare param file 
echo "VERBOSITY=1" >> $DenoptimCGParFile
echo "inpSDF=$inpSDF" > $DenoptimCGParFile
echo "outSDF=$DenoptimCG3Dout" >> $DenoptimCGParFile
echo "FS-scaffoldLibFile=$scaffoldLib" >> $DenoptimCGParFile
echo "FS-fragmentLibFile=$fragmentLib" >> $DenoptimCGParFile
echo "FS-cappingFragmentLibFile=$cappingLib" >> $DenoptimCGParFile
echo "FS-compMatrixFile=$cpm" >> $DenoptimCGParFile
echo "FS-ROTBONDSDEFFILE=$rotSpaceDef" >> $DenoptimCGParFile
echo "toolOpenBabel=$obabel" >> $DenoptimCGParFile
echo "atomOrderingScheme=1" >> $DenoptimCGParFile
echo "wrkDir=$wrkDir" >> $DenoptimCGParFile
echo "PSSROT=$pathToTinkerBin/pssrot" >> $DenoptimCGParFile
echo "XYZINT=$pathToTinkerBin/xyzint" >> $DenoptimCGParFile
echo "INTXYZ=$pathToTinkerBin/intxyz" >> $DenoptimCGParFile
echo "PARAM=$tinkerForceField" >> $DenoptimCGParFile
# Activate ring closure 
echo "RC-CLOSERINGS" >> $DenoptimCGParFile
# Ring Search step
echo "RCKEYFILE=$tinkerRSKeyFile" >> $DenoptimCGParFile
echo "RC-RCPOTENTIALTYPE=BONDOVERLAP" >> $DenoptimCGParFile
echo "RSPSSROTPARAMS=$tinkerRSSubmitFile" >> $DenoptimCGParFile
# Conformational search
echo "KEYFILE=$tinkerKeyFile" >> $DenoptimCGParFile
echo "PSSROTPARAMS=$tinkerSubmitFile" >> $DenoptimCGParFile

$java -jar $pathToJarFiles/DenoptimCG.jar $DenoptimCGParFile

# Cleanup files tmp files
echo "removing $wrkDir/$molNum"_rs1."*"
rm -f $wrkDir/$molNum"_rs1."*
echo "removing $wrkDir/$molNum"_cs0."*"
rm -f $wrkDir/$molNum"_cs0."*

if [ -f $DenoptimCG3Dout ]; then
    if grep -q "MOL_ERROR" $DenoptimCG3Dout ; then
        cat $DenoptimCG3Dout> $outSDF
        exit $E_OPTERROR
    fi
else
    echo "$DenoptimCG3Dout not found."
    errmsg="#DenoptimCG: $DenoptimCG3Dout not found."
    $obabel -isdf $inpSDF -osdf -O $outSDF --property "MOL_ERROR" "$errmsg"
    exit $E_OPTERROR
fi

#
# FITNESS: cyclicity
#
echo "Starting calculation of FITNESS..."
nrings=$($obprop $DenoptimCG3Dout | grep num_rings | awk '{print $2}')
natms=$($obprop $DenoptimCG3Dout | grep num_atoms | awk '{print $2}')
nhyd=$($obprop $DenoptimCG3Dout | grep formula | awk -F'[H]' '{print $2}')
fitness=$( echo "$natms*$nrings/$nhyd" | bc -l )
echo "Descriptors: NATMS=$natms NR=$nrings NH=$nhyd FIT=$fitness"
re='^[0-9]+([.][0-9]+)?$'
if ! [[ $fitness =~ $re ]]
then
   echo "Unable to get fitness: MW=$mw NR=$nrings FIT=$fitness"
   $obabel -isdf $DenoptimCG3Dout -osdf -O $outSDF --property "MOL_ERROR" "#Unable to calculate ratio: NATMS=$natms NR=$nrings NH=$nhyd FIT=$fitness "
   exit $E_OPTERROR
fi


#
# Prepare final SDF file
#
echo "All done!"
$obabel -isdf $DenoptimCG3Dout -osdf -O $outSDF --property "FITNESS" "$fitness"

exit 0
