[TOC]

# DENOPTIM Developer's Manual

This manual integrates the [JavaDoc](annotated.html) to provide an overview on the software logic and facilitate understanding, development, and debug of the code. It is a manual meant for developers, not for users. If you are looking for information on how to use DENOPTIM, check the [user's manual](user_manual.md).

* * *

## Execution Logic

The [Main](#denoptim.main.Main) class is meant to be the only JAVA Main class in the DENOPTIM package and is the execution entry point for all the functionality implemented in the DENOPTIM package. Accordingly, the [main()](#denoptim.main.Main.main) method defines the actual [Behavior](#denoptim.main.Behavior) of the software upon parsing the list of program arguments. The [Behavior](#denoptim.main.Behavior) may correspond to
* returning an error in case of unacceptable list or program arguments (i.e., a `null` [RunType](#denoptim.main.Main.RunType)),
* terminating, possibly upon printing some information such as the help message (i.e., a `DRY` [RunType](#denoptim.main.Main.RunType)),
* launching the graphical user interface (i.e., a `GUI` [RunType](#denoptim.main.Main.RunType)),
* launching the execution of anyone among the [main program tasks](#Programs) (i.e., any other [RunType](#denoptim.main.Main.RunType). The specific implementation of [ProgramTask](#denoptim.task.ProgramTask) for a given [RunType](#denoptim.main.Main.RunType) is defined within the definitions of a [RunType](#denoptim.main.Main.RunType)). 

The latter occurs by creating an instance of [ProgramTask](#denoptim.task.ProgramTask), typically by passing as parameters to the constructor the pathname to a working directory and the pathname to a configuration file defining any settings that control the specifics of the task at hand. Any such implementation of [ProgramTask](#denoptim.task.ProgramTask) is a callable where the usual `call` method, in addition to dealing with general-purpose process management tasks, embeds a call to method [runProgram()](#denoptim.task.ProgramTask.runProgram), which is where the main task is coded. 

Typically, any parsing of the configuration file occurs at the beginning of the [runProgram()](#denoptim.task.ProgramTask.runProgram) and populated an instance of [RunTimeParameters](#denoptim.programs.RunTimeParameters). The latter is a class meant to collect program-specific settings and  utilities, such as logger and randomizer, that are used in various parts on a program. Therefore, [RunTimeParameters](#denoptim.programs.RunTimeParameters) are passed around to child methods to make these settings and utilities available where needed.

Implementations of [ProgramTask](#denoptim.task.ProgramTask)s are collected in sub-packages of the [denoptim.programs](#denoptim.programs) package together with the corresponding program-specific implementation of [RunTimeParameters](#denoptim.programs.RunTimeParameters).


* * *

## Run types


| Run | Description|
| --- | -----------|
| __GUI__ | [Graphical User Interface](#GUI)|
| __GA__  | [Genetic Algorithm](#GA)|
| __FSE__ | [Fragment Space Exploration](#FSE) |
| __FRG__ | [Fragmenter](#FRG) |
| __GE__  | [Graph Editing](#GE) |
| __GO__  | [Genetic Operation](#GO) |
| __FIT__ | [Fitness Evaluation](#FIT) |
| __B3D__ | [Build 3D molecular entities](#B3D)|
| __GI__  | [Graph Isomorphism](#GI) |
| __CLG__ | [Comparison of lists of Graph](#CLG) |


### Genetic Algorithm {#GA}

The [ProgramTask](#denoptim.task.ProgramTask) implementation dealing with GA experiments is the [GARunner](#denoptim.programs.denovo.GARunner). Besides class [EvolutionaryAlgorithm](#denoptim.ga.EvolutionaryAlgorithm) which ultimately performs the evolutionary experiment, besides the settings [GARunner](#denoptim.programs.denovo.GARunner) creates an [ExternalCommandListener](#denoptim.ga.ExternalCmdsListener) in its arguments. Such listener is responsible for the interaction between an ongoing evolutionary experiment and the user (see [here](#GAInteract)).

The specifics of the evolutionary algorithm's implementation are deeply affected by its capability of running two parallelization schemes: _synchronous_ and _asynchronous_. The synchronous scheme follows the classical evolutionary optimization algorithm where each generation is isolated from the others. In the asynchronous scheme, instead, new candidates are created continuously without stopping the generation of candidates at the end of a generations. Because of this peculiarity, selection of candidates for mutation and crossover is done upon requesting a _crossover_ or _mutation_ event.


### Fragment Space Exploration  {#FSE}

The [ProgramTask](#denoptim.task.ProgramTask) implementation dealing with combinatorial experiments is the [FragSpaceExplorer](#denoptim.programs.combinatorial.FragSpaceExplorer), while the algorithm is coded in the [CombinatorialExplorerByLayer](#denoptim.combinatorial.CombinatorialExplorerByLayer).
Given a graph, i.e., the so-called root graph, all the combinations of fragments that can be attached on the root graph are obtained from the [FragsCombinationIterator](#denoptim.fragspace.FragsCombinationIterator). Once the combination of fragments to be attached on the root graph is defined, the construction of the resulting graph, or graphs (multiple graphs can result by closing different sets of fundamental rings on the same spanning tree) is done by the [GraphBuildingTask](#denoptim.combinatorial.CombinatorialExplorerByLayer).

This type of run has the capability to periodically save [CheckPoints](#denoptim.combinatorial.CheckPoint) that allow to restart the exploration should it be interrupted by any reason.


### Graph Editing {#GE}

The [ProgramTask](#denoptim.task.ProgramTask) dealing with standalone graph editing tasks is the [GraphEditor](#denoptim.programs.grapheditor.GraphEditor). This class essentially allows to prepare everything that may be needed to call method [DGraph.editGraph](#denoptim.graph.DGraph.editGraph()).


### Genetic Operation {#GO}

The [ProgramTask](#denoptim.task.ProgramTask) dealing with standalone genetic operations is [GeneOpsRunner](#denoptim.programs.genetweeker.GeneOpsRunner). This class essentially allows to prepare everything that may be needed to perform mutations and crossover operations outside a genetic algorithm.


### Fitness {#FIT}

The [ProgramTask](#denoptim.task.ProgramTask) dealing with standalone fitness calculations [FitnessRunner](#denoptim.programs.fitnessevaluator.FitnessRunner). An instance of [FPRunner](#denoptim.programs.fitnessevaluator.FPRunner) is created to run a task that may use the __internal__ or an __external__ fitness provider. The internal fitness provider uses only functionality available within the DENOPTIM package, while the external fitness provider runs a [ProcessHandler](#denoptim.task.ProcessHandler) to create an external subprocess and retrieve the fitness from the output of such process (NB: not the log or return value).


### 3D Molecular Models Builder {#B3D}

The [ProgramTask](#denoptim.task.ProgramTask) dealing with standalone construction of three-dimensional (3D) molecular models is the [MolecularModelBuilder](#denoptim.programs.moldecularmodelbuilder.MolecularModelBuilder). The construction of one or more 3D models for a [Candidate](#denoptim.graph.Candidate) is [MultiMolecularModelBuilder](#denoptim.programs.moldecularmodelbuilder.MolecularModelBuilder).


### Graph Isomorphism Analyzer {#GI}

The [ProgramTask](#denoptim.task.ProgramTask) dealing with standalone detection of graph isomorphism is [Isomorphism](#denoptim.programs.isomorphism.Isomorphism). Here, two graphs are compared by means of the method [DGraph.isIsomorphicTo](#denoptim.graph.DGraph.isIsomorphicTo()). his method uses the Vento-Foggia algorithm (see [DOI:10.1109/TPAMI.2004.75](http://ieeexplore.ieee.org/xpl/articleDetails.jsp?arnumber=1323804)) implemented in the [JGraphT library](https://jgrapht.org/).


### Graph List Comparator {#CLG}

The [ProgramTask](#denoptim.task.ProgramTask) dealing with standalone comparison of lists of graphs is [GraphListsHandler](#denoptim.programs.graphlisthandler.GraphListsHandler). Here the method [isIsomorphicTo](#denoptim.graph.DGraph.isIsomorphicTo()) is use to compare each member of a list with each member of another list.


### Fragmenter {#FRG}

The [ProgramTask](#denoptim.task.ProgramTask) performing fragmentation tasks and management of fragment libraries is the [Fragmenter](#denoptim.programs.fragmenter.Fragmenter). 

When fragmentation is requires, the [ParallelFragmentationAlgorithm](#denoptim.fragmenter.ParallelFragmentationAlgorithm) can parallelize the work as follows:
1. Split input structures in N batches.​
2. Run N fragmentation threads in parallel.​ Each thread contributes creating a molecular weight (MW)-resolved fragment collections. MW is an intrinsic property, and therefore it’s thread-safe.​ Moreover, in case of need to identify isomorphic fragments, a single fragment is used to represent each isomorphic family (i.e., the list of fragments isomorphic to each other).

Also when the task is the extraction of representative geometries, the work can be parallelized. This is done by [ParallelConformerExtractionAlgorithm](#denoptim.fragmenter.ParallelConformerExtractionAlgorithm), which runs one thread for each isomorphic family in an asynchronous parallelization scheme.
 

### Graphical User Interface {#GUI}

Contrarily to most of the other run types, the launching of the GUI occurs directly via running [GUI](#denoptim.gui.GUI) without involving any [ProgramTask](#denoptim.task.ProgramTask) implementation. 

