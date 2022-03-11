# CHANGELOG

## From 2.x to 3.0.0
* Use of Maven to build and test.
* Creation of single Main method to start any DENOPTIM run or GUI, and open files from command line.
* Introduction of Templates (recursive graphs).
* Refactoring of genetic algorithm.
* Introduction of new mutation operators.
* Growth probability can be controlled by molecular size
* Introduction JSON format for vertexes and graphs.

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


