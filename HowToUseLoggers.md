# How to use loggers in UTBotJava

If you develop different modules of our project like CE, CLI or else, you might need to use loggers or even add your own ones to UTBot. 
<br/> Let\`s see how you can work with it. 🙂

🟢**1. Find out where appropriate log4j2.xml configuration file is**

It depends on two factors:

- Find the project you will run. For instance, for Idea plugin it is **_utbot-intellij_** subproject
- Chose appropriate log4j2.xml. If you are going to run tests for framework, it\`s in the test folder of utbot-framework subproject.

The file is usually in the resource folder. 
<br/>
<br/>
<br/>
🟢**2. Find out the logger name**

The easiest way is:

- Go in the code that you are going to debug. Let’s assume it is a method in org.utbot.framework.plugin.api.TestCaseGenerator.
- Find out if there is a KotlinLogging object that is used to create a **logger**
- If such a logger exists, use the fully qualified class name as the logger name in the next steps
<br/>

🟢**3. Add logger**

Open log4j2.xml and add the logger in the loggers section like this

```
<Logger name=" org.utbot.framework.plugin.api.TestCaseGenerator " level="info">
    <AppenderRef ref="Console"/>
</Logger>
```
<br/>
<br/>

🟢**4. Logger level**

Depending on the desired amount of information, change the **level** attribute. If you do not see output in the console, the wrong level value could be the reason. The **trace** is most verbose level. 
<br/>
<br/>

🟢**5. Output**

Sometimes the logging information could be printed in a wrong destination. In that case, change the AppenderRef tag. It can be used with Console value or some other value (for instance, FrameworkAppender) 
<br/>
<br/>

🟢**6. Message format**

If you do not like the format for logging output, you can change it in PatternLayout tag (see log4j2.xml in utbot-framework/src/test/resources/) 
<br/>
<br/>

🟢**7. Multiple loggers**

Sometimes it is handy to add an extra logger to a Kotlin class in order to log different functionality independently. 

The primary logger is usually defined as 

`private val logger = KotlinLogging.logger {} `


You may add an extra logger 

`private val timeoutLogger = KotlinLogging.logger(logger.name + ".timeout") `


Having this logger, you can use it in code with different log levels in parallel with the primary logger.
<br/>
<br/>

🟢**7.1 To enable the logger**

1. Go to the file you are currently working on
2. Select the file in the project tab (alt-f1)
3. Find the closest log4j2.xml file (usually it is located in the resources file), enable the logger with a desirable log level
 

`<Logger name="org.utbot.engine.UtBotSymbolicEngine.timeout" level="debug"/>`


