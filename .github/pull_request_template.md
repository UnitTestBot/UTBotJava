## Labels (hint)

Choose the obligatory labels:
- "ctg" (category): _bug-fix_, _enhancement_, _refactoring_, etc.
- "comp" (component): _symbolic-engine_, _fuzzing_, _codegen_, etc.

Feel free to apply more labels to your PR, e.g.: _lang-java_, _priority-minor_, _spec-performance_

## Title (hint)

Describe what you've changed or added in terms of functionality.

For example:

> Add summaries for the nested classes

> Support test generation for paths with spaces in JavaScript

> Remove packageName property not defined in Java 8

Check that the title contains
* no branch name
* no GitHub nickname
* no copy-pasted issue title

## Description

Fixes # (issue)

Add more info _if needed_:
* context/purpose for implementing changes
* detailed description of the changes made

## How to test

### Automated tests

Please specify the _automated tests_ for your code changes: you should either mention the existing tests or add the new ones.

For example:

> The proposed changes are verified with tests:
> `utbot-fuzzing/src/test/kotlin/org/utbot/fuzzing/FuzzerSmokeTest.kt`

### Manual tests

If it is impossible to provide the automated tests, please reason why. Usually, it is relevant only for UI- or documentation-related PRs.
If this is your case, share the detailed _manual scenarios_ that help to verify your changes.

## Self-check list

Check off the item if the statement is true. Hint: [x] is a marked item.

Please do not delete the list or its items.

- [ ] I've set the proper **labels** for my PR (at least, for category and component).
- [ ] PR **title** and **description** are clear and intelligible.
- [ ] I've added enough **comments** to my code, particularly in hard-to-understand areas.
- [ ] The functionality I've repaired, changed or added is covered with **automated tests**.
- [ ] **Manual tests** have been provided optionally.
- [ ] The **documentation** for the functionality I've been working on is up-to-date.