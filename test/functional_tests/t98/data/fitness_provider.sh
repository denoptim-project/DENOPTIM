#!/bin/bash
#
# This is a fitness evaluation script for testing purposes.
# Fitness is only based on number of rings and C atoms
#


###############################################################################
#
# Parameters
#
###############################################################################

#Settings for the execution of DENOPTIM tools
java=java
pathToJarFiles="$DENOPTIM_HOME/build"


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

function addPropertyToSingleMolSDF() {
    propName=$1
    propValue=$2
    file=$3
    # we want this to be sed-free...
    n=$(grep -n '$$$$' $file | cut -f1 -d:)
    n=$((n-=1))
    head -n "$n" $file > $file"_tmp"
    echo "> <$propName>" >> $file"_tmp"
    echo "$propValue" >> $file"_tmp"
    echo "" >> $file"_tmp"
    echo '$$$$' >> $file"_tmp"
    mv $file"_tmp" $file
}

#
# Main
#
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
grep -A1 InChi $inpSDF | tail -n 1 > $molUniqueFile
$java -jar $pathToJarFiles/UpdateUID.jar -m $molUniqueFile -s $inpSDF -k $UIDFILE
if grep -q "MOL_ERROR" $inpSDF
then
    cat $inpSDF > $outSDF
    rm  "$molUniqueFile"
    exit $E_OPTERROR
fi

#
# FITNESS: number of rings
#
nRings=$(grep -c "DENOPTIMRing" "$inpSDF")
nRings=$((nRings+1))
nCAtoms=$(grep -c " C " "$inpSDF")
fitness=$(echo "10.0 * $nRings - $nCAtoms" | bc -l)
if [[ ! $fitness =~ ^[+-]?([0-9]+)?([.])?[0-9]+$ ]] ; then
    cp $inpSDF $outSDF
    addPropertyToSingleMolSDF "MOL_ERROR" "#Fitness: not a number" $outSDF
    exit $E_FATAL
fi

#
# Prepare final SDF file
#
cp $inpSDF $outSDF
addPropertyToSingleMolSDF "FITNESS" $fitness $outSDF

#
# Cleanup
#
rm "$molUniqueFile"
rm "$inpSDF"

#
# Exit
# 
echo "All done!"
exit 0
