# Collecting features

Now we collect 13 features, that will be described in original [paper](https://files.sri.inf.ethz.ch/website/papers/ccs21-learch.pdf), except constraint representation, but it can be extended.

* `stack` - size of state’s current call stack.
* `successor` - number of successors of state’s current basic block. 
* `testCase` - number of test cases generated so far
* `coverageByBranch` - number of instructions, which was covered first time on our last branch
* `coverageByPath` - number of instructions, which was covered first time on our path
* `depth` - number of forks already performed along state’s path.
* `cpicnt` - number of instructions visited in state's current function.
* `icnt` - number of times for which st ate’s current instruction has
  been visited
* `covNew` - number of instructions executed by st ate since the last
  time a new instruction is covered
* `subpath` - number of times for which st ate’s subpaths have been
  visited. The length of the subpaths can be 1, 2, 4, or 8 respectively
