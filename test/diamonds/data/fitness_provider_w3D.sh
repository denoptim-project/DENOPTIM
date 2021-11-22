#!/bin/bash
###############################################################################
#
# Fitness provider script
# =======================
#
# This scripts recovers the fitness/mol_error from a list.
#
# @param $1 pathname of input SDF file: it must contain the graph 
#        representation of the candidate system of which we are calculating 
#        the fitness.
# @param $2 pathname of the output SDF file where to report the fitness 
#        (possibly with some additional information attached) or the error/s
#        justifying rejecting of this candidate.
# @param $3 pathname of the working space, i.e., a directory.
# @param $4 numerical ID of the task this script is asked to perform.
# @param $5 pathname to the file collecting the candidates unique identifiers 
#        collected up to the moment when this script is asked to evaluate a new
#        candidate
#
###############################################################################

###############################################################################
# Settings and Parameters
###############################################################################

#Java
java="$javaDENOPTIM"

#Denoptim home
pathToDenoptimJars="$DENOPTIMJarFiles"

#Tinker (bin folder)
pathToTinkerBin="/Users/marco/tools/TinkerLFMM_RCP_2/bin"

#Flag controlling remotion of tmp files. 0:doit, 1:don't
cleanup=0

#Exit code for incomplete evaluation of fitness
E_OPTERROR=0 # '0' leads to rejection of this single molecule
E_FATAL=1    # '1' stops the master DEMOPTIM job

#Initialization of job management variables
beginTime=$(date +%s)
errMsg="Error not assigned."


###############################################################################
# Functions
###############################################################################

#
# Function that takes the value of an SDF property from a file
# @param $1 name of the property
# @param $2 the SDF file to analyze
#
function getProperty() {
    propName="$1"
    file="$2"
    propValue=$(grep -A1 "> *<${propName}>" "$file" | tail -n 1)
}

#
# Function to append a property to an SDF file. Does not overwrite any existing
# property in the SDF-
# @param $1 the name of the property to append
# @param $2 the property value of the property
# @param $3 the SDF file to which append the given property
#
function addPropertyToSingleMolSDF() {
    propName="$1"
    propValue="$2"
    file="$3"
    # we want this to be sed-free...
    n=$(grep -n '$$$$' "$file" | cut -f1 -d:)
    n=$((n-=1))
    head -n "$n" "$file" > "${file}_tmp"
    echo "> <$propName>" >> "${file}_tmp"
    echo "$propValue" >> "${file}_tmp"
    echo "" >> "${file}_tmp"
    echo '$$$$' >> "${file}_tmp"
    mv "${file}_tmp" "$file"
}

#
# Abandon this script due to some error, but first store the latest result
# in the output and append it with information on why we are abandoning
# @param $1 the pathname of the latest result (i.e., an SDF file)
# @param $2 exit status
#
function abandon {
    latestSDF="$1"
    es="$2"
    cp "$latestSDF" "$outSDF"
    addPropertyToSingleMolSDF "MOL_ERROR" "$errMsg" "$outSDF"
    echo " "
    echo "ERROR MESSAGE: "
    echo "$errMsg"
    echo " "
    #NB: the exit code is detected by DENOPTIM
    exit "$es" 
    #NB: we trap the EXIT signal 
}

#
# Abandon this script due to some error in child processes. The child process
# has already reported the error in the output SDF file.
# @param $1 the pathname of the latest result (i.e., an SDF file)
#
function abandonDueToChild {
    latestSDF="$1"
    errMsg=$(grep -A1 "MOL_ERROR" "$latestSDF" | tail -n 1)
    es=$(grep -A1 "EXIT_STATUS" "$latestSDF" | tail -n 1)
    cp "$latestSDF" "$outSDF"
    echo " "
    echo "ERROR MESSAGE from: $latestSDF (Status: $es)"
    echo "'""$errMsg""'"
    echo " "
    #NB: the exit code is detected by DENOPTIM
    exit "$es" 
    #NB: we trap the EXIT signal 
}

#
# Cleanup all tmp files according to the cleanup flag
#
function cleanUpTmpFiles {
    if [ "$cleanup" == 0 ]
    then
        #rm -f "$inpSDF"
        rm -f "$molUniqueFile"
        #rm -f "$log"
        rm -f "$preOutSDF"
    fi
}

#
# Perform all tasks to be done when exiting the script for whatever reason
#
function finish {
    finishTime=$(date +%s)
    runTime=$(($finishTime - $beginTime))
    date
    echo "Finished in $runTime seconds" 
    cleanUpTmpFiles
    #Add here any mandatory task to be run on termination
}
trap finish EXIT


###############################################################################
# Main
###############################################################################

#
# Parse arguments
#
if [ "$#" -ne 5 ]
then
    echo " "
    echo "Usage: `basename $0` required number of arguments not supplied"       
    echo "5 parameters must be supplied (in this order):"
    echo " <input.sdf> <output.sdf> <workingDirectory> <taskID> <UIDFile>"
    echo " "
    exit 1
fi
inpSDF="$1"
outSDF="$2"
wrkDir="$3"
taskId="$4"
UIDFILE="$5"
locDir="$(pwd)"

molNum=`basename "$inpSDF" "_inp.sdf"`

preOutSDF="$wrkDir/preOut_${molNum}.sdf"


#
# Setup Log file
#
log="$wrkDir/${molNum}_FProvider.log"
# From here redirect stdout and stderr to log file
exec > $log
exec 2>&1
echo "Starting fitness calculation (ID:$taskId) at $beginTime" 

#
# Build 3D via ring-closing potential
#
echo "Starting DenoptimCG..."
DnCG3Dinp="$wrkDir/${molNum}_inp.sdf"
DnCG3Dout="$wrkDir/${molNum}_3D-DnCG.sdf"
DnCGParFile="$wrkDir/${molNum}_DnCG.par"
echo "CG-VERBOSITY=1" > "$DnCGParFile"
echo "CG-inpSDF=$DnCG3Dinp" >> "$DnCGParFile"
echo "CG-outSDF=$DnCG3Dout" >> "$DnCGParFile"
echo "FS-scaffoldLibFile=$wrkDir/../../data/lib_frags.sdf" >> "$DnCGParFile"
echo "FS-fragmentLibFile=$wrkDir/../../data/lib_frags.sdf" >> "$DnCGParFile"
echo "FS-cappingFragmentLibFile=$wrkDir/../../data/lib_cap.sdf" >> "$DnCGParFile"
echo "FS-compMatrixFile=$wrkDir/../../data/CPMap.par" >> "$DnCGParFile"
echo "FS-RotBondsDefFile=$wrkDir/../../data/rotatableBonds-1.0" >> "$DnCGParFile"
echo "RC-CloseRings" >> "$DnCGParFile"
echo "FS-RCCompMatrixFile=$wrkDir/../../data/RC-CPMap" >> "$DnCGParFile"
echo "CG-atomOrderingScheme=1" >> "$DnCGParFile"
echo "CG-workDir=$wrkDir" >> "$DnCGParFile"
echo "CG-TOOLPSSROT=$pathToTinkerBin/pssrot" >> "$DnCGParFile"
echo "CG-TOOLXYZINT=$pathToTinkerBin/xyzint" >> "$DnCGParFile"
echo "CG-TOOLINTXYZ=$pathToTinkerBin/intxyz" >> "$DnCGParFile"
echo "CG-ForceFieldFile=$wrkDir/../../data/uff_vdw.prm" >> "$DnCGParFile"
echo "CG-KEYFILE=$wrkDir/../../data/conf_search.key" >> "$DnCGParFile"
echo "CG-PSSROTPARAMS=$wrkDir/../../data/submit_PSSROT" >> "$DnCGParFile"
echo "CG-RCKeyFile=$wrkDir/../../data/conf_search_rcp.key" >> "$DnCGParFile"
echo "CG-RCPSSROTPARAMS=$wrkDir/../../data/submit_RC-PSSROT" >> "$DnCGParFile"
#echo "=" >> "$DnCGParFile"

#run builder
"$java" -jar "$pathToDenoptimJars/DenoptimCG.jar" "$DnCGParFile" 

# Cleanup files tmp files
#echo "Removing $wrkDir/${molNum}_cs0.*"
#rm -f "$wrkDir/${molNum}_cs0."*

# Check outcome
if [ -f "$DnCG3Dout" ]; then
    if grep -q "MOL_ERROR" "$DnCG3Dout" ; then
        echo "Exiting! Found ERROR in output of DenoptimCG"
        cat "$DnCG3Dout" > "$outSDF"
        exit "$E_OPTERROR"
    fi
else
    echo "$DnCG3Dout not found."
    errMsg="#DenoptimCG: $DnCG3Dout not found."
    abandon "$inpSDF" "$E_OPTERROR"
fi

#
# Calculate Fitness
#
echo "Calculation of fitness"
FPinp="$DnCG3Dout"
FPout="$wrkDir/${molNum}_FP.sdf"
FPParFile="$wrkDir/${molNum}_FP.par"
echo 'FP-Equation=${taniToGoal}' > "$FPParFile"
echo "FP-DescriptorSpecs=\${parametrized('taniToGoal','TanimotoSimilarity','PubchemFingerprinter, FILE:/Users/marco/tools/DENOPTIM_graphTemplate/test/diamonds/data/goal.sdf')}" >> "$FPParFile"
echo 'FP-No3dTreeModel' >> "$FPParFile"
echo "FR-Input=$FPinp" >> "$FPParFile"
echo "FR-Output=$FPout" >> "$FPParFile"

#run fitness runner
"$java" -jar "$pathToDenoptimJars/FitnessRunner.jar" "$FPParFile"

# Check outcome
if [ ! -f "$FPout" ]; then
    echo "$FPout not found."
    errMsg="#FitnessRunner: $FPout not found."
    abandon "$inpSDF" "$E_OPTERROR"
fi

mv "$FPout" "$outSDF"

echo "All done. Returning $outSDF"


#
# Exit
# 
exit 0
