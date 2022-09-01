DENOPTIM Developer's Manual
===========================

Introduction
------------

The different capabilities of DENOPTIM are well described by its so-called [Programs](#Programs).
Each program it can work in a stand-alone fashion or in parallel to others. 
The starting point for each of this programs is the class denoptim.Main in the homonymous package.
(this due to the maven structure)

From the command line a typical input as the form:

    denoptim (-r) GA,FIT,FSE... file1 file2 ... fileN
             (-h)
             (-v)

where the word denoptim stands for an alias for the DENOPTIM's path on the machine (it is the alias automatically generated when DENOPTIM is installed with Conda). 
The arguments in parentheses are the so-called _options_,the three possibilities that DENOPTIM can perform from the command line.  

These are:  
__help (-h)__: prints the help message which as aid for the users     
__version (-v)__: prints the version of DENOPTIM running on the user's machine,   
__run (-r)__: option that executes one of the possible DENOPTIM programs.

GA, FIT, FSE etc are _RunTypes_, enum variables from which a run is identified and called from the command line.
The files are the input files specified by the user.

This command is firstly parsed, checked and collected in a so-called __Behavior__. This step is to verify the validity of the input commad and the input files.

Once this step is completed, this information is used to build a __ProgramTask__ (i.e., the program requested by the user).

A ProgramTask is defined by a programTaskImplememter (a class used to start the program) and the input files.

The ProgramTaskImplementer is a class specific for each program. These classes can be found in the packages named denoptim.programs."*" (Each package has an additional name). 
Here, in these classes, there is always the method __runProgram()__.
This method has a similar structure for all the programs with the actions:

*	Read the parameters file
*	Check the parameters
*	Process the parameters
*	Start the program specific logger
*	Print parameters

Except the action check the parameters and process the parameters, which are performed by a specific class associated with the specific program task (e.g.,for the GA this is performed by the class GAParameters in the package denoptim.programs.denovo) all the others are performed by the class RunTimePartameters in the package denoptim.programs. These actions create a unique set of parameters (i.e., settings) that serve as argument for the specific class that performs the program.

This is the workflow for every denoptim program. At this point every kind of program is handled by different packages and every program has its principal package. These are discussed for each program below:


* * *

## Runs

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
| __DRY__ | [Dry Run](Dry.md) |

Graphical User Interface {#GUI}
------------------------







Genetic Algorithm {#GA}
-----------------

The Genetic Algorithm is ultimately performed by the class denoptim.EvolutionaryAlgorithm in the package denoptim.ga. 
The first difference of the Evolutionary ALgorithm is that besides the settings it also include an ExternalCommand Listener in its arguments. Such listener is the class that makes possible for the user to interact with the algorithm during its execution.
The structure of the algorithm is influenced by its capability of running to parallelization schemes: synchronous and asynchronous. The synchronous is somewhat the classical genetic optimization algorithm where each generation is perfectly separated by the others. The asynchronous scheme instead loses this clear definition of a generation since a generation doesn't necessarily need to be completed in order for other generations to keep propagate. This forces some deviation of the algorithm from what coould be considered a more standard GA. For example the selection step is not operated when the all generation is completed but just before a genetic operation such as crossover or mutation is required to generate a new candidate. This means that the selection is done on the _current_(or past?) population rather than the complete generation.    

Although all the main functionalities the genetic algotithm are in this package there are several more packages involved such as denoptim.graph for graph manipulation, denoptim.fitness for fitness evaluation and many others such monitoring and logging.

Fragment Space Exploration  {#FSE}
--------------------------







Fragmenter {#FRG}
----------





Graph Editing {#GE}
-------------


Genetic Operation {#GO}
-----------------



Fitness {#FIT}
-------

In progress of the fitness evaluation program there is a difference from others programs in how packages are called. It starts in the main, which calls the ProgramTaskImplementer in the package denoptim.programs.fitnessevaluator which does some prliminary settings of the task but than calls the class FitnessTask in the package denoptim.task. At this point there is the discriminatio of running an internal or an external fitness provider. An internal fitness provider is mainly run by the package denoptim.fitness (this is the usual progress of a run). In case of an external fitness provider a Process Handler is used to retrieve data from an external program. 



3D Molecular Models Builder {#B3D}
---------------------------




Graph Isomorphism Analyzer {#GI}
--------------------------



Graph List Comparator {#CLG}
---------------------


## Other Packages

| Package | Description| 
| ------- | -----------|
| denoptim.logging | -----------|
| denoptim.utils | -----------|
| denoptim.json | -----------|
| denoptim.files | -----------|
| denoptim.integration.tinker | -----------|
| denoptim.task | -----------|


### Test

#### Unit Tests

The Unit Test classes can be found in their respective packages and they appear as an or more additional class/es. If you want to check on this independentely from the main packages you can find the Unit Tests on the voice __Files__ in the menu bar above.

#### Functional Tests

(?)
### How does it work

This documentation has the goal of showing, with relative detail, how DENOPTIM performs the runs layed out in the [User's manual](http://htmlpreview.github.io/?https://github.com/denoptim-project/DENOPTIM/blob/master/doc/user_manual.html). 
After a brief introduction, the documentation deals with each possible run that DENOPTIM can perform. The runs are described in order of relevance and in a sort of narrative fashion to clear the overarching logic of it but with extensive reference to the actual code.  
The next part of the documentation is about all those packages which are not specifically involved in only one type of run but operate on a more general level (e.g, denoptim.utils and denoptim.logging).
The final part is about Testing.