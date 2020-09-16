#!/bin/sh

rm -r /tmp/denoptim_t99
cd ~/code/DENOPTIM/build
make all
cd ~/code/DENOPTIM/test/functional_tests
bash runOnly_t99.sh
cat /tmp/denoptim_t99/t99/t99.log
