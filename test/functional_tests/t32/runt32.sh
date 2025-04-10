#!/bin/bash

wrkDir=`pwd`
logFile="t32.log"
paramFile="t32.params"

function checkLog() {
    if ! grep -q 'Completed Mol2Graph' "$1"
    then
        echo " "
        echo "Test 't30' NOT PASSED (symptom: completion msg not found in $1)"
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
        echo "Test 't30' NOT PASSED (symptom: found $n instead of $expected matches of $query in $file)"
        return 1
    fi
    return 0
}

function chopJSONFile() {
  file=$1
  indent="$2"
  startLines=($(grep -n "^$indent{" "$file"  | awk -F':' '{print $1}'))
  endLines=($(grep -n "^$indent}" "$file"  | awk -F':' '{print $1}'))
  if [ 3 -ne "${#startLines[@]}" ] && [ 3 -ne "${#endLines[@]}" ]; then
    echo " "
    echo "Test 't30' NOT PASSED (symptom: wrong match in json format of $file)"
    return -1
  fi
  for i in $(seq 0 $((${#startLines[@]} -1)) )
  do
    head -n ${endLines[$i]} "$file" | tail -n $((${endLines[$i]} - ${startLines[$i]})) > "${file}_$i"
  done
}

function scaffoldContainsElement() {
    element=$1
    file=$2
    return $(sed '/SCAFFOLD/q' "$file" | grep elSymbol | grep -q "$element")
}

"$javaDENOPTIM" -jar "$denoptimJar" -r M2G "t32-1.params" > "t32-1.log" 2>&1
if ! checkLog t32-1.log ; then exit -1 ; fi

if ! checkMatchCount graphId graphs-1.json 3 ; then exit -1 ; fi
if ! checkMatchCount vertexId graphs-1.json 22 ; then exit -1 ; fi
if ! checkMatchCount '"gRings": \[$' graphs-1.json 1 ; then exit -1 ; fi
if ! checkMatchCount '"gRings": \[\],$' graphs-1.json 2 ; then exit -1 ; fi

if ! chopJSONFile graphs-1.json "  "; then exit -1 ; fi
if scaffoldContainsElement W graphs-1.json_0 ; then
  echo "Test 't32' NOT PASSED (sympton: wrong scaffold in graphs-1.json_0)"
  exit -1
fi
if scaffoldContainsElement W graphs-1.json_1 ; then
  echo "Test 't32' NOT PASSED (sympton: wrong scaffold in graphs-1.json_1)"
  exit -1
fi
if scaffoldContainsElement W graphs-1.json_2 ; then
  echo "Test 't32' NOT PASSED (sympton: wrong scaffold in graphs-1.json_2)"
  exit -1
fi

"$javaDENOPTIM" -jar "$denoptimJar" -r M2G "t32-2.params" > "t32-2.log" 2>&1
if ! checkLog t32-2.log ; then exit -1 ; fi

if ! checkMatchCount graphId graphs-2.sdf 3 ; then exit -1 ; fi
if ! checkMatchCount vertexId graphs-2.sdf 20 ; then exit -1 ; fi
if ! checkMatchCount '"gRings": \[$' graphs-2.sdf 1 ; then exit -1 ; fi
if ! checkMatchCount '"gRings": \[\],$' graphs-2.sdf 2 ; then exit -1 ; fi

if ! chopJSONFile graphs-2.sdf "" ; then exit -1 ; fi
if ! scaffoldContainsElement W graphs-2.sdf_0 ; then
  echo "Test 't32' NOT PASSED (sympton: wrong scaffold in graphs-2.sdf_0)"
  exit -1
fi
if ! scaffoldContainsElement W graphs-2.sdf_1 ; then
  echo "Test 't32' NOT PASSED (sympton: wrong scaffold in graphs-2.sdf_1)"
  exit -1
fi
if ! scaffoldContainsElement W graphs-2.sdf_2 ; then
  echo "Test 't32' NOT PASSED (sympton: wrong scaffold in graphs-2.sdf_2)"
  exit -1
fi

"$javaDENOPTIM" -jar "$denoptimJar" -r M2G "t32-3.params" > "t32-3.log" 2>&1
if ! checkLog t32-3.log ; then exit -1 ; fi

if ! checkMatchCount '"vertexType": "Template"' graphs-3.json 1 ; then exit -1 ; fi
if ! checkMatchCount '"isRCV": true,' graphs-3.json 2 ; then exit -1 ; fi

echo "Test 't32' PASSED"
exit 0
