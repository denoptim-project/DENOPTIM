# DENOPTIM
De Novo OPTimization of organic and Inorganic Molecules (DENOPTIM) is a software for de novo design and optimization of functional compounds.

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

  * misc: miscellaneous utilities that may be useful in DENOPTIM-based work.

* [test](./test): contains some automated functionality tests and the published test case.



## Quick start
To get started you first have to compile DENOPTIM and its programs.

1. Preparation. Make sure you have Java installed (1.7 or above). If the following does not results in a version statement or your version is too old, you can get and install Java, for instance, from www.oracle.com:
 
        java -version
        javac -version

2. Compile DENOPTIM and all accessories.
 
    cd build
    bash build-all.sh

3. Done!

After compilation you can run the functionality tests (takes 2-3 minutes).

    cd test/functional_tests
    bash runAllTests.sh

Or, you can play with the optimization of organometallic ligands sets that weaken the carbonyl bond in Pt(CO)(L)(X)<sub>2</sub> complexes (takes 5-10 minutes).

    cd test/PtCOLX2
    bash runEvolutionaryExperiment.sh


## User Manual

The complete user manual is available [under the doc folder](./doc/DENOPTIM_MANUAL.odt)


## Cite DENOPTIM
DENOPTIM: Software for Computational de Novo Design of Organic and Inorganic Molecules, Vishwesh Venkatraman, Marco Foscato, and Vidar R. Jensen, <i>J. Chem. Inf. Model</i> <b>2019</b> (submitted)

