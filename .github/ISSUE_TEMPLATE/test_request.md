---
name: Manual testing checklist
about: Checklist of testing process
title: ''
labels: ''
assignees: ''

---

**Initial set-up**

*Check that the IntelliJ Idea UTBot plugin can be successfully installed*

- [ ] Choose appropriate workflow from the list (by default, filter by main branch and take the latest one) https://github.com/UnitTestBot/UTBotJava/actions/workflows/publish-plugin-and-cli.yml
- [ ] Download plugin
- [ ] Check downloaded zip-file size < 100 MB
- [ ] Open IntelliJ IDE
- [ ] Remove previously installed UTBot plugin
- [ ] Clone or reuse UTBot project (https://github.com/UnitTestBot/UTBotJava.git)
- [ ] Open the project in the IDE
- [ ] Install the downloaded plugin

*Go through manual scenarios*

**Manual scenario #1**

- [ ] Use default plugin settings
- [ ] Open the utbot-sample/src/main/java/org/utbot/examples/algorithms/ArraysQuickSort.java file
- [ ] Generate tests for the class
- [ ] Remove results
- [ ] Generate tests for the methods
 

**Manual scenario #2**

- [ ] Use default plugin settings
- [ ] Open the utbot-sample/src/main/java/org/utbot/examples/mock/CommonMocksExample.java file
- [ ] Generate tests with all available (mocking) options
 

**Manual scenario #3**

- [ ] Create a new Gradle project
- [ ] Add a simple java file to test
- [ ] Generate a test with a new test root
 

**Manual scenario #4**

- [ ] Create a new Maven project
- [ ] Add a simple java file to test
- [ ] Generate a test with a new test root

**Manual scenario #5**

- [ ] Create a new Idea project
- [ ] Add a simple java file to test
- [ ] Generate tests for several classes
