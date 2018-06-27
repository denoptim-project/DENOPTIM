#!/bin/bash

echo "Executing build-all.sh ..."

echo
echo "# # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # #"
echo

echo "build-denoptim.sh"
bash build-denoptim.sh

if [ "$?" != "0" ]; then
    echo "Execution failed for build-denoptim.sh."
    exit -1
fi

echo
echo


echo "build-denoptimga.sh"
bash build-denoptimga.sh

if [ "$?" != "0" ]; then
    echo "Execution failed for build-denoptimga.sh."
    exit -1
fi

echo
echo


echo "build-denoptimcg.sh"
bash build-denoptimcg.sh

if [ "$?" != "0" ]; then
    echo "Execution failed for build-denoptimcg.sh."
    exit -1
fi

echo
echo



echo "build-checkcatalystconstraints.sh"

echo "WARNING! Skipping compilation of outdated code 'CheckCatalystConstraints'."
#bash build-checkcatalystconstraints.sh

if [ "$?" != "0" ]; then
    echo "Execution failed for build-checkcatalystconstraints.sh."
    exit -1
fi

echo
echo


echo "build-preparemopac.sh"
bash build-preparemopac.sh

if [ "$?" != "0" ]; then
    echo "Execution failed for build-preparemopac.sh."
    exit -1
fi

echo
echo


echo "build-getmopaccoordinates.sh"
echo "WARNING! Skipping compilation of outdated code 'GetMOPACCoordinates'."
#bash build-getmopaccoordinates.sh

if [ "$?" != "0" ]; then
    echo "Execution failed for build-getmopaccoordinates.sh."
    exit -1
fi


echo
echo

echo "build-EVA.sh"
echo "WARNING! Skipping compilation of outdated code 'EVA'."
#bash build-EVA.sh

if [ "$?" != "0" ]; then
    echo "Execution failed for build-EVA.sh."
    exit -1
fi


echo
echo

echo "build-checkanchor.sh"
bash build-checkanchor.sh

if [ "$?" != "0" ]; then
    echo "Execution failed for build-checkanchor.sh."
    exit -1
fi

echo
echo


echo "build-preparfitnessoutput.sh"
bash build-preparfitnessoutput.sh

if [ "$?" != "0" ]; then
    echo "Execution failed for build-preparfitnessoutput.sh."
    exit -1
fi

echo
echo

echo "build-fitnessevaluationrulcl2ch2.sh"
echo "WARNING! Skipping compilation of outdated code 'FitnessEvaluationRuLCl2CH2/'."
#bash build-fitnessevaluationrulcl2ch2.sh

if [ "$?" != "0" ]; then
    echo "Execution failed for build-fitnessevaluationrulcl2ch2.sh."
    exit -1
fi

echo
echo

echo "build-fitnessruch2bndlng.sh"
bash build-fitnessruch2bndlng.sh

if [ "$?" != "0" ]; then
    echo "Execution failed for build-fitnessruch2bndlng.sh."
    exit -1
fi

echo
echo


echo "build-updateuid.sh"
bash build-updateuid.sh

if [ "$?" != "0" ]; then
    echo "Execution failed for build-updateuid.sh."
    exit -1
fi

echo
echo


echo "build-fragspaceexplorer.sh"
bash build-fragspaceexplorer.sh

if [ "$?" != "0" ]; then
    echo "Execution failed for build-fragspaceexplorer.sh."
    exit -1
fi

echo
echo

echo "build-serconverter.sh"
bash build-serconverter.sh

if [ "$?" != "0" ]; then
    echo "Execution failed for build-serconverter.sh."
    exit -1
fi

echo
echo

echo "build-checkpointreader.sh"
bash build-checkpointreader.sh

if [ "$?" != "0" ]; then
    echo "Execution failed for build-checkpointreader.sh."
    exit -1
fi

echo
echo

echo "build-testoperator.sh"
bash build-testoperator.sh

if [ "$?" != "0" ]; then
    echo "Execution failed for build-testoperator.sh."
    exit -1
fi

echo
echo

echo "build-grapheditor.sh"
bash build-grapheditor.sh

if [ "$?" != "0" ]; then
    echo "Execution failed for build-grapheditor.sh."
    exit -1
fi


if [ "$1" = "makedist" ]; then
    if [ -d "dist" ]; then
        rm -rf dist
    fi
    mkdir dist 
    cp -r lib dist
    mv *.jar dist
fi
