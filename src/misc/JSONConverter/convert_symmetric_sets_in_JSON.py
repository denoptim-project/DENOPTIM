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

def processGraph(jGraph):
    if 'symVertices' in jGraph:
        symVertexes = jGraph['symVertices']
        if len(symVertexes)!=0:
            print('TODO: deal with symmetrix vertexes')
            raise TypeError('Not implemented yet')

    if 'gVertices' in jGraph:
        for jVertex in jGraph['gVertices']:
            if 'lstSymAPs' in jVertex:
                lstSymAPs = jVertex['lstSymAPs']
                if len(lstSymAPs)!=0:
                    newListOfSymAPs = []
                    for symSet in lstSymAPs:
                        apIdxs = symSet['symIds']
                        apIDs = getApIDs(apIdxs,jVertex)
                        newListOfSymAPs.append(apIDs)
                        print('Changing',apIdxs,'->',apIDs)
                    jVertex['lstSymAPs'] = newListOfSymAPs

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
                        processGraph(data)
                        outfile.write(json.dumps(data, indent=2))
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
    with open(input_pathname, 'r') as file:
        data = json.load(file)
        if isinstance(data, dict):
            processGraph(data)
        elif isinstance(data, list):
            for jGraph in data:
                processGraph(jGraph)
        else:
            print('Type of JSON is not recognized.')
    with open(output_pathname, 'w') as outfile:
        json.dump(data, outfile, indent=2)

print('All done')
