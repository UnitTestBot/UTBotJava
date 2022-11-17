# Naming and labeling conventions

---

## Naming conventions

### How to name a branch

We use feature branches for development. Our best practice is to use the "my-github-username" prefix for each branch and to split words with the low line, e.g.:

**_githubuser/my_feature_branch_**

### How to name issues, commits and pull requests

We have been using GitHub for a while, and now we have a couple of tips for naming issues, commits and pull requests (
PRs). You are welcome to stick to them too 🙂

Our favorite recipes are:

**issue title = feature request or bug description + issue ID**

**commit message = PR title = fix description + issue ID + (PR number)**

How to insert the issue ID into the commit message and the PR title?<br>
— Manually.

How to append the PR number to the PR title?<br>
— It appends automatically.

How to insert the PR number into the commit message?<br>
—    *Push* the feature branch + *Create pull request* on GitHub and then →<br>

1) The preferred and the easiest flow:
   <br>*Squash and merge* on GitHub → the PR number automatically appends to the resulting commit message
2) The flow for advanced users:
   <br>(a)    squash the commits locally → insert the PR number in parentheses (!) manually into the resulting commit
   message + *Force Push* the resulting commit → *Rebase and merge* on GitHub
   <br>or
   <br>(b)    change the commit message locally → insert the PR number in parentheses (!) manually + *Force Push* the
   commit → *Rebase and merge* on GitHub

## Labeling conventions

To choose the proper labels for your issue or PR, refer to the [Label usage guidelines](https://github.com/UnitTestBot/UTBotJava/wiki/Labels-usage-guidelines).