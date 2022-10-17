## Библиотека loguru

### \_colorama.py

#### PyCharm

```python
def should_colorize(stream: PyUnionType: {isatty} | None): pass
```

#### UnitTestBot

```python
def should_colorize(stream: types.NoneType): pass
def should_colorize(stream: builtins.bool): pass
def should_colorize(stream: builtins.float): pass
def should_colorize(stream: builtins.complex): pass
def should_colorize(stream: builtins.bytearray): pass
def should_colorize(stream: typing.Dict[typing.Any, typing.Any]): pass
def should_colorize(stream: builtins.frozenset): pass
def should_colorize(stream: typing.List[typing.Any]): pass
def should_colorize(stream: typing.Dict[typing.List[typing.Any], builtins.str]): pass
def should_colorize(stream: typing.List[typing.List[typing.Any]]): pass
def should_colorize(stream: builtins.object): pass
def should_colorize(stream: typing.Dict[typing.List[typing.Any], typing.List[typing.Any]]): pass
```

### \_datetime.py

#### PyCharm

```python
def aware_now(): pass

def __format__(self: Any, spec: PyStructuralType(endswith, __contains__)): pass
```

#### UnitTestBot

```python
def aware_now(): pass

def __format__(self: loguru._datetime.datetime, spec: builtins.str): pass
def __format__(self: loguru._datetime.datetime, spec: builtins.int): pass
```

### \_filter.py

#### PyCharm

```python
def filter_none(record: PyStructuralType(__getitem__)): pass
```

#### UnitTestBot

```python
def filter_none(record: builtins.str): pass
def filter_none(record: builtins.bytes): pass
def filter_none(record: typing.Dict[typing.Any, typing.Any]): pass
def filter_none(record: typing.Dict[builtins.str, builtins.str]): pass
def filter_none(record: typing.Dict[builtins.str, builtins.int]): pass
def filter_none(record: typing.List[builtins.str]): pass
def filter_none(record: builtins.memoryview): pass
def filter_none(record: typing.Dict[builtins.str, builtins.bool]): pass
def filter_none(record: typing.List[builtins.bool]): pass
def filter_none(record: typing.Set[typing.Any]): pass
```

### \_string_parsers.py

#### PyCharm

```python
def parse_size(size: Any): pass
```

#### UnitTestBot

```python
def parse_size(size: builtins.int): pass
def parse_size(size: builtins.str): pass
def parse_size(size: builtins.range): pass
def parse_size(size: builtins.BaseException): pass
def parse_size(size: builtins.bytes): pass
def parse_size(size: typing.Dict[builtins.int, builtins.int]): pass
def parse_size(size: typing.Dict[builtins.int, builtins.str]): pass
def parse_size(size: typing.List[builtins.int]): pass
def parse_size(size: builtins.memoryview): pass
def parse_size(size: typing.Dict[builtins.int, typing.Dict[typing.Any, typing.Any]]): pass
def parse_size(size: typing.List[typing.Dict[typing.Any, typing.Any]]): pass
def parse_size(size: typing.Set[typing.Any]): pass
```

## Проект django-cms

### cms/api.py

#### PyCharm

```python
def _verify_apphook(apphook: Any, namespace: Any): pass
```

#### UnitTestBot

```python
def _verify_apphook(apphook: builtins.type, namespace: builtins.int): pass
def _verify_apphook(apphook: builtins.type, namespace: builtins.bool): pass
def _verify_apphook(apphook: build.lib.cms.plugin_base.CMSPluginBase, namespace: builtins.int): pass
def _verify_apphook(apphook: builtins.type, namespace: builtins.str): pass
def _verify_apphook(apphook: build.lib.cms.plugin_base.CMSPluginBase, namespace: builtins.bool): pass
def _verify_apphook(apphook: build.lib.cms.models.pluginmodel.CMSPlugin, namespace: builtins.int): pass
def _verify_apphook(apphook: builtins.type, namespace: builtins.float): pass
def _verify_apphook(apphook: build.lib.cms.plugin_base.CMSPluginBase, namespace: builtins.str): pass
def _verify_apphook(apphook: build.lib.cms.models.pluginmodel.CMSPlugin, namespace: builtins.bool): pass
def _verify_apphook(apphook: builtins.type, namespace: builtins.range): pass
def _verify_apphook(apphook: build.lib.cms.plugin_base.CMSPluginBase, namespace: builtins.float): pass
def _verify_apphook(apphook: build.lib.cms.models.pluginmodel.CMSPlugin, namespace: builtins.str): pass
def _verify_apphook(apphook: builtins.type, namespace: builtins.complex): pass
def _verify_apphook(apphook: build.lib.cms.plugin_base.CMSPluginBase, namespace: builtins.range): pass
def _verify_apphook(apphook: build.lib.cms.models.pluginmodel.CMSPlugin, namespace: builtins.float): pass
def _verify_apphook(apphook: cms.plugin_base.CMSPluginBase, namespace: builtins.int): pass
def _verify_apphook(apphook: builtins.type, namespace: builtins.BaseException): pass
def _verify_apphook(apphook: build.lib.cms.plugin_base.CMSPluginBase, namespace: builtins.complex): pass
def _verify_apphook(apphook: build.lib.cms.models.pluginmodel.CMSPlugin, namespace: builtins.range): pass
def _verify_apphook(apphook: cms.plugin_base.CMSPluginBase, namespace: builtins.bool): pass
def _verify_apphook(apphook: cms.models.pluginmodel.CMSPlugin, namespace: builtins.int): pass
...
```

### cms/toolbar/items.py

#### PyCharm

```python
def may_be_lazy(thing: Any): pass

def find_first(self: PyStructuralType(find_items), item_type: Any, attributes: PyClassType: dict): pass
```

#### UnitTestBot

```python
def may_be_lazy(thing: builtins.int): pass
def may_be_lazy(thing: builtins.bool): pass
def may_be_lazy(thing: builtins.str): pass
def may_be_lazy(thing: builtins.float): pass
def may_be_lazy(thing: builtins.range): pass
def may_be_lazy(thing: builtins.complex): pass
def may_be_lazy(thing: builtins.BaseException): pass
def may_be_lazy(thing: builtins.bytearray): pass
def may_be_lazy(thing: builtins.bytes): pass
...

def find_first(self: builtins.int, item_type: builtins.int): pass
def find_first(self: builtins.int, item_type: builtins.bool): pass
def find_first(self: builtins.bool, item_type: builtins.int): pass
def find_first(self: builtins.int, item_type: builtins.str): pass
def find_first(self: builtins.bool, item_type: builtins.bool): pass
def find_first(self: builtins.str, item_type: builtins.int): pass
def find_first(self: builtins.int, item_type: builtins.float): pass
def find_first(self: builtins.bool, item_type: builtins.str): pass
def find_first(self: builtins.str, item_type: builtins.bool): pass
def find_first(self: builtins.float, item_type: builtins.int): pass
def find_first(self: builtins.int, item_type: builtins.range): pass
def find_first(self: builtins.bool, item_type: builtins.float): pass
...
```

### cms/view.py

#### PyCharm

```python
def _clean_redirect_url(redirect_url: PyStructuralType(__getitem__, startswith), language: Any): pass
```

#### UnitTestBot

```python
def _clean_redirect_url(redirect_url: builtins.str, language: builtins.str): pass
def _clean_redirect_url(redirect_url: builtins.str, language: builtins.object): pass
def _clean_redirect_url(redirect_url: builtins.str, language: builtins.int): pass
def _clean_redirect_url(redirect_url: builtins.str, language: builtins.bytearray): pass
def _clean_redirect_url(redirect_url: builtins.str, language: datetime.timedelta): pass
def _clean_redirect_url(redirect_url: builtins.str, language: collections.UserString): pass
def _clean_redirect_url(redirect_url: builtins.str, language: builtins.bool): pass
def _clean_redirect_url(redirect_url: builtins.str, language: builtins.range): pass
def _clean_redirect_url(redirect_url: builtins.str, language: builtins.complex): pass
def _clean_redirect_url(redirect_url: builtins.str, language: builtins.BaseException): pass
def _clean_redirect_url(redirect_url: builtins.str, language: typing.Dict[typing.Any, typing.Any]): pass
def _clean_redirect_url(redirect_url: builtins.str, language: typing.Dict[builtins.str, builtins.str]): pass
def _clean_redirect_url(redirect_url: builtins.str, language: builtins.frozenset): pass
def _clean_redirect_url(redirect_url: builtins.str, language: typing.Dict[builtins.str, builtins.int]): pass
def _clean_redirect_url(redirect_url: builtins.str, language: typing.List[typing.Any]): pass
def _clean_redirect_url(redirect_url: builtins.str, language: typing.List[builtins.str]): pass
def _clean_redirect_url(redirect_url: builtins.str, language: typing.Dict[builtins.int, builtins.str]): pass
```

## Библиотека алгоритмов TheAlgorithms/Python

### sorts/bogo_sort.py

#### PyCharm

```python
def bogo_sort(collection: PyStructuralType(__len__, __getitem__)): pass
```

#### UnitTestBot

```python
def bogo_sort(collection: builtins.bytearray): pass
def bogo_sort(collection: typing.List[typing.Any]): pass
def bogo_sort(collection: typing.List[builtins.int]): pass
def bogo_sort(collection: typing.List[builtins.str]): pass
def bogo_sort(collection: typing.List[builtins.bool]): pass
def bogo_sort(collection: typing.List[builtins.float]): pass
def bogo_sort(collection: typing.List[builtins.range]): pass
def bogo_sort(collection: typing.List[builtins.complex]): pass
def bogo_sort(collection: typing.List[builtins.BaseException]): pass
def bogo_sort(collection: typing.List[builtins.bytearray]): pass
def bogo_sort(collection: typing.List[builtins.bytes]): pass
def bogo_sort(collection: typing.List[typing.Dict[typing.Any, typing.Any]]): pass
def bogo_sort(collection: typing.List[builtins.frozenset]): pass
def bogo_sort(collection: typing.List[typing.List[typing.Any]]): pass
def bogo_sort(collection: typing.List[builtins.memoryview]): pass
def bogo_sort(collection: typing.List[builtins.object]): pass
def bogo_sort(collection: typing.List[builtins.tuple]): pass
def bogo_sort(collection: typing.List[builtins.type]): pass
```

### sorts/bubble_sort.py

#### PyCharm

```python
def bubble_sort(collection: PyStructuralType(__len__, __getitem__)): pass
```

#### UnitTestBot

```python
def bubble_sort(collection: builtins.bytearray): pass
def bubble_sort(collection: typing.Dict[typing.Any, typing.Any]): pass
def bubble_sort(collection: typing.Dict[builtins.int, builtins.int]): pass
def bubble_sort(collection: typing.Dict[builtins.int, builtins.str]): pass
def bubble_sort(collection: typing.List[typing.Any]): pass
def bubble_sort(collection: typing.List[builtins.int]): pass
def bubble_sort(collection: builtins.memoryview): pass
def bubble_sort(collection: typing.List[builtins.str]): pass
def bubble_sort(collection: typing.Dict[builtins.int, builtins.bool]): pass
def bubble_sort(collection: typing.List[builtins.bool]): pass
def bubble_sort(collection: typing.List[builtins.float]): pass
def bubble_sort(collection: typing.Dict[builtins.int, builtins.float]): pass
def bubble_sort(collection: typing.List[builtins.bytearray]): pass
def bubble_sort(collection: typing.Dict[builtins.float, builtins.int]): pass
def bubble_sort(collection: typing.List[builtins.bytes]): pass
def bubble_sort(collection: typing.List[builtins.frozenset]): pass
def bubble_sort(collection: typing.List[typing.List[typing.Any]]): pass
def bubble_sort(collection: typing.Dict[builtins.float, builtins.str]): pass
def bubble_sort(collection: typing.List[builtins.tuple]): pass
def bubble_sort(collection: typing.Dict[builtins.float, builtins.bool]): pass
def bubble_sort(collection: typing.Dict[builtins.complex, builtins.int]): pass
def bubble_sort(collection: typing.Dict[builtins.float, builtins.float]): pass
def bubble_sort(collection: typing.Dict[builtins.complex, builtins.str]): pass
def bubble_sort(collection: typing.Dict[builtins.int, builtins.bytearray]): pass
def bubble_sort(collection: typing.Dict[builtins.complex, builtins.bool]): pass
def bubble_sort(collection: typing.Dict[builtins.int, builtins.bytes]): pass
def bubble_sort(collection: typing.Dict[builtins.complex, builtins.float]): pass
```

### dynamic_programming/fractional_knapsack.py

#### PyCharm

```python
def frac_knapsack(vl: Any, wt: Any, w: PyStructuralType(__sub__), n: Any): pass
```

#### UnitTestBot

```python
def frac_knapsack(vl: data_structures.linked_list.singly_linked_list.LinkedList, wt: data_structures.linked_list.singly_linked_list.LinkedList, w: linear_algebra.src.lib.Vector, n: divide_and_conquer.convex_hull.Point): pass
def frac_knapsack(vl: data_structures.linked_list.singly_linked_list.LinkedList, wt: data_structures.linked_list.singly_linked_list.LinkedList, w: matrix.sherman_morrison.Matrix, n: divide_and_conquer.convex_hull.Point): pass
def frac_knapsack(vl: data_structures.linked_list.singly_linked_list.LinkedList, wt: data_structures.linked_list.singly_linked_list.LinkedList, w: matrix.sherman_morrison.Matrix, n: matrix.matrix_class.Matrix): pass
def frac_knapsack(vl: data_structures.linked_list.singly_linked_list.LinkedList, wt: data_structures.heap.min_heap.MinHeap, w: linear_algebra.src.lib.Vector, n: builtins.int): pass
def frac_knapsack(vl: data_structures.linked_list.singly_linked_list.LinkedList, wt: data_structures.heap.min_heap.MinHeap, w: matrix.sherman_morrison.Matrix, n: divide_and_conquer.convex_hull.Point): pass
def frac_knapsack(vl: data_structures.heap.min_heap.MinHeap, wt: data_structures.linked_list.singly_linked_list.LinkedList, w: matrix.sherman_morrison.Matrix, n: divide_and_conquer.convex_hull.Point): pass
def frac_knapsack(vl: data_structures.linked_list.singly_linked_list.LinkedList, wt: data_structures.linked_list.singly_linked_list.LinkedList, w: linear_algebra.src.lib.Vector, n: builtins.str): pass
def frac_knapsack(vl: data_structures.linked_list.singly_linked_list.LinkedList, wt: data_structures.linked_list.singly_linked_list.LinkedList, w: matrix.sherman_morrison.Matrix, n: builtins.int): pass
def frac_knapsack(vl: data_structures.linked_list.singly_linked_list.LinkedList, wt: data_structures.heap.min_heap.MinHeap, w: linear_algebra.src.lib.Vector, n: builtins.bool): pass
def frac_knapsack(vl: data_structures.linked_list.singly_linked_list.LinkedList, wt: data_structures.heap.min_heap.MinHeap, w: matrix.sherman_morrison.Matrix, n: matrix.matrix_class.Matrix): pass
def frac_knapsack(vl: data_structures.linked_list.singly_linked_list.LinkedList, wt: matrix.sherman_morrison.Matrix, w: linear_algebra.src.lib.Vector, n: builtins.int): pass
def frac_knapsack(vl: data_structures.linked_list.singly_linked_list.LinkedList, wt: matrix.sherman_morrison.Matrix, w: matrix.sherman_morrison.Matrix, n: divide_and_conquer.convex_hull.Point): pass
...
```

### hashes/md5.py

#### PyCharm

```python
def frac_knapsack(vl: Any, wt: Any, w: PyStructuralType(__sub__), n: Any): pass
```

#### UnitTestBot

```python
```

### hashes/md5.py

#### PyCharm

```python
def rearrange(bit_string_32: PyStructuralType(__len__)): pass

def reformat_hex(i: Any): pass
```

#### UnitTestBot

```python
def rearrange(bit_string_32: linear_algebra.src.lib.Vector): pass
def rearrange(bit_string_32: data_structures.linked_list.__init__.LinkedList): pass
def rearrange(bit_string_32: graphs.minimum_spanning_tree_prims2.MinPriorityQueue): pass
def rearrange(bit_string_32: builtins.str): pass
def rearrange(bit_string_32: typing.Dict[typing.Any, typing.Any]): pass
def rearrange(bit_string_32: typing.Dict[data_structures.linked_list.doubly_linked_list.DoublyLinkedList, builtins.int]): pass
def rearrange(bit_string_32: typing.List[data_structures.linked_list.doubly_linked_list_two.LinkedList]): pass

def reformat_hex(i: builtins.int): pass
def reformat_hex(i: linear_algebra.src.lib.Vector): pass
def reformat_hex(i: matrix.sherman_morrison.Matrix): pass
def reformat_hex(i: builtins.float): pass
def reformat_hex(i: builtins.range): pass
def reformat_hex(i: builtins.complex): pass
def reformat_hex(i: builtins.object): pass
def reformat_hex(i: typing.Dict[data_structures.linked_list.doubly_linked_list.DoublyLinkedList, builtins.int]): pass
def reformat_hex(i: typing.List[data_structures.linked_list.doubly_linked_list_two.LinkedList]): pass
```

## Наши тесты

### graph.py

```python
from __future__ import annotations
from collections import deque
from typing import List


class Node:
    def __init__(self, name: str, children: List[Node]):
        self.name = name
        self.children = children

    def __repr__(self):
        return f'<Node: {self.name}>'

    def __eq__(self, other):
        if isinstance(other, Node):
            return self.name == other.name
        else:
            return False


def bfs(nodes):
    if len(nodes) == 0:
        return []

    visited = []
    queue = deque(nodes)
    while len(queue) > 0:
        node = queue.pop()
        if node not in visited:
            visited.append(node)
            for child in node.children:
                queue.append(child)
    return visited
```

#### PyCharm

```python
def bfs(nodes: PyStructuralType(__len__)): pass
```

#### UnitTestBot

```python
def bfs(nodes: typing.Dict[typing.Any, typing.Any]): pass
def bfs(nodes: builtins.frozenset): pass
def bfs(nodes: typing.List[typing.Any]): pass
def bfs(nodes: typing.Set[typing.Any]): pass
def bfs(nodes: builtins.tuple): pass
```

### list_of_datatime.py

#### PyCharm

```python
def get_data_labels(dates: Any): pass
```

#### UnitTestBot

```python
def get_data_labels(dates: typing.Dict[typing.Any, typing.Any]): pass
def get_data_labels(dates: typing.List[typing.Any]): pass
def get_data_labels(dates: typing.Set[typing.Any]): pass
def get_data_labels(dates: typing.List[datetime.time]): pass
def get_data_labels(dates: typing.Set[datetime.time]): pass
def get_data_labels(dates: typing.List[datetime.datetime]): pass
def get_data_labels(dates: typing.Set[datetime.datetime]): pass
def get_data_labels(dates: typing.Dict[datetime.time, builtins.str]): pass
def get_data_labels(dates: typing.List[typing.Any[builtins.object]]): pass
def get_data_labels(dates: builtins.frozenset): pass
def get_data_labels(dates: typing.Set[typing.Any[builtins.object]]): pass
...
```

### test_coverage.py

```python
def hard_function(x):
    if x % 100 == 0:
        return 1
    elif x + 100 < 400:
        return 2
    else:
        if x == complex(1, 2):
            return x
        elif len(str(x)) > 3:
            return 3
        else:
            return 4
```

#### PyCharm

```python
def hard_function(x: PyStructuralType(__mod__, __add__)): pass
```

#### UnitTestBot

```python
def hard_function(x: builtins.int): pass
def hard_function(x: builtins.bool): pass
def hard_function(x: builtins.float): pass
def hard_function(x: builtins.range): pass
```

### type_inference.py

```python
def type_inference(number, string, string_sep, list_of_number, dict_str_to_list):
    new_string = '_' + string + '_' * number
    new_string = new_string.capitalize() + string_sep + new_string[::-1]

    if len(list_of_number) < len(new_string):
        list_of_number += [0] * (len(new_string) - len(list_of_number))

    dict_str_to_list[string] = []
    for key in dict_str_to_list.keys():
        list_of_number.append(key)

    return list_of_number
```

#### PyCharm

```python
def type_inference(number: Any, string: Any, string_sep: Any, list_of_number: PyStructuralType(__len__, append), dict_str_to_list: PyStructuralType(__setitem__, keys)): pass
```

#### UnitTestBot

```python
def type_inference(number: builtins.int, string: builtins.str, string_sep: builtins.str, list_of_number: typing.List[typing.Any], dict_str_to_list: typing.Dict[typing.Any, typing.Any]): pass
def type_inference(number: builtins.int, string: builtins.str, string_sep: builtins.str, list_of_number: typing.List[builtins.int], dict_str_to_list: typing.Dict[typing.Any, typing.Any]): pass
```

### type_inference_2.py

```python
def g(x):

    def f(y):
        if y in [0, 100, 200, 500]:
            return y // 100

            if f(x) > 10:
               return x ** 2
```

#### PyCharm

```
def g(x: Any): pass
```

#### UnitTestBot

```python
def g(x: builtins.int): pass
def g(x: builtins.float): pass
def g(x: builtins.complex): pass
def g(x: builtins.bool): pass
```
