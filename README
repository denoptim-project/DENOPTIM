-------------------------------------------------------------------------------
                               General Information
-------------------------------------------------------------------------------
De Novo OPTimization of organic and Inorganic Molecules (DENOPTIM) is a
software for de novo design and optimization of functional compounds. 

------------------------------------------------------------------------------
                   Content of the source code folder tree
------------------------------------------------------------------------------

build: contains scripts for building the DENOPTIM package from source.

contrib: contains additional source and data that may be used in relation 
         to DENOPTIM

doc: contains documentation and user manual 

lib: The lib directory contains all third-party libraries used by DENOPTIM.

src: contains the source code of DENOPTIM's core and modules:      

    DENOPTIM: Generic library of functions and data structures.

    DenoptimCG: Generator of 3D conformations.

    DenoptimGA: Genetic algorithm engine that uses the DENOPTIM library  
                for molecular design.

    DenoptimRND: Dummy evolutionary algorithm using only random selection of
                 new members.

    FragSpaceExplorer: Combinatorial algorithm for exploration of fragment 
                       spaces.

    misc: miscellaneous utilities that may be useful in DENOPTIM-based work.

test: contains some automated functionality tests and the published test case.



-------------------------------------------------------------------------------
                                  Quick start
-------------------------------------------------------------------------------
To get started you first have to compile DENOPTIM and its programs.

1) Preparation
Make sure you have Java installed (1.7 or above) by running the following 
commands
	java -version
	javac -version
If this does not results in a version statement or version is too old, please 
get and install Java, for instance, from www.oracle.com.


2) Compile DENOPTIM
Separate shell scripts have been created to compile the programs. In addition 
a "build-all" script can be used to compile all the programs. Optionally, 
all compiled programs can be finally stored in the "dist" directory which is 
created by the script.

To compile all programs simply type 
    cd build
    bash build-all.sh 
 
3) Denoptim is now ready!

After compilation you can run the functionality tests (takes 2-3 minutes).  
	cd test/functional_tests
    bash runAllTests.sh
Or, you can play with the optimization of organometallic ligands sets that 
weaken the carbonyl bond in Pt(CO)(L)(X)2 complexes (takes 5-10 minutes).
	cd test/PtCOLX2
	bash runEvolutionaryExperiment.sh



-------------------------------------------------------------------------------
                                  User Manual
-------------------------------------------------------------------------------
For information see the user manual at __


-------------------------------------------------------------------------------
                                 Cite DENOPTIM
-------------------------------------------------------------------------------
DENOPTIM: Software for Computational de Novo Design of Organic and Inorganic Molecules 
Vishwesh Venkatraman,†,‡ Marco Foscato,#,‡ and Vidar R. Jensen, J, Chem. Inf. Mod 2019 (submitted)


    
