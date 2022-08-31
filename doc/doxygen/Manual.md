DENOPTIM Developer's Manual
===========================

Introduction
------------

DENOPTIM can perform different "exeperiments" called [Runs](#Programs) . Each run is defined by a enum variable called _RunType_ (variable from which the run is called from the command line) and it has a _ProgramTaskImpl_(ementer) that specifies the actual class which performs the run. Besides, every run has also a _description_ and  a boolean variable _isCLIEnabled_ to explicit wheter or not the run can be called from th CLI (Command Line Input).

DENOPTIM Command Line Input example 

    denoptim (-r) GA,FIT,FSE... file1 file2 ... fileN
             (-h)
             (-v)

The word denoptim stands for an alias for the DENOPTIM's path on the machine (it is the alias automatically generated 
when DENOPTIM is installed with Conda). The arguments in parentheses are the so-called _options_ that DENOPTIM has(see below), GA, FSE, etc are the _RunTypes_ and the files are the input files specified by the user.

The starting point of every run is the package denoptim.main  which contains the classes denoptim.main.Main, denoptim.main.CLIOptions , denoptim.main.Behavior.  

The class [CLIOptions](#denoptim.main.CLIOptions) defines the three possibility that DENOPTIM can perform from the command line.  
These are:  
__help (-h)__: prints the help message which as aid for the users     
__version (-v)__: prints the version of DENOPTIM running on the user's machine,   
__run (-r)__: option that, combined with a RunType (e.g., GA), performs one of the possible DENOPTIM runs.

The class [Behavior](#denoptim.main.Behavior) defines what a Behavior is. A behavior collects the information expressed in the command line and it is defined by a RunType, the parsed command line arguments, an exitStatus (integer vauable which if different from zero means an error has occurred), an error message (errorMsg) and a help message (helpMsg).

In the method [Main](#denoptim.main.Main), in the homonymous class, the first action is to define a Behavior ([defineProgramBehavior()](#denoptim.main.Main.defineProgramBehavior())). Once a behavior is defined and checked, the method main() constructs a so-called [Program Task](#denoptim.task.ProgramTask) which is defined by the _programTaskImpl_, a list of input files and a working directory.
Once all these arguments have been checked the method [runProgramTask()](#denoptim.main.Main.runProgramTask()) (in Main) performs the run, i.e., calls a [StaticTaskManager](#denoptim.task.StaticTaskManager) which submits the task (runs the respective Program Task with the relative arguments, i.e., files and working directory) and waits for its completion.

The program task implementers, aka the classes that actually perform the runs are to find in the packages __denoptim.programs."something"__ (e.g, for GA denoptim.programs.denovo). Usually, in these packages there is a class which is the program task implementer (e.g., GARunner for GA) and one class that defines and manages all the parameters necessary for then run (e.g., GAParameters for GA).
These classes then call different packages depending on the run. (e.g., for GA the main package is denoptim.ga).

These are the types of run and they, with their respective packages, will be discussed in depth in the next chapter.

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