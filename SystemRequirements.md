# System requirements

---

### To generate tests with UnitTestBot:

you have to install IntelliJ IDEA (versions from 2022.1 to 2022.1.4 are supported).

### To contribute to UnitTestBot:

you have to install

- IntelliJ IDEA (versions from 2022.1 to 2022.1.4 are supported);

- JDK 11;

- Kotlin 1.7.0 or later.

Please check your development environment:

- ```JAVA_HOME``` environment variable should contain the path to JDK 11 installation directory;

- ```PATH``` environment variable should contain the path to the ```bin``` folder of JDK 11 installation directory;

- ```KOTLIN_HOME``` environment variable should contain the path to the ```kotlinc``` folder of Kotlin (1.7.0 or later) installation 
  directory;
- Project SDK  (1) and Gradle SDK (2) should be set to JDK 11:
    <br>(1) **IntelliJ IDEA → File → Project Structure → Project Settings → Project → SDK**,
    <br>(2) **IntelliJ IDEA → File → Settings → Build, Execution, Deployment → Build Tools →  Gradle**.

Please note: if the environment variables lead to unsupported JDK or Kotlin versions, you won't be able to build the UnitTestBot project.
