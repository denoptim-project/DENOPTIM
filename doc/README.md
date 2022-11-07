HTML Documentation                  
==================

## Online Documentation
An online version of the documentation is available at this [link](https://denoptim-project.github.io/DENOPTIM/).


## Offline Documentation
Documentation can be built locally to make it available offline. This is particularly useful to test changes to the documentation.

### Build
The directory where this README file is located contains all the source files needed to build the documentation with [Doxygen](https://doxygen.nl/index.html). The Doxyfile and DoxygenLayout files define input and configurationa of Doxygen.

To build the html pages of the documentation run the following command from within the `doc`

  doxygen


### Usage
Once you have built the local documentation, its entry point will be file ./html/index.html. You can open such file with any browser, double click on it or open it from the command line. For example,

  firefox ./html/index.html
