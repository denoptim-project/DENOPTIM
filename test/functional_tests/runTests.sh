#!/bin/bash
################################################################################
#                                                                              #
#                                Run all tests                                 #
#                                                                              #
################################################################################
#
# Use this script to run all the tests.
# Run with '-h' to print the usage instructions and exit.
#

function printUsage() {
cat <<EOF

  Use this script to run the functional tests.

  Usage:

  ./runTests.sh [-t TESTNAME] [-r] [-b] [-h]

  Options:

  -h           prints this help message and exits.

  -t TESTNAME  runs only test TESTNAME.

  -r           remove previously existing workspace.

  -b           rebuild with 'mvn clean' and 'mvn package' before running tests.

EOF
}

for arg in "$@"
do
    if [ "$arg" = "-h" ]
    then
        printUsage
        exit 0
    fi
done

#
# Settings:
#

# Activate the conda environment defined in ../../environment.yml
envFile="$(cd "$(dirname "$0")/../.." && pwd)/environment.yml"
if [ ! -f "$envFile" ]
then
    echo "ERROR: Cannot find environment file '$envFile'."
    exit 1
fi
envName=$(grep -m 1 "^name:" "$envFile" | awk '{print $2}')
if [ -z "$envName" ]
then
    echo "ERROR: Could not read the environment name from '$envFile'."
    exit 1
fi

if ! command -v conda >/dev/null 2>&1
then
    for candidate in \
        "$HOME/miniforge3/bin/conda" \
        "$HOME/mambaforge/bin/conda" \
        "$HOME/miniconda3/bin/conda" \
        "$HOME/anaconda3/bin/conda" \
        "/opt/conda/bin/conda"
    do
        if [ -x "$candidate" ]
        then
            export PATH="$(dirname "$candidate"):$PATH"
            break
        fi
    done
fi
if ! command -v conda >/dev/null 2>&1
then
    echo "ERROR: conda was not found. Cannot activate environment '$envName'."
    exit 1
fi
condaBase="$(conda info --base 2>/dev/null)"
if [ -z "$condaBase" ] || [ ! -f "$condaBase/etc/profile.d/conda.sh" ]
then
    echo "ERROR: Could not locate conda's profile script to activate '$envName'."
    exit 1
fi
# shellcheck source=/dev/null
source "$condaBase/etc/profile.d/conda.sh"
conda activate "$envName"
if [ $? -ne 0 ]
then
    echo "ERROR: Failed to activate conda environment '$envName'."
    echo "Create it with: conda env create -f \"$envFile\""
    exit 1
fi
echo "Using conda environment: $envName"

# Path to TINKER executables
# Required only by test t1tnk. Such test will be skipped if the following is empty.
# Use of the Tinker engine is now obsolete, but this is kept for backwards compatibility.
export tinkerPathDENOPTIM=""

# Directory created for running tests (must be shorter than 40 characters).
wDir="/tmp/denoptim_test"

# Environment
export SHELL="/bin/bash"
export DENOPTIM_HOME="$(cd ../.. ; pwd)"
export javaDENOPTIM="java"


############################################
# Function for running a Test
############################################

function runTest() {
    testName="$1"
    echo "Running test '$testName'... (ctrl-c to stop)"

    #generate the directory
    testDir="$wDir/$testName"
    mkdir "$testDir"
    if [ ! -d "$testDir" ]
    then
        echo " ERROR! Unabel to create directory $testDir"
        exit
    fi

    #prepare the files
    cd "$testDir"
    if [ ! -d $DENOPTIM_HOME/test/functional_tests/$testName ]; then
        echo "ERROR: Test could not be run because folder '$DENOPTIM_HOME/test/functional_tests/$testName' does not exist"
        exit -1
    fi
    cp -r "$DENOPTIM_HOME/test/functional_tests/$testName/"* .

    #Run the test
    ./"run$testName.sh"

    #get exit value and, in case of error, stop
    status=$?
    if [ "$status" -ne 0 ]
    then
	echo "ERROR running test $testName! Find relevant files in $testDir"
	echo " "
	exit -1
    fi

    #Back home
    cd "$wDir"
}

############################################
# Main
############################################

# Process arguments
chosenTest=""
overwrite=1
runBuild=0
while [ $# -gt 0 ]
do
    case "$1" in
        "-r") 
            overwrite=0
            shift
            ;;
        "-b")
            runBuild=1
            shift
            ;;
        "-h")
            printUsage
            exit 0
            ;;
        "-t") 
            if [ $# -gt 1 ]
            then
                chosenTest="$2"
                shift 2
            else
                echo "ERROR: -t option requires a test name"
                exit 1
            fi
            ;;
        *)
            # Unknown argument - ignore it (for backwards compatibility)
            shift
            ;;
    esac
done

if [ "$runBuild" -eq 1 ]
then
    echo "Building DENOPTIM (mvn clean, then mvn package)"
    if ! command -v mvn >/dev/null 2>&1
    then
        echo "ERROR: cannot build software. Maven not found."
        exit 1
    fi
    (
        cd "$DENOPTIM_HOME" || exit 1
        mvn clean && mvn package
    )
    if [ $? -ne 0 ]
    then
        echo "ERROR: Maven returned a non-zero exit status."
        exit 1
    fi
    echo "BUILDING DONE!"
fi

denoptimJar=$(find "$DENOPTIM_HOME/target" -name "denoptim*-jar-with-dependencies.jar")
if [ ! -f "$denoptimJar" ]
then
    nFound=$(find "$DENOPTIM_HOME/target" -name "denoptim*-jar-with-dependencies.jar" | wc -l)
    if [ "$nFound" -eq 0 ] ; then
        echo "Cannot find DENOPTIM's jar. Build the project with 'mvn package', or rerun this script with -b."
    elif [ "$nFound" -gt 1 ] ; then
        echo "Found multiple versions of DENOPTIM's jar. Make sure you do 'mvn clean' before running 'mvn package' and ensuring its successful completion."
    else
        echo "Cannot interprete this string as a list of jar files: $denoptimJar"
    fi
    exit -1
fi
export denoptimJar
echo "Using DENOPTIM jar: $denoptimJar"

# Detect the version of SED
if ! command -v sed &> /dev/null
then
    echo "Command |sed| not found. Aborting."
    exit -1
else
    if ! command -v man &> /dev/null
    then
        # We assume we are running bash under Windows
        export sedInPlace="-i"
    else
        if man sed | grep -q "BSD"
        then
            export sedInPlace="-i ''"
        else
            if man sed | grep -q "GNU"
            then
                export sedInPlace="-i"
            else
                echo " ERROR! Could not detect 'sed' version."
                echo " Expecting to find BSD or GNU."
                exit 1
            fi
        fi
    fi
fi

# Number of slave cores (NB: do not change)
export DENOPTIMslaveCores=4

# Make working directory
if [ -d "$wDir" ]
then
    if [ $overwrite -eq 0 ]
    then
        rm -fr "$wDir"
    else
        echo " "
        echo "ERROR! Old $wDir exists already! Add '-r' to remove it and run new tests."
        echo " "
        exit
    fi
fi
mkdir "$wDir"
if [ ! -d "$wDir" ]
then
    echo " "
    echo "ERROR! Unable to create working directory."
    echo " "
    exit
fi

cd "$wDir"

# Run a selected test
if [ ! -z "$chosenTest" ]
then
    if [[ "$chosenTest" =~ ^[t,p].*$ ]]
    then
        # Test name already starts with 't' or 'p'
        runTest "$chosenTest"
        exit 0
    elif [[ "$chosenTest" =~ ^[0-9]+[a-z]*$ ]]
    then
        # Test name starts with digits (e.g., "12a", "1", "12")
        runTest "t$chosenTest"
        exit 0
    else
        echo "ERROR: Invalid test name format: $chosenTest"
        echo "Test name should start with 't' or 'p', or be a number (e.g., 't1', '12a', '1')"
        exit 1
    fi
fi

# Run all tests

#
# t1: use DenoptimCG to build funny molecules using the RingClosingMM server.
#
runTest "t1"

#
# t1tnk: use DenoptimCG to build funny molecules using the Tinker and MM engine.
#
runTest "t1tnk"

#
# t2: parallel combinatorial builder from given root (default format: STRING)
#
runTest "t2"

#
# t2a: combinatorial builder forces to build a specific graph
# This is meant to quickly indentify misfunctioning handing of potential cycles)
#
runTest "t2a"

#
# t2b: t2 with internal fitness evaluation
#
runTest "t2b"

#
# t2c: parallel combinatorial builder from given root with internal fitness
# and atom/bond specific descriptors
#
runTest "t2c"

#
# t2d: with python driven fitness provider
#
runTest "t2d"

#
# t2e: with closability defined only by ring size
#
runTest "t2e"

#
# t3: parallel combinatorial builder with enforced symmetry
#
runTest "t3"

#
# t4: parallel combinatorial builder from given roots with symmetry constraints
#
runTest "t4"

#
# t5: parallel combinatorial building til exhaustion of space
#
runTest "t5"

#
# p6: partial exploration of fragment space that generates checkpoint and serialized graphs for other tests.
#
runTest "p6"

#
# t6: restart FragSpaceExplorer from checkpoint file; non-first, non-last iteration
#
runTest "t6"

#
# t7: removed
#

#
# t8: removed
#

#
# t9: FragSpaceExplorer with symmetry constraints
#
runTest "t9"

#
# t10: FragSpaceExplorer with symmetry constraints and enforced symmetry
#
runTest "t10"

#
# t11: Genetic algorithm running with enforced symmetry.
#
runTest "t11"

#
# t12: Genetic algorithm running with symmetry constraints.
#
runTest "t12"

#
# t12a: t12 with internal fitness
#
runTest "t12a"

#
# t12b: Syncronous genetic algorithm running with symmetry constraints.
#
runTest "t12b"

#
# t12c: t12b with internal fitness
#
runTest "t12c"

#
# t12d: Genetic algorithms without writing the candidates from intermediate generations. Writes only final results.
#
runTest "t12d"

#
# t12e: Genetic algorithms without survival of parents and with mutation coupled to crossover.
#
runTest "t12e"

#
# t12f: Genetic algorithms with generation of two offspring from crossover and selection of the best sibling as member of the population.
#
runTest "t12f"

#
# t12g: Initial population given as molecules to fragment from SMILES
#
runTest "t12g"

# #
# # t12h: Initial population given as molecules to fragment from SDF file
# #
# runTest "t12h"

#
# t12i: Genetic algorithm using ADDFUSEDRING mutation to generat aromatic systems
#
runTest "t12i"

#
# t13: DenoptimGA recovering symmetry from SDF field with graph representation
#
runTest "t13"

#
# t14: test crossover operator
#
runTest "t14"

#
# t15: test crossover operator on molecules with symmetry and rings
#
runTest "t15"

#
# t16: GraphEditor
#
runTest "t16"

#
# t17: Evolution based on random seletion of known graphs
#
#TODO: refactor DenoptimRND?
#runTest "t17"

#
# t18: removed
#

#
# t19: removed

#
# t20: crossover with templates (outside templates)
#
runTest 't20'

#
# t21: delete-mutation on template
#
runTest 't21'

#
# t22: extend-mutation on template
#
runTest 't22'

#
# t23: substitution-mutation on template
#
runTest 't23'

#
# t24: graph isomorphism (includes multiple tests)
#
runTest 't24'

#
# t25: standalone fitness evaluation runner
#
runTest 't25'

#
# t26: test mutation on template (includes multiple tests)
#
runTest "t26"

#
# t27: test interaction of GA with external instructions to add/remove candidates
#
runTest "t27"

#
# t28: test crossover inside templates
#
runTest "t28"

#
# t29: test graph editor upgrade
#
runTest "t29"

#
# t30: test fragmenter
#
runTest "t30"

#
# t31: test Py4J integration
#
runTest "t31"

#
# t32: test conversion of molecules to graphs
#
runTest "t32"

#
# t33: test fragmentation based ona template graph
#
runTest "t33"

echo "All done!"
exit 0
