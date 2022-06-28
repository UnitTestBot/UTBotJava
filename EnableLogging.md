# How to enable UTBot logging in IntelliJ IDEA

In UTBot we use loggers like the following one:


`val logger = Logger.getInstance(CodeGenerator::class.java)`


Instead of sending loggers output in the standard stream or a file, we can put them right into IntelliJ IDEA. So, let\`s do it then! 😃


1. In UTBot repository find Gradle > Tasks > intellij > runIde and choose it with the right button click

![image](https://user-images.githubusercontent.com/106974353/175880783-57a190f1-283d-448f-984b-8acd62af657c.png)


2. Select Modify Run Configuration... item, and then Modify options > Specify logs to be shown in console

![image](https://user-images.githubusercontent.com/106974353/175881032-944bc31a-bd13-43c1-9ebf-e2b542984b7d.png)


3. Click ➕ and add new Idea log file (or choose any name you want for it 😉) in the Log files to be shown in console section

![image](https://user-images.githubusercontent.com/106974353/175881081-4612493b-a8fb-4c5b-b3b2-edaa4bea0703.png)


4. Restart the 'runIde' task, check that the new tab is present in your IDE

![image](https://user-images.githubusercontent.com/106974353/175881135-6fa393fb-4f62-4f39-b009-dea9bc742411.png)


5. And we\`re done! 😃 Narrow logging messages to the loggers you are interested in

![image](https://user-images.githubusercontent.com/106974353/175881203-9e6e1ed2-3ba7-4ea9-a18a-a5ce314a13ab.png)


If you want to use the existing loggers in UTBot or you`re a contributor and you have your own loggers to add, please watch [this article](HowToUseLoggers.md) to know how to do it in a better way.

