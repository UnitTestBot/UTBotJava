## File 1

Source: https://github.com/AtsushiSakai/PythonRobotics/blob/master/PathPlanning/DynamicWindowApproach/dynamic_window_approach.py

Top-level functions (with only positional arguments):

- [ ] `def dwa_control(x, config, goal, ob)`
- [ ] `def motion(x, u, dt)`
- [ ] `def calc_dynamic_window(x, config)`
- [ ] `def predict_trajectory(x_init, v, y, config)`
- [ ] `def calc_control_and_trajectory(x, dw, config, goal, ob)`
- [ ] `def calc_obstacle_cost(trajectory, ob, config)`
- [ ] `def calc_to_goal_cost(trajectory, goal)`
- [x] `def plot_robot(x, y, yaw, config)`

Used time limit: 25 seconds.

Command:

    java -jar utbot-cli-python-2023.01-SNAPSHOT.jar infer_types -p python3 -t 25000 "/home/tochilinak/Documents/projects/utbot/PythonRobotics/PathPlanning/DynamicWindowApproach/dynamic_window_approach.py" <function>

### <span style="color:green">OK:</span> `def plot_robot(x, y, yaw, config)`

```
typing.Callable[[builtins.int, builtins.int, builtins.int, dynamic_window_approach.Config], typing.Any]
typing.Callable[[builtins.float, builtins.int, builtins.int, dynamic_window_approach.Config], typing.Any]
typing.Callable[[builtins.int, builtins.float, builtins.int, dynamic_window_approach.Config], typing.Any]
typing.Callable[[builtins.int, builtins.int, builtins.float, dynamic_window_approach.Config], typing.Any]
typing.Callable[[builtins.bool, builtins.int, builtins.int, dynamic_window_approach.Config], typing.Any]
typing.Callable[[builtins.float, builtins.float, builtins.int, dynamic_window_approach.Config], typing.Any]
typing.Callable[[builtins.float, builtins.int, builtins.float, dynamic_window_approach.Config], typing.Any]
typing.Callable[[builtins.int, builtins.bool, builtins.int, dynamic_window_approach.Config], typing.Any]
typing.Callable[[builtins.int, builtins.float, builtins.float, dynamic_window_approach.Config], typing.Any]
typing.Callable[[builtins.int, builtins.int, builtins.bool, dynamic_window_approach.Config], typing.Any]
...
```

### <span style="color:red">ERROR:</span> `def dwa_control(x, config, goal, ob)`

Found annotations (why?):
```
typing.Callable[[builtins.object, builtins.object, builtins.object, builtins.object], typing.Any]
typing.Callable[[builtins.str, builtins.object, builtins.object, builtins.object], typing.Any]
typing.Callable[[builtins.object, builtins.str, builtins.object, builtins.object], typing.Any]
typing.Callable[[builtins.object, builtins.object, builtins.str, builtins.object], typing.Any]
typing.Callable[[builtins.object, builtins.object, builtins.object, builtins.str], typing.Any]
typing.Callable[[numpy.str_, builtins.object, builtins.object, builtins.object], typing.Any]
typing.Callable[[builtins.object, numpy.str_, builtins.object, builtins.object], typing.Any]
typing.Callable[[builtins.object, builtins.object, numpy.str_, builtins.object], typing.Any]
typing.Callable[[builtins.object, builtins.object, builtins.object, numpy.str_], typing.Any]
...
```

### <span style="color:yellow">FAIL (TODO):</span> `def motion(x, u, dt)`

Reason: no analysis of __slices__.

### <span style="color:red">ERROR:</span> `def calc_dynamic_window(x, config)`

Thrown exception. Probable reason: __strange annotations (with commas, etc.)__.

### <span style="color:red">ERROR:</span> `def predict_trajectory(x_init, v, y, config)`

Found annotations (why?):
```
typing.Callable[[builtins.object, builtins.object, builtins.object, dynamic_window_approach.Config], typing.Any]
typing.Callable[[dynamic_window_approach.Config, builtins.object, builtins.object, dynamic_window_approach.Config], typing.Any]
typing.Callable[[builtins.object, dynamic_window_approach.Config, builtins.object, dynamic_window_approach.Config], typing.Any]
typing.Callable[[builtins.object, builtins.object, dynamic_window_approach.Config, dynamic_window_approach.Config], typing.Any]
typing.Callable[[builtins.bool, builtins.object, builtins.object, dynamic_window_approach.Config], typing.Any]
typing.Callable[[dynamic_window_approach.Config, dynamic_window_approach.Config, builtins.object, dynamic_window_approach.Config], typing.Any]
typing.Callable[[dynamic_window_approach.Config, builtins.object, dynamic_window_approach.Config, dynamic_window_approach.Config], typing.Any]
typing.Callable[[builtins.object, builtins.bool, builtins.object, dynamic_window_approach.Config], typing.Any]
...
```

### <span style="color:yellow">FAIL:</span> `def calc_control_and_trajectory(x, dw, config, goal, ob)`

### <span style="color:yellow">FAIL (TODO):</span> `def calc_obstacle_cost(trajectory, ob, config)`

Reason: no analysis of __slices__.

### <span style="color:yellow">FAIL (TODO):</span> `def calc_to_goal_cost(trajectory, goal)`

Reason: no analysis of __slices__.

## File 2

Source: https://github.com/AtsushiSakai/PythonRobotics/blob/master/PathPlanning/DubinsPath/dubins_path_planner.py

Top-level functions (with only positional arguments):
- [ ] `def _mod2pi(theta)`
- [x] `def _calc_trig_funcs(alpha, beta)`
- [ ] `def _LSL(alpha, beta, d)`
- [ ] `def _RSR(alpha, beta, d)`
- [ ] `def _LSR(alpha, beta, d)`
- [ ] `def _RSL(alpha, beta, d)`
- [ ] `def _RLR(alpha, beta, d)`
- [ ] `def _LRL(alpha, beta, d)`
- [ ] `def _dubins_path_planning_from_origin(end_x, end_y, end_yaw, curvature, step_size, planning_funcs)`
- [ ] `def _interpolate(length, mode, max_curvature, origin_x, origin_y, origin_yaw, path_x, path_y, path_yaw)`
- [ ] `def _generate_local_course(lengths, modes, max_curvature, step_size)`

Used time limit: 25 seconds.

Command:

    java -jar utbot-cli-python-2023.01-SNAPSHOT.jar infer_types -p python3 -t 25000 "/home/tochilinak/Documents/projects/utbot/PythonRobotics/PathPlanning/DubinsPath/dubins_path_planner.py" <function>

### <span style="color:green">OK:</span> `def _calc_trig_funcs(alpha, beta)`

```
typing.Callable[[builtins.float, builtins.float], typing.Any]
typing.Callable[[builtins.bool, builtins.float], typing.Any]
typing.Callable[[builtins.float, builtins.bool], typing.Any]
typing.Callable[[builtins.int, builtins.float], typing.Any]
typing.Callable[[builtins.bool, builtins.bool], typing.Any]
typing.Callable[[builtins.float, builtins.int], typing.Any]
typing.Callable[[builtins.int, builtins.bool], typing.Any]
typing.Callable[[builtins.bool, builtins.int], typing.Any]
typing.Callable[[numpy.timedelta64, builtins.bool], typing.Any]
typing.Callable[[builtins.int, builtins.int], typing.Any]
typing.Callable[[builtins.bool, numpy.timedelta64], typing.Any]
typing.Callable[[enum.IntEnum, builtins.float], typing.Any]
...
```

### <span style="color:yellow">OK or ERROR? :</span> `def _mod2pi(theta)`

```
typing.Callable[[builtins.bool], typing.Any]
typing.Callable[[builtins.object], typing.Any]
typing.Callable[[builtins.int], typing.Any]
typing.Callable[[sys.UnraisableHookArgs], typing.Any]
typing.Callable[[pathlib.PurePath], typing.Any]
typing.Callable[[pathlib.PurePosixPath], typing.Any]
typing.Callable[[pathlib.PureWindowsPath], typing.Any]
typing.Callable[[pathlib.Path], typing.Any]
typing.Callable[[pathlib.PosixPath], typing.Any]
typing.Callable[[pathlib.WindowsPath], typing.Any]
typing.Callable[[builtins.function], typing.Any]
typing.Callable[[builtins.staticmethod[typing.Any]], typing.Any]
...
```

### <span style="color:red">ERROR:</span> `def _LSL(alpha, beta, d)`

Thrown exception. Probable reason: __strange annotations (with commas, etc.)__.

### <span style="color:red">ERROR:</span> `def _RSR(alpha, beta, d)`

Thrown exception. Probable reason: __strange annotations (with commas, etc.)__.

### <span style="color:yellow">FAIL (TODO):</span> `def _LSR(alpha, beta, d)`

?

### <span style="color:red">ERROR:</span> `def _RSL(alpha, beta, d)`

Thrown exception. Probable reason: __strange annotations (with commas, etc.)__.

### <span style="color:red">ERROR:</span> `def _RLR(alpha, beta, d)`

Thrown exception. Probable reason: __strange annotations (with commas, etc.)__.

### <span style="color:yellow">FAIL (TODO):</span> `def _LRL(alpha, beta, d)`

?

### <span style="color:yellow">FAIL:</span> `def _dubins_path_planning_from_origin(end_x, end_y, end_yaw, curvature, step_size, planning_funcs)`

Reason: signature includes list of Callable. For now it is too complicated.


### <span style="color:yellow">FAIL (TODO?):</span> `def _interpolate(length, mode, max_curvature, origin_x, origin_y, origin_yaw, path_x, path_y, path_yaw)`

Reason: last 3 arguments are complicated.

### <span style="color:red">ERROR:</span> `def _generate_local_course(lengths, modes, max_curvature, step_size)`

Exception was thrown during code analysis (before type inference).

```
java.lang.IndexOutOfBoundsException: Index -1 out of bounds for length 6
	at java.base/jdk.internal.util.Preconditions.outOfBounds(Preconditions.java:64)
	at java.base/jdk.internal.util.Preconditions.outOfBoundsCheckIndex(Preconditions.java:70)
	at java.base/jdk.internal.util.Preconditions.checkIndex(Preconditions.java:248)
	at java.base/java.util.Objects.checkIndex(Objects.java:372)
	at java.base/java.util.ArrayList.get(ArrayList.java:459)
	at java.base/java.util.Collections$UnmodifiableList.get(Collections.java:1310)
	at org.utbot.python.newtyping.ast.ParseUtilsKt.parseForStatement(ParseUtils.kt:43)
	at org.utbot.python.newtyping.ast.visitor.hints.HintCollector.processForStatement(HintCollector.kt:203)
	at org.utbot.python.newtyping.ast.visitor.hints.HintCollector.collectFromNodeAfterRecursion(HintCollector.kt:73)
	at org.utbot.python.newtyping.ast.visitor.Visitor.innerVisit(Visitor.kt:27)
	at org.utbot.python.newtyping.ast.visitor.Visitor.innerVisit(Visitor.kt:25)
	at org.utbot.python.newtyping.ast.visitor.Visitor.visit(Visitor.kt:8)
	at org.utbot.python.newtyping.inference.TypeInferenceProcessor$inferTypes$7.invokeSuspend(TypeInferenceProcessor.kt:82)
	at kotlin.coroutines.jvm.internal.BaseContinuationImpl.resumeWith(ContinuationImpl.kt:33)
	at kotlin.sequences.SequenceBuilderIterator.hasNext(SequenceBuilder.kt:129)
	at org.utbot.cli.language.python.PythonTypeInferenceCommand.run(PythonTypeInferenceCommand.kt:81)
	at com.github.ajalt.clikt.parsers.Parser.parse(Parser.kt:204)
	at com.github.ajalt.clikt.parsers.Parser.parse(Parser.kt:213)
	at com.github.ajalt.clikt.parsers.Parser.parse(Parser.kt:17)
	at com.github.ajalt.clikt.core.CliktCommand.parse(CliktCommand.kt:396)
	at com.github.ajalt.clikt.core.CliktCommand.parse$default(CliktCommand.kt:393)
	at com.github.ajalt.clikt.core.CliktCommand.main(CliktCommand.kt:411)
	at com.github.ajalt.clikt.core.CliktCommand.main(CliktCommand.kt:436)
	at org.utbot.cli.language.python.ApplicationKt.main(Application.kt:31)
```

## File 3

Source: https://github.com/AtsushiSakai/PythonRobotics/blob/master/utils/angle.py

Top-level functions:
- [ ] `def rot_mat_2d(angle)`
- [ ] `def angle_mod(x, zero_2_2pi, degree)`

### <span style="color:red">ERROR:</span> `def rot_mat_2d(angle)`

Incorrect annotations:
```
typing.Callable[[builtins.object], typing.Any]
typing.Callable[[builtins.int], typing.Any]
typing.Callable[[builtins.str], typing.Any]
typing.Callable[[numpy.str_], typing.Any]
typing.Callable[[builtins.bool], typing.Any]
typing.Callable[[enum.IntEnum], typing.Any]
typing.Callable[[enum.auto], typing.Any]
typing.Callable[[enum.IntFlag], typing.Any]
typing.Callable[[signal.Signals], typing.Any]
typing.Callable[[signal.Handlers], typing.Any]
typing.Callable[[signal.Sigmasks], typing.Any]
typing.Callable[[argparse.FileType], typing.Any]
typing.Callable[[_operator.methodcaller], typing.Any]
typing.Callable[[builtins.function], typing.Any]
typing.Callable[[builtins.staticmethod[typing.Any]], typing.Any]
...
```

### <span style="color:yellow">FAIL (TODO):</span> `def angle_mod(x, zero_2_2pi, degree)`

???