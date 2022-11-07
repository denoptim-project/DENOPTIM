# DENOPTIM

![Anaconda_Platforms](https://anaconda.org/denoptim-project/denoptim/badges/platforms.svg) ![Anaconda_License](https://anaconda.org/denoptim-project/denoptim/badges/license.svg) ![Anaconda_Version](https://anaconda.org/denoptim-project/denoptim/badges/version.svg) ![Anaconda_last](
https://anaconda.org/denoptim-project/denoptim/badges/latest_release_date.svg) ![Anaconda_Installer](https://anaconda.org/denoptim-project/denoptim/badges/installer/conda.svg) ![Anaconda_Downloads](
https://anaconda.org/denoptim-project/denoptim/badges/downloads.svg)

## Introduction
DENOPTIM (De Novo OPTimization of In/organic Molecules) is a software meant for <i>de novo</i> design and virtual screening of functional compounds. In practice, DENOPTIM builds chemical entities by assembling building blocks (i.e., fragments), analyzes each chemical entity as to produce its figure of merit (i.e., fitness), and designs new entities based on the properties of known entities.

DENOPTIM is cross-platform, i.e., runs on Windows, Linux, and MacOS, and comes with a graphical user interface, i.e., the GUI. However, computationally demanding molecular design jobs, which we call <i>DENOPTIM experiments</i>, are typically run in the background, as batch processes. Yet, the GUI allows to create input files for such DENOPTIM experiments (Figure 1a), in addition to visualize and edit molecular fragments (Figure 1b) and other DENOPTIM's data structures, and, finally, inspect the output produced by DENOPTIM experiments (Figure 1c).

![Figure 1](./doc/figures/gui_snapshots.png)

## Installation
You can either get latest version of DENOPTIM from the <a href="https://repo.anaconda.com/">Anaconda repository</a>, or you can built DENOPTIM from source code.

### Install From Conda (recommended)

```
conda install -c conda-forge  denoptim
```

or

```
conda install -c denoptim-project  denoptim
```


### Build From Source
Download and extract the <a href="https://github.com/denoptim-project/DENOPTIM/releases/latest">latest release</a> to create a folder we'll call `DENOPTIM_HOME`. In the following, remember to replace `$DENOPTIM_HOME` with the pathname of the extracted DENOPTIM's distribution folder on your system.

Make sure you have an environment that includes JAVA and Maven. Such environment, which we call `dnp_devel`, can be created by manual installation of both JAVA and Maven, or it can be created using conda:
```
cd $DENOPTIM_HOME
conda env create -f environment.yml
conda activate dnp_devel
```

Verify the requirements by running the two commands: Both should return a message declaring the respective versions.
```
javac -version
mvn -version
```

Now, you can build DENOPTIM with
```
mvn package
```

Once maven has finished, you can create call DENOPTIM using a command like the following (NB: replace `$DENOPTIM_HOME` and `${VERSION}` as with the values that apply to your installation):
On Linux/Mac and GitBash on Windows:
```
java -jar $DENOPTIM_HOME/target/denoptim-${VERSION}-jar-with-dependencies.jar
```
On Windows Anaconda prompt:
```
java -jar $DENOPTIM_HOME\target\denoptim-${VERSION}-jar-with-dependencies.jar
```
In the rest of this document, we well use `denoptim` to refer to the above command. In practice, you should create an alias so that 
```
denoptim="java -jar $DENOPTIM_HOME/target/denoptim-${VERSION}-jar-with-dependencies.jar"
```

#### Testing the functionality
The [test/functional_tests](./test/functional_tests/) folder collects tests with automated checking of the results. There test are meant to verify the retention of specific functionality during development phase (i.e., integration tests), but they also are complete example of how to use denoptim from a the terminal on Linux/Mac or Windows via GitBash. 

This is how to run the tests:
```
cd $DENOPTIM_HOME/test/functional_tests
bash runAllTests.sh
```
The results will be collected in a temporary folder (typically `/tmp/denoptim_test`).

## User Manual
The complete user manual is available [on line](https://denoptim-project.github.io/DENOPTIM).

## Tutorials
See the [tutorials page](https://denoptim-project.github.io/tutorials).

## Quick Start
The entry point of any DENOPTIM activity is the command `denoptim`, which results from the [installation](#installation).
To launch the graphical user interface (GUI) run the command without any argument:
```
denoptim
```
Otherwise, run the following in a Win/Mac/Linux command line to get help message.
```
denoptim -h
```
For example, this command
```
denoptim -r GA input_parameters
```
starts an artificial evolution experiment using the genetic algorithm and parameters specified in the <code>&lt;input_parameters&gt;</code> file, which is a text file containing [parameters and keywords](https://htmlpreview.github.io/?https://github.com/denoptim-project/DENOPTIM/blob/master/doc/user_manual.html#Toc35546_1191730726).

### Pre-configured Examples
Complete examples pre-configured to run DENOPTIM experiments on your local client can be found under the [test](./test) folder. Each of these test will run a short experiments and collect the output in a folder that can be inspected with the GUI (see below).
* [test/PtCOLX2](./test/PtCOLX2): genetic algorithm experiment for the optimization of organometallic ligands sets that weaken the carbonyl bond in Pt(CO)(L)(X)<sub>2</sub> complexes (less then 5 minutes on a regular laptop). To run this example:
```
cd $DENOPTIM_HOME/test/PtCOLX2
bash runEvolutionaryExperiment.sh
```
* [test/PtCOLX2_FSE](./test/PtCOLX2_FSE): virtual screening of organometallic ligands sets that weaken the carbonyl bond in Pt(CO)(L)(X)<sub>2</sub> complexes (less then 5 minutes on a regular laptop). To run this example:
```
cd $DENOPTIM_HOME/test/PtCOLX2_FSE
bash runCombinatorialExperiment.sh
```

The GUI can be used to inspect the results. To this end, copy the pathname that is printed by DENOPTIM in the terminal after `Output files associated with the current run are located in $path_to_your_output` and use the GUI to open it:
```
denoptim $path_to_your_output
```

## Contributing
Open an issue to point out any unreported and unexpected behaviors, bugs, or just to discuss changes to code or documentation. To make actual changes to the code follow the [git workflow](https://guides.github.com/introduction/flow/) practices as indicated in file [CONTRIBUTING.md](./CONTRIBUTING.md).


## Cite DENOPTIM
1) DENOPTIM: Software for Computational de Novo Design of Organic and Inorganic Molecules; Marco Foscato, Vishwesh Venkatraman, and Vidar R. Jensen, <i>J. Chem. Inf. Model</i>, <b>2019</b>, 59, 10, 4077-4082 (<a href="https://doi.org/10.1021/acs.jcim.9b00516">https://doi.org/10.1021/acs.jcim.9b00516</a>)
2) Foscato, M.; Occhipinti, G.; Venkatraman, V.; Alsberg, B. K.; Jensen, V. R.; Automated Design of Realistic Organometallic, Molecules from Fragments; <i>J. Chem. Inf. Model.</i> <b>2014</b>, 54, 767–780.
3) Foscato, M.; Venkatraman, V.; Occhipinti, G.; Alsberg, B. K.; Jensen, V. R.; Automated Building of Organometallic Complexes from 3D Fragments; <i>J. Chem. Inf. Model.</i> <b>2014</b>, 54, 1919–1931.
4) Foscato, M.; Houghton, B. J.; Occhipinti, G.; Deeth, R. J.; Jensen, V. R.; Ring Closure To Form Metal Chelates in 3D Fragment-Based de Novo Design. <i>J. Chem. Inf. Model.</i> <b>2015</b>, 55, 1844-1856.

## Acknowledgments
The Research Council of Norway (RCN) is acknowledged for financial support. 
