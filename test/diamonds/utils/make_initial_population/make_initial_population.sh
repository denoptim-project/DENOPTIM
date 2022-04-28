#!/bin/bash

rm initial_population.sdf

denoptimJar=$(find  -name "../../../../target/denoptim*-jar-with-dependencies.jar")
java -jar  "$denoptimJar" -r FIT -f fitness.params
