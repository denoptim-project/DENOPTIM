#!/bin/bash

# Building FitnessEvaluationRuLCl2CH2
if [ -d ../lib ]; then
    if [ -d lib ]; then
        cp -r ../lib/*.jar lib/
    else
        cp -r ../lib .        
    fi 
fi



if [ ! -f lib/DENOPTIM.jar ]; then
	echo "Failed to create FitnessEvaluationRuLCl2CH2.jar. Cannot locate DENOPTIM.jar in lib"
    exit -1
fi

find ../src/misc/FitnessEvaluationRuLCl2CH2/src/ -name *.java > javafiles.txt
javac -cp lib/cdk-1.4.19.jar:lib/vecmath.jar:lib/DENOPTIM.jar @javafiles.txt -encoding utf-8 -d .

if [ "$?" != "0" ]; then
    rm javafiles.txt
	echo "Failed to create FitnessEvaluationRuLCl2CH2.jar."
    exit -1
fi

rm javafiles.txt


echo "Manifest-Version: 1.0" > manifest.mf
echo "Main-Class: fitnessevaluationrulcl2ch2.FitnessEvaluationRuLCl2CH2" >> manifest.mf
echo "Class-Path: lib/cdk-1.4.19.jar lib/vecmath.jar lib/DENOPTIM.jar" >> manifest.mf
echo >> manifest.mf

jar cvfm FitnessEvaluationRuLCl2CH2.jar manifest.mf fitnessevaluationrulcl2ch2 


if [ "$?" = "0" ]; then
     rm -rf manifest.mf fitnessevaluationrulcl2ch2
else
	echo "Failed to create FitnessEvaluationRuLCl2CH2.jar."
    exit -1
fi

echo "--------------------- Done building FitnessEvaluationRuLCl2CH2.jar ---------------------"
