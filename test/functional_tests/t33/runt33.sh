#!/bin/bash

wrkDir=`pwd`
logFile="t33.log"
paramFile="t33.params"

function checkLog() {
    if ! grep -q 'Completed Fragmenter-1' "$1"
    then
        echo " "
        echo "Test 't33' NOT PASSED (symptom: completion msg not found in $1)"
        return 1
    fi
    return 0
}

function checkMatchCount() {
    query=$1
    file=$2
    expected=$3
    n=0;n=$(grep -c "$query" "$file")
    if [ "$expected" -ne "$n" ]; then
        echo " "
        echo "Test 't33' NOT PASSED (symptom: found $n instead of $expected matches of $query in $file)"
        return 1
    fi
    return 0
}

function scaffoldContainsElement() {
    element=$1
    file=$2
    return $(sed '/SCAFFOLD/q' "$file" | grep elSymbol | grep -q "$element")
}

"$javaDENOPTIM" -jar "$denoptimJar" -r FRG "t33.params" > "t33.log" 2>&1
if ! checkLog t33.log ; then exit -1 ; fi

output="$(grep "Output files" "t33.log" | cut -c 61-)"
if [ ! -d "$output" ]; then
    echo "Test 't33' NOT PASSED (symptom: could not find output folder '$output')"
    exit -1
fi

fragmentsFile="$output/Fragments.sdf"
if [ ! -f "$fragmentsFile" ]; then
    echo "Test 't33' NOT PASSED (symptom: could not find fragments file '$fragmentsFile')"
    exit -1
fi

if ! checkMatchCount '$$$$' $fragmentsFile 11 ; then exit -1 ; fi
if ! checkMatchCount '"RuXa"' $fragmentsFile 3 ; then exit -1 ; fi
if ! checkMatchCount '"RuXb"' $fragmentsFile 3 ; then exit -1 ; fi
if ! checkMatchCount '"RuL"' $fragmentsFile 6 ; then exit -1 ; fi

echo "Test 't33' PASSED"
exit 0
