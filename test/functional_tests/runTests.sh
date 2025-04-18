#!/bin/bash
################################################################################
#                                                                              #
#                                Run all tests                                 #
#                                                                              #
################################################################################
#
# Use this script to run all the tests.

#
# Usage:
#
# ./runTests.sh [-t TESTNAME] [-r]
#
# Options:
# -t TESTNAME  runs only test TESTNAME.
# -r           remove previously existing workspace.
#

#
# Settings:
#

# Path to TINKER executables
# Currently required only by t1 that will be skipped if the following is empty.
export tinkerPathDENOPTIM=""

# Directory created for running tests (must be shorter than 40 characters).
wDir="/tmp/denoptim_test"

# Environment
export SHELL="/bin/bash"
export DENOPTIM_HOME="$(cd ../.. ; pwd)"
export javaDENOPTIM="java"
export denoptimJar=$(find "$DENOPTIM_HOME/target" -name "denoptim*-jar-with-dependencies.jar")

if [ ! -f "$denoptimJar" ]
then
    nFound=$(find "$DENOPTIM_HOME/target" -name "denoptim*-jar-with-dependencies.jar" | wc -l)
    if [ "$nFound" -eq 0 ] ; then
        echo "Cannot find DENOPTIM's jar. Make sure you have built the project by running 'mvn package' and ensuring its successful completion."
    elif [ "$nFound" -gt 1 ] ; then
        echo "Found multiple versions of DENOPTIM's jar. Make sure you do 'mvn clean' before running 'mvn package' and ensuring its successful completion."
    else
        echo "Cannot interprete this string as a list of jar files: $denoptimJar"
    fi
    exit -1
fi
echo "Using DENOPTIM jar: $denoptimJar"


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
args=($@)
for ((i=0; i<$#; i++))
do
    arg=${args[$i]}
    case "$arg" in
        "-r") overwrite=0 ;;
        "-t") if [ $((i+1)) -lt $# ];
              then
                  chosenTest=${args[$((i+1))]}
              fi;;
    esac
done

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
        runTest "$chosenTest"
        exit 0
    elif [[ "$chosenTest" =~ ^[1-9]*$ ]]
    then
        runTest "t$chosenTest"
        exit 0
    fi
fi

# Run all tests

#
# t1: use DenoptimCG to build funny molecules.
#
runTest "t1"

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

echo "All done!"
exit 0
