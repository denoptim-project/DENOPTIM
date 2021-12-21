#!/bin/bash

# Building DenoptimCG

if [ ! -f DENOPTIM-GUI.jar ]; then
    echo "Failed to locate DENOPTIM-GUI.jar"
    exit -1
fi
cp DENOPTIM-GUI.jar lib/

find ../src/DenoptimCG/src/ -name *.java > javafiles.txt
javac -cp lib/cdk-2.3.jar:lib/jgrapht-core-1.4.0.jar:lib/gson-2.8.6.jar:lib/vecmath.jar:lib/DENOPTIM-GUI.jar:lib/gson-2.8.6.jar:lib/commons-io-2.4.jar @javafiles.txt -encoding utf-8 -d .


if [ "$?" != "0" ]; then
    rm javafiles.txt
	echo "Failed to create DenoptimCG.jar."
    exit -1
fi

rm javafiles.txt


echo "Manifest-Version: 1.0" > manifest.mf
echo "Main-Class: denoptimcg.DenoptimCG" >> manifest.mf
echo "Class-Path: lib/cdk-2.3.jar lib/jgrapht-core-1.4.0.jar lib/gson-2.8.6.jar lib/vecmath.jar lib/DENOPTIM-GUI.jar lib/gson-2.8.6.jar lib/commons-io-2.4.jar" >> manifest.mf
echo >> manifest.mf

jar cvfm DenoptimCG.jar manifest.mf denoptimcg

if [ "$?" = "0" ]; then
    rm -rf manifest.mf denoptimcg
else
	echo "Failed to create DenoptimCG.jar."
    exit -1
fi

echo "--------------------- Done building DenoptimCG.jar ---------------------"
