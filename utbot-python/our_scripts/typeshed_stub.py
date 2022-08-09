import ast
import importlib
import json
import tqdm
import sys, os
import pickle
pickle.loads

from contextlib import contextmanager
from collections import defaultdict
from typing import Any
from typeshed_client import get_stub_names, get_search_context, OverloadedName, ImportedName

from ast_json_encoders import AstClassEncoder, AstFunctionDefEncoder


MODULES = [
    '__future__', '_testinternalcapi', 'getpass', 'runpy', '_abc', '_testmultiphase', 'gettext', 'sched',
    '_aix_support', '_thread', 'glob', 'secrets', '_ast', '_threading_local', 'graphlib', 'select', '_asyncio',
    '_tracemalloc', 'grp', 'selectors', '_bisect', '_uuid', 'gzip', 'setuptools', '_blake2', '_warnings',
    'hashlib', 'shelve', '_bootsubprocess', '_weakref', 'heapq', 'shlex', '_bz2', '_weakrefset', 'hmac', 'shutil',
    '_codecs', '_xxsubinterpreters', 'html', 'signal', '_codecs_cn', '_xxtestfuzz', 'http', 'site', '_codecs_hk',
    '_zoneinfo', 'idlelib', 'smtpd', '_codecs_iso2022', 'abc', 'imaplib', 'smtplib', '_codecs_jp', 'aifc', 'imghdr',
    'sndhdr', '_codecs_kr', 'antigravity', 'imp', 'socket', '_codecs_tw', 'argparse', 'importlib', 'socketserver',
    '_collections', 'array', 'inspect', 'spwd', '_collections_abc', 'ast', 'io', 'sqlite3', '_compat_pickle',
    'asynchat', 'ipaddress', 'sre_compile', '_compression', 'asyncio', 'itertools', 'sre_constants', '_contextvars',
    'asyncore', 'json', 'sre_parse', '_crypt', 'atexit', 'keyword', 'ssl', '_csv', 'audioop', 'lib2to3', 'stat',
    '_ctypes', 'base64', 'linecache', 'statistics', '_ctypes_test', 'bdb', 'locale', 'string', '_curses', 'binascii',
    'logging', 'stringprep', '_curses_panel', 'binhex', 'lzma', 'struct', '_datetime', 'bisect', 'mailbox',
    'subprocess', '_dbm', 'builtins', 'mailcap', 'sunau', '_decimal', 'bz2', 'marshal', 'symtable',
    '_distutils_hack', 'cProfile', 'math', 'sys', '_elementtree', 'calendar', 'mimetypes', 'sysconfig',
    '_functools', 'cgi', 'mmap', 'syslog', '_gdbm', 'cgitb', 'modulefinder', 'tabnanny', '_hashlib', 'chunk',
    'multiprocessing', 'tarfile', '_heapq', 'cmath', 'netrc', 'telnetlib', '_imp', 'cmd', 'nis', 'tempfile', '_io',
    'code', 'nntplib', 'termios', '_json', 'codecs', 'ntpath', 'test', '_locale', 'codeop', 'nturl2path', 'textwrap',
    '_lsprof', 'collections', 'numbers', 'this', '_lzma', 'colorsys', 'opcode', 'threading', '_markupbase',
    'compileall', 'operator', 'time', '_md5', 'concurrent', 'optparse', 'timeit', '_multibytecodec', 'configparser',
    'os', 'tkinter', '_multiprocessing', 'contextlib', 'ossaudiodev', 'token', '_opcode', 'contextvars', 'pathlib',
    'tokenize', '_operator', 'copy', 'pdb', 'trace', '_osx_support', 'copyreg', 'pickle', 'traceback', '_pickle',
    'crypt', 'pickletools', 'tracemalloc', '_posixshmem', 'csv', 'pip', 'tty', '_posixsubprocess', 'ctypes', 'pipes',
    'turtle', '_py_abc', 'curses', 'pkg_resources', 'turtledemo', '_pydecimal', 'dataclasses', 'pkgutil', 'types',
    '_pyio', 'datetime', 'platform', 'typing', '_queue', 'dbm', 'plistlib', 'unicodedata', '_random', 'decimal',
    'poplib', 'unittest', '_sha1', 'difflib', 'posix', 'urllib', '_sha256', 'dis', 'posixpath', 'uu', '_sha3',
    'distutils', 'pprint', 'uuid', '_sha512', 'doctest', 'profile', 'venv', '_signal', 'email', 'pstats', 'warnings',
    '_sitebuiltins', 'encodings', 'pty', 'wave', '_socket', 'ensurepip', 'pwd', 'weakref', '_sqlite3', 'enum',
    'py_compile', 'webbrowser', '_sre', 'errno', 'pyclbr', 'wsgiref', '_ssl', 'faulthandler', 'pydoc', 'xdrlib',
    '_stat', 'fcntl', 'pydoc_data', 'xml', '_statistics', 'filecmp', 'pyexpat', 'xmlrpc', '_string', 'fileinput',
    'queue', 'xxlimited', '_strptime', 'fnmatch', 'quopri', 'xxlimited_35', '_struct', 'fractions', 'random',
    'xxsubtype', '_symtable', 'ftplib', 're', 'zipapp', 'functools', 'readline', 'zipfile', '_testbuffer', 'gc',
    'reprlib', 'zipimport', '_testcapi', 'genericpath', 'resource', 'zlib', '_testimportmultiple', 'getopt',
    'rlcompleter', 'zoneinfo'
]


BUILTIN_TYPES = [
    # 'ArithmeticError', 'AssertionError', 'AttributeError', 'BaseException', 'BlockingIOError', 'BrokenPipeError',
    # 'BufferError', 'BytesWarning', 'ChildProcessError', 'ConnectionAbortedError', 'ConnectionError',
    # 'ConnectionRefusedError', 'ConnectionResetError', 'DeprecationWarning', 'EOFError', 'EncodingWarning',
    # 'EnvironmentError', 'Exception', 'FileExistsError', 'FileNotFoundError', 'FloatingPointError', 'FutureWarning',
    # 'GeneratorExit', 'IOError', 'ImportError', 'ImportWarning', 'IndentationError', 'IndexError', 'InterruptedError',
    # 'IsADirectoryError', 'KeyError', 'KeyboardInterrupt', 'LookupError', 'MemoryError', 'ModuleNotFoundError',
    # 'NameError', 'NotADirectoryError', 'NotImplementedError', 'OSError', 'OverflowError',
    # 'PendingDeprecationWarning', 'PermissionError', 'ProcessLookupError', 'RecursionError', 'ReferenceError',
    # 'ResourceWarning', 'RuntimeError', 'RuntimeWarning', 'StopAsyncIteration', 'StopIteration', 'SyntaxError',
    # 'SyntaxWarning', 'SystemError', 'SystemExit', 'TabError', 'TimeoutError', 'TypeError', 'UnboundLocalError',
    # 'UnicodeDecodeError', 'UnicodeEncodeError', 'UnicodeError', 'UnicodeTranslateError', 'UnicodeWarning',
    # 'UserWarning', 'ValueError', 'Warning', 'ZeroDivisionError',
    'Exception',
    'bool', 'bytearray', 'bytes', 'classmethod',
    'complex', 'dict', 'enumerate', 'filter', 'float', 'frozenset', 'int', 'list', 'map', 'memoryview', 'object',
    'property', 'range', 'reversed', 'set', 'slice', 'staticmethod', 'str', 'super', 'tuple', 'type', 'zip'
]

BUILTIN_FUNCTIONS = [
    # 'ArithmeticError', 'AssertionError', 'AttributeError', 'BaseException', 'BlockingIOError', 'BrokenPipeError',
    # 'BufferError', 'BytesWarning', 'ChildProcessError', 'ConnectionAbortedError', 'ConnectionError',
    # 'ConnectionRefusedError', 'ConnectionResetError', 'DeprecationWarning', 'EOFError', 'EncodingWarning',
    # 'EnvironmentError', 'Exception', 'FileExistsError', 'FileNotFoundError', 'FloatingPointError', 'FutureWarning',
    # 'GeneratorExit', 'IOError', 'ImportError', 'ImportWarning', 'IndentationError', 'IndexError', 'InterruptedError',
    # 'IsADirectoryError', 'KeyError', 'KeyboardInterrupt', 'LookupError', 'MemoryError', 'ModuleNotFoundError',
    # 'NameError', 'NotADirectoryError', 'NotImplementedError', 'OSError', 'OverflowError', 'PendingDeprecationWarning',
    # 'PermissionError', 'ProcessLookupError', 'RecursionError', 'ReferenceError', 'ResourceWarning', 'RuntimeError',
    # 'RuntimeWarning', 'StopAsyncIteration', 'StopIteration', 'SyntaxError', 'SyntaxWarning', 'SystemError',
    # 'SystemExit', 'TabError', 'TimeoutError', 'TypeError', 'UnboundLocalError', 'UnicodeDecodeError',
    # 'UnicodeEncodeError', 'UnicodeError', 'UnicodeTranslateError', 'UnicodeWarning', 'UserWarning', 'ValueError',
    # 'Warning', 'ZeroDivisionError',
    'abs', 'aiter', 'all', 'anext', 'any', 'ascii', 'bin', 'bool', 'breakpoint',
    'bytearray', 'bytes', 'callable', 'chr', 'classmethod', 'compile', 'complex', 'copyright', 'credits', 'delattr',
    'dict', 'dir', 'display', 'divmod', 'enumerate', 'eval', 'exec', 'filter', 'float', 'format', 'frozenset',
    'get_ipython', 'getattr', 'globals', 'hasattr', 'hash', 'help', 'hex', 'id', 'input', 'int', 'isinstance',
    'issubclass', 'iter', 'len', 'license', 'list', 'locals', 'map', 'max', 'memoryview', 'min', 'next', 'object',
    'oct', 'open', 'ord', 'pow', 'print', 'property', 'range', 'repr', 'reversed', 'round', 'set', 'setattr', 'slice',
    'sorted', 'staticmethod', 'str', 'sum', 'super', 'tuple', 'type', 'vars', 'zip'
]


class StubFileCollector:
    def __init__(self, dataset_directory: str):
        self.methods_dataset: dict[str, Any] = defaultdict(list)
        self.fields_dataset: dict[str, Any] = defaultdict(list)
        self.functions_dataset: dict[str, Any] = defaultdict(list)
        self.classes_dataset: list[Any] = []
        self.assigns_dataset: dict[str, Any] = defaultdict(list)
        self.ann_assigns_dataset: dict[str, Any] = defaultdict(list)
        self.dataset_directory = dataset_directory

    def create_module_table(self, module_name: str, python_version: tuple[int, int] = (3, 10)):

        stub = get_stub_names(
            module_name,
            search_context=get_search_context(version=python_version)
        )

        def _ast_handler(ast_: ast.AST):
            if isinstance(ast_, OverloadedName):
                for definition in ast_.definitions:
                    _ast_handler(definition)
            elif isinstance(ast_, ast.ClassDef):
                if not ast_.name.startswith('_'):
                    json_data = AstClassEncoder().default(ast_)
                    class_name = f'{module_name}.{ast_.name}'
                    json_data['className'] = class_name
                    json_data['methods'] = [
                        method | {'className': class_name}
                        for method in json_data['methods']
                    ]
                    json_data['fields'] = [
                        field | {'className': class_name}
                        for field in json_data['fields']
                    ]

                    self.classes_dataset.append(json_data)

                    for method in json_data['methods']:
                        self.methods_dataset[method['name']].append(method)

                    for field in json_data['fields']:
                        self.fields_dataset[field['name']].append(field)

            elif isinstance(ast_, (ast.FunctionDef, ast.AsyncFunctionDef)):
                json_data = AstFunctionDefEncoder().default(ast_)
                function_name = f'{module_name}.{ast_.name}'
                json_data['name'] = function_name
                self.functions_dataset[ast_.name].append(
                    json_data | {'className': None}
                )

            elif isinstance(ast_, ast.AnnAssign):
                pass
                # if isinstance(ast_.annotation, ast.Name) and isinstance(ast_.target, ast.Name):
                #     self.ann_assigns_dataset[ast_.target.id].append({
                #         'annotation': ast_.annotation.id,
                #         'value': None if ast_.value is None else astor.code_gen.to_source(ast_.value)
                #     })
            elif isinstance(ast_, ast.Assign):
                pass
                # if isinstance(ast_.value, ast.Name):
                #     for target in ast_.targets:
                #         if isinstance(target, ast.Name):
                #             self.assigns_dataset[target.id].append({
                #                 'link': ast_.value.id
                #             })
            elif isinstance(ast_, ImportedName):
                pass
                # print('ImportedName:')
                # print(f'{ast_.module_name} {ast_.name}')
            else:
                pass
                # print('Not supported type')
                # print(astor.code_gen.to_source(ast_))

        ast_nodes: set[str] = set()

        if stub is None:
            return

        for name, name_info in stub.items():
            ast_nodes.add(name_info.ast.__class__.__name__)
            _ast_handler(name_info.ast)

    def save_method_annotations(self):
        # with open(f'{self.dataset_directory}/class_annotations.json', 'w') as fout:
        #     print(json.dumps(self.classes_dataset, sort_keys=True, indent=True), file=fout)
        #
        # with open(f'{self.dataset_directory}/field_annotations.json', 'w') as fout:
        #     print(json.dumps(defaultdict_to_array(self.fields_dataset), sort_keys=True, indent=True), file=fout)
        #
        # with open(f'{self.dataset_directory}/method_annotations.json', 'w') as fout:
        #     print(json.dumps(defaultdict_to_array(self.methods_dataset), sort_keys=True, indent=True), file=fout)

        with open(f'{self.dataset_directory}/function_annotations.json', 'w') as fout:
            print(json.dumps(defaultdict_to_array(self.functions_dataset), sort_keys=True, indent=True), file=fout)


def defaultdict_to_array(dataset):
    return [
        {
            'name': name,
            'definitions': types,
        }
        for name, types in dataset.items()
    ]


def parse_submodule(module_name: str, collector_: StubFileCollector):
    collector_.create_module_table(module_name)
    try:
        submodules = [
            f'{module_name}.{submodule}' if module_name != 'builtins' else submodule
            for submodule in importlib.import_module(module_name).__dir__()
        ]
        for submodule in submodules:
            if type(eval(submodule)) == 'module':
                parse_submodule(submodule, collector_)
    except ModuleNotFoundError:
        pass
    except ImportError:
        pass
    except NameError:
        pass
    except AttributeError:
        pass


@contextmanager
def suppress_stdout():
    with open(os.devnull, "w") as devnull:
        old_stdout = sys.stdout
        sys.stdout = devnull
        try:
            yield
        finally:
            sys.stdout = old_stdout


if __name__ == '__main__':
    # create_method_table(BUILTIN_TYPES, 'builtins')
    # create_functions_table(BUILTIN_FUNCTIONS, 'builtins')
    # create_module_table('builtins')

    with suppress_stdout():
        collector = StubFileCollector('../src/main/resources')
        for module in tqdm.tqdm(MODULES):
            parse_submodule(module, collector)
        collector.save_method_annotations()
