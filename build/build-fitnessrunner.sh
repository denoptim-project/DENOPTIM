#!/bin/bash

# Building FitnessRunner

if [ ! -f DENOPTIM-GUI.jar ]; then
    echo "Failed to locate DENOPTIM-GUI.jar"
    exit -1
fi
cp DENOPTIM-GUI.jar lib/

find ../src/FitnessRunner/src/ -name *.java > javafiles.txt
javac -cp lib/cdk-2.3.jar:lib/jgrapht-core-1.4.0.jar:lib/DENOPTIM-GUI.jar:lib/gson-2.8.6.jar:lib/commons-lang3-3.1.jar:lib/javax.servlet-1.4.jar:lib/commons-el-1.0.jar @javafiles.txt -encoding utf-8 -d .

if [ "$?" != "0" ]; then
    rm javafiles.txt
	echo "Failed to create FitnessRunner.jar."
    exit -1
fi

rm javafiles.txt


echo "Manifest-Version: 1.0" > manifest.mf
echo "Main-Class: fitnessrunner.FitnessRunner" >> manifest.mf
echo "Class-Path: lib/cdk-2.3.jar lib/jgrapht-core-1.4.0.jar lib/vecmath.jar lib/DENOPTIM-GUI.jar lib/gson-2.8.6.jar lib/commons-lang3-3.1.jar lib/javax.servlet-1.4.jar lib/commons-el-1.0.jar" >> manifest.mf
echo >> manifest.mf

jar cvfm FitnessRunner.jar manifest.mf fitnessrunner 


if [ "$?" = "0" ]; then
     rm -rf manifest.mf fitnessrunner
else
	echo "Failed to create FitnessRunner.jar."
    exit -1
fi

echo "--------------------- Done building FitnessRunner.jar ---------------------"
