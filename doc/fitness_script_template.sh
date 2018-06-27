#!/bin/bash

# We will assume that DENOPTIM related jar files are located in 
DENOPTIM_DIST=/home/user/DENOPTIM

# We will attempt to use MOPAC to carry out geometry optimisation
MOPAC_EXE=/opt/mopac/MOPAC2012.exe

# The following describes the general framework for fitness evaluation for a 
# proposed molecule
# Typical steps include
# 1) create initial 3D conformation
# 2) Check uniqueness of the molecule (MANDATORY)
# 3) A higher resolution conformer search if required
# 4) Geometry optimization of the molecule. This requires charges and multiplicities 
#    to be set properly. OpenBabel may not always be correct. 


# Predefined functions can be used to perform routine operations such as removal 
# of files etc., 
function cleanup() {
    # t stores $1 argument passed to cleanup()
    FILE=$1
    if [ -f $FILE ];
    then
        rm $FILE
    fi   
}


#-------------------------------------------------------------------------------
#----------------------------- ARGUMENT PARSING --------------------------------

# DENOPTIM calls the fitness script with 5 arguments. 
NO_ARGS=0
E_OPTERROR=65

if [ $# -eq "$NO_ARGS" ] # should check for no arguments
then
	echo "Usage: `basename $0` arguments "	
	exit $E_OPTERROR
fi

if [ "$#" -lt 5 ]
then
	echo "Usage: `basename $0` required number of arguments not supplied"	
	exit $E_OPTERROR
fi

# 2D coordinates
inpSDF=$1
# will contain the optimized 3D coordinates along with the fitness and AD
outSDF=$2
# the working directory
wrkDir=$3
# the task ID
taskId=$4
# Location of the UID file
UIDFILE=$5

fname=`basename $inpSDF .sdf`

#-------------------------------------------------------------------------------


# The evaluation task typically starts with a conformational search.
# Using openbabel/cxcalc to generate an initial conformation
# We will use cxcalc to generate a low energy conformation

# temporary file to store the conformation
tmpSDFFile=$wrkDir/$fname"_"$taskId"_C.sdf"

cxcalc -o $tmpSDFFile lowestenergyconformer --timelimit 2000 $inpSDF

if [ -f $tmpSDFFile ]; then
    lcnt=`wc -l < $tmpSDFFile`
    if [ $lcnt == 0 ]
    then
        /usr/local/bin/obabel -isdf $inpSDF -osdf -O $outSDF --property "MOL_ERROR" "cxcalc failed execution"
        echo "cxcalc failed execution on $inpSDF." >> $errFile
        exit $E_OPTERROR
    fi
fi


#-------------------------------------------------------------------------------
#---------------------------INCHI/UNIQUE MOLECULE CHECK-------------------------
# CALCULATE THE INCHI CODE FOR THE MOLECULE
# Can use obabel or CDK or any routine to calculate a unique key for the molecule
# Dump the unique signature to the $molUniqueFile which will then be provided to 
# the java program that will add to the INCHIKEY FILE suggested by DENOPTIM
# and update the $tmpSDFFile with the INCHI key or MOL_ERROR as appropriate

/usr/local/bin/obabel -isdf $tmpSDFFile -oinchi -xK -xw > $molUniqueFile
java -jar $DENOPTIM_DIST/UpdateUID.jar -m $molUniqueFile -s $tmpSDFFile -k $UIDFILE
if [ -f $tmpSDFFile ];  then
    if grep -q "MOL_ERROR" $tmpSDFFile ; then        
        cat $tmpSDFFile > $outSDF
        echo "UID: MOL_ERROR tag found in $outSDF." >> $errFile
        exit 0
    fi
fi

#-------------------------------------------------------------------------------
#---------------------------------DENSER CONFORMER SEARCH-----------------------

tmpOptSDFFile=$wrkDir/$fname"_"$taskId"_O.sdf"
cxcalc -o $tmpOptSDFFile conformers -m 50 --timelimit 3000 $tmpSDFFile

if [ -f $tmpOptSDFFile ]; then
    lcnt=`wc -l < $tmpOptSDFFile`
    if [ $lcnt == 0 ]
    then
        /usr/local/bin/obabel -isdf $tmpSDFFile -osdf -O $outSDF --property "MOL_ERROR" "cxcalc failed execution"
        echo "cxcalc failed execution on $outSDF." >> $errFile
        exit $E_OPTERROR
    fi
fi

#-------------------------------------------------------------------------------
#--------------------------------GEOMETRY OPTIMIZATION--------------------------
mopFile_A=$wrkDir/$fname"_"$taskId"_A.mop"
mopoutFile_A=$wrkDir/$fname"_"$taskId"_A.out"

echo "TOOLOPENBABEL=/usr/local/bin/obabel" > $parFile
echo "INPSDF=$tmpOptSDFFile" >> $parFile
echo "MOPFILE=$mopFile_A" >> $parFile
echo "KEYWORDS=$DENOPTIM_DIST/MOPAC_KEYWORDS.txt" >> $parFile
echo "TASKID=$taskId" >> $parFile

java -jar /home/vishwesv/Software/NEWDENOPTIM/PrepareMOPAC/dist/PrepareMOPAC.jar $parFile

OUT=$?
if [ $OUT != 0 ];then
    echo "PrepareMOPAC failed execution." >> $errFile
    remfiles
    exit $E_OPTERROR
fi

# MOPAC/DFT/TDDFT/MD optimization
$MOPAC_EXE $mopFile_A
# Extract the final optimized coordinates
# java code to read MOPAC output and write out SDF
# alternatively use babel

if [ -f $mopoutFile_A ]; then
    if  grep -q "NOT ENOUGH TIME" $mopoutFile_A ; then
        echo "MOL_ERROR. See $mopoutFile_A" >> $errFile
        /usr/local/bin/obabel -isdf $inpSDF -osdf -O $outSDF --property "MOL_ERROR" "NOT ENOUGH TIME FOR ANOTHER CYCLE"
        exit 0;
    fi
else
    echo "Unable to locate $mopoutFile_A" >> $errFile
    exit $E_OPTERROR
fi

if [ -f $mopoutFile_A ]; then
    if grep -q "EXCESS NUMBER" $mopoutFile_A ; then
        echo "MOL_ERROR. See $mopoutFile_A" >> $errFile
        /usr/local/bin/obabel -isdf $inpSDF -osdf -O $outSDF --property "MOL_ERROR" "EXCESS NUMBER OF OPTIMIZATION CYCLES"
        exit 0;
    fi
else
    echo "Unable to locate $mopoutFile_A" >> $errFile
    exit $E_OPTERROR
fi

#-------------------------------------------------------------------------------
#------------------------------FITNESS------------------------------------------

# Calculate the fitness of the molecule. This could be the energy or a related 
# quantity 
# the fitness must be added as a <MOL_FITNESS> tag in the output SDF file
# You can use obabel or any other suitable program to do this.
/usr/local/bin/obabel -isdf $outSDF --property "MOL_FITNESS" $VALUE



# if all OK else exit with a non-zero ERROR code
exit 0
