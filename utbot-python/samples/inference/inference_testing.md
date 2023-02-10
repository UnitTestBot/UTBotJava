## File 1

Source: https://github.com/AtsushiSakai/PythonRobotics/blob/master/PathPlanning/DynamicWindowApproach/dynamic_window_approach.py

Top-level functions (with only positional arguments):

- [ ] `def dwa_control(x, config, goal, ob)`
- [x] `def motion(x, u, dt)`
- [x] `def calc_dynamic_window(x, config)`
- [x] `def predict_trajectory(x_init, v, y, config)`
- [x] `def calc_control_and_trajectory(x, dw, config, goal, ob)`
- [x] `def calc_obstacle_cost(trajectory, ob, config)`
- [x] `def calc_to_goal_cost(trajectory, goal)`
- [x] `def plot_robot(x, y, yaw, config)`

Used time limit: 25 seconds.

Command:

    java -jar utbot-cli-python-2023.02-SNAPSHOT.jar infer_types -p python3 -s /home/tochilinak/Documents/projects/utbot/PythonRobotics -t 25000 "/home/tochilinak/Documents/projects/utbot/PythonRobotics/PathPlanning/DynamicWindowApproach/dynamic_window_approach.py" <function>


### <span style="color:green">OK:</span> `def motion(x, u, dt)`

```
typing.Callable[[builtins.list[typing.Any], builtins.list[typing.Any], builtins.float], typing.Any]
typing.Callable[[builtins.dict[typing.Any, typing.Any], builtins.list[typing.Any], builtins.float], typing.Any]
typing.Callable[[builtins.list[typing.Any], builtins.dict[typing.Any, typing.Any], builtins.float], typing.Any]
typing.Callable[[builtins.list[builtins.float], builtins.list[builtins.int], builtins.float], typing.Any]
typing.Callable[[builtins.dict[typing.Any, typing.Any], builtins.dict[typing.Any, typing.Any], builtins.float], typing.Any]
typing.Callable[[ctypes.pointer[typing.Any], builtins.list[typing.Any], builtins.float], typing.Any]
typing.Callable[[builtins.list[typing.Any], ctypes.pointer[typing.Any], builtins.float], typing.Any]
typing.Callable[[array.array[typing.Any], builtins.list[typing.Any], builtins.float], typing.Any]
typing.Callable[[builtins.list[builtins.float], builtins.dict[builtins.int, builtins.int], builtins.float], typing.Any]
typing.Callable[[builtins.dict[builtins.int, builtins.float], builtins.list[builtins.int], builtins.float], typing.Any]
```


### <span style="color:green">OK:</span> `def calc_dynamic_window(x, config)`

```
typing.Callable[[builtins.list[typing.Any], PathPlanning.DynamicWindowApproach.dynamic_window_approach.Config], typing.Any]
typing.Callable[[builtins.list[builtins.int], PathPlanning.DynamicWindowApproach.dynamic_window_approach.Config], typing.Any]
typing.Callable[[builtins.dict[typing.Any, typing.Any], PathPlanning.DynamicWindowApproach.dynamic_window_approach.Config], typing.Any]
typing.Callable[[builtins.list[builtins.bool], PathPlanning.DynamicWindowApproach.dynamic_window_approach.Config], typing.Any]
typing.Callable[[builtins.list[builtins.float], PathPlanning.DynamicWindowApproach.dynamic_window_approach.Config], typing.Any]
typing.Callable[[builtins.dict[builtins.int, builtins.int], PathPlanning.DynamicWindowApproach.dynamic_window_approach.Config], typing.Any]
typing.Callable[[ctypes.pointer[typing.Any], PathPlanning.DynamicWindowApproach.dynamic_window_approach.Config], typing.Any]
typing.Callable[[array.array[typing.Any], PathPlanning.DynamicWindowApproach.dynamic_window_approach.Config], typing.Any]
typing.Callable[[types.MappingProxyType[typing.Any, typing.Any], PathPlanning.DynamicWindowApproach.dynamic_window_approach.Config], typing.Any]
...
```


### <span style="color:green">OK:</span> `def predict_trajectory(x_init, v, y, config)`

```
typing.Callable[[builtins.int, builtins.int, builtins.int, PathPlanning.DynamicWindowApproach.dynamic_window_approach.Config], typing.Any]
typing.Callable[[builtins.list[typing.Any], builtins.int, builtins.int, PathPlanning.DynamicWindowApproach.dynamic_window_approach.Config], typing.Any]
typing.Callable[[builtins.list[builtins.int], builtins.int, builtins.int, PathPlanning.DynamicWindowApproach.dynamic_window_approach.Config], typing.Any]
typing.Callable[[builtins.list[builtins.list[typing.Any]], builtins.int, builtins.int, PathPlanning.DynamicWindowApproach.dynamic_window_approach.Config], typing.Any]
typing.Callable[[builtins.list[builtins.str], builtins.int, builtins.int, PathPlanning.DynamicWindowApproach.dynamic_window_approach.Config], typing.Any]
typing.Callable[[builtins.int, builtins.list[typing.Any], builtins.int, PathPlanning.DynamicWindowApproach.dynamic_window_approach.Config], typing.Any]
typing.Callable[[builtins.int, builtins.list[builtins.int], builtins.int, PathPlanning.DynamicWindowApproach.dynamic_window_approach.Config], typing.Any]
typing.Callable[[builtins.int, builtins.int, builtins.list[typing.Any], PathPlanning.DynamicWindowApproach.dynamic_window_approach.Config], typing.Any]
typing.Callable[[builtins.list[builtins.list[builtins.int]], builtins.int, builtins.int, PathPlanning.DynamicWindowApproach.dynamic_window_approach.Config], typing.Any]
typing.Callable[[builtins.int, builtins.int, builtins.list[builtins.int], PathPlanning.DynamicWindowApproach.dynamic_window_approach.Config], typing.Any]
typing.Callable[[builtins.int, builtins.list[builtins.list[typing.Any]], builtins.int, PathPlanning.DynamicWindowApproach.dynamic_window_approach.Config], typing.Any]
typing.Callable[[builtins.list[builtins.list[builtins.list[typing.Any]]], builtins.int, builtins.int, PathPlanning.DynamicWindowApproach.dynamic_window_approach.Config], typing.Any]
typing.Callable[[builtins.int, builtins.int, builtins.list[builtins.list[typing.Any]], PathPlanning.DynamicWindowApproach.dynamic_window_approach.Config], typing.Any]
...
```

### <span style="color:green">OK:</srpan> `def calc_control_and_trajectory(x, dw, config, goal, ob)`
```
typing.Callable[[builtins.list[typing.Any], builtins.list[typing.Any], PathPlanning.DynamicWindowApproach.dynamic_window_approach.Config, builtins.list[typing.Any], builtins.list[typing.Any]], typing.Any]
typing.Callable[[builtins.list[builtins.int], builtins.list[builtins.list[typing.Any]], PathPlanning.DynamicWindowApproach.dynamic_window_approach.Config, builtins.list[builtins.list[typing.Any]], builtins.list[builtins.list[typing.Any]]], typing.Any]
typing.Callable[[builtins.list[builtins.list[typing.Any]], builtins.list[builtins.int], PathPlanning.DynamicWindowApproach.dynamic_window_approach.Config, builtins.list[builtins.list[typing.Any]], builtins.list[builtins.list[typing.Any]]], typing.Any]
typing.Callable[[builtins.list[builtins.int], builtins.list[builtins.list[builtins.list[typing.Any]]], PathPlanning.DynamicWindowApproach.dynamic_window_approach.Config, builtins.list[builtins.list[builtins.list[typing.Any]]], builtins.list[builtins.list[builtins.list[typing.Any]]]], typing.Any]
typing.Callable[[builtins.list[typing.Any], builtins.str, PathPlanning.DynamicWindowApproach.dynamic_window_approach.Config, builtins.list[typing.Any], builtins.list[typing.Any]], typing.Any]
typing.Callable[[builtins.list[builtins.int], builtins.str, PathPlanning.DynamicWindowApproach.dynamic_window_approach.Config, builtins.list[builtins.list[typing.Any]], builtins.list[builtins.list[typing.Any]]], typing.Any]
typing.Callable[[builtins.list[typing.Any], builtins.list[typing.Any], PathPlanning.DynamicWindowApproach.dynamic_window_approach.Config, builtins.int, builtins.list[typing.Any]], typing.Any]
typing.Callable[[builtins.list[typing.Any], builtins.list[typing.Any], PathPlanning.DynamicWindowApproach.dynamic_window_approach.Config, builtins.list[typing.Any], builtins.int], typing.Any]
...
```

### <span style="color:green">OK:</span> `def calc_obstacle_cost(trajectory, ob, config)`

```
typing.Callable[[builtins.dict[typing.Any, typing.Any], builtins.dict[typing.Any, typing.Any], PathPlanning.DynamicWindowApproach.dynamic_window_approach.Config], typing.Any]
typing.Callable[[numpy.lib.arrayterator.Arrayterator[typing.Any, typing.Any], builtins.dict[typing.Any, typing.Any], PathPlanning.DynamicWindowApproach.dynamic_window_approach.Config], typing.Any]
typing.Callable[[builtins.dict[typing.Any, typing.Any], numpy.lib.arrayterator.Arrayterator[typing.Any, typing.Any], PathPlanning.DynamicWindowApproach.dynamic_window_approach.Config], typing.Any]
typing.Callable[[numpy.lib.arrayterator.Arrayterator[typing.Any, typing.Any], numpy.lib.arrayterator.Arrayterator[typing.Any, typing.Any], PathPlanning.DynamicWindowApproach.dynamic_window_approach.Config], typing.Any]
typing.Callable[[numpy.ndarray[typing.Any, typing.Any], builtins.dict[typing.Any, typing.Any], PathPlanning.DynamicWindowApproach.dynamic_window_approach.Config], typing.Any]
```

### <span style="color:green">OK:</span> `def calc_to_goal_cost(trajectory, goal)`

```
typing.Callable[[builtins.dict[typing.Any, typing.Any], builtins.dict[typing.Any, typing.Any]], typing.Any]
typing.Callable[[builtins.dict[builtins.tuple[typing.Any, ...], builtins.float], builtins.dict[builtins.float, builtins.float]], typing.Any]
typing.Callable[[builtins.dict[builtins.tuple[builtins.float, ...], builtins.float], builtins.dict[builtins.float, builtins.float]], typing.Any]
typing.Callable[[builtins.dict[builtins.tuple[builtins.int, ...], builtins.float], builtins.dict[builtins.float, builtins.float]], typing.Any]
typing.Callable[[builtins.dict[builtins.tuple[typing.Any, ...], builtins.int], builtins.dict[builtins.float, builtins.float]], typing.Any]
typing.Callable[[builtins.dict[builtins.tuple[builtins.float, ...], builtins.int], builtins.dict[builtins.float, builtins.float]], typing.Any]
typing.Callable[[builtins.dict[builtins.tuple[builtins.int, ...], builtins.int], builtins.dict[builtins.float, builtins.float]], typing.Any]
typing.Callable[[builtins.dict[builtins.tuple[typing.Any, ...], builtins.float], builtins.dict[builtins.int, builtins.float]], typing.Any]
typing.Callable[[builtins.dict[builtins.tuple[typing.Any, ...], builtins.float], builtins.dict[builtins.float, builtins.int]], typing.Any]
typing.Callable[[numpy.ma.core.MaskedConstant, builtins.dict[typing.Any, typing.Any]], typing.Any]
typing.Callable[[builtins.dict[typing.Any, typing.Any], builtins.str], typing.Any]
typing.Callable[[numpy.ma.core.MaskedConstant, builtins.dict[builtins.float, builtins.float]], typing.Any]
typing.Callable[[builtins.dict[builtins.tuple[builtins.float, ...], builtins.float], builtins.dict[builtins.float, builtins.int]], typing.Any]
typing.Callable[[builtins.dict[builtins.tuple[builtins.int, ...], builtins.float], builtins.dict[builtins.float, builtins.int]], typing.Any]
...
```

### <span style="color:green">OK:</span> `def plot_robot(x, y, yaw, config)`

`list` is strange here, but without stubs for matplotlib mypy doesn't consider this a mistake.

```
typing.Callable[[builtins.int, builtins.int, builtins.int, PathPlanning.DynamicWindowApproach.dynamic_window_approach.Config], typing.Any]
typing.Callable[[builtins.list[typing.Any], builtins.int, builtins.int, PathPlanning.DynamicWindowApproach.dynamic_window_approach.Config], typing.Any]
typing.Callable[[builtins.int, builtins.list[typing.Any], builtins.int, PathPlanning.DynamicWindowApproach.dynamic_window_approach.Config], typing.Any]
typing.Callable[[builtins.int, builtins.list[builtins.int], builtins.int, PathPlanning.DynamicWindowApproach.dynamic_window_approach.Config], typing.Any]
typing.Callable[[builtins.list[builtins.int], builtins.int, builtins.int, PathPlanning.DynamicWindowApproach.dynamic_window_approach.Config], typing.Any]
typing.Callable[[builtins.list[builtins.list[typing.Any]], builtins.int, builtins.int, PathPlanning.DynamicWindowApproach.dynamic_window_approach.Config], typing.Any]
typing.Callable[[builtins.int, builtins.int, builtins.bool, PathPlanning.DynamicWindowApproach.dynamic_window_approach.Config], typing.Any]
typing.Callable[[builtins.str, builtins.int, builtins.int, PathPlanning.DynamicWindowApproach.dynamic_window_approach.Config], typing.Any]
...
```

### <span style="color:yellow">EXTRA:</span> `def dwa_control(x, config, goal, ob)`

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

## File 2

Source: https://github.com/AtsushiSakai/PythonRobotics/blob/master/PathPlanning/DubinsPath/dubins_path_planner.py

Top-level functions (with only positional arguments):
- [ ] `def _mod2pi(theta)`
- [x] `def _calc_trig_funcs(alpha, beta)`
- [x] `def _LSL(alpha, beta, d)`
- [x] `def _RSR(alpha, beta, d)`
- [ ] `def _LSR(alpha, beta, d)`
- [x] `def _RSL(alpha, beta, d)`
- [x] `def _RLR(alpha, beta, d)`
- [ ] `def _LRL(alpha, beta, d)`
- [x] `def _dubins_path_planning_from_origin(end_x, end_y, end_yaw, curvature, step_size, planning_funcs)`
- [x] `def _interpolate(length, mode, max_curvature, origin_x, origin_y, origin_yaw, path_x, path_y, path_yaw)`
- [x] `def _generate_local_course(lengths, modes, max_curvature, step_size)`

Used time limit: 25 seconds.

Command:

    java -jar utbot-cli-python-2023.02-SNAPSHOT.jar infer_types -p python3 -s /home/tochilinak/Documents/projects/utbot/PythonRobotics -t 25000 "/home/tochilinak/Documents/projects/utbot/PythonRobotics/PathPlanning/DubinsPath/dubins_path_planner.py" <function>

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

### <span style="color:green">OK:</span> `def _LSL(alpha, beta, d)`

```
typing.Callable[[builtins.float, builtins.float, builtins.int], typing.Any]
typing.Callable[[builtins.bool, builtins.float, builtins.int], typing.Any]
typing.Callable[[enum.IntEnum, builtins.float, builtins.int], typing.Any]
typing.Callable[[enum.auto, builtins.float, builtins.int], typing.Any]
typing.Callable[[enum.IntFlag, builtins.float, builtins.int], typing.Any]
typing.Callable[[_pytest.config.ExitCode, builtins.float, builtins.int], typing.Any]
typing.Callable[[signal.Signals, builtins.float, builtins.int], typing.Any]
typing.Callable[[signal.Handlers, builtins.float, builtins.int], typing.Any]
typing.Callable[[signal.Sigmasks, builtins.float, builtins.int], typing.Any]
typing.Callable[[builtins.complex, builtins.float, builtins.int], typing.Any]
```


### <span style="color:green">OK:</span> `def _RSR(alpha, beta, d)`

```
typing.Callable[[builtins.float, builtins.float, builtins.int], typing.Any]
typing.Callable[[builtins.float, builtins.int, builtins.int], typing.Any]
typing.Callable[[builtins.float, builtins.str, builtins.int], typing.Any]
typing.Callable[[builtins.float, builtins.bool, builtins.int], typing.Any]
typing.Callable[[builtins.float, enum.IntEnum, builtins.int], typing.Any]
typing.Callable[[builtins.float, enum.auto, builtins.int], typing.Any]
typing.Callable[[builtins.float, enum.IntFlag, builtins.int], typing.Any]
typing.Callable[[builtins.float, _pytest.config.ExitCode, builtins.int], typing.Any]
typing.Callable[[builtins.float, signal.Signals, builtins.int], typing.Any]
typing.Callable[[builtins.float, signal.Handlers, builtins.int], typing.Any]
typing.Callable[[builtins.float, signal.Sigmasks, builtins.int], typing.Any]
typing.Callable[[builtins.float, builtins.complex, builtins.int], typing.Any]
```

### <span style="color:green">OK:</span> `def _RSL(alpha, beta, d)`

```
typing.Callable[[builtins.float, builtins.float, builtins.int], typing.Any]
typing.Callable[[builtins.float, builtins.float, builtins.bool], typing.Any]
typing.Callable[[builtins.float, builtins.float, enum.IntEnum], typing.Any]
typing.Callable[[builtins.float, builtins.float, enum.auto], typing.Any]
typing.Callable[[builtins.float, builtins.float, enum.IntFlag], typing.Any]
typing.Callable[[builtins.float, builtins.float, _pytest.config.ExitCode], typing.Any]
typing.Callable[[builtins.float, builtins.float, signal.Signals], typing.Any]
typing.Callable[[builtins.float, builtins.float, signal.Handlers], typing.Any]
typing.Callable[[builtins.float, builtins.float, signal.Sigmasks], typing.Any]
typing.Callable[[builtins.float, builtins.float, builtins.float], typing.Any]
typing.Callable[[builtins.float, builtins.float, _decimal.Decimal], typing.Any]
typing.Callable[[builtins.float, builtins.float, builtins.complex], typing.Any]
```

### <span style="color:green">OK:</span> `def _RLR(alpha, beta, d)`

```
typing.Callable[[builtins.float, builtins.float, builtins.float], typing.Any]
typing.Callable[[builtins.bool, builtins.float, builtins.float], typing.Any]
typing.Callable[[builtins.float, builtins.bool, builtins.float], typing.Any]
typing.Callable[[builtins.int, builtins.float, builtins.float], typing.Any]
typing.Callable[[builtins.float, builtins.int, builtins.float], typing.Any]
typing.Callable[[enum.IntEnum, builtins.float, builtins.float], typing.Any]
typing.Callable[[builtins.float, enum.IntEnum, builtins.float], typing.Any]
typing.Callable[[enum.auto, builtins.float, builtins.float], typing.Any]
typing.Callable[[builtins.float, enum.auto, builtins.float], typing.Any]
typing.Callable[[enum.IntFlag, builtins.float, builtins.float], typing.Any]
typing.Callable[[builtins.float, enum.IntFlag, builtins.float], typing.Any]
...
```

### <span style="color:green">OK:</span> `def _dubins_path_planning_from_origin(end_x, end_y, end_yaw, curvature, step_size, planning_funcs)`

```
typing.Callable[[builtins.int, builtins.int, builtins.int, builtins.float, builtins.int, builtins.list[typing.Any]], typing.Any]
typing.Callable[[builtins.bool, builtins.int, builtins.int, builtins.float, builtins.int, builtins.list[typing.Any]], typing.Any]
typing.Callable[[builtins.int, builtins.bool, builtins.int, builtins.float, builtins.int, builtins.list[typing.Any]], typing.Any]
typing.Callable[[builtins.int, builtins.int, builtins.bool, builtins.float, builtins.int, builtins.list[typing.Any]], typing.Any]
typing.Callable[[builtins.int, builtins.int, builtins.int, builtins.float, builtins.list[typing.Any], builtins.list[typing.Any]], typing.Any]
typing.Callable[[builtins.int, builtins.int, builtins.int, builtins.float, builtins.int, builtins.list[builtins.staticmethod[typing.Any]]], typing.Any]
typing.Callable[[builtins.int, builtins.int, builtins.int, builtins.float, builtins.int, builtins.list[builtins.staticmethod[builtins.list[typing.Any]]]], typing.Any]
typing.Callable[[builtins.int, builtins.bool, builtins.int, builtins.float, builtins.int, builtins.list[builtins.staticmethod[typing.Any]]], typing.Any]
typing.Callable[[builtins.float, builtins.int, builtins.int, builtins.float, builtins.int, builtins.list[typing.Any]], typing.Any]
typing.Callable[[builtins.bool, builtins.bool, builtins.int, builtins.float, builtins.int, builtins.list[typing.Any]], typing.Any]
typing.Callable[[builtins.int, builtins.bool, builtins.int, builtins.float, builtins.int, builtins.list[builtins.staticmethod[builtins.list[typing.Any]]]], typing.Any]
typing.Callable[[builtins.bool, builtins.int, builtins.int, builtins.float, builtins.int, builtins.list[builtins.staticmethod[typing.Any]]], typing.Any]
typing.Callable[[builtins.int, builtins.int, builtins.int, builtins.float, builtins.int, builtins.list[builtins.staticmethod[builtins.list[builtins.float]]]], typing.Any]
...
```


### <span style="color:green">OK:</span> `def _interpolate(length, mode, max_curvature, origin_x, origin_y, origin_yaw, path_x, path_y, path_yaw)`

```
typing.Callable[[builtins.float, builtins.str, builtins.float, builtins.float, builtins.float, builtins.float, builtins.list[typing.Any], builtins.list[typing.Any], builtins.list[typing.Any]], typing.Any]
typing.Callable[[builtins.float, builtins.str, builtins.float, builtins.float, builtins.float, builtins.float, builtins.list[builtins.float], builtins.list[builtins.float], builtins.list[builtins.float]], typing.Any]
typing.Callable[[builtins.float, builtins.str, builtins.float, builtins.float, builtins.float, builtins.float, builtins.list[builtins.object], builtins.list[builtins.float], builtins.list[builtins.float]], typing.Any]
typing.Callable[[builtins.float, builtins.str, builtins.float, builtins.float, builtins.float, builtins.float, array.array[typing.Any], builtins.list[typing.Any], builtins.list[typing.Any]], typing.Any]
typing.Callable[[builtins.float, builtins.str, builtins.float, builtins.float, builtins.float, builtins.float, array.array[builtins.float], builtins.list[builtins.float], builtins.list[builtins.float]], typing.Any]
```

### <span style="color:green">OK:</span> `def _generate_local_course(lengths, modes, max_curvature, step_size)`

```
typing.Callable[[builtins.list[typing.Any], builtins.list[typing.Any], builtins.int, builtins.int], typing.Any]
typing.Callable[[builtins.list[builtins.int], builtins.list[builtins.int], builtins.int, builtins.int], typing.Any]
typing.Callable[[builtins.list[typing.Any], builtins.list[typing.Any], builtins.list[typing.Any], builtins.int], typing.Any]
typing.Callable[[builtins.list[typing.Any], builtins.list[typing.Any], builtins.int, builtins.bool], typing.Any]
typing.Callable[[builtins.dict[typing.Any, typing.Any], builtins.list[typing.Any], builtins.int, builtins.int], typing.Any]
typing.Callable[[builtins.dict[builtins.int, builtins.int], builtins.list[builtins.int], builtins.int, builtins.int], typing.Any]
typing.Callable[[builtins.list[builtins.int], builtins.list[builtins.int], builtins.int, builtins.bool], typing.Any]
typing.Callable[[builtins.list[builtins.int], builtins.list[builtins.list[typing.Any]], builtins.int, builtins.int], typing.Any]
typing.Callable[[builtins.list[builtins.int], builtins.list[builtins.list[builtins.int]], builtins.int, builtins.int], typing.Any]
...
```


### <span style="color:yellow">EXTRA:</span> `def _LSR(alpha, beta, d)`

```
typing.Callable[[builtins.float, builtins.object, builtins.int], typing.Any]
typing.Callable[[builtins.object, builtins.float, builtins.int], typing.Any]
typing.Callable[[builtins.float, builtins.float, builtins.int], typing.Any]
typing.Callable[[builtins.bool, builtins.object, builtins.int], typing.Any]
typing.Callable[[builtins.int, builtins.float, builtins.int], typing.Any]
typing.Callable[[builtins.float, builtins.int, builtins.int], typing.Any]
typing.Callable[[enum.IntEnum, builtins.object, builtins.int], typing.Any]
typing.Callable[[builtins.bool, builtins.float, builtins.int], typing.Any]
typing.Callable[[builtins.int, builtins.str, builtins.int], typing.Any]
typing.Callable[[builtins.str, builtins.int, builtins.int], typing.Any]
typing.Callable[[builtins.float, builtins.bool, builtins.int], typing.Any]
typing.Callable[[enum.auto, builtins.object, builtins.int], typing.Any]
typing.Callable[[enum.IntEnum, builtins.float, builtins.int], typing.Any]
```

### <span style="color:yellow">EXTRA:</span> `def _LRL(alpha, beta, d)`

```
typing.Callable[[builtins.float, builtins.object, builtins.float], typing.Any]
typing.Callable[[builtins.bool, builtins.object, builtins.float], typing.Any]
typing.Callable[[builtins.float, builtins.float, builtins.float], typing.Any]
typing.Callable[[builtins.int, builtins.object, builtins.float], typing.Any]
typing.Callable[[builtins.bool, builtins.float, builtins.float], typing.Any]
typing.Callable[[builtins.float, builtins.str, builtins.float], typing.Any]
typing.Callable[[enum.IntEnum, builtins.object, builtins.float], typing.Any]
typing.Callable[[builtins.int, builtins.float, builtins.float], typing.Any]
typing.Callable[[builtins.bool, builtins.str, builtins.float], typing.Any]
typing.Callable[[builtins.float, builtins.bool, builtins.float], typing.Any]
typing.Callable[[enum.auto, builtins.object, builtins.float], typing.Any]
typing.Callable[[enum.IntEnum, builtins.float, builtins.float], typing.Any]
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


## File 3

Source: https://github.com/AtsushiSakai/PythonRobotics/blob/master/utils/angle.py

Top-level functions:
- [ ] `def rot_mat_2d(angle)`
- [ ] `def angle_mod(x, zero_2_2pi, degree)`

### <span style="color:yellow">EXTRA:</span> `def rot_mat_2d(angle)`

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

## File 4

!!!!!!!!!!!! https://github.com/AllAlgorithms/python/blob/master/algorithms/dynamic-programming/kadanes_algorithm.py
