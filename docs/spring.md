# Automated test generation for Spring-based code

Java developers actively use the Spring framework to implement the inversion of control and dependency injection. 
Testing Spring-based applications differs significantly from testing standard Java programs. Thus, we customized 
UnitTestBot to analyze Spring projects.

<!-- TOC -->
  * [General notes](#general-notes)
    * [Limitations](#limitations)
    * [Testability](#testability)
  * [Standard unit tests](#standard-unit-tests)
    * [Example](#example)
    * [Use cases](#use-cases)
  * [Spring-specific unit tests](#spring-specific-unit-tests)
    * [Example](#example-1)
    * [Use cases](#use-cases-1)
    * [Side effects](#side-effects)
    * [Mechanism](#mechanism)
  * [Integration tests](#integration-tests)
    * [Service layer](#service-layer)
      * [Side effects](#side-effects-1)
      * [Use cases](#use-cases-2)
    * [Controller layer](#controller-layer)
      * [Example](#example-2)
    * [Microservice layer](#microservice-layer)
<!-- TOC -->

## General notes

UnitTestBot proposes three approaches to automated test generation:
* [standard unit tests](#standard-unit-tests) that mock environmental interactions;
* [Spring-specific unit tests](#spring-specific-unit-tests) that use information about the Spring application context to reduce the number of 
  mocks;
* and [integration tests](#integration-tests) that validate interactions between application components.

Hereinafter, by _components_ we mean Spring components.

For classes under test, one should select an appropriate type of test generation based on their knowledge 
about the Spring specifics of the current class. Recommendations on how to choose the test type are provided below.
For developers who are new to Spring, there is a "default" generation type.

### Limitations

UnitTestBot Java with Spring support uses symbolic execution to generate unit tests, so typical problems
related to this technique may appear: it may be not so efficient for multithreaded programs, functions with calls to 
external libraries, processing large collections, etc.

### Testability

Note that UnitTestBot may generate unit tests more efficiently if your code is written to be unit-testable: the 
functions are not too complex, each function implements one logical unit, static and global data are used 
only if required, etc. Difficulties with automated test generation may have "diagnostic" value: it 
may mean that you should refactor your code.

## Standard unit tests

The easiest way to test Spring applications is to generate unit tests for components: to
mock the external calls found in the method under test and to test just this method's
functionality. UnitTestBot Java uses the Mockito framework that allows to mark
the to-be-mocked objects with the `@Mock` annotation and to use the `@InjectMock`
annotation for the tested instance injecting all the mocked fields. See [Mockito](https://site.mockito.org/) 
documentation for details.

### Example

Consider generating unit tests for the `OrderService` class that autowires `OrderRepository`:

```java

@Service
public class OrderService {

@Autowired
private OrderRepository orderRepository ;

public List<Order> getOrders () {
return orderRepository.findAll ();
}
}

public interface OrderRepository extends JpaRepository <Order, Long>
```
Then we mock the repository and inject the resulting mock into a service:

```java
public final class OrderServiceTest {
    @InjectMocks
    private OrderService orderService

    @Mock
    private OrderRepository orderRepositoryMock

    @Test
    public void testGetOrders () {
        when(orderRepositoryMock .findAll()).thenReturn((List)null)

            List actual = orderService .getOrders()
            assertNull(actual)
	}
```

This test type does not process the Spring context of the original application. The components are tested in 
isolation. 

It is convenient when the component has its own meaningful logic and may be useless when its main responsibility is to call other components.

Note that if you autowire several beans of one type or a collection into the class under test, the code of test 
class will be a bit different: for example, when a collection is autowired, it is marked with `@Spy` annotation due 
to Mockito specifics (not with `@Mock`).

### Use cases

When to generate standard unit tests:
* _Service_ or _DAO_ layer of Spring application is tested.
* Class having no Spring specific is tested.
* You would like to test your code in isolation.
* You would like to generate tests as fast as possible.
* You would like to avoid starting application context and be sure the test generation process has no Spring-related side effects.
* You would like to generate tests in one click and avoid creating specific profiles or configuration classes for 
  testing purposes.

We suggest using this test generation type for the users that are not so experienced in Spring or would like to get 
test coverage for their projects without additional efforts.

## Spring-specific unit tests

This is a modification of standard unit tests generated for Spring projects that may allow us to get more 
meaningful tests.

### Example

Consider the following class under test

```java
@Service
public class GenderService {

@Autowired
public Human human

public String getGender () {
return human.getGender();
}
}
```
where `Human` is an interface that has just one implementation actually used in current project configuration.

```java
public interface Human {
String getGender();
}

public class Man implements Human {
public String getGender() {
return “man”
}
}
```

The standard unit test generation approach is to mock the _autowired_ objects. It means that the generated test will be 
correct but useless. However, there is just one implementation of the `Human` interface, so we may use it directly 
and generate a test like this:

```java
@Test
public void testGetGender_HumanGetGender() {
GenderService genderService = new GenderService();
genderService.human = new Man();
String actual = genderService.getGender();
assertEquals(“man”, actual);
}
```

Actually, dependencies in Spring applications are often injected via interfaces, and they often have just one actual 
implementation, so it can be used in the generated tests instead of an interface. If a class is injected itself, it 
will also be used in tests instead of a mock.

You need to select a configuration to guide the process of creating unit tests. We support all commonly used 
approaches to configure the application:
* using an XML file,
* Java annotation,
* or automated configuration in Spring Boot.

Although it is possible to use the development configuration for testing purposes, we strictly recommend creating a separate one.

### Use cases

When to generate Spring-specific unit tests:
* to reduce the amount of mocks in generated tests
* and to use real object types instead of their interfaces, obtaining tests that simulate the method under test execution.

### Side effects

We do not recommend generating Spring-specific unit tests, when you would like to maximize line coverage.
The goal of this approach is to cover the lines that are relevant for the current configuration and are to be used 
during the application run. The other lines are ignored.

When a concrete object is created instead of mocks, it is analyzed with symbolic execution. It means that the 
generation process may take longer and may exceed the requested timeout.

### Mechanism

A Spring application is created to simulate a user one. It uses configuration importing users one with an additional 
bean of a special _bean factory post processor_.

This _post processor_ is called when bean definitions have already been created, but actual bean initialization has 
not been started. It gets all accessible information about bean types from the definitions and destroys these 
definitions after that.

Further Spring context initialization is gracefully crashed as bean definitions do not exist anymore. Thus, this 
test generation type is still safe and will not have any Spring-related side effects.

Bean type information is used in symbolic execution to decide if we should mock the current object or instantiate it.

## Integration tests

The main difference of integration testing is that it tests the current component while taking interactions with 
other classes into account.

### _Service_ layer

Consider an `OrderService` class we have already seen. Actually, this class has just one 
responsibility: to return the result of a call to the repository. So, if we mock the repository, our unit test is 
actually useless. However, we can test this service in interaction with the repository: save some information to the 
database and verify if we have successfully read it in our method. Thus, the test method looks as follows.

```java

@Autowired
private OrderService orderService

@Autowired
private OrderRepository orderRepository

@Test
public void testGetOrderById() throws Exception {
Order order = new Order();
Order order1 = orderRepository.save(order);
long id = (Long) getFieldValue(order1, "com.rest.order.models.Order ", "id“);

Order actual = orderService.getOrderById(id);
assertEquals (order1, actual);
}
```
The key idea of integration testing is to initialize the context of a Spring application and to autowire a bean of 
the class under test, and the beans it depends on. The main difficulty is to mutate the initial _autowired_ state of the 
object under test to another state to obtain meaningful tests (e.g. save some data to related repositories).
Here we use fuzzing methods instead of symbolic execution.

You should take into account that our integration tests do not use mocks at all. It also means that if the method 
under test contains calls to other microservices, you need to start the microservice unless you want to test your 
component under an assumption that the microservice is not responding. 
Writing tests manually, users can investigate the expected behavior of the external service for the current scenario,
but automated test generation tools have no way to do it.

Note that XML configuration files are currently not supported in integration testing. However, you may create a Java 
configuration class importing your XML file as a resource. The list of supported test 
frameworks is reduced to JUnit 4 and JUnit 5; TestNG is not supported for integration tests.

To run integration tests properly, several annotations are generated for the class with tests (some of them may be 
missed: for example, we can avoid setting active profiles via the annotation if a default profile is used).

* `@SpringBootTest` for Spring Boot applications
* `@RunWith(SpringRunner.class)`/`@ExtendWith(SpringExtension.class)` depending on the test framework
* `@BootstrapWith(SpringBootTestContextBootstrapper.class)` for Spring Boot applications
* `@ActiveProfiles(profiles = {profile_names})` to activate requested profiles
* `@ContextConfiguration(classes = {configuration_classes})` to initialize a proper configuration
* `@AutoConfugureTestDatabase`

Two additional annotations are:

* `@Transactional`: using this annotation is not a good idea for some developers because it can 
hide problems in the tested code. For example, it leads to getting data from the transaction cache instead of real 
communication with database.
However, we need to use this annotation during the test generation process due to the 
efficiency reasons and the current fuzzing approach. Generating tests in transaction but not running them in 
transaction may sometimes lead to failing tests.
In future, we are going to modify the test generation process and to use `EntityManager` and manual flushing to the 
database, so running tests in transaction will not have a mentioned disadvantage any more.

* `@DirtiesContext(classMode=BEFORE_EACH_TEST_METHOD)`: although running test method in transaction rollbacks most 
actions in the context, there are two reasons to use `DirtiesContext`. First, we are going to remove 
`@Transactional`. After that, the database `id` sequences are not rolled back with the transaction, while we would 
like to have a clean context state for each new test to avoid unobvious dependencies between them.

Currently, we do not have proper support for Spring security issues in UnitTestBot. We are going to improve it in 
future releases, but to get at least some results on the classes requiring authorization, we use `@WithMockUser` for 
applications with security issues.

#### Side effects

Actually, yes! Integration test generation requires Spring context initialization that may contain unexpected 
actions: HTTP requests, calls to other microservices, changing the computer parameters. So you need to 
validate the configuration carefully before trying to generate integration tests. We strictly recommend avoiding 
using _production_ and _development_ configuration classes for testing purposes, and creating separate ones.

#### Use cases

When to generate integration tests:
* You have a properly prepared configuration class for testing
* You would like to test your component in interaction with others
* You would like to generate tests without mocks
* You would like to test a controller
* You consent that generation may be much longer than for unit tests

### _Controller_ layer

When you write tests for controllers manually, it is recommended to do it a bit differently. Of course, you may just 
mock the other classes and generate unit tests looking similarly to the tests we created for services, but they may 
not be representative. To solve this problem, we suggest a specific integration test generation approach for controllers.

#### Example

Consider testing the following controller method:

```java

@RestController
@RequestMapping(value = "/api")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @GetMapping(path = "/orders")
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok().body(orderService.getOrders());
    }
}
```
UnitTestBot generates the following integration test for it:

```java
@Test
public void testGetAllOrders() throws Exception {
Object[] objectArray = {};
MockHttpServletRequestBuilder mockHttpServletRequestBuilder = get("/api/orders", objectArray);

ResultActions actual = mockMvc.perform(mockHttpServletRequestBuilder);

actual.andDo(print());
actual.andExpect((status()).is(200));
actual.andExpect((content()).string("[]"));
}
```

Note that generating specific tests for controllers is now in active development, so some parameter annotations and 
types have not been supported yet. For example, we have not supported the `@RequestParam` annotation yet. For now, 
specific integration tests for controllers are just an experimental feature.

### _Microservice_ layer

Actually, during integration test generation we create one specific test that can be considered as a test for the 
whole microservice. It is the `contextLoads` test, and it checks if a Spring application context has started normally.
If this test fails, it means that your application is not properly configured, so the failure of other tests is not caused by the regression in the tested code.

Normally, this test is very simple:

```java
/**
* This sanity check test fails if the application context cannot start.
  */
  @Test
  public void contextLoads() {
  }
```

If there are context loading problems, the test contains a commented exception type, a message, and a 
track trace, so it is easier to investigate why context initialization has failed.

