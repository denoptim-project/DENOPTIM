#!/bin/bash

# Building GraphListsHandler
if [ ! -f lib/DENOPTIM.jar ]; then
	echo "Failed to create GraphListsHandler.jar. Cannot locate DENOPTIM.jar in lib"
    exit -1
fi

find ../src/misc/GraphListsHandler/src/ -name *.java > javafiles.txt
javac -cp lib/cdk-1.4.19.jar:lib/DENOPTIM.jar:lib/commons-io-2.4.jar @javafiles.txt -encoding utf-8 -d .

if [ "$?" != "0" ]; then
    rm javafiles.txt
	echo "Failed to create GraphListsHandler.jar."
    exit -1
fi

rm javafiles.txt


echo "Manifest-Version: 1.0" > manifest.mf
echo "Main-Class: graphlistshandler.GraphListsHandler" >> manifest.mf
echo "Class-Path: lib/cdk-1.4.19.jar lib/DENOPTIM.jar lib/commons-io-2.4.jar" >> manifest.mf
echo >> manifest.mf

jar cvfm GraphListsHandler.jar manifest.mf graphlistshandler 


if [ "$?" = "0" ]; then
     rm -rf manifest.mf graphlistshandler
else
	echo "Failed to create GraphListsHandler.jar."
    exit -1
fi

echo "--------------------- Done building GraphListsHandler.jar ---------------------"
