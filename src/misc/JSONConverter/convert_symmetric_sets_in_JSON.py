#!/usr/bin/env python3
#
# Script to upgrade JSON format files to symmetry format in use from DENOPTIM version 3.3.0
#

import os
import json
import argparse

parser = argparse.ArgumentParser(description='Script to convert JSON formated files with DENOPTIM graph/vertexes to the format for symmetric sets of attachment points and vertexes that is introduced from DENOPTIM version 3.3.0.')
parser.add_argument("-i", "--input", required=True, help="Input file (JSON) to convert.")
parser.add_argument("-o", "--output", required=True, help="Output file (converted JSON).")
args = parser.parse_args()

input_pathname = args.input
output_pathname = args.output

def getApIDs(apIndexes,vertex):
    apIDs = []
    for index in apIndexes:
        apIDs.append(vertex['lstAPs'][index]['id'])
    return apIDs


def processAPs(lstSymAPs,jVertex):
    if len(lstSymAPs)!=0:
        newListOfSymAPs = []
        for symSet in lstSymAPs:
            apIdxs = symSet['symIds']
            apIDs = getApIDs(apIdxs,jVertex)
            newListOfSymAPs.append(apIDs)
            print('Changing symmetric AP pointers:',apIdxs,'->',apIDs)
        jVertex['lstSymAPs'] = newListOfSymAPs

def processJson(jsonString):
    # jsonString is a graph
    if 'symVertices' in jsonString:
        symVertexes = jsonString['symVertices']
        if len(symVertexes)!=0:
            newListOfSymVrtxs = []
            for symSet in symVertexes:
                vrtxsIDs = symSet['symIds']
                newListOfSymVrtxs.append(vrtxsIDs)
                print('Reformatting symmetric vertex pointers',vrtxsIDs)
            jsonString['symVertices'] = newListOfSymVrtxs
    if 'gVertices' in jsonString:
        for jVertex in jsonString['gVertices']:
            if 'lstSymAPs' in jVertex:
                lstSymAPs = jVertex['lstSymAPs']
                processAPs(lstSymAPs,jVertex)
            if 'innerGraph' in jVertex:
                jInnerGraph = jVertex['innerGraph']
                processJson(jInnerGraph)

    # JSON string is an editing task that contains Graph
    if 'incomingGraph' in jsonString:
        print('Processing JSON file with "incomingGraph"')
        jGraph = jsonString['incomingGraph']
        processJson(jGraph)


def processJsonFile(input_pathname):
    with open(input_pathname, 'r') as file:
        data = json.load(file)
        if isinstance(data, dict):
            processJson(data)
        elif isinstance(data, list):
            for item in data:
                processJson(item)
        else:
            print('Type of JSON is not recognized.')
    with open(output_pathname, 'w') as outfile:
        json.dump(data, outfile, indent=2)

#
# This is where the 'main' part begins
#
if input_pathname.endswith('.sdf'):
    tmp = input_pathname + "_tmpJsonConversion";
    with open(input_pathname, 'r') as infile, open(tmp, 'wt') as outfile:
        for line in infile:
            if '<GraphJson>' in line:
                print("Found a JSON Graph");
                outfile.write(line);
                jsonString = ''
                for line in infile:
                    if line == "\n":
                        data = json.loads(jsonString)
                        processJson(data)
                        outfile.write(json.dumps(data, indent=2))
                        outfile.write("\n\n");
                        break;
                    else:
                        jsonString += line
            elif '<VertexJson>' in line:
                print("Found a JSON Vertex");
                outfile.write(line);
                jsonString = ''
                for line in infile:
                    if line == "\n":
                        vertexData = json.loads(jsonString)
                        if 'innerGraph' in vertexData:
                            processJson(vertexData['innerGraph'])
                        if 'lstSymAPs' in vertexData:
                            processAPs(vertexData['lstSymAPs'], vertexData)
                        outfile.write(json.dumps(vertexData, indent=2))
                        outfile.write("\n\n");
                        break;
                    else:
                        jsonString += line
            else:
                outfile.write(line);
    # Rename to generate final output. We do it like this to allow overwriting
    # the same file read as input.
    os.rename(tmp, output_pathname)

elif input_pathname.endswith('.json'):
    processJsonFile(input_pathname)

else:
    print('WARNING: Input pathname has no extension: assuming it is a JSON file')
    processJsonFile(input_pathname)

print('All done')
