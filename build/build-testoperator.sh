#!/bin/bash

# Building TestOperator
if [ ! -f lib/DENOPTIM.jar ]; then
	echo "Failed to create TestOperator.jar. Cannot locate DENOPTIM.jar in lib"
    exit -1
fi

find ../src/misc/TestOperator/src/ -name *.java > javafiles.txt
javac -cp lib/cdk-1.4.19.jar:lib/DENOPTIM.jar:DenoptimGA.jar @javafiles.txt -encoding utf-8 -d .

if [ "$?" != "0" ]; then
    rm javafiles.txt
	echo "Failed to create TestOperator.jar."
    exit -1
fi

rm javafiles.txt


echo "Manifest-Version: 1.0" > manifest.mf
echo "Main-Class: testoperator.TestOperator" >> manifest.mf
echo "Class-Path: lib/cdk-1.4.19.jar lib/DENOPTIM.jar DenoptimGA.jar" >> manifest.mf
echo >> manifest.mf

jar cvfm TestOperator.jar manifest.mf testoperator 


if [ "$?" = "0" ]; then
     rm -rf manifest.mf testoperator
else
	echo "Failed to create TestOperator.jar."
    exit -1
fi

echo "--------------------- Done building TestOperator.jar ---------------------"
