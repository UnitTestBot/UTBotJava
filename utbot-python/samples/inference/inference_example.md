## Библиотека loguru

### \_colorama.py

#### Old
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
#### New
```
typing.Callable[[io.TextIOWrapper], typing.Any]
typing.Callable[[io.StringIO], typing.Any]
typing.Callable[[codecs.StreamReaderWriter], typing.Any]
typing.Callable[[io.IOBase], typing.Any]
typing.Callable[[io.TextIOBase], typing.Any]
typing.Callable[[io.FileIO], typing.Any]
typing.Callable[[io.BytesIO], typing.Any]
typing.Callable[[io.BufferedReader], typing.Any]
typing.Callable[[io.BufferedWriter], typing.Any]
typing.Callable[[io.BufferedRandom], typing.Any]
typing.Callable[[codecs.StreamRecoder], typing.Any]
typing.Callable[[io.RawIOBase], typing.Any]
typing.Callable[[io.BufferedIOBase], typing.Any]
typing.Callable[[io.BufferedRWPair], typing.Any]
typing.Callable[[types.SimpleNamespace], typing.Any]
typing.Callable[[types.ModuleType], typing.Any]
typing.Callable[[types.MethodType], typing.Any]
typing.Callable[[types.GenericAlias], typing.Any]
```

### \_datetime.py

TODO

### \_filters.py

#### Old

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

#### New
```
typing.Callable[[builtins.dict[typing.Any, typing.Any]], typing.Any]
typing.Callable[[builtins.dict[builtins.str, builtins.int]], typing.Any]
typing.Callable[[builtins.dict[builtins.str, builtins.list[typing.Any]]], typing.Any]
typing.Callable[[ctypes.CDLL], typing.Any]
typing.Callable[[ctypes.PyDLL], typing.Any]
typing.Callable[[builtins.dict[builtins.str, builtins.list[builtins.int]]], typing.Any]
typing.Callable[[builtins.dict[builtins.str, builtins.list[builtins.list[typing.Any]]]], typing.Any]
typing.Callable[[builtins.dict[builtins.str, builtins.list[builtins.str]]], typing.Any]
typing.Callable[[email.message.Message], typing.Any]
typing.Callable[[builtins.dict[builtins.str, builtins.list[builtins.list[builtins.int]]]], typing.Any]
typing.Callable[[email.message.EmailMessage], typing.Any]
typing.Callable[[builtins.dict[builtins.str, builtins.str]], typing.Any]
typing.Callable[[builtins.dict[builtins.str, builtins.list[builtins.list[builtins.list[typing.Any]]]]], typing.Any]
typing.Callable[[types.MappingProxyType[typing.Any, typing.Any]], typing.Any]
typing.Callable[[builtins.dict[builtins.str, builtins.list[builtins.list[builtins.list[builtins.int]]]]], typing.Any]
...
```

### \_string_parsers.py

Actually, the only correct variant is str. The code is just messy.

#### Old

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

#### New

```
typing.Callable[[builtins.str], typing.Any]
typing.Callable[[builtins.int], typing.Any]
```

## Проект django-cms

### cms/api.py

#### Old


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

#### New

```
typing.Callable[[builtins.str, builtins.int], typing.Any]
```

### cms/toolbar/items.py

#### Old

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
```

#### New

```
typing.Callable[[builtins.int], typing.Any]
typing.Callable[[builtins.list[typing.Any]], typing.Any]
typing.Callable[[builtins.list[builtins.int]], typing.Any]
typing.Callable[[builtins.str], typing.Any]
typing.Callable[[builtins.list[builtins.list[typing.Any]]], typing.Any]
typing.Callable[[builtins.bool], typing.Any]
typing.Callable[[builtins.float], typing.Any]
typing.Callable[[builtins.list[builtins.str]], typing.Any]
...
```

#### find_first: TODO