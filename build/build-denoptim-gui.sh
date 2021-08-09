#!/bin/bash
# Script that build a number of jar files. These will have different filenames 
# and different manifest as to make the execution of the main method more intuitive.

# WARNING! The names of jar arkives *GUI.jar, DenoptimGA.jar, FragSpaceExplorer.jar, and FitnessRunner.jar are hardcoded in utility class src/DENOPTIM/src/denoptim/fitness/DescriptorUtils.java.
#

rm -rf lib
cp -r ../lib lib
cp -r ../src/GUI/images images

find ../src/DENOPTIM/src/ ../src/DenoptimGA/src ../src/FragSpaceExplorer/src ../src/GUI/src ../src/FitnessRunner/src -name *.java > javafiles.txt

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

jar cvfm DENOPTIM-GUI.jar manifest.mf denoptim denoptimga fragspaceexplorer gui images fitnessrunner

if [ $? -ne 0 ]; then
    rm -rf manifest.mf gui denoptim denoptimga fragspaceexplorer images
    echo "Failed to create DENOPTIM-GUI.jar."
    exit -1
fi
echo "--------------------- Done building DENOPTIM-GUI.jar ---------------------"

# Make a jar for the DeniptimGA for background jobs

jars=$(ls -1 lib/*.jar | while read l ; do echo $l"@@" ; done | tr -d "\n" | sed 's/@@/ /g')
jarsAndImages="$jars images"
echo "Manifest-Version: 1.0" > manifest.mf
echo "Main-Class: denoptimga.DenoptimGA" >> manifest.mf
echo "Class-Path: $(echo $jarsAndImages | fold -w58 | awk '{print " "$0}')" >> manifest.mf
echo >> manifest.mf

jar cvfm DenoptimGA.jar manifest.mf denoptim denoptimga fragspaceexplorer gui images fitnessrunner

if [ $? -ne 0 ]; then
    rm -rf manifest.mf gui denoptim denoptimga fragspaceexplorer images
    echo "Failed to create DenoptimGA.jar."
    exit -1
fi
echo "--------------------- Done building DenoptimGA.jar ----------------------"

# Make a jar for the FragSpaceExplorer for background jobs

jars=$(ls -1 lib/*.jar | while read l ; do echo $l"@@" ; done | tr -d "\n" | sed 's/@@/ /g')
jarsAndImages="$jars images"
echo "Manifest-Version: 1.0" > manifest.mf
echo "Main-Class: fragspaceexplorer.FragSpaceExplorer" >> manifest.mf
echo "Class-Path: $(echo $jarsAndImages | fold -w58 | awk '{print " "$0}')" >> manifest.mf
echo >> manifest.mf

jar cvfm FragSpaceExplorer.jar manifest.mf denoptim denoptimga fragspaceexplorer gui images fitnessrunner

if [ $? -ne 0 ]; then
    rm -rf manifest.mf gui denoptim denoptimga fragspaceexplorer images
    echo "Failed to create FragSpaceExplorer.jar."
    exit -1
fi
echo "------------------ Done building FragSpaceExplorer.jar  ------------------"

# Make a jar for the FitnessRunner for background jobs

jars=$(ls -1 lib/*.jar | while read l ; do echo $l"@@" ; done | tr -d "\n" | sed 's/@@/ /g')
jarsAndImages="$jars images"
echo "Manifest-Version: 1.0" > manifest.mf
echo "Main-Class: fitnessrunner.FitnessRunner" >> manifest.mf
echo "Class-Path: $(echo $jarsAndImages | fold -w58 | awk '{print " "$0}')" >> manifest.mf
echo >> manifest.mf

jar cvfm FitnessRunner.jar manifest.mf denoptim denoptimga fitnessrunner gui images

if [ $? -ne 0 ]; then
    rm -rf manifest.mf gui denoptim denoptimga fitnessrunner images
    echo "Failed to create FitnessRunner.jar."
    exit -1
fi
echo "------------------ Done building FitnessRunner.jar  ------------------"


rm -rf manifest.mf gui denoptim denoptimga fragspaceexplorer images fitnessrunner
