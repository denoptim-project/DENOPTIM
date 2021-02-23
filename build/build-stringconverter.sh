#!/bin/bash

# Building StringConverter

if [ ! -f DENOPTIM-GUI.jar ]; then
    echo "Failed to locate DENOPTIM-GUI.jar"
    exit -1
fi
cp DENOPTIM-GUI.jar lib/

find ../src/misc/StringConverter/src/ -name *.java > javafiles.txt
javac -cp lib/cdk-2.3.jar:lib/DENOPTIM-GUI.jar:lib/gson-2.8.6.jar @javafiles.txt -encoding utf-8 -d .

if [ "$?" != "0" ]; then
    rm javafiles.txt
	echo "Failed to create StringConverter.jar."
    exit -1
fi

rm javafiles.txt


echo "Manifest-Version: 1.0" > manifest.mf
echo "Main-Class: stringconverter.StringConverter" >> manifest.mf
echo "Class-Path: lib/cdk-2.3.jar lib/gson-2.8.6.jar lib/vecmath.jar lib/DENOPTIM-GUI.jar " >> manifest.mf
echo >> manifest.mf

jar cvfm StringConverter.jar manifest.mf stringconverter 


if [ "$?" = "0" ]; then
     rm -rf manifest.mf stringconverter
else
	echo "Failed to create StringConverter.jar."
    exit -1
fi

echo "--------------------- Done building StringConverter.jar ---------------------"
