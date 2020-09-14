#!/bin/bash

# build script for DENOPTIM
find ../src/DENOPTIM/src/ -name *.java > javafiles.txt

jarsColumnSeparated=$(ls -1 lib/*.jar | while read l ; do echo $l"@@" ; done | tr -d "\n" | sed 's/@@/:/g' | sed 's/\:$//g')

javac -cp "$jarsColumnSeparated" @javafiles.txt -encoding utf-8 -d .

if [ "$?" != "0" ]; then
    rm javafiles.txt
	echo "Failed to create DENOPTIM.jar."
    exit -1
fi

rm javafiles.txt

jars=$(ls -1 lib/*.jar | while read l ; do echo $l"@@" ; done | tr -d "\n" | sed 's/@@/ /g')

echo "Manifest-Version: 1.0" > manifest.mf
echo "Class-Path: $jars" >> manifest.mf
echo >> manifest.mf

jar cvfm DENOPTIM.jar manifest.mf denoptim

if [ "$?" = "0" ]; then
    rm manifest.mf
    rm -rf denoptim 
    mv DENOPTIM.jar lib/
    cp lib/DENOPTIM.jar ../lib
else
    echo "Failed to create DENOPTIM.jar."
    exit -1
fi

echo "--------------------- Done building DENOPTIM.jar ---------------------"
