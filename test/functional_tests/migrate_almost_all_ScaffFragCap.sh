#!/bin/bash

h="$(pwd)"
ls -d p* t* | while read d 
do
  cd "$d"
  if [[ "$d" == "t1" ]]; then
      cd "$h"
      continue;
  fi
  if [[ "$d" == "t8" ]]; then
      cd "$h"
      continue;
  fi
  if [[ "$d" == "t25" ]]; then
      cd "$h"
      continue;
  fi
  if [[ "$d" == "t27" ]]; then
      cd "$h"
      continue;
  fi

  echo "----> $d"

  tName="$(basename "$d")"
  parFile=$(ls -1 ${tName}*.params* | head -n 1)
  for query in "FS-scaffoldLibFile" "FS-fragmentLibFile" "FS-cappingFragmentLibFile"
  do
    baseNameSdfFile=$(basename "$(grep -i "$query" "$parFile")")
    sdfFile="$(find . -name "$baseNameSdfFile")"
    migPar="${baseNameSdfFile}_migratedV3.par"
    echo "MIGRATEV2TOV3-inputFile=$sdfFile" > "$migPar"
    echo "MIGRATEV2TOV3-outputFile=${sdfFile}_migratedV3" >> "$migPar"
    sed  's/OTF_WDIR/data/g' "$parFile" >> "$migPar"

    java -jar /Users/marco/tools/DENOPTIM_graphTemplate/build/MigrateV2ToV3.jar "$migPar" > ${baseNameSdfFile}_migratedV3.log
    if [ $? -ne 0 ]
    then
        echo "ERROR!"
        exit -1
    fi
    mv "$sdfFile" "${sdfFile}_originalV2"
    mv "${sdfFile}_migratedV3" "$sdfFile"
  done

  cd "$h"
done
