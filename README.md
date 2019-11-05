# DENOPTIM

## Introduction
DENOPTIM (De Novo OPTimization of In/organic Molecules) is a software meant for <i>de novo</i> design and virtual screening of functional compounds. In practice, DENOPTIM is meant for building chemical entities by assembling building blocks (i.e., fragments), processing each chemical entity as to produce its figure of merit (i.e., fitness), and designing new entities based on the properties of entities generated before.

## Content of the source code folder tree

* [build](./build): contains scripts for building the DENOPTIM package from source.

* [contrib](./contrib): contains additional source and data that may be used in relation to DENOPTIM

* [doc](./doc): contains documentation and user manual

* [lib](./lib): The lib directory contains all third-party libraries used by DENOPTIM.

* [src](./src): contains the source code of DENOPTIM's core and modules:

  * DENOPTIM: Generic library of functions and data structures.

  * DenoptimCG: Generator of 3D conformations.

  * DenoptimGA: Genetic algorithm engine that uses the DENOPTIM library for molecular design.

  * DenoptimRND: Dummy evolutionary algorithm using only random selection of new members (no genetic operators).

  * FragSpaceExplorer: Combinatorial algorithm for exploration of fragment spaces.
  
  * GUI: the user-friendly graphical interface.

  * misc: miscellaneous utilities that may be useful in DENOPTIM-based work.

* [test](./test): contains some automated functionality tests and the published test case.

## Installation
1. The prerequisite for installing DENOPTIM is having Java (1.5 or above). Make sure you have Java installed and its executables are in $PATH: If the following does not result in version statements or the version is too old, you can get and install Java from www.oracle.com or http://openjdk.java.net/:

        java -version
        javac -version

2. Download/clone this GitHub repository (use the green "Clone or download" button). From now on, we assume that <code>$DENOPTIM_HOME</code> is the folder you have downloaded/cloned from the GitHub repository. 

3. Compile DENOPTIM and all the accessories in the src folder.

        cd $DENOPTIM_HOME/build
        bash build-all.sh

4. Done! The DENOPTIM package is ready. Start the Graphical User Interface by double-clicking on the GUI.jar or by running

        java -jar $DENOPTIM_HOME/build/GUI.jar

After compilation you can run the functionality tests (takes 2-3 minutes).

    cd $DENOPTIM_HOME/test/functional_tests
    bash runAllTests.sh

The tests will use a temporary folder <code>/tmp/denoptim_test</code> where you can find all files related to these tests.

In addition, you can play with the optimization of organometallic ligands sets that weaken the carbonyl bond in Pt(CO)(L)(X)<sub>2</sub> complexes (takes 10-15 minutes).

    cd $DENOPTIM_HOME/test/PtCOLX2
    bash runEvolutionaryExperiment.sh

This will create a playground folder at <code>/tmp/denoptim_PtCO</code> where the evolutionary experiment will be run. Once the experiment is completed, you'll find also the results in the same folder.

## Usage
DENOPTIM programs are typically run using the following command

    java -jar <program.jar> <input_parameters>

where <code>&lt;program.jar&gt;</code> is the JAR file of the program you what to run, for instance <code>DenoptimGA.jar</code> for the the genetic algorithm, or <code>DenoptimCG.jar</code> for the 3D-structure builder, or <code>FragSpaceExplorer.jar</code> for the virtual screening tool, and <code>&lt;input_parameters&gt;</code> is a text file containing [keywords](https://htmlpreview.github.io/?https://github.com/denoptim-project/DENOPTIM/blob/master/doc/user_manual.html#Toc35546_1191730726).
A complete example of usage of the genetic algorithm can be found under the test folder at (./test/PtCOLX2).

## User Manual
The complete user manual is available under the <code>doc</code> folder and is accessible [on line](http://htmlpreview.github.com/?https://github.com/denoptim-project/DENOPTIM/blob/master/doc/user_manual.html)

## Contributing
Open an issue to point out any unreported and unexpected behaviors, bugs, or just to discuss changes to code or documentation. To make actual changes follow the [git workflow](https://guides.github.com/introduction/flow/) practices as indicated in the (./CONTRIBUTING.md).

## License
DENOPTIM is licensed under the terms of the GNU Affero GPL version 3.0 license. 
Instead, additional libraries used by DENOPTIM programs are licensed according to their respective licenses:
* cdk: GNU Lesser GPL Version 2.1
* commons-cli: Apache License Version 2.0
* commons-io: Apache License Version 2.0
* commons-lang: Apache License Version 2.0
* commons-math: Apache License Version 2.0
* vecmath: GNU GPL Version 2

## Cite DENOPTIM
1) DENOPTIM: Software for Computational de Novo Design of Organic and Inorganic Molecules; Marco Foscato, Vishwesh Venkatraman, and Vidar R. Jensen, <i>J. Chem. Inf. Model</i> <b>2019</b> ASAP (<a href="https://doi.org/10.1021/acs.jcim.9b00516">https://doi.org/10.1021/acs.jcim.9b00516</a>)
2) Foscato, M.; Occhipinti, G.; Venkatraman, V.; Alsberg, B. K.; Jensen, V. R.; Automated Design of Realistic Organometallic, Molecules from Fragments; <i>J. Chem. Inf. Model.</i> <b>2014</b>, 54, 767–780.
3) Foscato, M.; Venkatraman, V.; Occhipinti, G.; Alsberg, B. K.; Jensen, V. R.; Automated Building of Organometallic Complexes from 3D Fragments; <i>J. Chem. Inf. Model.</i> <b>2014</b>, 54, 1919–1931.
4) Foscato, M.; Houghton, B. J.; Occhipinti, G.; Deeth, R. J.; Jensen, V. R.; Ring Closure To Form Metal Chelates in 3D Fragment-Based de Novo Design. <i>J. Chem. Inf. Model.</i> <b>2015</b>, 55, 1844-1856.

## Acknowledgments
The Research Council of Norway (RCN) is acknowledged for financial support. 
