DENOPTIM Developer's Manual
===========================

Introduction
------------

The different capabilities of DENOPTIM are well described by its different [Programs](#Programs).
Each program works in a stand-alone fashion or in parallel to others, and the starting point for each one of these programs is the class denoptim.Main in the homonymous package.
(due to maven structure)

From the command line a typical input has the form:

    _denoptim_ (-r) GA,FIT,FSE... file1 file2 ... fileN
             (-h)
             (-v)

the word _denoptim_ stands for an alias for the DENOPTIM's path on the machine (it is the alias automatically generated when DENOPTIM is installed with Conda). 
The arguments in parentheses are the so-called _options_,the three possibilities that DENOPTIM can perform from the command line. These are:  
__help (-h)__: prints the help message as an aid for the user    
__version (-v)__: prints the version of DENOPTIM running on the user's machine   
__run (-r)__: executes one of the possible DENOPTIM programs.

__GA__, __FIT__, __FSE__ etc are _RunTypes_, i.e., the acronym to identify a run and call it from the command line.
The files are the input files specified by the user.

The command is firstly parsed, checked and collected in a so-called [Behavior](#denoptim.main.Behavior). This step verifies the validity of the input command and the input files.
Once this step is completed, this information is used to build a __ProgramTask__ (i.e., the program requested by the user).

A ProgramTask is defined by a __programTaskImplememter__, i.e., the class used to start the program, and the input files as its argumemts.

The ProgramTaskImplementer is a class specific to each program. These classes can be found in the packages named denoptim.programs."*" (where * stands for a name associated to the given program). 
Each one of these classes executes a method call __runProgram()__.
This method has a similar structure for all the programs with the actions:
*	Read the parameters file
*	Check the parameters
*	Process the parameters
*	Start the program specific logger
*	Print parameters  

Except the action check the parameters and process the parameters, which are performed by a specific class associated with the specific program task (e.g.,for the GA this is performed by the class GAParameters in the package denoptim.programs.denovo) all the others are performed by the class RunTimeParameters in the package denoptim.programs. These actions create a unique set of parameters (i.e., settings) that serve as argument for the class performing the program.
This class is usually located in a package identified by a name descriptive of the associated program(e.g., denoptim.ga for the Genetic Algotithm, denoptim.fragmeneter for the fragmentation algorithm).

This is the typical workflow for every denoptim program with few minor exceptions. At this point, every program is handled by different packages and every program has its principal package. These are discussed for each program below:


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

This run is implemented by the class [GARunner](#denoptim.prgrams.denovo.GARunner) in the package denoptim.prgrams.denovo. 
The first difference of this program is that, the class [EvolutionaryAlgorithm](#denoptim.ga.EvolutionaryAlgorithm) (in the package denoptim.ga) which ultimately performs the run, besides the settings, it also include an [ExternalCommandListener](#denoptim.ga.ExternalCmdsListener) in its arguments. Such listener is the class that makes possible for the user to interact with the algorithm during its execution (see [here](#GAInteract)). 
The run is mainly operated by this package ant its workflow is easily trackable through the different classes.
Noteworthy, the structure of the algorithm is influenced by its capability of running two parallelization schemes: synchronous and asynchronous. The synchronous is somewhat the classical genetic optimization algorithm where each generation is perfectly distinguished from the others. The asynchronous scheme instead loses this clear definition since a generation does not necessarily need to be completed in order for others to keep propagate. This forces some deviation of the algorithm from what could be considered a more standard GA. For example the selection step is not operated when the all generation is completed but just before a genetic operation such as _crossover_ or _mutation_ is required to generate a new candidate. This means that the selection is done on the _current_(or past?) population rather than on a completed generation.    

Although all the main functionalities the genetic algorithm are in this package there are several more packages involved such as denoptim.graph for graph manipulation, denoptim.fitness for fitness evaluation and many others such as logging or 3D molecular model building.

### Fragment Space Exploration  {#FSE}


As usual, after passing by Main, this run is hadled by its ProgramTask implementer, i.e., the class [FragSpaceExplorer](#denoptim.programs.combinatorial.FragSpaceExplorer) in the package denoptim.programs.combinatorial. Here the settings for the experiment are built and passed to the class [CombinatorialExplorerByLayer](#denoptim.combinatorial.CombinatorialExplorerByLayer) in the package denoptim.combinatorial. 

This class handles the possible roots graph provided buy the users and grows them through all the possible combinations (given the Fragment Space) layer-by-layer stopping at the level expressed by the user.
All the combinations of fragments that can be attached to given root graph are obtained with the class [FragsCombinationIterator](#denoptim.fragspace.FragsCombinationIterator) in the package denoptim.fragspace.
The construction is done in an asynchronous fashion and each task of building a new candidate is executed by the class [GraphBuildingTask](#denoptim.combinatorial.CombinatorialExplorerByLayer) in the same package. This class extends each graph as specified, relying mainly on the package denoptim.graph. Each task can than call other external subtasks for generating 3D molecular molecular models (denoptim.molecularmodeling) or for fitness evaluation (denoptim.fitness).
Being usually composed of a great number of tasks and hence being prone to errors this run periodically generates [CheckPoints](#denoptim.combinatoral.CheckPoint). These are collections of all the relevant info needed to keep track of the history of all the candidates generated so far, and, in case or failure, work as starting points for further attempts. 


### Graph Editing {#GE}


After being checked by the methods in main this run is implemented in the class [GraphEditor](#denoptim.programs.grapheditor.GraphEditor) in the pakage denoptim.programs.grapheditor. Here,the run is executed by the instructons provided by the user with the method editGraph defined in the class [DGraph](#denoptim.graph.DGraph) in the package denoptim.graph. This method can perform three editing operations: 

- __REPLACECHILD__
- __CHANGEVERTEX__
- __DELETEVERTEX__

All these functions are mainly operated by classes in the same package denoptim.graph.
More on these at [Graph Editing Task Files](#GraphEditingTaskFiles)

### Genetic Operation {#GO}

This run is to be considered as a sub set the Genetic Algorithm program. The programtask Implementer for this run is the class [GeneOpsRunner](#denoptim.programs.genetweeker.GeneOpsRunner) in the package denoptim.programs.genetweeker. Here the settings are not arguments for a class defined in a particular packages but, depending on what specified by the user, this class can run a __Mutation__ or a __Crossover__ operations. Although these two functions are defined in this class they ultimately rely on the class [GraphOperations](#denoptim.ga.GraphOperations) in the package denoptim.ga.

### Fitness {#FIT}


In the progress of the fitness evaluation program there is a difference from others programs in how packages are called. It starts in the main, which calls the ProgramTaskImplementer in the package denoptim.programs.fitnessevaluator which does some preliminary settings of the task but than calls the class [FitnessTask](#denoptim.task.FitnessTask) in the package denoptim.task. At this point there is the discrimination of running an __internal__ or an __external__ fitness provider. An internal fitness provider is mainly run by the package denoptim.fitness (this is the usual progress of a run). In case of an external fitness provider a [ProcessHandler](#denoptim.task.ProcessHandler) is used to interface DENOPTIM with an external program. 


### 3D Molecular Models Builder {#B3D}


The programTask implementer for this run is the class [MolecularModelBuilder](#denoptim.programs.molecularmodelbuilder.MolecularModelBuilder) in the package denoptim.programs.molecularmodelbuilder. This program constructs a 3D molecular model given a graph of a DENOPTIM candidate. The class that actually handles the construction is [MultiMolecularModelBuilder](#denoptim.molecularmodeling.MultiMolecularModelBuilder) in the package denoptim.molecularmodeling. Here the 3D model is constructed with also relying on the package denoptim.integration.tinker (see [here](#3DBuilder)]). Each model is managed and stored as a [ChemicalObjectModel](#denoptim.molecularmodeling.ChemicalObjectModel).


### Graph Isomorphism Analyzer {#GI}


This program is implemented in the class [Isomorphism](#denoptim.programs.isomorphism.Isomorphism) in the package denoptim.programs.isomorphism. Here two graphs are compared by means of the method [isIsomorphicTo](#denoptim.graph.Dgraph.isIsomorphicTo()) defined in the class [DGraph](#denoptim.graph.DGraph) in the package denoptim.graph. Here the graph are compared with the Vento-Foggia algorithm(add link) using the external library org.jgrapht.alg.isomorphism.VF2GraphIsomorphismInspector. (see the user manual for more [info](#GraphIsomorphism)).

### Graph List Comparator {#CLG}


This program allows to deal with lists of graphs and more specifically, search for denoptim isomorphism within these lists. The run is implemented by the class [GraphListsHandler](#denoptim.programs.graphlistshandler.GraphListsHandler) in the package denoptim.programs.graphlistshandler.
Here the method [isIsomorphicTo](#denoptim.graph.Dgraph.isIsomorphicTo()) is run through all the lists provided in the input files.


### Graphical User Interface {#GUI}

### Fragmenter {#FRG}

## Other Packages

| Package | Description| 
| ------- | -----------|
| denoptim.fragspace| Package that handles the space of building blocks. The space comprised of the lists of building blocks and the rules governing the connection of building blocks to form a Denoptim Graph|
| denoptim.logging | Package that manages and defines the tools used to monitor and log all the the operations performed throughout any given run|
| denoptim.utils | Package that collects all the utilities needed by different DENOPTIM tasks |
| denoptim.json | Package used to deal with json files |
| denoptim.files | Package for the management of the file provided to or by DENOPTIM |
| denoptim.constants | Package that collects all the general constants of DENOPTIM |
| denoptim.exception| Package that handles the eventual exception thrown by DENOPTIM |
| denoptim.ion | package that contains all the utility methods for input and output |


## Test

### Unit Tests

The Unit Test classes can be found in their respective packages and they appear as one or more additional class/es. If you want to check on this independentely from the main packages you can find the Unit Tests on the voice __Files__ in the menu bar above.

### Functional Tests

