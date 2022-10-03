DENOPTIM Developer's Manual
===========================

Introduction
------------

The different capabilities of DENOPTIM are well described by its different [Programs](#Programs).
Each program works in a stand-alone fashion or in parallel to others, and each one starts in the class denoptim.main in the homonymous package.

From the command line a typical input has the form:

    denoptim (-r) GA,FIT,FSE... file1 file2 ... fileN
             (-h)
             (-v)

the word _denoptim_ stands for an alias for the DENOPTIM's path on the machine (it is the alias automatically generated when DENOPTIM is installed with Conda).
The arguments in parentheses are the so-called _options_, i.e., the three possibilities that DENOPTIM can perform from the command line. These are:  
__help (-h)__: prints the help message as an aid for the user    
__version (-v)__: prints the version of DENOPTIM running on the user's machine   
__run (-r)__: executes one of the possible DENOPTIM programs.

__GA__, __FIT__, __FSE__ etc are _RunTypes_, i.e., the acronym to identify a run and call it from the command line.
The files are the input files specified by the user.

The command is firstly parsed, checked and collected in a so-called [Behavior](#denoptim.main.Behavior).
This step verifies the validity of the input command and the input files.
Once this step is completed, this information builds an implementation of [ProgramTask](#denoptim.task.ProgramTask) (i.e., the program requested by the user). These classes can be found inthe subpackages of denoptim.programs.A [ProgramTask](#denoptim.task.ProgramTask) has the method [runProgram()](#denoptim.task.ProgramTask.runProgram()) that performs the following steps:

*	Read the parameters file
*	Check the parameters
*	Process the parameters
*	Start the program specific logger
*	Print parameters  

Except the methods _check the parameters_ and _process the parameters_, which are performed by a specific class associated with the specific program task (e.g.,for the GA this is performed by the class [GAParameters](#denoptim.programs.denovo.GAParameters) in the package denoptim.programs.denovo) all the others are performed by the class [RunTimeParameters](#denoptim.programs.RunTimeParameters) in the package denoptim.programs. These methods create a unique set of parameters (i.e., settings) that serve as argument for the class performing the program.
This class is usually located in a package identified by a name descriptive of the associated program(e.g., denoptim.ga for the Genetic Algotithm, denoptim.fragmenter for the fragmentation algorithm).

This is the typical workflow for every denoptim program with few minor exceptions. At this point, every program is handled by different packages and every program has its principal package. These are discussed below for each program:


* * *

Runs
----

| Run | Description|
| --- | -----------|
| __GUI__ | [Graphical User Interface](#GUI)|
| __GA__  | [Genetic Algorithm](#GA)|
| __FSE__ | [Fragment Space Exploaration](#FSE) |
| __FRG__ | [Fragmenter](#FRG) |
| __GE__  | [Graph Editing](#GE) |
| __GO__  | [Genetic Operation](#GO) |
| __FIT__ | [Fitness Evaluation](#FIT) |
| __B3D__ | [Build 3D molecular entities](#B3D)|
| __GI__  | [Graph Isomorphism](#GI) |
| __CLG__ | [Comparison of lists of Graph](#CLG) |


### Genetic Algorithm {#GA}

This run is implemented by the class [GARunner](#denoptim.programs.denovo.GARunner) in the package denoptim.programs.denovo.
A peculiarity of this program is that, the class [EvolutionaryAlgorithm](#denoptim.ga.EvolutionaryAlgorithm) (in the package denoptim.ga) which ultimately performs the run, besides the settings, it also includes an [ExternalCommandListener](#denoptim.ga.ExternalCmdsListener) in its arguments. Such listener is the class that makes possible for the user to interact with the algorithm during its execution (see [here](#GAInteract)).
The run is mainly operated by this package and its workflow is easily trackable through the different classes.
Noteworthy, the structure of the algorithm is influenced by its capability of running two parallelization schemes: _synchronous_ and _asynchronous_. The synchronous scheme is somewhat the classical genetic optimization algorithm where each generation is perfectly distinguished from the others. The asynchronous scheme instead loses this clear definition since a generation does not necessarily need to be completed in order for others to keep propagate. This forces some deviation of the algorithm from what could be considered a more standard GA. For example the selection step is not operated when the all generation is completed but just before a genetic operation such as _crossover_ or _mutation_ is required to generate a new candidate. This means that the selection is done on the _current (or past) population_ rather than on a _completed generation_.    

Although all the main functionalities of the genetic algorithm are in this package there are several more packages involved such as denoptim.graph for graph manipulation, denoptim.fitness for fitness evaluation and many others such as logging or 3D molecular model building.

### Fragment Space Exploration  {#FSE}


As usual, after passing by Main, this run is hadled by its [ProgramTask](#denoptim.task.ProgramTask), i.e., the class [FragSpaceExplorer](#denoptim.programs.combinatorial.FragSpaceExplorer) in the package denoptim.programs.combinatorial. Here the settings for the experiment are built and passed to the class [CombinatorialExplorerByLayer](#denoptim.combinatorial.CombinatorialExplorerByLayer) in the package denoptim.combinatorial.

This class handles the possible root graphs provided buy the user and grows them through all the possible combinations layer-by-layer stopping at the level expressed by the user.
All the combinations of fragments that can be attached to a given root graph are obtained with the class [FragsCombinationIterator](#denoptim.fragspace.FragsCombinationIterator) in the package denoptim.fragspace.
The construction is done in an asynchronous fashion and each task of building a new candidate is executed by the class [GraphBuildingTask](#denoptim.combinatorial.CombinatorialExplorerByLayer) in the same package. This class extends each graph as specified, relying mainly on the package denoptim.graph. Each task can than call other external subtasks for generating 3D molecular models (denoptim.molecularmodeling) or for fitness evaluation (denoptim.fitness).
Being usually composed of a great number of tasks, and hence being prone to errors, this run periodically generates [CheckPoints](#denoptim.combinatorial.CheckPoint), i.e., collections of all the relevant info needed to keep track of the history of all the candidates generated so far, and, in case of failure, work as starting points for further attempts.


### Graph Editing {#GE}


The [ProgramTask](#denoptim.task.ProgramTask) of this run is the class [GraphEditor](#denoptim.programs.grapheditor.GraphEditor) in the package denoptim.programs.grapheditor. Here,the run is executes the method [editGraph](#denoptim.graph.DGraph.editGraph()) defined in the class [DGraph](#denoptim.graph.DGraph) in the package denoptim.graph. This method can perform three editing operations:

- __REPLACECHILD__
- __CHANGEVERTEX__
- __DELETEVERTEX__

All these functions are mainly operated by classes in the same package denoptim.graph.
More on these at [Graph Editing Task Files](#GraphEditingTaskFiles)

### Genetic Operation {#GO}

This run is to be considered as a subset the Genetic Algorithm program. The [ProgramTask](#denoptim.task.ProgramTask) of this run is the class [GeneOpsRunner](#denoptim.programs.genetweeker.GeneOpsRunner) in the package denoptim.programs.genetweeker. Here the settings are not arguments for a class defined in a particular packages but, depending on what specified by the user, this class can run a __Mutation__ or a __Crossover__ operations. Although these two functions are defined in this class they ultimately rely on the class [GraphOperations](#denoptim.ga.GraphOperations) in the package denoptim.ga.

### Fitness {#FIT}


In the overall progress of the fitness evaluation program there is a difference from the others programs in how packages are called. It starts in the main, which calls the [ProgramTask](#denoptim.task.ProgramTask) in the package denoptim.programs.fitnessevaluator which does some preliminary settings of the task but than calls the class [FitnessTask](#denoptim.task.FitnessTask) in the package denoptim.task. At this point there is the program discriminates between running an __internal__ or an __external__ fitness provider. An internal fitness provider is mainly run by the package denoptim.fitness (this is the usual progress of a run). In case of an external fitness provider a [ProcessHandler](#denoptim.task.ProcessHandler) is used to interface DENOPTIM with an external program.


### 3D Molecular Models Builder {#B3D}


The [ProgramTask](#denoptim.task.ProgramTask) of this run is the class [MolecularModelBuilder](#denoptim.programs.moldecularmodelbuilder.MolecularModelBuilder) in the package denoptim.programs.molecularmodelbuilder. This program constructs a 3D molecular model given a graph of [Candidate](#denoptim.graph.Candidate). The class that actually handles the construction is [MultiMolecularModelBuilder](#denoptim.programs.moldecularmodelbuilder.MolecularModelBuilder) in the package denoptim.molecularmodeling. Here the 3D model is constructed also relying on the package denoptim.integration.tinker (see [here](#ThreeDBuilder)]). Each model is managed and stored as a [ChemicalObjectModel](#denoptim.molecularmodeling.ChemicalObjectModel).


### Graph Isomorphism Analyzer {#GI}


For this run the [ProgramTask](#denoptim.task.ProgramTask) is the class [Isomorphism](#denoptim.programs.isomorphism.Isomorphism) in the package denoptim.programs.isomorphism. Here, two graphs are compared by means of the method [isIsomorphicTo](#denoptim.graph.DGraph.isIsomorphicTo()) defined in the class [DGraph](#denoptim.graph.DGraph) in the package denoptim.graph. The graphs are compared with the Vento-Foggia algorithm (see [DOI:10.1109/TPAMI.2004.75](http://ieeexplore.ieee.org/xpl/articleDetails.jsp?arnumber=1323804)) using the external library org.jgrapht.alg.isomorphism.VF2GraphIsomorphismInspector. (see the user manual for more [info](#GraphIsomorphism)).

### Graph List Comparator {#CLG}


This program allows to deal with lists of graphs and more specifically, search for denoptim isomorphism within these lists. The run is implemented by the class [GraphListsHandler](#denoptim.programs.graphlisthandler.GraphListsHandler) in the package denoptim.programs.graphlistshandler.
Here the method [isIsomorphicTo](#denoptim.graph.DGraph.isIsomorphicTo()) is run through all the lists provided in the input files.


### Fragmenter {#FRG}

The [ProgramTask](#denoptim.task.ProgramTask) for this run is the class [Fragmenter](#denoptim.programs.fragmenter.Fragmenter) in the  package denoptim.programs.fragmenter. Besides the usual parameter class this package contains the classes [CuttingRule](#denoptim.programs.fragmenter.CuttingRule) and [MatchedBond](denoptim.programs.fragmenter.MatchedBond) which define these two respective core concepts of the run. The [runProgram](denoptim.task.ProgramTask.runProgram()) method of this run has the potential of calling two different tasks: the Fragmentation ([ParallelFragmentationAlgorithm](#denoptim.fragmenter.ParallelFragmentationAlgorithm)) and the Extraction of Conformers([ConformerExtractorTask](#denoptim.fragmenter.ConformerExtractorTask)). Both this methods are collected in the package denoptim.fragmenter.
The conformer extraction task analyzes an isomorphic family of fragments and identifies the most representative one, which is usually made available for the successive fragmentation.
The fragmentation task instead creates fragments from a list of candidates by chopping parts matching the SMARTS queries expressed by the user.  

### Graphical User Interface {#GUI}

## Other Packages

| Package | Description|
| ------- | -----------|
| denoptim.fragspace| Package that handles the space of building blocks, i.e., the space comprised of the lists of [Vertices](#denoptim.graph.Vertex) and the rules governing their connections to form a DENOPTIM [Graph](#denoptim.graph.DGraph)|
| denoptim.logging | Package that manages and defines the tools used to monitor and log all the the operations performed throughout any given run|
| denoptim.utils | Package that collects all the utilities needed by different DENOPTIM tasks |
| denoptim.json | Package used to manage json files |
| denoptim.files | Package for the management of the file provided to or by DENOPTIM |
| denoptim.constants | Package that collects all the general constants of DENOPTIM |
| denoptim.exception| Package that handles the eventual exception thrown by DENOPTIM |
| denoptim.io | Package that contains all the utility methods for input and output |


## Test

### Unit Tests

The Unit Test classes can be found in their respective packages and they appear as one or more additional class/es. If you want to check on this independentely from the main packages you can find the Unit Tests on the voice __Files__ in the menu bar above.

### Functional Tests

Under the directory _test_ there are five different examples of DENOPTIM's runs and some of its most important features.
Besides, there is a folder which collects all the so-called functional test which are a collection of automated runs testing the functionality of DENOPTIM (integration tests). 
All these tests and examples can be run by executing the relative shell script present in each folder.
