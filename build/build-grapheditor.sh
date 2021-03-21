#!/bin/bash

# Building GraphEditor

if [ ! -f DENOPTIM-GUI.jar ]; then
    echo "Failed to locate DENOPTIM-GUI.jar"
    exit -1
fi
cp DENOPTIM-GUI.jar lib/

find ../src/misc/GraphEditor/src/ -name *.java > javafiles.txt
javac -cp lib/cdk-2.3.jar:lib/DENOPTIM-GUI.jar:DenoptimGA.jar:lib/gson-2.8.6.jar @javafiles.txt -encoding utf-8 -d .

if [ "$?" != "0" ]; then
    rm javafiles.txt
	echo "Failed to create GraphEditor.jar."
    exit -1
fi

rm javafiles.txt


echo "Manifest-Version: 1.0" > manifest.mf
echo "Main-Class: grapheditor.GraphEditor" >> manifest.mf
echo "Class-Path: lib/cdk-2.3.jar lib/vecmath.jar lib/DENOPTIM-GUI.jar DenoptimGA.jar lib/gson-2.8.6.jar" >> manifest.mf
echo >> manifest.mf

jar cvfm GraphEditor.jar manifest.mf grapheditor 


if [ "$?" = "0" ]; then
     rm -rf manifest.mf grapheditor
else
	echo "Failed to create GraphEditor.jar."
    exit -1
fi

echo "--------------------- Done building GraphEditor.jar ---------------------"
