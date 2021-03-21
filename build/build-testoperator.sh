#!/bin/bash

# Building TestOperator

if [ ! -f DENOPTIM-GUI.jar ]; then
    echo "Failed to locate DENOPTIM-GUI.jar"
    exit -1
fi
cp DENOPTIM-GUI.jar lib/

find ../src/misc/TestOperator/src/ -name *.java > javafiles.txt
javac -cp lib/cdk-2.3.jar:lib/DENOPTIM-GUI.jar:DenoptimGA.jar:lib/gson-2.8.6.jar @javafiles.txt -encoding utf-8 -d .

if [ "$?" != "0" ]; then
    rm javafiles.txt
	echo "Failed to create TestOperator.jar."
    exit -1
fi

rm javafiles.txt


echo "Manifest-Version: 1.0" > manifest.mf
echo "Main-Class: testoperator.TestOperator" >> manifest.mf
echo "Class-Path: lib/cdk-2.3.jar lib/vecmath.jar lib/DENOPTIM-GUI.jar DenoptimGA.jar lib/gson-2.8.6.jar" >> manifest.mf
echo >> manifest.mf

jar cvfm TestOperator.jar manifest.mf testoperator 


if [ "$?" = "0" ]; then
     rm -rf manifest.mf testoperator
else
	echo "Failed to create TestOperator.jar."
    exit -1
fi

echo "--------------------- Done building TestOperator.jar ---------------------"
