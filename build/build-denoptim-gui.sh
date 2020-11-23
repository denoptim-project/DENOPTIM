#!/bin/bash

if [ -d ../lib ]; then
    if [ -d lib ]; then
        cp -r ../lib/*.jar lib/
    else
        cp -r ../lib .        
    fi 
fi
cp -r ../src/GUI/images images

find ../src/DENOPTIM/src/ ../src/DenoptimGA/src ../src/FragSpaceExplorer/src ../src/GUI/src -name *.java > javafiles.txt

jarsColumnSeparated=$(ls -1 lib/*.jar | while read l ; do echo $l"@@" ; done | tr -d "\n" | sed 's/@@/:/g' | sed 's/\:$//g')

javac -cp "$jarsColumnSeparated" @javafiles.txt -encoding utf-8 -d .

if [ "$?" != "0" ]; then
    rm javafiles.txt
	echo "Failed to compile classes."
    exit -1
fi
rm javafiles.txt

# Make a jar for the GUI

jars=$(ls -1 lib/*.jar | while read l ; do echo $l"@@" ; done | tr -d "\n" | sed 's/@@/ /g')
jarsAndImages="$jars images"
echo "Manifest-Version: 1.0" > manifest.mf
echo "Main-Class: gui.GUI" >> manifest.mf
echo "Class-Path: $(echo $jarsAndImages | fold -w58 | awk '{print " "$0}')" >> manifest.mf
echo >> manifest.mf

jar cvfm DENOPTIM-GUI.jar manifest.mf denoptim denoptimga fragspaceexplorer gui images

if [ $? -ne 0 ]; then
    rm -rf manifest.mf gui denoptim denoptimga fragspaceexplorer images
    echo "Failed to create DENOPTIM-GUI.jar."
    exit -1
fi
echo "--------------------- Done building DENOPTIM-GUI.jar ---------------------"

# Make a jar for the DeniptimGA for backgrownd jobs

jars=$(ls -1 lib/*.jar | while read l ; do echo $l"@@" ; done | tr -d "\n" | sed 's/@@/ /g')
jarsAndImages="$jars images"
echo "Manifest-Version: 1.0" > manifest.mf
echo "Main-Class: denoptimga.DenoptimGA" >> manifest.mf
echo "Class-Path: $(echo $jarsAndImages | fold -w58 | awk '{print " "$0}')" >> manifest.mf
echo >> manifest.mf

jar cvfm DenoptimGA.jar manifest.mf denoptim denoptimga fragspaceexplorer gui images

if [ $? -ne 0 ]; then
    rm -rf manifest.mf gui denoptim denoptimga fragspaceexplorer images
    echo "Failed to create DenoptimGA.jar."
    exit -1
fi
echo "--------------------- Done building DenoptimGA.jar ----------------------"

# Make a jar for the DeniptimGA for backgrownd jobs

jars=$(ls -1 lib/*.jar | while read l ; do echo $l"@@" ; done | tr -d "\n" | sed 's/@@/ /g')
jarsAndImages="$jars images"
echo "Manifest-Version: 1.0" > manifest.mf
echo "Main-Class: fragspaceexplorer.FragSpaceExplorer" >> manifest.mf
echo "Class-Path: $(echo $jarsAndImages | fold -w58 | awk '{print " "$0}')" >> manifest.mf
echo >> manifest.mf

jar cvfm FragSpaceExplorer.jar manifest.mf denoptim denoptimga fragspaceexplorer gui images

if [ $? -ne 0 ]; then
    rm -rf manifest.mf gui denoptim denoptimga fragspaceexplorer images
    echo "Failed to create FragSpaceExplorer.jar."
    exit -1
fi
echo "------------------ Done building FragSpaceExplorer.jar  ------------------"

rm -rf manifest.mf gui denoptim denoptimga fragspaceexplorer images
