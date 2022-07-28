# How to contribute to UTBot Java

To begin with, we are very thankful for your time and willingness to read this and contribute! 

The following guideline should help you with suggesting changes to our project, so feel free to use it for your contribution. 😃 


## I\`ve found a bug! How to report?

First of all, please check our [Issues](https://github.com/UnitTestBot/UTBotJava/issues) — this bug may have already been reported, and you just don\`t need to spend your time on a new one.

If you haven\`t found the relevant issue,  don\`t hesitate to [create a new one](https://github.com/UnitTestBot/UTBotJava/issues/new?assignees=&labels=&template=bug_report.md&title=), including as much detail as possible — the pre-made template will assist you in it.

In case you already have a PR with a solution, please remain so amazing and link it with the created issue.


## I have an improvement suggestion! 
Want a new feature or to change the existing one? We are very welcome your fresh ideas. 😃

Please [create an issue](https://github.com/UnitTestBot/UTBotJava/issues/new?assignees=&labels=&template=feature_request.md&title=) with your proposal and describe your idea with full information about it. By adding some examples you also bring much happiness to our souls!

Give us some time to review your proposal and provide you with our feedback. It will be decided who is preparing the pull request: we may need your help or we will take care of it all. 🙂


## Coding conventions
Our team adheres to the defined requirements to coding style to optimize for readability. You can take a look on this [Coding style guide](https://github.com/saveourtool/diktat/blob/master/info/guide/diktat-coding-convention.md) to better understand what we expect to see in your code. 


## Naming conventions
We have been using GitHub for a while, and now we have a couple of tips for naming issues, commits and pull requests (PRs). You are welcome to stick to them too 🙂

Our favorite recipes are:

   **issue title = feature request or bug description + issue ID**

   **commit message = PR title = fix description + issue ID + (PR number)**

How to insert the issue ID into the commit message and the PR title?<br>
—	Manually.

How to append the PR number to the PR title?<br>
—	It appends automatically.

How to insert the PR number into the commit message?<br>
—	*Push* the feature branch + *Create pull request* on GitHub and then →<br>
1) The preferred and the easiest flow:
<br>*Squash and merge* on GitHub → the PR number automatically appends to the resulting commit message
2) The flow for advanced users:
<br>(a)	squash the commits locally → insert the PR number in parentheses (!) manually into the resulting commit 
   message + *Force Push* the resulting commit → *Rebase and merge* on GitHub
<br>or
<br>(b)	change the commit message locally → insert the PR number in parentheses (!) manually + *Force Push* the 
   commit → *Rebase and merge* on GitHub


## How to setup development environment?

Please refer [Developer guide](https://github.com/UnitTestBot/UTBotJava/blob/main/DEVNOTE.md) to setup developer environment, build and run UTBot.


## How to test you PR? 

Currently, not all checks are automized. It's required to do manual testing after PR.
