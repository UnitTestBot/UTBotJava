<!---
name: SSH session with GitHub agent
route: /docs/java/ci/ssh-session-with-github-agent
parent: Documentation
menu: CI
description: How to setup SSH session with GitHub agent
--->

# SSH session with GitHub agent

It's available to use **action** letting set up SSH session with GitHub agent in your **workflows**. The detailed documentation with the examples of use can be found in the [official repository](https://github.com/mxschmitt/action-tmate).

The action setting SSH session can be easily plugged in your workflow with the example below:
```
- name: Setup tmate session
  uses: mxschmitt/action-tmate@v3
```

When the action is plugged in the workflow log (the part corresponding to tmate action log) can be found the URL. By the URL you can access the terminal of your host.

There are also some ways to setup action behavior. E.g., the default behavior of the action is to remain SSH session open until the workflow times out. It's available to setup timeout parameter yourself.