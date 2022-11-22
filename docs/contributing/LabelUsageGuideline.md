# Label usage guideline

We recommend to use labels only in these cases


![bug](https://user-images.githubusercontent.com/106974353/174105036-53ac8736-2e63-4a02-ac90-1aca34a8fb53.png)

Something isn't working.
Indicates an unexpected problem or unintended behavior.

#

![170533338-082f808e-b74b-437d-802e-568099036b1e-depositphotos-bgremover](https://user-images.githubusercontent.com/106974353/174105268-52897d9b-3939-4063-bfec-2572dcef67f4.png)

This label applies to the issues and pull requests related to `org.utbot.engine` package.
Use it if your issue or fix deals with model construction (including Soot and Jimple),
memory modeling, symbolic values, wrappers, mocking, value resolving, or interaction
with the SMT solver.

Path selector issues generally should also have the "engine" label, unless the problem
is specific to a ML-based path selection algorithm.

#

![171006007-3ad32d41-1968-4a43-ac4b-f68f016f978b-depositphotos-bgremover](https://user-images.githubusercontent.com/106974353/174105349-f33620af-8694-486b-95af-eaabfd1e4fa7.png)

This label applies to the issues and pull requests related to `org.utbot.intellij` module.
Use it if your changes in code are related to plugin UI appearance (mostly `ui` package)
or close functionality: frameworks installation, sarif reports generation, etc.

#

![171006369-d7810250-258d-4b8d-8321-2742bd0a81db-depositphotos-bgremover](https://user-images.githubusercontent.com/106974353/174105444-beab859e-ea77-47dd-a5c2-27c0be350e82.png)

This label applies to the issues and pull requests related to `org.utbot.framework.codegen`package.
Use it if your issue or fix deals with generating (rendering) code of unit tests based on obtained
from symbolic engine executions. It may relate to generation on both supported languages (Java and Kotlin).
Code generator related class names are often marked with `Cg` prefix or with `CodeGenerator` suffix.

#

![170533255-7fe1342b-4121-44f8-8678-78e52581235e-depositphotos-bgremover](https://user-images.githubusercontent.com/106974353/174105494-23cd502f-6181-445e-85fa-1f80ddc90e5f.png)

Indicates a need for improvements or additions to documentation.


#

![170533304-e0f95623-1fa5-427b-8545-ceb4113de597-depositphotos-bgremover (1)](https://user-images.githubusercontent.com/106974353/174105712-5ffc4157-142f-4971-8e4b-aced5ed2bc19.png)

This issue or pull request already exists.
Indicates similar issues, pull requests, or discussions.

#

![170537552-fba154f5-14b8-4054-aa3b-d0c7a040677f-depositphotos-bgremover](https://user-images.githubusercontent.com/106974353/174105774-4688face-7e82-4bb6-8b2a-2372e3fb6400.png)

New feature or request.

#

![170537570-ae56bc9f-19b7-4864-8a92-05e5b7f5f342-depositphotos-bgremover](https://user-images.githubusercontent.com/106974353/174105839-f0fbd000-b4c7-40fe-a261-bde8048de13b.png)

Good for newcomers.
Indicates a good issue for first-time contributors.

#

![170537578-37181739-204f-4527-a337-17333d45542d-depositphotos-bgremover](https://user-images.githubusercontent.com/106974353/174105932-596eb120-4f28-4e5c-842a-3fb4c5c375b7.png)

Extra attention is needed.
Indicates that a maintainer wants help on an issue or pull request.

#

![170537586-ef98f24c-d12d-47b3-95eb-e396c2a14337-depositphotos-bgremover](https://user-images.githubusercontent.com/106974353/174106017-4be04dff-0451-46f1-b552-e5f5f3730438.png)

This issue / PR doesn't seem right.
Indicates that an issue, pull request, or discussion is no longer relevant.

#

![170537612-daeed618-7cc2-44e6-9d67-d74939761dae-depositphotos-bgremover1](https://user-images.githubusercontent.com/106974353/174106553-506fe0bc-7ddb-47a7-9609-b7cd1b775f22.png)

Further information is requested.
Indicates that an issue, pull request, or discussion needs more information.

#

![170537619-538ec3a4-1f50-4f19-8bf3-71ce7e2d1afe-depositphotos-bgremover (3)](https://user-images.githubusercontent.com/106974353/174106628-08b7cd36-8dc7-4eb3-82c7-e917d7d11e8f.png)

This will not be worked on.
Indicates that work won't continue on an issue, pull request, or discussion.