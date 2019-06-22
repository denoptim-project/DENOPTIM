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
# Settings nad Parameters
###############################################################################

#Java
java="$javaDENOPTIM"

#Denoptim home
pathToDenoptimJars="$DENOPTIMJarFiles"

# Map of unique identifiers to fitness or rejection due to steric hindrance
uidToAtomClash="data/UIDsToAtomClash"
uidToFitness="data/UIDsToFitness"

# List of all molecular models
allMolsSDF="data/DB.sdf"

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
# Cleanup all tmp files accorsing to the cleanup flag
#
function cleanUpTmpFiles {
    if [ "$cleanup" == 0 ]
    then
        rm -f "$inpSDF"
        rm -f "$molUniqueFile"
        rm -f "$log"
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

molName=`basename "$inpSDF" .sdf`
molNum=`basename "$inpSDF" "_I.sdf"`

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
# Create UID from input mol
#
echo "Generating UID for $molNum..."
molUniqueFile="$wrkDir/${molNum}.uid"
grep -A1 InChi "$inpSDF" | tail -n 1 > "$molUniqueFile"
"$java" -jar "$pathToDenoptimJars/UpdateUID.jar" -m "$molUniqueFile" -s "$inpSDF" -k "$UIDFILE"
if grep -q "Already exists." "$inpSDF"
then
   addPropertyToSingleMolSDF "EXIT_STATUS" "$E_OPTERROR" "$inpSDF"
   abandonDueToChild "$inpSDF"
elif grep -q "MOL_ERROR" "$inpSDF"
then
   addPropertyToSingleMolSDF "EXIT_STATUS" "$E_FATAL" "$inpSDF"
   abandonDueToChild "$inpSDF"
fi


#
# Recover fitness
#
echo "Recover fitness from list..."
uid="$(cat "$molUniqueFile")"
cp "$inpSDF" "$preOutSDF"
if grep -q "$uid" "$uidToAtomClash"
then
    errMsg="#AtomClash: Found Atom Clashes"
    abandon "$preOutSDF" "$E_OPTERROR"  
fi
if ! grep -q "$uid" "$uidToFitness"
then
    errMsg="#UID: not found"
    abandon "$preOutSDF" "$E_OPTERROR"
else
    fitness=$(grep "$uid" "$uidToFitness" | awk '{print $2}')
    addPropertyToSingleMolSDF "FITNESS" "$fitness" "$preOutSDF"
fi


#
# Recover molecular model (bahs-only extraction of one molecules from SDF)
#
echo "Retriving molecular model from DB..."
nFile=$(wc -l "$allMolsSDF" | awk '{gsub(":"," ",$0); print $1}')
nFirst=$(grep -n -m1 "\$\$\$\$" "$allMolsSDF" | awk '{gsub(":"," ",$0); print $1}')
nUID=$(grep -n "$uid" "$allMolsSDF"  | awk '{gsub(":"," ",$0); print $1}')
nToEnd=$(tail -n "$((nFile-nUID))" "$allMolsSDF" | grep -n "\$\$\$\$" | head -n 1 | awk '{gsub(":"," ",$0); print $1}')
nEnd=$((nUID+nToEnd))
if [ "$nFirst" -gt "$nUID" ]
then
    head -n "$nEnd" "$allMolsSDF" > "$preOutSDF"
else
    nBegin=$(head -n "$nUID" "$allMolsSDF" | grep -n "\$\$\$\$"  | tail -n 1 | awk '{gsub(":"," ",$0); print $1}')
    nTot=$((nEnd-nBegin))
    head -n "$nEnd" "$allMolsSDF" | tail -n "$nTot" > "$preOutSDF"
fi
awk -v molNum="$molNum" '{gsub("CandidateEntity",molNum,$0); print $0}' "$preOutSDF" > "$outSDF"


#
# Finally make the output
#
echo "All done. Returning $outSDF"


#
# Cleanup is done in finish function upon trappin the exit command
#


#
# Exit
# 
exit 0
