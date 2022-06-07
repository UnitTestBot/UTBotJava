---
name: Manual testing plan
about: Checklist of testing process
title: ''
labels: ''
assignees: ''

---

**Initial set-up**

- [ ] Check that the IntelliJ Idea UTBot plugin can be successfully installed
- [ ] Choose appropriate workflow from the next list (by default, use the latest one) https://github.com/UnitTestBot/UTBotJava/actions/workflows/publish-plugin-and-cli.yml
- [ ] Open IntelliJ IDE
- [ ] Remove the latest version of the UTBot plugin
- [ ] Clone or reuse UTBot workspace (https://github.com/UnitTestBot/UTBotJava.git)
- [ ] Open the workspace in the IDE with the installed plugin
- [ ] Build workspace (Instruction is needed)
- [ ] Go through manual scenarios


**Manual scenario #1**

- [ ] Use default plugin settings
- [ ] Open the utbot-sample/src/main/java/org/utbot/examples/algorithms/ArraysQuickSort.java file
- [ ] Try to generate tests for the class
- [ ] Remove results
- [ ] Try to generate tests for the methods
 

**Manual scenario #2**

- [ ] Use default plugin settings
- [ ] Open the utbot-sample/src/main/java/org/utbot/examples/mock/CommonMocksExample.java file
- [ ] Try to generate tests with all available (mocking) options
 

**Manual scenario #3**

- [ ] Create a new Gradle project
- [ ] Add a simple java file to test
- [ ] Try to generate a test with a new test root
 

**Manual scenario #4**

- [ ] Create a new Idea project
- [ ] Add a simple java file to test
- [ ] Try to generate a test with a new test root

**Manual scenario #4**

- [ ] Create a new Idea project
- [ ] Add a simple java file to test
- [ ] Try to generate tests for several classes
