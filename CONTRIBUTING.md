# CONTRIBUTING to DENOPTIM

## Eclipse
Eclipse is highly recommended to whoever wants to contribute to the development of DENOPTIM. The DENOPTIM repository includes <code>.project</code> and <code>.classpath</code>. So you can easily import the entire DENOPTIM project into Eclipse.

## Git work-flow 
Nice guidelines can be found in the Internet. For instance, see (https://akrabat.com/the-beginners-guide-to-contributing-to-a-github-project/). Here is a summary of how to go with making changes to the source using the functionality provided by Git:

1. Fork the `master` branch. The easiest is to use the `Fork` button on the top right to make your own copy. This will eventually open the GitHub page of our copy of `master` in your browser. 

2. Get a local copy. Use the `Clone or download` button to get the URL and use it in this command:

    cd <somewhere>
    git clone <URL>

3. Add the original repo as an upstream to grab changes made by others to the original branch while you work on your branch.

    cd DENOPTIM
    git remote add upstream https://github.com/denoptim-project/DENOPTIM.git

4. Get the local copy of the software working fine before you make any change. Compile all your branch and run all tests. Make sure it's all working. If not, identify and possibly solve the problem or ask for help. Do not proceed unless it's all working fine.

5. Make a branch for the changes you want to introduce:

    git checkout master
    git pull upstream master
    git push origin master
    git checkout -b <name_of_branch>
    
6. Now you can do what you want to contribute. If you add new functionality or just new keywords, please add proper documentation and tests with data that can be distributed under the DENOPTIM license (no proprietary material). Commit to your branch with [proper commit messages](https://chris.beams.io/posts/git-commit/). 

7. Unless you are introducing major changes that are incompatible with previous version, you must incorporate changes made by others to the original `master`, and resolve potential conflicts:

    git pull upstream master 
    git push origin master
    
8. When you are done, run all tests and make sure your changes are being tested. If not, create new tests.

9. Create a pull request to your copy of `master`

    git push -u origin <name_of_branch>

10. In the web page of the browser you'll find a `Compare & pull request` button that will allow you to compare the changes you have made and request the inclusion of those changes in the original `master` branch.
