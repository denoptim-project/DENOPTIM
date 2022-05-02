DENOPTIM

*De novo* Optimization of In/organic Molecules

| 

*Version 3.0.2, April 2022*

User Manual

| 

--------------

.. container::
   :name: Table of Contents1

   **Table of Contents**

   `About DENOPTIM <#Toc40894_1191730726>`__
   `Installation <#Toc14757_7999724461>`__

   -  `Install From Conda (recommended) <#Toc14757_7999724222>`__
   -  `Build From Source <#Toc14757_7999724223>`__

   `Introduction to DENOPTIM <#Toc14757_7999724462>`__

   -  `Representation of Chemical Entities in
      DENOPTIM <#Toc14747_799972446>`__

      -  `Graphs <#Toc43997_808352928>`__
      -  `Vertexes <#Toc14749_799972446>`__
      -  `Edges <#Toc14749_799972987>`__
      -  `Attachment Points and Attachment Point
         Class <#Toc14753_799972446>`__
      -  `Symmetry <#Toc43999_808352928>`__

   -  `Fitness evaluation <#Toc42056_808352928>`__

   `Programs of the DENOPTIM package <#Toc14759_799972446>`__

   -  `GUI: the Graphical User Interface <#Toc13237_799972445>`__
   -  `Genetic Algorithm Runner <#Toc13237_799972446>`__

      -  `Genetic Operations <#Toc40898_1191730726>`__
      -  `Substitution Probability <#Toc44001_808352928>`__
      -  `Parent Selection Strategies <#Toc40900_1191730726>`__
      -  `GA Parallelization Schemes <#Toc14761_799972446>`__
      -  `GA Run Input <#Toc17842_799972446>`__
      -  `GA Run Output <#Toc16845_799972446>`__
      -  `Interacting with Ongoing GA Run <#Toc17843_799972446>`__

   -  `FragSpaceExplorer <#Toc14852_799972446>`__

      -  `FragSpaceExplorer Input <#Toc17844_799972446>`__
      -  `FragSpaceExplorer Output <#Toc17846_799972446>`__
      -  `Restart FragSpaceExplorer from Checkpoint
         file <#Toc5490_1476255988>`__

   -  `Graph Editor <#Toc9999_2>`__
   -  `Genetic Operator <#Toc9999_4>`__
   -  `Fitness Provider Runner <#Toc9999_5>`__
   -  `3D Molecular Models Builder <#Toc9999_6>`__
   -  `Graph Isomorphism Analyzer <#Toc9999_7>`__
   -  `Graph List Comparator <#Toc9999_8>`__

   `Keywords <#Toc35546_1191730726>`__

   -  `Space of Graphs Building Blocks <#Toc40912_1191730726>`__
   -  `Ring closing machinery <#Toc40914_1191730726>`__
   -  `Genetic Algorithm (GA) Runner <#Toc40916_1191730726>`__
   -  `3D Molecular Models Builder <#Toc40918_1191730726>`__
   -  `FragmentSpaceExplorer <#Toc13097_799972446>`__
   -  `Fitness provider <#Toc13097_123456789>`__
   -  `Stand-alone Graph Editor <#Toc88888_1>`__
   -  `Stand-alone Fitness Runner <#Toc88888_3>`__
   -  `Stand-alone Graph Isomorphism Analyzer <#Toc88888_4>`__
   -  `Stand-alone Graph List Comparator <#Toc88888_5>`__

   `File Formats <#Toc40920_1191730726>`__

   -  `Vertexes <#Toc16847_799972446>`__
   -  `Compatibility Matrix file <#Toc14763_799972446>`__
   -  `Ring closures Compatibility Matrix file <#Toc16849_799972446>`__
   -  `Initial Population File <#Toc16851_799972446>`__
   -  `Candidate Chemical Entity Files <#Toc10339_2456711212>`__
   -  `Fitness Files <#Toc16853_799972446>`__
   -  `Graph Editing Task Files <#Toc9999_3>`__

--------------

About DENOPTIM
==============

DENOPTIM is an open source software suite for the *de novo* design of
chemical entities. The name *DENOPTIM* stands for "*De novo*
Optimization of In/organic Molecules" and wants to highlight that
DENOPTIM is designed to handle chemical entities way beyond the strict
formalisms of organic chemistry (i.e., valence rules). In fact, the
development of DENOPTIM started in
`2012 <https://doi.org/10.1021/ja300865u>`__ as a response to the need
of designing transition metal complexes and organometallic catalysts
that cannot not even be represented unambiguously by standard chemical
representations (e.g., multi-hapto metal-ligand complexes, ion pairs,
highly reactive catalyst intermediates).

How To Cite
-----------

To cite DENOPTIM, please cite `J. Chem. Inf. Model. 2019, 59, 10,
4077–4082 <https://doi.org/10.1021/acs.jcim.9b00516>`__

--------------

Installation
============

Install From Conda (recommended)
--------------------------------

DENOPTIM can be installed on using conda package manager from the
command line on Linux/MacOS. On Windows this can be done on the Anaconda
prompt or by using `Git Bash <%3Ccode%3EDENOPTIM_HOME%3C/code%3E>`__
terminal:

::

   conda install -c denoptim denoptim

You should now have the ``denoptim`` command available. Run the
following and verify you get a help message:

::

   denoptim -h

The installation from conda has been completed.

Build From Source
-----------------

DENOPTIM can be built from source using
`Maven <https://maven.apache.org/>`__.

First, download the `latest
release <https://github.com/denoptim-project/DENOPTIM/releases/latest>`__.
This is more than enough for regular users. However, if you are after
developing the DENOPTIM code then fork the repository and work with git.

Extract the zip/tar.gz archive you have downloaded to generate a folder
we'll call ``DENOPTIM_HOME``. In the following, remember to replace
``$DENOPTIM_HOME`` with the pathname of the extracted DENOPTIM's
distribution folder on your system.

Make sure you have an environment that includes JAVA and Maven. Such
environment can be created with by manual installation of both JAVA and
Maven manually, or it can be created using conda:

::

   cd $DENOPTIM_HOME
   conda env create -f environment.yml
   conda activate dnp_env

Verify the requirements by running the two commands: Both should return
a message declaring the respective versions.

::

   javac -version
   mvn -version

Now, you can build DENOPTIM with

::

   mvn package

Once maven has finished, you can create call DENOPTIM using a command
like the following (NB: replace $DENOPTIM_HOME and ${VERSION} as with
the values that apply to your installation):

On Linux/Mac and GitBash on Windows:

::

   java -jar $DENOPTIM_HOME/target/denoptim-${VERSION}-jar-with-dependencies.jar

On Windows Anaconda prompt:

::

   java -jar $DENOPTIM_HOME\target\denoptim-${VERSION}-jar-with-dependencies.jar

The installation from source has been completed.

--------------

Introduction to DENOPTIM
========================

Representation of Chemical Entities
-----------------------------------

Graphs
~~~~~~

DENOPTIM perceives chemical entities as graphs (outer graph in Figure
1): Graphs are collections of vertexes and edges. Vertexes and edges are
organized in a spanning tree (T), which is acyclic by definition, and a
set of fundamental cycles (Fc). The latter set defined the chords: edges
that do not belong to the spanning tree and that define cycles of
vertexes.

.. figure:: figures/graphs.png
   :alt: *Figure 1: Graph representation of chemical entities in
   DENOPTIM.*
   :width: 750px

   *Figure 1: Graph representation of chemical entities in DENOPTIM.*

Vertexes
~~~~~~~~

Vertexes are abstract objects: graph's building blocks that are
decorated with attachment points (AP, the arrows in Figure 1). One
attachment points represent the possibility to form one connection with
one other vertex.

Vertexes come in three forms:

-  **Molecular fragment**: chemical building blocks containing one or
   more atoms and bonds. Molecular fragments define the relation between
   attachment point on the vertex and atoms contained in the vertex,
   thus allowing, if needed, to represent open valences on molecular
   fragments as attachment points on vertexes. However, attachment
   points do not need to represent a given valence. The content of a
   molecular fragment vertex is constant.
-  **Template**: vertexes that embed a graph. The attachment points on a
   template vertex, if any, are the reflection of the attachment points
   of to vertexes belonging to the embedded graph that are not used to
   made connections in such embedded graph. The structure of the
   embedded graph's spanning tree and the identify of each vertex may be
   fixed, in which case the template is a contained for a constant
   sub-graph, or may be editable allowing, for instance, to alter the
   embedded graph while retaining the structure defined by the template.
-  **Empty vertexes**: can contain properties in any form, but do not
   contain atoms or embedded graphs. Empty vertexes are best interpreted
   as either property carriers or place-holding vertexes.

Independently on their nature, vertexes are further classified according
to their role as building blocks. There are three roles for vertexes:

-  *scaffolds* are vertexes used only to start the construction of a
   graph and are the only vertex that can be used for this purpose. In
   fact, the scaffold is always the seed of the spanning trees of
   automatically built graphs. This role is often granted to a
   chemically critical building block, for example the metal center in a
   transition metal complex.
-  *fragments* are vertexes that can be used freely in the construction
   of a graph, but cannot be used to start a new graph. This is the
   regular role for most building blocks.
-  *capping groups* are vertexes that have only one attachment point and
   are used to stop the grows of a chain off vertexes, typically to
   saturate open valences (see Capping procedure, below).

Edges
~~~~~

Edges represent an abstract relation between vertexes. An edge is the
connection between two attachment points. Edges are often the reflection
of chemical bonds, pseudo-bond, spatial or relational information.

Attachment Points and Attachment Point Class
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Attachment points are owned by vertexes and represent the possibility of
that specific vertex to form a connection, i.e., one edge, with one
attachment point that is owned by another vertex. Attachment points can
be decorated with information defining i) the atoms that holds an open
valence (if any), ii) the spatial arrangement of the connected vertex in
relation to the vertex holding the attachment point, and iii) a label
used to classify the attachment points.

The latter property, the label, is a string with no spaces specifying
the so-called **class of the attachment point** (or attachment point
class, APClass. The APClass can be defined also by the user (e.g., via
the GUI) as long as it adheres to the following syntax.

::

   <string>:<int>

where

-  ``<string>`` is an alphanumeric string, and
-  ``<int>`` is a positive integer number.

This syntax is best explain by providing an example of how molecular
fragment can be generated from whole molecules. In particular, we
consider our own fragmentation algorithm (see `J. Chem. Inf. Model.
2014, 54, 3, 767–780 <https://doi.org/10.1021/ci4007497>`__ -
implementation available at
https://github.com/denoptim-project/GM3DFragmenter). This fragmentation
algorithm uses cutting rules to identify the bond to break in order to
generate fragments. For each broken bond two attachment points are
generated (one on each side of the broken bond) and, in order to
distinguish between the two sides of the broken bond, these attachment
points are given two complementary APClasses. These APClasses share a
common part (the reference to the cutting rule) and have an additional
suffix that makes them distinguishable. For example, if the cutting rule
is named ``myCuttingRule``, the two APs will have APClass
``myCuttingRule:0`` and ``myCuttingRule:1``.

Notably, APClasses are a way to encode the chemical environment of a the
two sides of a bond that was broken to generate a pair of attachment
points. Conversely, this encoded information on chemical environment can
be exploited to control how attachment points are best combined to form
vertex-vertex connections. In particular, while APClasses can be used to
rebuilt only the types of connections that were originally broken in the
fragmentation procedure, they also allow to introduce compatibilities
across different APClasses. To this end, DENOPTIM used a so-called
Compatibility Matrix (see below) to define which combinations of
APClasses are to be considered as compatible when building or editing
graph by attaching or replacing vertexes.

The construction and editing of graphs is thus controlled by the APClass
compatibilities and independent from valence rules. This allows to
define a completely customizable set of rules for building any kind of
entity, including models of disconnected systems, active species and
transition states. However, in order to make sure that the entities
built this way are valid chemical entities, an additional procedure in
needed to saturate the free attachment points that, if left unused,
would result in unwanted open valences. This procedure is called
"capping" and it adds "capping groups" (see above) on specifically
defined free attachment points.

The use of capping groups is particularly useful when standard
“hydrogenation” routine (i.e., addition of H to every open valence) is
expected to fail. This occurs when the chemical entities under
construction are not standard organic molecules similar to those for
which “hydrogenation” tools have been trained.

The settings of the capping procedure define the list of APClasses that
need to be capped, and the APClasses of the capping groups to use for
each of the APClasses that need capping. These settings are part of the
Compatibility Matrix file (see below).

Symmetry
~~~~~~~~

DENOPTIM can repeat any graph operation on sets of APs that are related
by high degree of topological similarity. APs are considered symmetric
when they have

-  same attachment point class,
-  same element as the AP’s source atom (if any),
-  same type and number of bonds connected to the source atom (if any),
-  same element and number of atoms connected to the source atom (if
   any).

Users can control the probability of symmetric operations on specific
APClasses and include symmetry constraints also based on APClasses (see
keywords section, below).

Evaluation of Chemical Entities: Fitness Evaluation
---------------------------------------------------

The design of functional chemical entities depends on the capability of
evaluating how candidate entities are fit for a given purpose: the
evaluation of the fitness. In DENOPTIM, such evaluation aims to
associate the internal representation of a chemical entity, i.e., a
graph, with a numerical value, i.e., the fitness. For the sake of
generality, DENOPTIM does not take any assumption as to how the fitness
is calculated, but it assumes that a "fitness provider" is defined for
the purpose of evaluating the fitness of each candidate.

Fitness providers can be of two types:

-  internal fitness provider: it's DENOPTIM's code that is executed
   within the DENOPTIM virtual machine to calculate descriptors from
   chemical representation that are built on-the-fly for each chemical
   entity from its graph representation. The descriptors are then
   combined into a numerical fitness value according to an equation that
   must be defined in the input. The internal fitness provider does not
   perform any molecular modeling, such as geometry optimization and the
   like.
-  external fitness provider: is software external to DENOPTIM, and that
   DENOPTIM runs as a sub-process to compute the fitness of a chemical
   entity. Any ``bash`` or ``python`` script can be executed as an
   external fitness provider. An external fitness provider is expected
   to do the following tasks:

   #. Read the input defining the entity to evaluate from an `SDF file
      produced by DENOPTIM <#Toc10339_2456711212>`__.
   #. Perform any molecular modeling tasks, including, for example, 3D
      model construction, property calculations, output file parsing,
      and post processing.
   #. Calculate the fitness value from the results of the molecular
      modeling tasks.
   #. Create an output SDF file (see `Fitness
      Files <#Toc16853_799972446>`__) for storing the fitness value of
      the candidate entity (or a properly formatted error message) in
      addition to any useful output data that might be used to
      post-process the results.

   When DENOPTIM algorithms are asked to use an external fitness
   provider, they execute suck task, wait for completion, and retrieve
   the final results from the `output fitness
   file <#Toc16853_799972446>`__. Note that DENOPTIM programs do not
   interact with the running script.

--------------

DENOPTIM Programs
=================

Graphical User Interface
------------------------

The graphical user interface (GUI) is the main interface used for
preparing and inspecting DENOPTIM files and experiments. To launch the
GUI, you can chose one of the following:

-  if you installed via conda (recommended), you can run the command
   following command.

   .. code:: bash

      denoptim

-  alternatively, double click on the
   ``denoptim-${PKG_VERSION}-jar-with-dependencies.jar`` file, the
   location of which depends on how you have installed DENOPTIM: this
   file will be under ``target`` subfolder of the DENOPTIM distribution
   folder (the folder downloaded to install the release) if you have
   built DENOPTIM from source, of it will be under the
   ``${path_to_active_env_location}/lib/`` for installation via conda.

-  another alternative is to run the following command, again the
   ``${path_to_denoptim_jar}`` depends on how you have installed
   DENOPTIM (see previous point):

   .. code:: bash

       java -jar ${path_to_denoptim_jar}/denoptim-${PKG_VERSION}-jar-with-dependencies.jar 

For installation via conda, remember to activate the appropriate conda
environment before issuing any of the above command.

The GUI provides instruction to itself in the form of tool tip messages
(hold the cursor still over a button or plot to get a short message
explaining what is the functionality of that component) and
question-mark-buttons ("?") that can be clicked to display more detailed
instructions.

The GUI can be used to open files directly from the command line:

.. code:: bash

   denoptim path/to/file1 path/to/file2 path/to/file3 

The above command will open the three files if their format is
recognized as one of the file formats DENOPTIM's gui can open. These
include:

-  SDF and JSON files containing vertexes, graphs or candidate entities
-  text files containing list of `Keywords and
   values <#Toc35546_1191730726>`__ (i.e., input parameter files) and
   `Compatibility Matrix file <#Toc14763_799972446>`__.
-  folders containing the results of denovo and combinatorial design
   (see `GA Output <#Toc16845_799972446>`__ and `FragSpaceExplorer
   Output <#Toc17846_799972446>`__ respectively)

Finally, the GUI offers also the possibility to configure and run de
novo design and virtual screening experiments that can otherwise be run
by background processes as explained below. The threads executing
GUI-controlled experiments are fully dependent on the GUI, and will be
terminated upon closure of the GUI (the user receives a warning, in such
cases).

Genetic Algorithm Runner
------------------------

De novo design driven by a genetic algorithm using graphs as genome for
chemical entities. The overall schematic of the genetic algorithm (GA)
is shown in Figure 3. The algorithm begins with a population of
candidates that can be given as input or generated from scratch. Next, a
given number of generations are created by producing new offspring
either from application of the genetic operators, or by building new
entities from scratch.

.. figure:: figures/ea.gif
   :alt: *Figure 3: Evolutionary optimization scheme.*
   :width: 700px

   *Figure 3: Evolutionary optimization scheme.*

Genetic Operations
~~~~~~~~~~~~~~~~~~

Genetic operations are meant to generate graph from scratch (i.e.,
construction operator), from one parent (i.e., mutation operator), or
from a pair of parents (i.e., crossover operator). These events all have
to respect the APClass compatibility rules that, together with the
libraries of building blocks, define the space of graph building blocks
(BB Space). In addition, for symmetric graphs, addition or removal of a
vertex can be propagated across all sites related by symmetry. Notably,
the vertex corresponding to the scaffold is not subject to crossover or
mutation.

The construction operator generates candidate by taking random choices
on the scaffold vertex to use as graph seed, on the AP and incoming
vertexes to use to grow a branch layer-by-layer. The random events are
controlled by probabilities setting the likeliness of graph extension
events according to graph features such the number of atoms in the
corresponding chemical entity and the topological complexity (see
`Substitution Probability <#Toc44001_808352928>`__).

Mutation alters only one graph according to one of the following
mutation types:

-  *change branch*: removes a branch of the graph and builds a
   replacement.
-  *add link*: insets a vertex in between two previously-connected
   vertices.
-  *change link*: replaces a non-terminal vertex while retaining the
   connectivity with downstream branches.
-  *delete link*: removes a non-terminal vertex and reconnects the open
   ends generated by removal of the original vertex.
-  *delete chain*: removes an entire branch in acyclic graphs or the
   part of a cyclic graph between two branching points thus opening the
   ring by removing the bridging chain.
-  *extend*: extends a branch of the graph.
-  *delete*: removes a branch starting from a vertex.

The mutation operator can be configured to perform also multi-site
mutations: mutation that can alter a graph more multiple times on
possibly independent location and performing independent mutation types.

Crossover involves the swapping of one subgraph from each of the parents
to the other parent. The subgraphs to swap (a.k.a. the crossover sites)
need not to be terminal (i.e., crossover is not limited to dangling
branches) and can be of any size, from a single vertex to an entire
branch attached to the scaffold. When template vertexes are present in
the parent graphs, crossover sites are defined so that the subgraphs to
swap belong to graph that are at the same level of embedding in the
recursive template-graph structure on the two parents. Moreover, the
subgraphs can include only vertices belonging to the same level of
embedding. Finally, while crossover generates two offspring from two
parents, only one offspring is considered to be the product of a single
crossover event.

Substitution Probability
~~~~~~~~~~~~~~~~~~~~~~~~

The generation of very large graphs or graphs with too many incident
edges on a single vertex can be reduced by tuning the so-called
substitution probability: the probability of appending a vertex on to a
free AP. The substitution probability (*P*) results from the product of
two components: the growth probability (*P\ g*), and the crowding
probability (*P\ c*). The growth probability (*P\ g*) limits the growth
of an entity. This limit can be expressed in terms of graph deepness
(i.e., using the *level* of a vertex to determine the likeliness of
further graph extension on that vertex, see Figure 4) or in terms of
molecular size (i.e., number of heavy atoms, irrespectively of graph
size).

.. figure:: figures/graph_levels.png
   :alt: *Figure 4: identification of levels in a graph. Each label L\ x
   identifies the shell of vertexes of level X. Red, dashed lines
   represent the border between levels. Yellow circles represent free
   APs. Squares of different color represent vertexes of different kind
   (scaffold, normal vertex, or capping group). To illustrate, vertex 1
   is the scaffold vertex (i.e., represented by a red square) and is in
   level -1 (L\ -1), vertexes 2 (normal vertex represented by blue
   square), 3 (another normal vertex), and 5 (a capping group
   represented by a green square) are at level 0 (L\ 0), and so on.*
   :width: 429px

   *Figure 4: identification of levels in a graph. Each label L\ x
   identifies the shell of vertexes of level X. Red, dashed lines
   represent the border between levels. Yellow circles represent free
   APs. Squares of different color represent vertexes of different kind
   (scaffold, normal vertex, or capping group). To illustrate, vertex 1
   is the scaffold vertex (i.e., represented by a red square) and is in
   level -1 (L\ -1), vertexes 2 (normal vertex represented by blue
   square), 3 (another normal vertex), and 5 (a capping group
   represented by a green square) are at level 0 (L\ 0), and so on.*

The **growth probability** can be controlled by one of the following
schemes (where *L* is either the vertex *level* or the number of heavy
atoms):

-  **EXP_DIFF**: Given a growth multiplier factor *λ*, the probability
   of addition is given by
   |figures/exp_diff.png|
-  **TANH**: Given a growth multiplier factor *λ*, the probability of
   addition is determined by
   |figures/tanh.png|
-  **SIGMA**: This scheme is based on two factors σ\ :sub:`1` (the
   steepness of the function where the probability is 50%) and
   σ\ :sub:`2` (the value of *L* where the probability is 50%), and
   calculates the probability as
   |figures/sigma.png|
-  **UNRESTRICTED**: This scheme allows for unrestricted growth (*p = 1*
   at all levels).

The **crowding probability** is meant to reduce the likeliness of adding
incident edges to a vertex that is already using one or more APs rooted
on the same source atom that holds the AP for which we are evaluating
the substitution probability. Practically, for a free AP (*AP\ i*) we
define the *crowdedness* (*C\ AP\ i*) as the number of non-free APs that
are rooted on the same source atom holding *AP\ i*.

For example, we consider vertex 3 in Figure 4 (i.e., the blue square at
the center of the figure), and assume that the molecular representation
of this vertex consists of a single atom holding four attachment points.
To calculate the crowdedness of *AP\ 2* (i.e., the yellow circle located
below the blue square representing vertex 3) we count the number of APs
that i) share the same source atom of *AP\ 2*, and ii) are used to form
edges with vertexes that are not capping groups. While condition *i* is
satisfied by all four APs of the vertex, only two APs satisfy condition
*ii*. The crowdedness of *AP\ 2* is thus *C\ AP\ 2 = 2*.

The crowding probability for *AP\ i* is then calculated using the
crowdedness *C\ AP\ i* according to any of the schemes presented above
(**EXP_DIFF**, **TANH**, **SIGMA**, **UNRESTRICTED**) using *C\ AP\ i*
in place of *L*.

Parents Selection Strategies
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

An important aspect of any evolutionary algorithm is that of the
selection of the best set of individuals (chromosomes) for the next
generation. It is also essential to maintain enough diversity in the
genetic material so that the solution/chemical space is sufficiently
covered. In DENOPTIM the following selection strategies are available:

-  **Random**: In this scheme, chromosomes are randomly selected.
-  **Tournament (TS)**: In this scheme, a group of size n (typically 2)
   is randomly chosen from the current population. The fittest from the
   group is then added to the mating pool.
-  **RouletteWheel (RW)**: In this sampling scheme, each chromosome is
   assigned a portion/slice of the roulette wheel, the area of the
   portion being proportional to its fitness. The wheel is then spun a
   number of times and the chromosome corresponding to the slice that
   comes under the wheel marker is added to the pool.
-  **Stochastic universal sampling (SUS)**: differs from RW in that it
   uses *N* (number of selections required) equally spaced pointers. The
   population is shuffled randomly and a single random number *r* in the
   range [0, *1/N*] is generated. The *N* individuals are then chosen by
   generating the *N* pointers, starting with *r* and spaced by *1/N*,
   and selecting the individuals whose fitness spans the positions of
   the pointers. Because the individuals are selected entirely based on
   their positions in the population, SUS has zero bias.

GA Parallelization Schemes
~~~~~~~~~~~~~~~~~~~~~~~~~~

Two parallelization schemes based on the consumer-producer pattern have
been implemented. In the following, a batch implies a set of jobs, i.e.,
fitness function evaluations (generally equal to the number of
processors available) submitted for parallel execution. The first such
scheme (SCH-1) uses a serial batch processing scheme wherein, the
subsequent batches can only be submitted on completion of the previous.
This implies that while some jobs may be completed sooner than others,
the program will still wait for the other submitted tasks to complete
before submitting another batch.

To remove this waiting time, the second scheme (SCH-2) enables
continuous submission so that new jobs may be launched whenever free
threads are available. Notably, a side effect of this scheme is that
results from a long running calculation, for e.g. started in
``Generation #1``, may only become available in ``Generation #20``. In
such cases, the evaluated molecule will become part of the population
only in generation #20. The fixed generational behavior of the GA is
subsequently lost.

GA Run Input
~~~~~~~~~~~~

A GA run can be performed either by defining the input in the GUI and
running the experiment from the gui, or by issuing this command:

.. code:: bash

   denoptim -r GA input_parameters_file

where ``input_parameters_file`` is a text file collecting the
configuration parameters for the GA program. Such file can have any name
and contains one input parameter per line. The ``#`` character is used
to denote a comment and lines starting with this character are not
processed. Each input parameter is specified by means of a keyword. The
complete list of keywords is available in the
`Keywords <#Toc35546_1191730726>`__ section. Examples of input files can
be found in the test folder. Note that ``input_parameters_file`` file
can be generated with the GUI from "New Evolutionary De Novo Design"
button.

GA Run Output
~~~~~~~~~~~~~

The program outputs a number of files during its execution. At the
beginning of the execution, the program creates a directory
``RUNYYYYMMDDHHMMSS`` and a log file ``RUNYYYYMMDDHHMMSS.log`` that are
named according to the date (``YYYYMMDD``) and time (``HHMMSS``). All
relevant files generated by DENOPTIN associated with the current run are
stored in this folder. Instead, files managed by external fitness
provider script are not directly used by the GA runner code and can thus
be placed anywhere. Files pertaining to a specific generation are stored
in sub-folders ``RUNYYYYMMDDHHMMSS/Gen#`` (where ``#`` is the generation
number), while the final results of an evolutionary experiment are
collected in sub-folder ``RUNYYYYMMDDHHMMSS/Final`` (see Figure 5).

.. figure:: figures/ga_folders.png
   :alt: *Figure 5: Directory structure resulting from a GA run.*
   :width: 500px

   *Figure 5: Directory structure resulting from a GA run.*

Once a generation is completed, the software creates a summary of the
current population. For instance, the summary for generation ``N``,
where ``N`` is an integer number, is available as a TXT file
(``GenN/GenN.txt``).

The output from an ongoing or completes GA experiment can be inspected
with the GUI:

.. code:: bash

   denoptim /path/to/RUNYYYYMMDDHHMMSS

Interacting with Ongoing GA Runs
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

During a GA run the user often desires to intervene with actions
altering the course of the experiment. The GARunner runs a command
listener that checks to instructions deposited in a dedicated location
of the file system. Depending on the instruction found, DENOPTIM will
try to perform the corresponding tasks as soon as compatible with the
ongoing algorithm. Once an instruction has been found and interpreted by
DENOPTIM (this event is reported in the log file), it cannot be
undone/overwritten, even if it is still to be performed.

The user can formulate instruction in the form of properly formatted
ascii files (i.e., an *intructions_file*) that are deposited under the
``RUNYYYYMMDDHHMMSS/interface`` folder, i.e., a file-system location
that is specific for a single GA run. The *intructions_file* has effects
only on the specific GA run that generated the ``RUNYYYYMMDDHHMMSS``
folder.

The *intructions_file* is a text file. The name of any such file is
irrelevant: any file will be read irrespectively of its name. Each
*intructions_file* is red line-by-line: each line can be arbitrarily
long and will be interpreted as a single instruction. Each line begins
with a keyword that defines the kind of instruction. The keyword is
separated from the rest of the line by a blank space. After the blank
space a variable number of arguments can be given depending on the
specific instruction (see below). Currently the following instructions
can be given to Denoptim:

-  Terminate the GA experiment. Keyword: ``STOP_GA``; Arguments: no
   further arguments required. Just type the ``STOP_GA`` keyword.
-  Remove one or more candidate from the current population. Keyword
   ``REMOVE_CANDIDATE``, Arguments: space-separated list of candidate
   names (e.g., ``REMOVE_CANDIDATE M00000028 M00000031`` asks to remove
   both candidates from the population, if they are still in the
   population).
-  Evaluate one or more manually generated candidates and possibly
   include them into the population. Keyword ``ADD_CANDIDATE``,
   Arguments: space-separated list of pathnames to files defining the
   candidate to be added (e.g.,
   ``ADD_CANDIDATE /tmp/candidate_1.sdf /tmp/candidate_2.sdf``)

FragSpaceExplorer
-----------------

FragSpaceExplorer performs systematic exploration of the fragment space,
or, more appropriately, of the space of graph building blocks - BB
Space). The tool can be used to visit all the combination of building
blocks that can be obtained from a given starting point (i.e., one or
more graphs) and a defined BB Space, i.e., an ensemble of building
blocks, connection rules and constraints. FragSpaceExplorer can be used
for combinatorial approaches, where a number of alternative chemical
entities need to be generated and evaluated, and also to enumerate
entities or just to appreciate the characteristics of a designed BB
Space. For instance, one may want to know how many acceptable chemical
entities are encoded in a BB Space one has configured, or the amount of
redundancy in such space (i.e., number of different graphs that encode
the same chemical entity).

A set of initial graphs (from now on referred as to the roots) can be
given as input or build from a library of scaffolds. FragSpaceExplorer
iterates over all roots and all the combination of building blocks:
vertexes of any type, including molecular fragments, capping groups, if
any, and the “empty fragment”, which represents the possibility of
leaving the AP unused if permitted. Only one vertex is appended on each
attachment point, thus each root is coated by vertexes layer-by-layer.
The graphs generated by adding N layers of vertexes belong to the
L\ :sub:`N-1` and are used as roots to create the graphs of L\ :sub:`N`
(Figure 6).

.. figure:: figures/fse_levels.png
   :alt: *Figure 6: Numbering of the levels (L) in FragSpaceExplorer.*
   :width: 300px

   *Figure 6: Numbering of the levels (L) in FragSpaceExplorer.*

The exploration terminates with the exhaustion of the combinatorial
space or with the completion of the maximum allowed level set by the
user (see `Keywords <#Toc35546_1191730726>`__).

All graphs are complete (i.e., no capping groups needed, no forbidden
free AP found) and acceptable (i.e., respecting all constraints imposed
by the user; for instance max. number of heavy atoms, max. molecular
weight, etc.) can be sent to a fitness evaluation task performed by an
internal or external fitness provider. For example, the external subtask
may include 3D model construction and property evaluation, or
calculation of a fitness. Such fitness is recorded for each candidate,
but is not used to guide the combinatorial search in any way.

FragSpaceExplorer Input
~~~~~~~~~~~~~~~~~~~~~~~

A FragSpaceExplorer run or FSE run) can be performed either by defining
the input in the GUI and running the experiment from the gui, or by
issuing this command:

.. code:: bash

   denoptim -r FSE input_parameters_file

where ``input_parameters_file`` is a text file collecting the
configuration parameters for the FragSpaceExplorer program. In
particular, the only required input is the `definition of the BB
Space <#Toc40912_1191730726>`__. The ``input_parameters_file`` file can
have any name, and contains one input parameter per line. The ``#``
character is used to denote a comment and lines starting with this
character are not processed. Each input parameter is specified by means
of a keyword. The complete list of keywords is available in the
`Keywords <#Toc35546_1191730726>`__ section. Examples of input files can
be found in the test folder. Note that ``input_parameters_file`` file
can be generated with the GUI from "New Virtual Screening" button.

FragSpaceExplorer Output
~~~~~~~~~~~~~~~~~~~~~~~~

The output generated by FragSpaceExplorer is structured as the following
folder tree:

-  ``FSEYYYYMMDDHHMMSS.log`` (log file)
-  ``FSEYYYYMMDDHHMMSS.chk`` (checkpoint file)
-  ``FSEYYYYMMDDHHMMSS``

   -  ``FSE-Level_-1``

      -  ``FSE-Level_-1.txt`` list of generated graphs as simplified
         (human readable) strings.
      -  ``dg_1.json`` serialized graph representation of chemical
         entity with graphID=1
      -  ``dg_2.json`` serialized graph representation of chemical
         entity with graphID=2
      -  …

   -  … other ``FSE-Level-`` folders

The output from an ongoing or completes FSE experiment can be inspected
with the GUI:

.. code:: bash

   denoptim /path/to/FSEYYYYMMDDHHMMSS

Restart FragSpaceExplorer from Checkpoint file
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

FragSpaceExplorer can be very long, so a checkpoint file is dumped at
regular time intervals (by default every N graphs, where N=100; see
`Keywords <#Toc35546_1191730726>`__ section) to keep track of where the
exploration has arrived and, in case of abrupt termination of the
execution, restart the FSE experiment from the checkpoint file.
Calculation that were running when the termination occurred or that were
submitted after the generation of the checkpoint file are considered to
be lost and will be re-submitted upon restart of the FSE run. Therefore,
to avoid duplicates the files related to those “lost” calculations
should be collected and isolated.

Graph Editor
------------

Graph Editor is a program for editing graphs from the command line. This
allows embedding a graph edit task in any workflow external to denoptim.
For example, in the fitness evaluation workflow of an external provider.

The graph editor can be run with this command:

.. code:: bash

   denoptim -r GE input_parameters_file

where ``input_parameters_file`` is a text parameters file with the
`keywords <#Toc35546_1191730726>`__ providing all input parameters.
Among these settings there is a text-based definition of the graph
editing task. See `Graph Editing Task Files <#Toc9999_3>`__.

Genetic Operator
----------------

This is a program for performing genetic operations from the command
line.

This program can be run with this command:

.. code:: bash

   denoptim -r GO input_parameters_file

where ``input_parameters_file`` is a text parameters file with the
`Keywords <#Toc35546_1191730726>`__ providing all input parameters.

Fitness Provider Runner
-----------------------

This is a program to lauch a fitness provider (internal or external) in
the same way as done from within GA or FSE runs, but straight from the
command line-

This program can be run with this command:

.. code:: bash

   denoptim -r FIT input_parameters_file

where ``input_parameters_file`` is a text parameters file with the
`Keywords <#Toc35546_1191730726>`__ providing all input parameters.

3D Molecular Models Builder
---------------------------

This is a program for converting graph representation into a 3D chemical
representation by exploring the 3D features of building blocks and, if
needed, perform a ring-closing conformational search (see `J. Chem. Inf.
Model. 2015, 55, 9
1844-1856 <https://doi.org/10.1021/acs.jcim.5b00424>`__).

This program can be run with this command:

.. code:: bash

   denoptim -r 3DB input_parameters_file

where ``input_parameters_file`` is a text parameters file with the
`Keywords <#Toc35546_1191730726>`__ providing all input parameters.

**WARNING:** This program requires `Tinker Molecular Modeling
software <https://dasher.wustl.edu/tinker/>`__ to be installed on the
system. The user is responsible for installing Tinker in accordance with
its license terms. Furthermore, the ring-closing conformational search
requires a customized version of Tinker. Contact
denoptim.project@gmail.com for instructions on how to modify Tinker and
use the customized version in accordance with Tinker license's terms.

Graph Isomorphism Analyzer
--------------------------

This is a program that analyzes graph aiming to detect
"DENOPTIM-isomorphism". "DENOPTIM-isomorphic" is a DENOPTIM-specific
definition of `graph
isomorphism <https://mathworld.wolfram.com/IsomorphicGraphs.html>`__
that differs from the most common meaning of isomorphism in graph
theory. In general, graph are considered undirected when evaluating
DENOPTIM-isomorphism. Next, since a graph is effectively a spanning tree
(ST_i={{vertexes}, {acyclic edges}}) with a set of fundamental cycles
(FC_i={C_1, C_2,...C_n}), any graph G={ST_i,FC_i} that contains one or
more cycles can be represented in multiple ways, G={ST_j,FC_j} or
G={ST_k,FC_k}, that differ by the position of the chord/s and by the
corresponding pair of ring-closing vertexes between which each chord is
defined. The DENOPTIM-isomorphism for two graphs G1 and G2 is given by
the common graph theory isomorphism between two undirected graphs U1 and
U2 build respectively from G1 and G2.

Finally,

-  vertexes are compared excluding their vertex ID,
-  edges are considered undirected and compared considering the bond
   type they declare and the identity of the attachment points connected
   thereby. This latter point has an important implication: two
   apparently equal graphs (same vertexes that are connected to each
   other forming the same vertex-chains) can be non-isomorphic when the
   APs used to connect two vertexes are not the same. Chemically, this
   means the stereochemistry around one or both vertexes, is different
   in the two graphs. Therefore two otherwise equal-looking graphs can
   very well be, de facto, not DENOPTIM-isomorphic.

This method makes use of the Vento-Foggia VF2 algorithm (see
`DOI:10.1109/TPAMI.2004.75 <http://ieeexplore.ieee.org/xpl/articleDetails.jsp?arnumber=1323804>`__)
as implemented in `JGraphT <https://jgrapht.org/>`__.

**WARNING:** Detection of isomorphism can be very slow for pathological
cases and for graphs with large symmetric systems!

The Graph Isomorphism Analyzer program can be run with this command:

.. code:: bash

   denoptim -r GI input_parameters_file

where ``input_parameters_file`` is a text parameters file with the
`Keywords <#Toc35546_1191730726>`__ providing all input parameters.

Graph List Comparator
---------------------

This program compares lists of graphs seeking for "DENOPTIM-isomorphic"
graphs (see `here <Toc9999_7>`__ for the definition of
"DENOPTIM-isomorphic").

This program can be run with this command:

.. code:: bash

   denoptim -r CGL input_parameters_file

where ``input_parameters_file`` is a text parameters file with the
`Keywords <#Toc35546_1191730726>`__ providing all input parameters.

--------------

Keywords
========

When preparing an input file remember these underlying conventions:

-  keyword can be accompanied by:

   -  no value (e.g., ``KEYWORD``)
   -  one value (e.g., ``KEYWORD=VALUE``),
   -  more space separated values (e.g., ``KEYWORD=VALUE1 VALUE2``);

-  keywords are case insensitive, but values are case sensitive;
-  the order of keywords in the file is irrelevant;
-  unless otherwise specified (some keyword do exploit repetition as a
   way to provide additional input), only the last occurrence of a
   keyword define the used value of the corresponding parameter.

The following tables list all the keywords grouped according to the main
functionality affected by the use of a keyword. Unless otherwise
specified, the use of the keywords is optional as most parameters have a
default value. Since, the settings are reported in the beginning of the
log file, the default value of a specific keyword can be found in the
log file of a run performed without providing that specific keyword in
the input file.

| 

Definition of the Space of Graph Building Blocks (BB Space)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

+---------------------------+------------------------------------------+
| Keyword                   | Description                              |
+---------------------------+------------------------------------------+
| General                   |                                          |
+---------------------------+------------------------------------------+
| ``FS-ScaffoldLibFile``    | Specifies the pathname of the file       |
|                           | containing the list of scaffolds.        |
|                           | **[REQUIRED]**                           |
+---------------------------+------------------------------------------+
| ``FS-FragmentLibFile``    | Specifies the pathname of the file       |
|                           | containing the list of fragments.        |
|                           | **[REQUIRED]**                           |
+---------------------------+------------------------------------------+
| ``FS                      | Specifies the pathname of the file       |
| -CappingFragmentLibFile`` | containing the list of capping groups.   |
+---------------------------+------------------------------------------+
| ``FS-CompMatrixFile``     | Specifies the pathname of the file       |
|                           | containing the compatibility matrix and  |
|                           | related information such as the AP-Class |
|                           | to bond order map, the capping map, and  |
|                           | the list of forbidden ends.              |
+---------------------------+------------------------------------------+
| ``FS-RCCompMatrixFile``   | Specifies the pathname of the file       |
|                           | containing the compatibility matrix for  |
|                           | ring closures.                           |
+---------------------------+------------------------------------------+
| ``FS-RotBondsDefFile``    | Specifies the pathname of the file       |
|                           | containing the definition of the         |
|                           | rotatable bonds by SMARTS.               |
+---------------------------+------------------------------------------+
| Graph filtering criteria  |                                          |
+---------------------------+------------------------------------------+
| ``FS-MaxHeavyAtom``       | Specifies the maximum number of heavy    |
|                           | (non-hydrogen) atoms for a candidate.    |
+---------------------------+------------------------------------------+
| ``FS-MaxMW``              | Specifies the maximum molecular weight   |
|                           | accepted for a candidate.                |
+---------------------------+------------------------------------------+
| ``FS-MaxRotatableBond``   | Specifies the maximum number of          |
|                           | rotatable bonds accepted for a           |
|                           | candidate.                               |
+---------------------------+------------------------------------------+
| Symmetry                  |                                          |
+---------------------------+------------------------------------------+
| ``FS-EnforceSymmetry``    | Requires to introduce constitutional     |
|                           | symmetry whenever possible. Corresponds  |
|                           | to setting the symmetric substitution    |
|                           | probability to 100%.                     |
+---------------------------+------------------------------------------+
| ``FS-ConstrainSymmetry``  | Introduces a constraint in the symmetric |
|                           | substitution probability. Requires two   |
|                           | arguments: the attachment point class to |
|                           | constrain (string), and the imposed      |
|                           | value of the symmetric substitution      |
|                           | probability (double 0.0-1.0). The        |
|                           | constraints defined by this keyword      |
|                           | overwrite the symmetric substitution     |
|                           | probability defined by GA parameters,    |
|                           | and the requirement of the               |
|                           | ``FS-EnforceSymmetry`` keyword.          |
|                           |                                          |
|                           | Multiple constraints can be defined one  |
|                           | by one.                                  |
|                           |                                          |
|                           | |                                        |
|                           |                                          |
|                           | Example:                                 |
|                           |                                          |
|                           | ``FS-Constra                             |
|                           | inSymmetry=apClassA:0             0.25`` |
|                           |                                          |
|                           | ``FS-Constr                              |
|                           | ainSymmetry=apClassB:1             0.0`` |
+---------------------------+------------------------------------------+

| 

Ring-Closing machinery
~~~~~~~~~~~~~~~~~~~~~~

+--------------------------------+-------------------------------------+
| Keyword                        | Description                         |
+--------------------------------+-------------------------------------+
| General                        |                                     |
+--------------------------------+-------------------------------------+
| ``RC-CloseRings``              | Requires the possibility of closing |
|                                | rings of fragments to be evaluated  |
|                                | for all graph containing ring       |
|                                | closing vertices. Also requires     |
|                                | chemical structures to be generated |
|                                | accordingly. No value needed.       |
+--------------------------------+-------------------------------------+
| ``RC-Verbosity``               | Specifies the verbosity level of    |
|                                | the ring closing machinery.         |
+--------------------------------+-------------------------------------+
| Ring related graph filtering   |                                     |
| criteria                       |                                     |
+--------------------------------+-------------------------------------+
| ``RC-MinNumberOfRingClosures`` | Specifies the minimum number of     |
|                                | rings to be closed in order to      |
|                                | accept a candidate graph.           |
+--------------------------------+-------------------------------------+
| ``RC-MaxNumberRingClosures``   | Specifies the maximum number of     |
|                                | ring closures per graph.            |
+--------------------------------+-------------------------------------+
| ``RC-MinRCAPerTypePerGraph``   | Specifies the minimum number of     |
|                                | ring closing attractors of the same |
|                                | type per graph.                     |
+--------------------------------+-------------------------------------+
| ``RC-MaxRCAPerTypePerGraph``   | Specifies the maximum number of     |
|                                | ring closing attractors of the same |
|                                | type that are accepted for a single |
|                                | graph.                              |
+--------------------------------+-------------------------------------+
| Ring Closability Conditions    |                                     |
+--------------------------------+-------------------------------------+
| ``                             | Defined the closability condition's |
| RC-EvaluationClosabilityMode`` | evaluation mode:                    |
|                                |                                     |
|                                | 0. only constitution of candidate   |
|                                |    ring,                            |
|                                | 1. only closability of 3D chain,    |
|                                | 2. both 0 and 1.                    |
+--------------------------------+-------------------------------------+
| ``RC-RequiredElementInRings``  | Specifies the elemental symbol that |
|                                | has to be contained in all          |
|                                | acceptable rings of fragments. The  |
|                                | shortest path is used to evaluate   |
|                                | this ring closing condition.        |
+--------------------------------+-------------------------------------+
| ``RC-ClosableRingSMARTS``      | Specifies a single constitutional   |
|                                | ring closability condition by a     |
|                                | single SMARTS string. This keyword  |
|                                | may be used multiple times to       |
|                                | provide a list of constitutional    |
|                                | ring closability conditions.        |
|                                |                                     |
|                                | Example:                            |
|                                |                                     |
|                                | ``RC-ClosableRingSMARTS=C1CCCCC1``  |
|                                |                                     |
|                                | ``RC-ClosableRingSMARTS=C1CCCCCC1`` |
+--------------------------------+-------------------------------------+
| ``RC-RingSizeBias``            | Specifies the bias associated to a  |
|                                | given ring size when selecting the  |
|                                | combination of rings (i.e., RCAs)   |
|                                | for a given graph.                  |
|                                |                                     |
|                                | The syntax is:                      |
|                                |                                     |
|                                | ``RC-RingS                          |
|                                | izeBias=<size>             <bias>`` |
|                                |                                     |
|                                | Multiple occurrence of this keyword |
|                                | can be used.                        |
|                                |                                     |
|                                | Example: the following lines give   |
|                                | to all 6-member rings a probability |
|                                | of being formed that is twice that  |
|                                | given to all 5-member rings.        |
|                                | Instead 7-membered rings will never |
|                                | be formed.                          |
|                                |                                     |
|                                | ``RC-RingSizeBias=5 1``             |
|                                |                                     |
|                                | ``RC-RingSizeBias=6 2``             |
|                                |                                     |
|                                | ``RC-RingSizeBias=7 0``             |
+--------------------------------+-------------------------------------+
| ``RC-MaxSizeNewRings``         | Specifies the maximum number of     |
|                                | ring members for rings created from |
|                                | scratch.                            |
+--------------------------------+-------------------------------------+
| ``                             | Requires evaluation of              |
| RC-CheckInterdependentChains`` | interdependent closability          |
|                                | condition. WARNING: this function   |
|                                | require exhaustive conformational   |
|                                | search, which is very time          |
|                                | consuming.                          |
+--------------------------------+-------------------------------------+
| Search for ring closing        |                                     |
| conformations in 3D            |                                     |
+--------------------------------+-------------------------------------+
| ``RC-MaxRotBonds``             | Specifies the maximum number of     |
|                                | rotatable bonds for which 3D chain  |
|                                | closability is evaluated. Chains    |
|                                | with a number of rotatable bonds    |
|                                | higher than this value are assumed  |
|                                | closable.                           |
+--------------------------------+-------------------------------------+
| ``RC-ConfSearchStep``          | Specifies the torsion angle step    |
|                                | (degrees) to be used for the        |
|                                | evaluation of 3D chain closability  |
|                                | by scanning the torsional space.    |
+--------------------------------+-------------------------------------+
| ``RC-ExhaustiveConfSearch``    | Requires the search for closable    |
|                                | conformations to explore the        |
|                                | complete rotational space. WARNING: |
|                                | this is very time consuming, but is |
|                                | currently needed to evaluate        |
|                                | closability of interdependent       |
|                                | chains.                             |
+--------------------------------+-------------------------------------+
| ``RC-LinearityLimit``          | Specifies the bond angle above      |
|                                | which the triplet of atoms is       |
|                                | considered linear.                  |
+--------------------------------+-------------------------------------+
| ``RC-RCCIndex``                | Specifies the pathname of the text  |
|                                | file containing the previously      |
|                                | encountered candidate closable      |
|                                | chains. This file constitutes the   |
|                                | index of the archive of ring        |
|                                | closing conformations.              |
+--------------------------------+-------------------------------------+
| ``RC-RCCFolder``               | Specifies the pathname of the root  |
|                                | folder containing the archive of    |
|                                | ring closing conformations.         |
+--------------------------------+-------------------------------------+
| Evaluation of ring closure in  |                                     |
| 3D conformations               |                                     |
+--------------------------------+-------------------------------------+
| ``RC-MaxDotProd``              | Specifies the maximum value that is |
|                                | considered acceptable for the dot   |
|                                | product of the AP-vectors at the    |
|                                | two end of a closing chain.         |
+--------------------------------+-------------------------------------+
| ``RC-DistanceToleranceFactor`` | Specifies the absolute normal       |
|                                | deviation of the ideal value (a     |
|                                | value between 0.0 and 1.0) that is  |
|                                | considered acceptable for distances |
|                                | when evaluating the 3D ring         |
|                                | closability of a conformation.      |
+--------------------------------+-------------------------------------+
| ``RC-                          | Specifies the factor multiplying    |
| ExtraDistanceToleranceFactor`` | the tolerance for inter-atomic      |
|                                | distances when evaluating the       |
|                                | closability of a chain by a         |
|                                | discrete (vs. continuous)           |
|                                | exploration of torsional space.     |
+--------------------------------+-------------------------------------+

| 

GA Runner
~~~~~~~~~

+---------------------------+------------------------------------------+
| Keyword                   | Description                              |
+---------------------------+------------------------------------------+
| General                   |                                          |
+---------------------------+------------------------------------------+
| ``GA-PrecisionLevel``     | Specifies the number of figures used to  |
|                           | report the fitness.                      |
+---------------------------+------------------------------------------+
| ``GA-MonitorDumpStep``    | The monitor dump is a record of how many |
|                           | attempted and failed operations are      |
|                           | performed to produce new candidate       |
|                           | population members. The record is        |
|                           | printed every N attempts to build a new  |
|                           | candidate, and N can be specified by     |
|                           | ``GA-MonitorDumpStep``.                  |
+---------------------------+------------------------------------------+
| ``GA-MonitorFile``        | Specifies the pathname where to write    |
|                           | monitor dumps and summaries. Dumps are   |
|                           | snapshots taken with a frequency         |
|                           | controlled by ``GA-MonitorDumpStep``,    |
|                           | while summaries are printed at the end   |
|                           | of every generation.                     |
+---------------------------+------------------------------------------+
| ``GA-RandomSeed``         | Specifies the seed number used by the    |
|                           | random number generator.                 |
+---------------------------+------------------------------------------+
| ``GA-                     | Requires to sort the candidates          |
| SortByIncreasingFitness`` | according to ascending rather than       |
|                           | descending fitness.                      |
+---------------------------+------------------------------------------+
| Genetic Algorithm         |                                          |
+---------------------------+------------------------------------------+
| ``GA-PopulationSize``     | Specifies the number of individuals in   |
|                           | the population.                          |
+---------------------------+------------------------------------------+
| ``GA-NumChildren``        | Specifies the number of children to be   |
|                           | generated for each generation.           |
+---------------------------+------------------------------------------+
| ``GA-NumGenerations``     | Specifies the maximum number of          |
|                           | generation.                              |
+---------------------------+------------------------------------------+
| ``GA-NumConvGen``         | Specifies the number of identical        |
|                           | generations before convergence is        |
|                           | reached.                                 |
+---------------------------+------------------------------------------+
| ``G                       | Controls the maximum number of attempts  |
| A-MaxTriesPerPopulation`` | to build a new graph, so that the        |
|                           | maximum number of attempts to build a    |
|                           | new graph is given by the size of the    |
|                           | population, times the factor given by    |
|                           | the                                      |
|                           | ``GA-MaxTriesPerPopulation``\ keyword.   |
+---------------------------+------------------------------------------+
| ``G                       | Controls the maximum number of attempts  |
| A-MaxGeneticOpsAttempts`` | to perform a genetic operation such as   |
|                           | crossover or mutation.                   |
+---------------------------+------------------------------------------+
| ``GA-GrowthProbScheme``   | Specifies the growth probability scheme. |
|                           | Acceptable values are: ``EXP_DIFF``,     |
|                           | ``TANH``, ``SIGMA``, and                 |
|                           | ``UNRESTRICTED``.See Genetic Operations. |
+---------------------------+------------------------------------------+
| ``G                       | Specifies the value of the factor λ used |
| A-LevelGrowthMultiplier`` | in graph level-based growth probability  |
|                           | schemes ``EXP_DIFF``, and ``TANH``.      |
+---------------------------+------------------------------------------+
| ``GA-Le                   | Specifies the value of parameter σ1 used |
| velGrowthSigmaSteepness`` | in graph level-based growth probability  |
|                           | scheme ``SIGMA``.                        |
+---------------------------+------------------------------------------+
| ``GA                      | Specifies the value of parameter σ2 used |
| -LevelGrowthSigmaMiddle`` | in graph level-based growth probability  |
|                           | scheme ``SIGMA``.                        |
+---------------------------+------------------------------------------+
| `                         | Specifies the value of the factor λ used |
| `GA-MolGrowthMultiplier`` | in graph molecular size-based growth     |
|                           | probability schemes ``EXP_DIFF``, and    |
|                           | ``TANH``.                                |
+---------------------------+------------------------------------------+
| ``GA-                     | Specifies the value of parameter σ1 used |
| MolGrowthSigmaSteepness`` | in graph molecular size-based growth     |
|                           | probability scheme ``SIGMA``.            |
+---------------------------+------------------------------------------+
| ``                        | Specifies the value of parameter σ2 used |
| GA-MolGrowthSigmaMiddle`` | in graph molecular size-based growth     |
|                           | probability scheme ``SIGMA``.            |
+---------------------------+------------------------------------------+
| ``GA-CrowdProbScheme``    | Specifies the crowding probability       |
|                           | scheme. Acceptable values are:           |
|                           | ``EXP_DIFF``, ``TANH``, ``SIGMA``, and   |
|                           | ``UNRESTRICTED``.                        |
+---------------------------+------------------------------------------+
| ``GA-CrowdMultiplier``    | Specifies the value of the factor λ used |
|                           | in crowding probability schemes          |
|                           | ``EXP_DIFF``, and ``TANH``.              |
+---------------------------+------------------------------------------+
| `                         | Specifies the value of parameter σ1 used |
| `GA-CrowdSigmaSteepness`` | in crowding probability scheme           |
|                           | ``SIGMA``.                               |
+---------------------------+------------------------------------------+
| ``GA-CrowdSigmaMiddle``   | Specifies the value of parameter σ2 used |
|                           | in crowding probability scheme           |
|                           | ``SIGMA``.                               |
+---------------------------+------------------------------------------+
| ``GA-XOverSelectionMode`` | Specifies the strategy for selecting     |
|                           | crossover partners. Acceptable values    |
|                           | are: ``RANDOM``, ``TS``, ``RW``, and     |
|                           | ``SUS``.See Genetic Operations.          |
+---------------------------+------------------------------------------+
| ``GA-CrossoverWeight``    | Specifies the relative weight of         |
|                           | crossover when generating new candidate  |
|                           | population members.                      |
+---------------------------+------------------------------------------+
| ``GA-MutationWeight``     | Specifies the relative weight of         |
|                           | mutation when generating new candidate   |
|                           | population members.                      |
+---------------------------+------------------------------------------+
| ``GA-M                    | Specifies the relative weight of         |
| ultiSiteMutationWeights`` | multi-site mutations, i.e., mutations    |
|                           | operations that involve multiple and     |
|                           | independent mutations on a single graph. |
|                           | Since each mutation is completely        |
|                           | independent, even a previously mutated   |
|                           | site can be mutates again. Therefore,    |
|                           | this can be seen an a multiple iteration |
|                           | mutation. A graph can be modified, for   |
|                           | example, first by the addition of a      |
|                           | link, and then by the change of a branch |
|                           | completely unrelated to the first        |
|                           | addition. This would be referred as a    |
|                           | two-sites mutation.                      |
|                           |                                          |
|                           | Provide values in a comma- or            |
|                           | space-separated list. The first value is |
|                           | the weight of one-site mutation, the     |
|                           | second the weight of two-sites mutation, |
|                           | and so on. The number of values given as |
|                           | argument determines the maximum number   |
|                           | mutation iterations that can a single    |
|                           | graph mutation operation perform. For    |
|                           | example,                                 |
|                           |                                          |
|                           | ::                                       |
|                           |                                          |
|                           |    GA-MultiSiteMutationWeights=10, 1     |
|                           |                                          |
|                           | enables up to two-sites mutation and     |
|                           | with a weight that is 1/10 of the        |
|                           | single-site mutation.                    |
+---------------------------+------------------------------------------+
| ``GA-ConstructionWeight`` | Specifies the relative weight of         |
|                           | construction from scratch when           |
|                           | generating new candidate population      |
|                           | members.                                 |
+---------------------------+------------------------------------------+
| `                         | Specifies the unspecific symmetric       |
| `GA-SymmetryProbability`` | substitution probability. Attachment     |
|                           | point-class specific values are defined  |
|                           | in the definition of the space of graph  |
|                           | building blocks.                         |
+---------------------------+------------------------------------------+
| `                         | Specifies the population members         |
| `GA-ReplacementStrategy`` | replacement strategy: ``ELITIST`` for    |
|                           | the elitist scheme (survival of the      |
|                           | fittest), use ``NONE`` for no            |
|                           | replacement (all candidates become       |
|                           | member of the population, which keeps    |
|                           | growing).                                |
+---------------------------+------------------------------------------+
| ``GA-Ke                   | Makes DENOPTIM save newly encountered    |
| epNewRingSystemVertexes`` | ring systems (i.e., cyclic subgraphs) as |
|                           | templates in the library of              |
|                           | general-purpose building blocks. No new  |
|                           | template will include a scaffold vertex  |
|                           | or be used as scaffold. See              |
|                           | ``GA-KeepNewRingSystemScaffolds`` to     |
|                           | enable the latter possibilities.         |
+---------------------------+------------------------------------------+
| ``GA-Kee                  | Makes DENOPTIM save newly encountered    |
| pNewRingSystemScaffolds`` | ring systems (i.e., cyclic subgraphs)    |
|                           | that contain any scaffold vertex as      |
|                           | template scaffolds.                      |
+---------------------------+------------------------------------------+
| ``GA-KeepN                | Specified a percentage of the current    |
| ewRingSystemFitnessTrsh`` | population fitness range in the form of  |
|                           | %/100 double (i.e., a value between 0    |
|                           | and 1). This value represents a          |
|                           | threshold limiting the possibility to    |
|                           | store a newly encountered ring system    |
|                           | only to those candidate items having a   |
|                           | fitness that in in the best fraction of  |
|                           | the instantaneous population range. For  |
|                           | example, giving a value of 0.10 will     |
|                           | make denoptim store new ring systems     |
|                           | only from newly encountered candidates   |
|                           | that are among the best 10% of the       |
|                           | population in the moment each of these   |
|                           | candidates is considered as a potential  |
|                           | population member.                       |
+---------------------------+------------------------------------------+
| Interface                 |                                          |
+---------------------------+------------------------------------------+
| ``GA-InitPoplnFile``      | Specifies the pathname of a file (can be |
|                           | an SDF file or a text file where each    |
|                           | line containing the pathname to a        |
|                           | single-molecule SDF file) containing     |
|                           | previously evaluated individuals to be   |
|                           | added to the initial population. If the  |
|                           | number of individuals is lower than the  |
|                           | specified population side, DENOPTIM will |
|                           | create additional individuals.           |
+---------------------------+------------------------------------------+
| ``GA-UIDFileIn``          | Specifies the pathname of a text file    |
|                           | collecting the list of unique individual |
|                           | identification strings (UID; one UID     |
|                           | each line) that are to be considered as  |
|                           | previously evaluated individuals.        |
|                           | DENOPTIM will ignore individuals for     |
|                           | which the UID is found in the file. This |
|                           | applies also to the members of the       |
|                           | initial population provided by the user  |
|                           | (see ``GA-InitPoplnFile`` keyword).      |
+---------------------------+------------------------------------------+
| ``GA-UIDFileOut``         | Specifies the pathname of the file,      |
|                           | i.e., the UIDFileOut, collecting the     |
|                           | list of unique individual identification |
|                           | strings(UID) encountered during an       |
|                           | evolutionary experiment. If no pathname  |
|                           | is given, a new UID file is generated    |
|                           | under the work space of the experiment.  |
|                           | UIDs from individuals found in an        |
|                           | initial population file, and those       |
|                           | specified via the ``GA-UIDFile``. In     |
|                           | keyword are collected in the             |
|                           | ``UIDFileOut`` file.                     |
+---------------------------+------------------------------------------+
| Parallelization           |                                          |
+---------------------------+------------------------------------------+
| ``GA-NumParallelTasks``   | Specifies the maximum number of parallel |
|                           | tasks to be performed.                   |
+---------------------------+------------------------------------------+
| ``GA-Parallelization``    | Specifies the parallelization scheme:    |
|                           | ``synchronous`` if parallel tasks are    |
|                           | submitted in batches, thus no new task   |
|                           | is submitted until the last of the       |
|                           | previous tasks is completed, or          |
|                           | ``asynchronous`` if a new parallel tasks |
|                           | is submitted as soon as any of the       |
|                           | previous task is completed.              |
+---------------------------+------------------------------------------+

| 

.. _d-molecular-models-builder-1:

3D Molecular Models Builder
~~~~~~~~~~~~~~~~~~~~~~~~~~~

+---------------------------+------------------------------------------+
| Keyword                   | Description                              |
+---------------------------+------------------------------------------+
| General                   |                                          |
+---------------------------+------------------------------------------+
| ``3DB-WorkDir``           | Specifies the pathname of the directory  |
|                           | where files will be created.             |
|                           | **[REQUIRED]**                           |
+---------------------------+------------------------------------------+
| ``3DB-InpSDF``            | Specifies the pathname to the input SDF  |
|                           | file that must contain graph             |
|                           | representation of the chemical object.   |
|                           | **[REQUIRED]**                           |
+---------------------------+------------------------------------------+
| ``3DB-OutSDF``            | Specifies the pathname of the output SDF |
|                           | file that will contain the generated     |
|                           | conformation. **[REQUIRED]**             |
+---------------------------+------------------------------------------+
| ``3DB-KeepDummyAtoms``    | Dummy atoms are used to handle           |
|                           | linearities and multi-hapto bonds. By    |
|                           | default all dummy atoms are removed      |
|                           | before returning the final structure.    |
|                           | This keyword prevents removal of the     |
|                           | dummy atoms. No value needed.            |
+---------------------------+------------------------------------------+
| ``3DB-Verbosity``         | Specifies the verbosity level.           |
+---------------------------+------------------------------------------+
| Interface                 |                                          |
+---------------------------+------------------------------------------+
| ``3DB-ToolPSSROT``        | Specifies the pathname of Tinker’s       |
|                           | ``pssrot`` executable (see               |
|                           | https://dasher.wustl.edu/tinker/).       |
|                           | **[REQUIRED]**                           |
+---------------------------+------------------------------------------+
| ``3DB-ToolXYZINT``        | Specifies the pathname of Tinker’s       |
|                           | ``xyzint`` executable (see               |
|                           | https://dasher.wustl.edu/tinker/).       |
|                           | **[REQUIRED]**                           |
+---------------------------+------------------------------------------+
| ``3DB-ToolINTXYZ``        | Specifies the pathname of Tinker’s       |
|                           | ``intxyz`` executable (see               |
|                           | https://dasher.wustl.edu/tinker/).       |
|                           | **[REQUIRED]**                           |
+---------------------------+------------------------------------------+
| ``3DB-ForceFieldFile``    | Specifies the pathname of the file that  |
|                           | defines Tinker’s force field parameters  |
|                           | (see https://dasher.wustl.edu/tinker/).  |
|                           | An example is available at               |
|                           | src/main/resources/data/uff_vdw.prm      |
|                           | **[REQUIRED]**                           |
+---------------------------+------------------------------------------+
| ``3DB-KeyFile``           | Specifies the pathname of the file with  |
|                           | Tinker’s keywords (see                   |
|                           | https://dasher.wustl.edu/tinker/). An    |
|                           | example is available at                  |
|                           | src/main/resources/data/build_uff.key    |
|                           | **[REQUIRED]**                           |
+---------------------------+------------------------------------------+
| ``3DB-RCKeyFile``         | Specifies the pathname of the Tinker’s   |
|                           | keywords used in ring-closing            |
|                           | conformational searches (see `J. Chem.   |
|                           | Inf. Model. 2015, 55, 9                  |
|                           | 1844-1856 <https                         |
|                           | ://doi.org/10.1021/acs.jcim.5b00424>`__) |
+---------------------------+------------------------------------------+
| ``3DB-PSSROTParams``      | Specifies the pathname of a text file    |
|                           | with the command line arguments for      |
|                           | standard conformational search with      |
|                           | Tinker’s ``pssrot``. An example is       |
|                           | available at                             |
|                           | `                                        |
|                           | `src/main/resources/data/submit_pssrot`` |
|                           | **[REQUIRED]**                           |
+---------------------------+------------------------------------------+
| ``3DB-RCPSSROTParams``    | Specifies the pathname of a text file    |
|                           | with the command line arguments for      |
|                           | ring-closing conformational search with  |
|                           | Tinker’s ``pssrot`` (see `J. Chem. Inf.  |
|                           | Model. 2015, 55, 9                       |
|                           | 1844-1856 <https:                        |
|                           | //doi.org/10.1021/acs.jcim.5b00424>`__). |
+---------------------------+------------------------------------------+

| 

FragmentSpaceExplorer
~~~~~~~~~~~~~~~~~~~~~

+---------------------------+------------------------------------------+
| Keyword                   | Description                              |
+---------------------------+------------------------------------------+
| General                   |                                          |
+---------------------------+------------------------------------------+
| ``FSE-WorkDir``           | Specifies the pathname of the directory  |
|                           | where files will be created.             |
+---------------------------+------------------------------------------+
| ``FSE-MaxLevel``          | Specifies the maximum number of layers   |
|                           | of fragments to consider. Note that the  |
|                           | root (i.e., scaffold or root graph) is   |
|                           | considered to belong to level = -1       |
|                           | according to DENOPTIM's practice (see    |
|                           | Figure 6). Therefore, when setting       |
|                           | FSE-MAXLEVEL=3 at most 4 layers of       |
|                           | fragments will be used (namely levels =  |
|                           | 0, 1, 2, and 3).                         |
+---------------------------+------------------------------------------+
| ``FSE-UIDFile``           | Specifies the pathname of the file with  |
|                           | unique chemical entity IDs.              |
+---------------------------+------------------------------------------+
| ``FSE-DBRootFolder``      | Specifies the pathname of the directory  |
|                           | where to place the folder tree of        |
|                           | generate graphs.                         |
+---------------------------+------------------------------------------+
| ``FSE-MaxWait``           | Specifies the wall time limit (in        |
|                           | seconds) for waiting for completion of   |
|                           | one or more tasks. Accepts only integer  |
|                           | numbers.                                 |
+---------------------------+------------------------------------------+
| ``FSE-WaitStep``          | Specifies the sleeping time (or time     |
|                           | step, in seconds) between checks for     |
|                           | completion of one or more tasks. Accepts |
|                           | only integer numbers.                    |
+---------------------------+------------------------------------------+
| ``FSE-NumOfProcessors``   | Specifies the number of asynchronous     |
|                           | processes that can be run in parallel.   |
|                           | Usually this corresponds to the number   |
|                           | of slave cores, if 1 such core           |
|                           | corresponds to 1 external task.          |
+---------------------------+------------------------------------------+
| ``FSE-Verbosity``         | Specifies the verbosity level.           |
+---------------------------+------------------------------------------+
| Definition of the root    |                                          |
| graphs (i.e., starting    |                                          |
| point of combinatorial    |                                          |
| exploration)              |                                          |
+---------------------------+------------------------------------------+
| ``FSE-RootGraphs``        | Specifies the pathname of a file         |
|                           | containing the list of root graphs.      |
+---------------------------+------------------------------------------+
| ``FSE-RootGraphsFormat``  | Specifies the format of the root graphs. |
|                           | Acceptable values are 'STRING' for human |
|                           | readable graphs as those reported by     |
|                           | DENOPTIM tools in SDF files (default),   |
|                           | or 'BYTE' for serialized graphs stored   |
|                           | in binary files.                         |
+---------------------------+------------------------------------------+
| Restart from checkpoint   |                                          |
| file                      |                                          |
+---------------------------+------------------------------------------+
| ``F                       | Specifies the distance between two       |
| SE-CheckPointStepLength`` | subsequent updates of the checkpoint     |
|                           | information as a number of generated     |
|                           | graphs.                                  |
+---------------------------+------------------------------------------+
| ``FS                      | Specifies the pathname of the checkpoint |
| E-RestartFromCheckpoint`` | file and makes FragSpaceExplorer restart |
|                           | from such file.                          |
+---------------------------+------------------------------------------+

| 

Fitness Provider
~~~~~~~~~~~~~~~~

Keyword

Description

General

``FP-No3dTreeModel``

Prevents reporting candidates using a three-dimensional molecular model
that is built by aligning each building block to the attachment point
vector of its parent building block. Such "three-dimensional tree"
(3d-tree) structure is not refined in any way, and is only meant to
provide a somewhat preliminary geometry to be further refined. Using
this keyword prevents the generation of such 3d-trees, and makes
denoptim build a molecular model that uses original Cartesian
coordinates of the building blocks as provided in the libraries of
scaffolds, fragments and capping groups.

Internal Fitness Provider

``FP-Equation``

Specifies the expression to be used for calculation of the fitness value
from available descriptors (i.e., from CDK library). Descriptor values,
i.e., variables, and numerical constants can be combined using operators
such as +, -, \*, /, % (Modulo/remainder), and parenthesis. The
expression must start with ``${`` and end with ``}``. For example,

::

   ${0.23*nBase - 1.1*naAromAtom + myVariable}

is a valid expression where ``nBase`` and ``naAromAtom`` are the names
of molecular descriptors implemented in the CDK library, and
``myVariable`` is the name of a user-defined variable. The latter is
defined by meas of a ``FP-DescriptorSpecs`` keyword, see below.

``FP-DescriptorSpecs``

Defines a custom descriptors and variable to be used in the expression
for the calculation of the fitness value. Examples of custom variables
are atom-specific descriptors that are calculated only on a user-defined
subset of atoms. To define such atom-specific descriptors use this
syntax:

::

   ${atomSpecific('<variableName>','<descriptor_name>','<SMARTS>')}

where

-  ``<variableName>`` is a string (without spaces) that identifies the
   custom descriptor in the expression of the fitness given by the
   ``FP-Equation`` keyword,
-  ``<descriptor_name>``, is the name of the descriptor in the CDK
   implementation,
-  ``<SMARTS>`` is a SMARTS string that specifies which atoms will
   contribute. If the SMARTS matches multiple atoms, the value of the
   custom descriptor is calculated as the average of the values for all
   atoms that match the SMARTS query.

External Fitness Provider

``FP-Source``

Specifies the pathname of executable to run to evaluate the fitness.

``FP-Interpreter``

Specifies the interpreter to use when running the external fitness
provider source file.

| 

Stand-alone Graph Editor
~~~~~~~~~~~~~~~~~~~~~~~~

Keyword

Description

General

``GRAPHEDIT-InputGraphs``

Pathname of a file containing the graphs to edit. **[REQUIRED]**

``GRAPHEDIT-GraphsEditsFile``

Pathname of a file defining the graph edit task. See `Graph Editing Task
Files <#Toc9999_3>`__. **[REQUIRED]**

``GRAPHEDIT-OutputGraphs``

Pathname of a file where to put the edited graphs. **[REQUIRED]**

``GRAPHEDIT-OutpotGraphsFormat``

Format of the output. Can be SDF or JSON.

``GRAPHEDIT-EnforceSymmetry``

Use y/yes to enforce the application of symmetry whenever possible.

| 

Stand-alone Fitness Runner
~~~~~~~~~~~~~~~~~~~~~~~~~~

+---------------------------+------------------------------------------+
| Keyword                   | Description                              |
+---------------------------+------------------------------------------+
| ``FR-Input``              | Pathname to the file containing the      |
|                           | graph for which the fitness has to be    |
|                           | calculated.                              |
+---------------------------+------------------------------------------+
| ``FR-Output``             | Pathname where results will be saved     |
|                           | (see `Fitness                            |
|                           | Files <#Toc16853_799972446>`__)          |
+---------------------------+------------------------------------------+
| ``FR-ExtractTemplates``   | Add this keyword to request the          |
|                           | extraction of template vertex from the   |
|                           | result of the fitness evaluation         |
|                           | process. This keyword is meant for       |
|                           | testing purposes.                        |
+---------------------------+------------------------------------------+

| 

Stand-alone Graph Isomorphism Analyzer
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

+---------------------------+------------------------------------------+
| Keyword                   | Description                              |
+---------------------------+------------------------------------------+
| ``Isomorphism-inpGraphA`` | Pathname to one of the graph to be       |
|                           | considered for the detection of          |
|                           | `DENOPTIM-isomorphism <Toc9999_7>`__.    |
+---------------------------+------------------------------------------+
| ``Isomorphism-inpGraphB`` | Pathname to the other of the graph to be |
|                           | considered for the detection of          |
|                           | `DENOPTIM-isomorphism <Toc9999_7>`__     |
+---------------------------+------------------------------------------+

Stand-alone Graph List Comparator
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

+---------------------------+------------------------------------------+
| Keyword                   | Description                              |
+---------------------------+------------------------------------------+
| ``G                       | Pathname to the file containing the      |
| RAPHLISTS-InputGraphs-A`` | first list of graphs.                    |
+---------------------------+------------------------------------------+
| ``G                       | Pathname to the file containing the      |
| RAPHLISTS-InputGraphs-B`` | second list of graphs.                   |
+---------------------------+------------------------------------------+

--------------

File Formats
============

.. _vertexes-1:

Vertexes
~~~~~~~~

Vertexes can be define in files with two different formats: the
`SDF <https://en.wikipedia.org/wiki/Chemical_table_file#SDF>`__, and
`JSON <https://en.wikipedia.org/wiki/JSON>`__.

SDF file format are best suited for vertex that contain molecular
fragment because they can be opened by any most molecular visualization
packages. Still, SDF format can be used any type of vertex.

The requirement for an the chemical representation contained in an SDF
file to be perceived as a vertex is is the presence of associated data
(also called tags, fields, or properties) that define the attachment
points, i.e., the ``<ATTACHMENT_POINT>`` property. The value of this
property must reflect the following syntax convention. For a single
attachment point rooted on an atom (square brackets indicate optional
components):

::

   n1#cA:csA[:btA][:xA%yA%zA]

For multiple attachment points rooted on the same atom (square brackets
indicate optional components):

::

   n2#cB:scB[:btB][:xB%yB%zB],cC:scC[:btC][:xC%yC%zC]

where:

-  n\ :sub:`1` is the 1-based index of the atom/pseudo-atom on which
   attachment point *A* is rooted, while n\ :sub:`2` is the 1-based
   index of the atom/pseudo-atom on which both attachment point *B* and
   *C* are rooted.
-  *c\ i* is the first part (i.e., the so-called "rule") of the APClass
   of attachment point *i*.
-  *sc\ i* is the second part (i.e., the so-called "subclass") of the
   APClass of attachment point *i*.
-  *bt\ i* defined the bond type of the APClass of attachment point *i*.
   Possible values are ``NONE``, ``SINGLE``, ``DOUBLE``, ``TRIPLE``,
   ``QUADRUPLE``, ``ANY``, and ``UNDEFINED``.
-  *x\ i*, *y\ i*, and *z\ i* are Cartesian coordinates defining the "AP
   vector". For APs rooted on atoms (the source atoms) the AP vector
   defines the ideal position where the atom that can be connected with
   the source atom should be placed upon formation of the bond.

In the SDF file strings pertaining a single source atom are separated by
spaces. So the overall result is the following:

.. code:: file

   …
   > <ATTACHMENT_POINTS>
   n1#cA:csA:btA:xA%yA%zA n2#cB:scB:btB:xB%yB%zB,cC:scC:btC:xC%yC%zC

   …

Considering the optional components, the following alternatives are also
recognized and used for APs that do not define bond types (NB: they
accept the default bond type, i.e., ``SINGLE``) and/or AP vectors.

-  ``n1#cA:csA:btA``
-  ``n1#cA:csA:xA%yA%zA``
-  ``n1#cA:csA``

Additional fields can be present in SDF files saved by DENOPTIM. In
particular, DENOPTIM new version always saves the JSON format as one of
the properties of the SDF file format.

Compatibility Matrix file
~~~~~~~~~~~~~~~~~~~~~~~~~

The compatibility matrix file is a text file that includes blocks of
information related to the APClass compatibility rules:

-  the definition of the actual AP class compatibility matrix (lines
   starting with ``RCN`` keyword, comma-separated entries),
-  the rules to append capping groups (``CAP`` keyword),
-  the rules to discharge a graph if an AP of a specific APClass is not
   used (``DEL`` keyword).

Example of a compatibility matrix file:

.. code:: file

   #Comment line
   RCN apclass1:0 apclass1:1,apclass2:0,apclass2:1
   RCN apclass1:1 apclass1:0
   RCN apclass2:0 apclass2:1
   # Note the any class name not found in the library of fragments can be used to impose that no fragment (excluding 
   # capping groups) is attached on APs of a specific class.
   # In this example, no fragment will be attached to APs of class apclass2:1 
   RCN apclass2:1 none
   …
   # Capping groups
   CAP apclass1:0 cap1:0
   CAP apclass1:0 cap2:0
   …
   # Forbidden ends
   DEL apclass1:0

Ring Closures Compatibility Matrix file
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The ring closures (RC) compatibility matrix file specifies the pairs of
AP classes that are allowed to form ring-closing bonds. Note that,
contrary to the standard compatibility matrix, the RC compatibility
matrix is symmetric. The syntax is the same as for a general-purpose
compatibility matrix.

.. code:: file

   #Comment line
   RCN apclass1:0 apclass1:1,apclass2:0,
   RCN apclass1:1 apclass1:0
   …

Initial Population File
~~~~~~~~~~~~~~~~~~~~~~~

SDF format is used to provide an initial set of fully characterized
candidates (i.e., initial population). To be properly interpreted by
DENOPTIM this SDF file must include the following tags:

-  ``<FITNESS>`` tag specifying the numerical fitness value.
-  ``<UID>`` tag specifying the unique identifier of the chemical entity
   (often the InChiKey).
-  ``<GraphJson>`` tag specifying the essential part of the graph
   representation of the chemical entity.

Candidate Chemical Entity Files
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

SDF format file used to communicate the definition of a candidate entity
to external fitness providers (see `Fitness
evaluation <#Toc42056_808352928>`__).

DENOPTIM programs output a non-optimized 3D-structure of the chemical
entity in SDF format together with its graph representation (tag
``<GraphJson>``) and additional information (i.e., SMILES, InChiKey,
unique identifier). By default, the Cartesian coordinates reported in
these files are those of a three-dimensional tree-like model built by
aligning each building block according to the attachment point vectors.
The resulting geometry is not refined, and is only meant to facilitate
further processing and visual inspection. While such geometry can be
used as starting point for further molecular modeling, it should never
be used to calculate any property dependent on the molecular geometry.
The alignment of the building blocks is skipped when using the
``FP-No3dTreeModel`` keyword. In this case, the SDF file will contain
Cartesian coordinates of the initial building blocks, and will therefore
be characterized by a correct topology but a non-sense arrangement of
the atoms.

Fitness Files
~~~~~~~~~~~~~

SDF format files used to store the final results for an evaluated
chemical entity and to receive the fitness value from any external
fitness provider. These files must contain one of these two tags:

-  ``<MOL_ERROR>`` tag specifying that the candidate entity cannot be
   coupled with a fitness value. The content of this field is used to
   report details on the reasons leading to this result. The following
   syntax must be used:

   .. code:: file

      …
      > <Mol_ERROR>
      #Keyword identifying error1: details characterizing the error1
      #Keyword identifying error2: details characterizing the error2

      …

-  ``<FITNESS>`` tag specifying the numerical value of the fitness (NB:
   ensure is not NaN!)

Additional tag defining an unique identifier of the entity (i.e.,
``> <UID>``) is also needed.

Any type and number of descriptors and properties can be included in the
SDF file using the '``> <DATA>``' syntax, where ``DATA`` is replaced by
the name of a specific data tag. The number and kind of descriptors
depends on the specific application.

Example of a fitness file with properly terminated calculation of the
fitness value:

.. code:: file

   Mol_000027 
     CDK     1101131251 

   126128  0  0  0  0  0  0  0  0999 V2000 
       0.5414   -0.1399   -0.2777 C   0  0  0  0  0  0  0  0  0  0  0  0 
   …
       4.0589   -3.8870  -13.3457 H   0  0  0  0  0  0  0  0  0  0  0  0 
     5  8  1  0  0  0  0 
   …
    43 89  1  0  0  0  0 
   M  END 
   > <cdk:Title> 
   Mol_000027 

   > <GraphJson> 
   …

   > <Descriptors1>
   2.322 

   > <Descriptors2>
   3.452 

   > <FITNESS>
   -10.096

   $$$$

Example of an SDF file with error due to violation of constraints:

.. code:: file

   Mol_000027 
     CDK     1101131251 

   126128  0  0  0  0  0  0  0  0999 V2000 
       0.5414   -0.1399   -0.2777 C   0  0  0  0  0  0  0  0  0  0  0  0 
   …
       4.0589   -3.8870  -13.3457 H   0  0  0  0  0  0  0  0  0  0  0  0 
     5  8  1  0  0  0  0 
   …
    43 89  1  0  0  0  0 
   M  END 
   > <cdk:Title> 
   Mol_000027 

   > <GraphJson> 
   …

   > <Descriptors1>
   2.322 

   > <Descriptors2>
   3.452 

   > <MOL_ERROR>
   #ViolationOfConstraint:  Minimum non-bonded distance threshold violated. 2.766890

   $$$$

Graph Editing Task Files
~~~~~~~~~~~~~~~~~~~~~~~~

Example of JSON syntax for defining the most detailed vertex query. Any
of the fields can be left removed. For example, removing the line with
``"vertexId"`` will make the vertex query match any vertex
irrespectively of its vertexID. Similarly, the block of lines pertaining
``"incomingEdgeQuery"`` (i.e., all the lines included in the curly
brackets immediately following "incomingEdgeQuery") will make the vertex
query match any vertex irrespectively of the properties of the edge that
connects such vertex to its parent vertex (if any).

::

     "vertexQuery": {
       "vertexId": 1,
       "buildingBlockId": 4,
       "buildingBlockType": "FRAGMENT",
       "vertexType": "MolecularFragment",
       "level": 2,
       "incomingEdgeQuery": {
         "srcVertexId": 1,
         "trgVertexId": 2,
         "srcAPID": 3,
         "trgAPID": 4,
         "bondType": "DOUBLE",
         "srcAPC": {
           "rule": "s",
           "subClass": 0
         },
         "trgAPC": {
           "rule": "t",
           "subClass": 0
         }
       },
       "outgoingEdgeQuery": {SAME SYNTAX of "incomingEdgeQuery"}
     }

The above is a complete list of fields that can be used to define a
vertex query, but a valid query can include any of those fields from
none (i.e., a query that matches everything) to all of them (i.e., the
most detailed query possible). For example, the following is a vertex
query meant to match only vertexes in level 2:

::

     "vertexQuery": {
         "level": 2
     }

A sequence of graph editing tasks is defining using JSON syntax as
following:

::

   [
     {
       "task": "REPLACECHILD",
       "vertexQuery": {...}
     }
     {
       "task": ...,
       "vertexQuery": {...}
     }
   ]

--------------

.. |figures/exp_diff.png| image:: figures/exp_diff.png
   :width: 131px
   :height: 45px
.. |figures/tanh.png| image:: figures/tanh.png
   :width: 138px
   :height: 21px
.. |figures/sigma.png| image:: figures/sigma.png
   :width: 174px
   :height: 41px
