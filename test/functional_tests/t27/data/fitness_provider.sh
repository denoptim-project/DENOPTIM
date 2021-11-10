#!/bin/bash
#
# This is a fitness evaluation script for testing purposes
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

molName=`basename $inpSDF _inp.sdf`

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
# FITNESS: # Cl and F atoms
#
ncl=$(grep -c " Cl " $inpSDF)
nf=$(grep -c " F " $inpSDF)
fitness=$((ncl + nf))

candIdTo50="M00000028"
candIdTo40="M00000031"
triggerRemovalAndAddition="Gen10"
triggerStop="Gen15" 
# NB: if you change these numbers be aware that dome candidates never reach this point
# of the fitness evaluation script. This because they are rejected due to duplicate UID.
# For example, M00000056 is abandoned before reaching this point.

removalAndAdditionTaskFile="$wrkDir/../interface/removal_task"
stopTaskFile="$wrkDir/../interface/stop_run_task"
newCandSrc="$wrkDir/../../newCandSrc.json"

if [[ "$candIdTo50" == $molName ]]; then
    fitness=50
    echo "Setting fitness to $fitness for this candidate"
fi
if [[ "$candIdTo40" == $molName ]]; then
    fitness=40
    echo "Setting fitness to $fitness for this candidate"
fi
if [[ "$wrkDir" == *"$triggerRemovalAndAddition"* ]]; then
    echo "Triggering removal of $candIdTo50 and $candIdTo40! See file $removalAndAdditionTaskFile"
    echo "REMOVE_CANDIDATE $candIdTo50 $candIdTo40" > "$wrkDir/tmpInstruction"
    echo "Triggering inclusion of $newCandSrc! See file $removalAndAdditionTaskFile"
    echo "ADD_CANDIDATE $newCandSrc" >> "$wrkDir/tmpInstruction"
    mv "$wrkDir/tmpInstruction" "$removalAndAdditionTaskFile"
fi
if [[ "$wrkDir" == *"$triggerStop"* ]]; then
    echo "Triggering manual stop of this GA run! See file $stopTaskFile"
    echo "STOP_GA" > $stopTaskFile
fi

if [[ ! $fitness =~ ^-?[0-9]+$ ]] ; then
    cp $inpSDF $outSDF
    addPropertyToSingleMolSDF "MOL_ERROR" "#Fitness: not an integer" $outSDF
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
