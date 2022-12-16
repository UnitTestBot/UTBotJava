# UnitTestBot contributing guide

---

## Welcome!

Hello and thanks for reading this!

As the UnitTestBot core team we develop tools for automated unit test generation to help programmers test their code
in a more effective way with less effort. We believe that software development is great fun when you spend your time
finding creative solutions and automate the things you already know. If you are curious about making test generation
fast, smart and reliable, we are happy to invite you for contributing!

We welcome absolutely everyone. With one big and kind request: please follow these guidelines to make our communication smooth and to keep UnitTestBot improving.

## Contributions we are looking for

There are so many ways to contribute. Choose yours and find the relevant short guide below.

| (1) Choose what you like and check the guideline:                                                                       | (2) Contribute:                                                                                                                               |
|-------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------|
| [Reporting a bug](#Reporting-a-bug)                                                                                     | Create a [bug reporting issue](https://github.com/UnitTestBot/UTBotJava/issues/new?assignees=&labels=&template=bug_report.md&title=)          |
| [Requesting a feature](#Requesting-a-feature)                                                                           | Create a [feature suggestion issue](https://github.com/UnitTestBot/UTBotJava/issues/new?assignees=&labels=&template=feature_request.md&title=) |
| [Contributing the code (bug fix or feature implementation)](#Contributing-the-code-(bug-fix-or-feature-implementation)) | Create a pull request                                                                                                                         |
| [Reproducing a reported bug](#Reproducing-a-reported-bug)                                                               | Comment on the issue                                                                                                                          |
| [Testing a pull request](#Testing-a-pull-request)                                                                       | Comment on the pull request                                                                                                                   |
| [Improving documentation](#Improving-documentation)                                                                     | Create an issue<br/>Create a pull request<br/>Comment on issues and PRs                                                                       |
| [Sharing ideas](#Sharing-ideas)                                                                                         | Start the [Discussion](https://github.com/UnitTestBot/UTBotJava/discussions) or join the existing one                                         |

# How to submit a contribution

## Reporting a bug

1. Check if the bug (a true bug!) has already been reported: search through [UnitTestBot issues](https://github.com/UnitTestBot/UTBotJava/issues). Please, don't duplicate.
2. Check with the [Naming and labeling conventions](docs/contributing/Conventions.md).
3. Make sure you have all the necessary information as per [template](https://github.com/UnitTestBot/UTBotJava/issues/new?assignees=&labels=&template=bug_report.md&title=) and create the bug reporting issue.

## Requesting a feature

1. Check if the feature has already been requested: search through [UnitTestBot issues](https://github.com/UnitTestBot/UTBotJava/issues).
2. Check with our [Naming and labeling conventions](docs/contributing/Conventions.md).
3. Make sure you are able to provide all the necessary information as per [template](https://github.com/UnitTestBot/UTBotJava/issues/new?assignees=&labels=&template=feature_request.md&title=) and create the feature request issue.

## Contributing the code (bug fix or feature implementation)

### "Good first issue"

If you have little experience in contributing, try to resolve the issues labeled as the ["good first"](https://github.com/UnitTestBot/UTBotJava/contribute) ones.

### Small or "obvious" fixes

Do you need to create an issue if you want to fix a bug?

* Quick fix → no issue → pull request.
* Takes time to fix → detailed issue → pull request.

### General flow for contributing code

1. Make sure you've carefully read the [Legal notes](#Legal-notes)!
2. Create your own fork of the code.
3. Clone the forked repository to your local machine.
4. Implement changes. Please refer to the [Developer guide](DEVNOTE.md) for **system requirements**, **code
   style** and
   **steps for building the project**.
5. Test your code:
   * Please, provide regression or integration tests for your code changes. If you don't do that, the reviewer can and highly likely **_will reject_** the PR. It is the contributor's responsibility to provide such tests or to reason why they are missing.
   * When implementing something new, it's great to find real users and ask them to try out your feature — to prove
     the necessity and quality of your suggestion.
6. Check with the [Naming and labeling conventions](docs/contributing/Conventions.md).
7. Create the pull request, and you'll see if the automated tests pass on GitHub. Your reviewer will possibly recommend
   you more tests.

## Reproducing a reported bug

If you reproduce an existing issue and it helps to get some additional context on the problem, feel free to comment on the issue.

## Testing a pull request

You can merge a pull request into your local copy of the project and test the changes. If you find something you'd like to share, add the outcome of your testing in a comment on the pull request.

## Improving documentation

Here at UnitTestBot we regard documentation as code. It means that the general flow for writing and reviewing docs
is the same as for the code. If you'd like to improve the existing docs or to add something new, please follow the flow:

1. Make sure you've carefully read the [Legal notes](#Legal-notes)!
2. Create your own fork of the code.
3. Clone the forked repository to your local machine.
4. Implement changes to docs (change the existing doc or create a new one). Usually, we create a new doc for a new feature, not for the small fixes. You are not obliged to write a detailed text about the feature you implement. You have to only describe it properly in both the related issue and the pull request, but it will be great if you still provide some docs.
6. Check with the [Naming and labeling conventions](docs/contributing/Conventions.md).
7. Create the pull request, and we'll review it.

* You can request a new doc — create an issue, using the [guide for a feature request](#Requesting-a-feature).
* You can comment on the docs-related issues or PRs.

## Sharing ideas

We have a lot of new ideas, but we always need more!

These are our main areas of interest:

* technologies for code analysis, generating unit tests, e. g. symbolic execution, fuzzing, machine learning, etc.;
* real-life examples of using UnitTestBot, as well as possible use cases, scenarios and user stories;
* practices and problems or UX research related to unit testing with or without automated test generation tools.

If you are keen on these things too, please share your ideas with us. Even if they are sketchy and not ready for being implemented or even requested right now, go ahead and join the existing [Discussions](https://github.com/UnitTestBot/UTBotJava/discussions) or [start](https://github.com/UnitTestBot/UTBotJava/discussions/new) the new one.

# Code review process
Please choose [denis-fokin](https://github.com/denis-fokin) as a reviewer. He will reassign your PR to someone else from the core team, if necessary.

We do our best in reviewing, but we can hardly specify the exact timeout for it. Be sure that we'll certainly answer your pull request!

# Legal notes

By contributing, you agree that your contributions will be licensed under the [Apache License 2.0](https://github.com/UnitTestBot/UTBotJava/blob/main/LICENSE).

Feel free to [contact us directly](https://www.utbot.org/about) if that's a concern.