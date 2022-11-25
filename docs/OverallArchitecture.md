# Overall Unit Test Bot Architecture

Unit Test Bot overall architecture can be presented as following bird-eye view. Look below to check each component's purpose.

```mermaid
flowchart TB
    subgraph Clients
        direction LR
        IntellijPlugin        
        MavenPlugin["Maven/Gradle plugins"]
        GithubAction-->MavenPlugin
        ExternalJavaClient[\External Java Client\]
        CLI
        ContestEstimator    
                     
    end    

    subgraph Facades
        direction LR
        EngineMain[[EngineMain]]
        UtBotJavaApi[[UtBotJavaApi]]
        GenerateTestsAndSarifReport[[GenerateTestsAndSarifReport]]       
    end
    IntellijPlugin--RD-->EngineMain
    MavenPlugin-->GenerateTestsAndSarifReport
    ExternalJavaClient-->UtBotJavaApi

    subgraph API["Generation API"]
        direction LR
        TestCaseGenerator[[TestCaseGenerator]]
        CodeGenerator[[CodeGenerator]]
    end
    Facades-->API
    CLI-->API
    ContestEstimator-->API
    
        

    subgraph Components
        direction LR
        SymbolicEngine-->jlearch
        SymbolicEngine-->ConcreteExecutor
        Fuzzer-->ConcreteExecutor
        SarifReport
        Minimizer
        CodeRenderer
        Summaries
        jlearch
    end    
    CodeGenerator-->CodeRenderer
    TestCaseGenerator-->SymbolicEngine
    TestCaseGenerator-->Fuzzer
    TestCaseGenerator-->Minimizer
    TestCaseGenerator-->Summaries
    GenerateTestsAndSarifReport-->SarifReport

    UTSettings((UTSettings))
    UTSettings<--RD/local-->Components    
    UTSettings<---->Clients
    TestCaseGenerator--warmup-->ConcreteExecutor
    ConcreteExecutor--RD-->InstrumentedProcess

```

## Typical interraction between components 

Interaction diagram started from Intellij plugin UI is presented below
```mermaid
sequenceDiagram
    autonumber
    actor user as User
    participant ij as Intellij plugin
    participant engine as Engine process
    participant concrete as Instrumented process
    
    user ->> ij: Invoke "Generate tests with UTBot"
    ij ->> ij: Calculate methods, framework to show
    ij ->> user: Show UI

    break user clicked "Cancel"
        user -->> user: Exit
    end
    user ->> ij: Click "Generate tests"
    ij ->> ij: Calculate what jar need to be installed
        
    opt Some jars need to be installed  
            ij ->> ij: Install jars
    end

    ij ->> engine: Start process
    activate engine
    ij ->> engine: Setup ut context
    
    loop For all files
        ij ->> engine: Generate UtExecutions
        loop for all UtExecutions for method found by engine
            engine ->> concrete: Run concretely            
        end
        engine --> engine: Minimize all tests for method
        engine --> engine: Summaries for method
    end
    ij ->> engine: Render code
    engine ->> ij: File with tests
    deactivate engine

    
```

# Clients

### Intellij plugin
> Module: [utbot-intellij](https://github.com/UnitTestBot/UTBotJava/tree/main/utbot-intellij)
>
> Purpose: UI interface for Java/Kotlin users


TODO (Vassily Kudryashov)

### Maven/gradle plugin

TODO (Nikita Stroganov)

### Github action

TODO (Nikita Stroganov)

### CLI

TODO (Nikita Stroganov)

### Contest estimator
The main responsibility of Contest estimator is running UTBot on prepared projects in advance and providing some statistics such as instruction coverage.

It is placed in [utbot-junit-contest][contest estimator 1] module and has several entry points:
- [ContestEstimator.kt][contest estimator 2] - the main entry point of Contest estimator, it runs UTBot on specified projects, calculates some statistics for target classes and projects and outputs them in a console. 
- [StatisticsMonitoring.kt][contest estimator 3] - an additional entry point of Contest estimator which does the same as the previous one, but can be configured from a file and dumps output statistics to a file. 
It is used to [monitor and chart][contest estimator 4] statistics every night.

[contest estimator 1]: ../utbot-junit-contest
[contest estimator 2]: ../utbot-junit-contest/src/main/kotlin/org/utbot/contest/ContestEstimator.kt
[contest estimator 3]: ../utbot-junit-contest/src/main/kotlin/org/utbot/monitoring/StatisticsMonitoring.kt
[contest estimator 4]: NightStatisticsMonitoring.md

# Components

### Symbolic engine
TODO (Alexey Menshutin)

### Concrete executor
TODO (Sergey Pospelov)

### Instrumented process
TODO (Rustam Sadykov)

### Code renderer
TODO (Egor Kulikov)

### Fuzzer
TODO (Maxim Pelevin)

### Minimizer
TODO (Sergey Pospelov)

### Summaries
TODO (Alexey Zinoviev)

### Sarif report
TODO (Nikita Stroganov)



# Cross-cutting subsystems

### Logging
TODO (Arteniy Kononov)

### RD
TODO (Arteniy Kononov)

### UTSettings
TODO (Vassily Kudryashov)

