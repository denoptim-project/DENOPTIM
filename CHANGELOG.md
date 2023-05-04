# CHANGELOG

## From 4.0 to 4.1.1
* Introduced ultrafast communication via sockets to retrieve descriptor scores/fitness from external tools (e.g., python libraries)
* Conversion of molecular representation into DENOPTIM graphs by on-the-fly fragmentation (via GUI or in GA run)
* Extended the functionality of the genetic algorithm by adding optional behavior.
* Generalized SUS selection strategy
* GUI can now handle also settings not directly defined in GUI's forms.
* Minor debugs.

## From 3.x to 4.0.0
* Removel limitation to max number fo AP and vertex indexes. Now AP and vertex I
Ds are unique only within a graph.
* It is now possible to control the maximum size of the subgraphs that the crossover operation can swap. See keyword GA-MaxXoverSubGraphSize.
* Simplified format of symmetric objects inf JSON strings. See src/misc/JSONConverter/convert\_symmetric\_sets\_in\_JSON.py for upgradinf existing files.
* Several minor debugs

## From 2.x to 3.0.0
* Use of Maven to build and test.
* Command line build and test on Windows.
* Creation of single Main method to start any DENOPTIM run or GUI, and open files from command line.
* Introduction of Templates (recursive graphs).
* Refactoring of genetic algorithm.
* Introduction of new mutation operators.
* Introduction of subgraph crossover operation.
* Growth probability can be controlled by molecular size.
* Introduction JSON format for vertexes and graphs.
* Introduction on interface mechanism for interaction with ongoing runs.

## From 1.1 to 2.0
* Expanding GUI functionality.
* GA and FSE controlled by GUI.
* Adding internal fitness provider.

## From 1.0.1 to 1.1.0
* Introduction of GUI
* Keywords changed:
  * DenoptimCG:
    * <code>GA-SortOrder</code> replaced by <code>GA-SortByIncreasingFitness</code>, which is a flag that takes null argument.
    * <code>GA-ReplacementStrategy</code> argument changed to string (NONE or ELITIST).
    * <code>GA-Parallelization</code> argument changed to string (synchronous or asynchronous).
  * DenoptimGA:
    * <code>GA-FitnessEvalScript</code> removed. Use <code>FP-Source</code> and <code>FP-Interpreter</code>
  * DenoptimRND: 
  * FragSpaceExplorer:
    * <code>FSE-ExternalTask</code> removed. Use <code>FP-Source</code> and <code>FP-Interpreter</code>


