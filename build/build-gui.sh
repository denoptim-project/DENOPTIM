#!/bin/bash

# Building GUI
if [ -d ../lib ]; then
    if [ -d lib ]; then
        cp -r ../lib/*.jar lib/
    else
        cp -r ../lib .        
    fi 
fi
cp -r ../src/GUI/images images


if [ ! -f lib/DENOPTIM.jar ]; then
	echo "Failed to create GUI.jar. Cannot locate DENOPTIM.jar in ../lib"
    exit -1
fi

find ../src/GUI/src/ -name *.java > javafiles.txt
javac -cp lib/cdk-1.4.19.jar:lib/commons-io-2.4.jar:lib/commons-lang3-3.1.jar:lib/vecmath.jar:lib/commons-math3-3.6.1.jar:lib/jfreechart-1.5.0.jar:lib/Jmol-14.29.55.jar:lib/JmolLib-14.29.55.jar:lib/gs-core-1.3.jar:lib/gs-ui-1.3.jar:lib/DENOPTIM.jar:denoptimga.jar @javafiles.txt -encoding utf-8 -d .

if [ "$?" != "0" ]; then
    rm javafiles.txt
	echo "Failed to create GUI.jar."
    exit -1
fi

rm javafiles.txt


echo "Manifest-Version: 1.0" > manifest.mf
echo "Main-Class: gui.GUI" >> manifest.mf
echo "Class-Path: lib/cdk-1.4.19.jar lib/commons-io-2.4.jar lib/commons-lang3-3.1.jar lib/vecmath.jar lib/commons-math3-3.6.1.jar lib/jfreechart-1.5.0.jar lib/Jmol-14.29.55.jar lib/JmolLib-14.29.55.jar lib/gs-core-1.3.jar lib/gs-ui-1.3.jar lib/DENOPTIM.jar demoptimga.jar images" >> manifest.mf
echo >> manifest.mf

jar cvfm GUI.jar manifest.mf gui images

if [ "$?" = "0" ]; then
    rm -rf manifest.mf gui images
else
	echo "Failed to create GUI.jar."
    exit -1
fi

echo "--------------------- Done building GUI.jar ---------------------"
