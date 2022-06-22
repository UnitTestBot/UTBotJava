# UTBot Java Developer Guide
 
 Here are the steps for you to jump into UTBot Java.
 
## Install UTBot Java from source
1. Clone UTBot Java repository via [Git](https://github.com/UnitTestBot/UTBotJava.git)
2. Open project in IDE

![image](https://user-images.githubusercontent.com/106974353/174806216-9d4969b4-51fb-4531-a6d0-94e3734a437a.png)

*don\`t forget to check your Project SDK and Gradle SDK are of 1.8 Java version.

![image](https://user-images.githubusercontent.com/106974353/174812758-fcbabb5b-0411-48d7-aefe-6d69873185e3.png)
![image](https://user-images.githubusercontent.com/106974353/174806632-ed796fb7-57dd-44b5-b499-e9eeb0436f15.png)

3. Open Gradle tool window
4. Launch Task utbot > Tasks > build > assemble

![image](https://user-images.githubusercontent.com/106974353/174807962-18c648fd-b67d-4556-90df-eee690abe6e2.png)

5. Wait for the build to be completed

Done! You\`re awesome and ready for digging the code. 😃

 
## Development of UTBot Java with IntelliJ IDEA

The majority of the code is written in Kotlin. 

The project is divided into Gradle subprojects. The most significant of them are: 
1. utbot-framework — all about the engine and tests for it

2. utbot-intellij — IDE plugin

3. utbot-sample — a framework with examples to demonstrate engine capacity
 
## Testing

The project contains many tests. They are usually placed in test root of the particular Gradle subproject.

While developing, it\`s useful to run tests from utbot-framework subproject. The majority of tests from this subproject generate tests for samples from the utbot-sample subproject.

