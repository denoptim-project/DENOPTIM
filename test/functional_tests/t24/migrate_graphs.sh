#!/bin/bash

h="$(pwd)"

parFile=t24-1.params

ls ./data/graph.sdf ./data/graph2.sdf ./data/graph3.sdf ./data/graph4.sdf ./data/graph5.sdf ./data/graph6.sdf ./data/graph7.sdf ./data/graph8.sdf ./data/graph9.sdf ./data/graph10.sdf ./data/graph11.sdf | while read sdfFile
do
    baseNameSdfFile=$(basename "$sdfFile")
    migPar="${baseNameSdfFile}_migratedV3.par"
    echo "MIGRATEV2TOV3-inputFile=$sdfFile" > "$migPar"
    echo "MIGRATEV2TOV3-outputFile=${sdfFile}_migratedV3" >> "$migPar"
    sed  's/OTF_WDIR/data/g' "$parFile" >> "$migPar"

    java -jar /Users/marco/tools/DENOPTIM_graphTemplate/build/MigrateV2ToV3.jar "$migPar" > ${baseNameSdfFile}_migratedV3.log
    if [ $? -ne 0 ]
    then
        echo "ERROR! While working on $sdfFile"
        exit -1
    fi
    mv "$sdfFile" "${sdfFile}_originalV2"
    mv "${sdfFile}_migratedV3" "$sdfFile"
done
