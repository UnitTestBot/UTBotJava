import ast
import json
import tqdm

from collections import defaultdict
from pprint import pprint
from typing import Any

import astor.code_gen

from typeshed_client import get_stub_names, get_search_context, OverloadedName, ImportedName
from typeshed_client.resolver import Resolver

from ast_json_encoders import AstClassEncoder, AstFunctionDefEncoder, AstAnnAssignEncoder


# MODULES = [
#     'getpass', 'runpy', 'gettext', 'sched', 'glob', 'secrets', 'graphlib', 'select', 'grp', 'selectors', 'gzip',
#     'setuptools', 'hashlib', 'shelve', 'heapq', 'shlex', 'hmac', 'shutil', 'html', 'signal', 'http', 'site', 'idlelib',
#     'smtpd', 'abc', 'imaplib', 'smtplib', 'aifc', 'imghdr', 'sndhdr', 'antigravity', 'imp', 'socket', 'argparse',
#     'importlib', 'socketserver', 'array', 'inspect', 'spwd', 'ast', 'io', 'sqlite3', 'asynchat', 'ipaddress',
#     'sre_compile', 'asyncio', 'itertools', 'sre_constants', 'asyncore', 'json', 'sre_parse', 'atexit', 'keyword',
#     'ssl', 'audioop', 'lib2to3', 'stat', 'base64', 'linecache', 'statistics', 'bdb', 'locale', 'string', 'binascii',
#     'logging', 'stringprep', 'binhex', 'lzma', 'struct', 'bisect', 'mailbox', 'subprocess', 'builtins', 'mailcap',
#     'sunau', 'bz2', 'marshal', 'symtable', 'cProfile', 'math', 'sys', 'calendar', 'mimetypes', 'sysconfig', 'cgi',
#     'mmap', 'syslog', 'cgitb', 'modulefinder', 'tabnanny', 'chunk', 'multiprocessing', 'tarfile', 'cmath', 'netrc',
#     'telnetlib', 'cmd', 'nis', 'tempfile', 'code', 'nntplib', 'termios', 'codecs', 'ntpath', 'test', 'codeop',
#     'nturl2path', 'textwrap', 'collections', 'numbers', 'this', 'colorsys', 'opcode', 'threading', 'compileall',
#     'operator', 'time', 'concurrent', 'optparse', 'timeit', 'configparser', 'os', 'tkinter', 'contextlib',
#     'ossaudiodev', 'token', 'contextvars', 'pathlib', 'tokenize', 'copy', 'pdb', 'trace', 'copyreg', 'pickle',
#     'traceback', 'crypt', 'pickletools', 'tracemalloc', 'csv', 'pip', 'tty', 'ctypes', 'pipes', 'turtle', 'curses',
#     'pkg_resources', 'turtledemo', 'dataclasses', 'pkgutil', 'types', 'datetime', 'platform', 'typing', 'dbm',
#     'plistlib', 'unicodedata', 'decimal', 'poplib', 'unittest', 'difflib', 'posix', 'urllib', 'dis', 'posixpath',
#     'uu', 'distutils', 'pprint', 'uuid', 'doctest', 'profile', 'venv', 'email', 'pstats', 'warnings', 'encodings',
#     'pty', 'wave', 'ensurepip', 'pwd', 'weakref', 'enum', 'py_compile', 'webbrowser', 'errno', 'pyclbr', 'wsgiref',
#     'faulthandler', 'pydoc', 'xdrlib', 'fcntl', 'pydoc_data', 'xml', 'filecmp', 'pyexpat', 'xmlrpc', 'fileinput',
#     'queue', 'xxlimited', 'fnmatch', 'quopri', 'xxlimited_35', 'fractions', 'random', 'xxsubtype', 'ftplib', 're',
#     'zipapp', 'functools', 'readline', 'zipfile', 'gc', 'reprlib', 'zipimport', 'genericpath', 'resource', 'zlib',
#     'getopt', 'rlcompleter', 'zoneinfo'
# ]
MODULES = ['__future__', '_testinternalcapi', 'getpass', 'runpy', '_abc', '_testmultiphase', 'gettext', 'sched', '_aix_support', '_thread', 'glob', 'secrets', '_ast', '_threading_local', 'graphlib', 'select', '_asyncio', '_tracemalloc', 'grp', 'selectors', '_bisect', '_uuid', 'gzip', 'setuptools', '_blake2', '_warnings', 'hashlib', 'shelve', '_bootsubprocess', '_weakref', 'heapq', 'shlex', '_bz2', '_weakrefset', 'hmac', 'shutil', '_codecs', '_xxsubinterpreters', 'html', 'signal', '_codecs_cn', '_xxtestfuzz', 'http', 'site', '_codecs_hk', '_zoneinfo', 'idlelib', 'smtpd', '_codecs_iso2022', 'abc', 'imaplib', 'smtplib', '_codecs_jp', 'aifc', 'imghdr', 'sndhdr', '_codecs_kr', 'antigravity', 'imp', 'socket', '_codecs_tw', 'argparse', 'importlib', 'socketserver', '_collections', 'array', 'inspect', 'spwd', '_collections_abc', 'ast', 'io', 'sqlite3', '_compat_pickle', 'asynchat', 'ipaddress', 'sre_compile', '_compression', 'asyncio', 'itertools', 'sre_constants', '_contextvars', 'asyncore', 'json', 'sre_parse', '_crypt', 'atexit', 'keyword', 'ssl', '_csv', 'audioop', 'lib2to3', 'stat', '_ctypes', 'base64', 'linecache', 'statistics', '_ctypes_test', 'bdb', 'locale', 'string', '_curses', 'binascii', 'logging', 'stringprep', '_curses_panel', 'binhex', 'lzma', 'struct', '_datetime', 'bisect', 'mailbox', 'subprocess', '_dbm', 'builtins', 'mailcap', 'sunau', '_decimal', 'bz2', 'marshal', 'symtable', '_distutils_hack', 'cProfile', 'math', 'sys', '_elementtree', 'calendar', 'mimetypes', 'sysconfig', '_functools', 'cgi', 'mmap', 'syslog', '_gdbm', 'cgitb', 'modulefinder', 'tabnanny', '_hashlib', 'chunk', 'multiprocessing', 'tarfile', '_heapq', 'cmath', 'netrc', 'telnetlib', '_imp', 'cmd', 'nis', 'tempfile', '_io', 'code', 'nntplib', 'termios', '_json', 'codecs', 'ntpath', 'test', '_locale', 'codeop', 'nturl2path', 'textwrap', '_lsprof', 'collections', 'numbers', 'this', '_lzma', 'colorsys', 'opcode', 'threading', '_markupbase', 'compileall', 'operator', 'time', '_md5', 'concurrent', 'optparse', 'timeit', '_multibytecodec', 'configparser', 'os', 'tkinter', '_multiprocessing', 'contextlib', 'ossaudiodev', 'token', '_opcode', 'contextvars', 'pathlib', 'tokenize', '_operator', 'copy', 'pdb', 'trace', '_osx_support', 'copyreg', 'pickle', 'traceback', '_pickle', 'crypt', 'pickletools', 'tracemalloc', '_posixshmem', 'csv', 'pip', 'tty', '_posixsubprocess', 'ctypes', 'pipes', 'turtle', '_py_abc', 'curses', 'pkg_resources', 'turtledemo', '_pydecimal', 'dataclasses', 'pkgutil', 'types', '_pyio', 'datetime', 'platform', 'typing', '_queue', 'dbm', 'plistlib', 'unicodedata', '_random', 'decimal', 'poplib', 'unittest', '_sha1', 'difflib', 'posix', 'urllib', '_sha256', 'dis', 'posixpath', 'uu', '_sha3', 'distutils', 'pprint', 'uuid', '_sha512', 'doctest', 'profile', 'venv', '_signal', 'email', 'pstats', 'warnings', '_sitebuiltins', 'encodings', 'pty', 'wave', '_socket', 'ensurepip', 'pwd', 'weakref', '_sqlite3', 'enum', 'py_compile', 'webbrowser', '_sre', 'errno', 'pyclbr', 'wsgiref', '_ssl', 'faulthandler', 'pydoc', 'xdrlib', '_stat', 'fcntl', 'pydoc_data', 'xml', '_statistics', 'filecmp', 'pyexpat', 'xmlrpc', '_string', 'fileinput', 'queue', 'xxlimited', '_strptime', 'fnmatch', 'quopri', 'xxlimited_35', '_struct', 'fractions', 'random', 'xxsubtype', '_symtable', 'ftplib', 're', 'zipapp', 'functools', 'readline', 'zipfile', '_testbuffer', 'gc', 'reprlib', 'zipimport', '_testcapi', 'genericpath', 'resource', 'zlib', '_testimportmultiple', 'getopt', 'rlcompleter', 'zoneinfo']


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


def generate_json_builtins():
    with open('stub_int.json', 'w') as fout:
        data = generate_json_class('int', 'builtins')
        print(data, file=fout)


def generate_json_class(module: str, typename: str):
    resolver = Resolver()
    encoder = AstClassEncoder()
    class_stub = resolver.get_fully_qualified_name(f'{module}.{typename}')
    return encoder.encode(class_stub.ast)


def create_method_table(types: list[str], module: str):
    def get_only_types(methods) -> list[str]:
        return [
            type_['type'] for type_ in methods
        ]

    methods_dataset = defaultdict(list)
    resolver = Resolver()
    encoder = AstClassEncoder()
    for t in types:
        print(f'Type: {t}')
        class_stub = resolver.get_fully_qualified_name(f'{module}.{t}')
        json_data = encoder.default(class_stub.ast)
        for method in json_data['methods']:
            methods_dataset[method['name']].append({
                'type': t,
                'method': method,
            })

    pprint(get_only_types(methods_dataset['__len__']))

    with open('method_annotations.json', 'w') as fout:
        print(json.dumps(methods_dataset, sort_keys=True, indent=True), file=fout)


def create_functions_table(functions: list[str], module: str):
    functions_dataset: dict[str, list] = defaultdict(list)
    resolver = Resolver()
    encoder = AstFunctionDefEncoder()
    for t in functions:
        print(f'Function: {t}')
        function_stub = resolver.get_fully_qualified_name(f'{module}.{t}')
        if function_stub is None:
            continue

        function_ast = function_stub.ast
        if function_ast is None:
            continue

        json_data = encoder.default(function_stub.ast)
        functions_dataset[t] = json_data

    with open('functions_annotations.json', 'w') as fout:
        print(json.dumps(functions_dataset, sort_keys=True, indent=True), file=fout)


def create_module_table(module_name: str, python_version: tuple[int, int] = (3, 10)):
    functions_dataset: dict[str, Any] = {}
    methods_dataset: dict[str, Any] = defaultdict(list)
    classes_dataset: dict[str, Any] = {}
    assigns_dataset: dict[str, Any] = {}
    ann_assigns_dataset: dict[str, Any] = {}
    imported_names_dataset: dict[str, Any] = {}

    stub = get_stub_names(
        module_name,
        search_context=get_search_context(version=python_version)
    )

    def _ast_handler(ast_: ast.AST):
        if isinstance(ast_, OverloadedName):
            for definition in ast_.definitions:
                _ast_handler(definition)
        elif isinstance(ast_, ast.ClassDef):
            json_data = AstClassEncoder().default(ast_)
            classes_dataset[ast_.name] = json_data
            for method in json_data['methods']:
                methods_dataset[method['name']].append({
                    'type': ast_.name,
                    'method': method,
                })
        elif isinstance(ast_, (ast.FunctionDef, ast.AsyncFunctionDef)):
            json_data = AstFunctionDefEncoder().default(ast_)
            functions_dataset[ast_.name] = json_data
        elif isinstance(ast_, ast.AnnAssign):
            if isinstance(ast_.annotation, ast.Name) and isinstance(ast_.target, ast.Name):
                ann_assigns_dataset[ast_.target.id] = {
                    'annotation': ast_.annotation.id,
                    'value': None if ast_.value is None else astor.code_gen.to_source(ast_.value)
                }
        elif isinstance(ast_, ast.Assign):
            if isinstance(ast_.value, ast.Name):
                for target in ast_.targets:
                    if isinstance(target, ast.Name):
                        assigns_dataset[target.id] = {
                            'link': ast_.value.id
                        }
        elif isinstance(ast_, ImportedName):
            print('ImportedName:')
            print(f'{ast_.module_name} {ast_.name}')
        else:
            print('Not supported type')
            print(astor.code_gen.to_source(ast_))

    ast_nodes: set[str] = set()
    for name, name_info in stub.items():
        ast_nodes.add(name_info.ast.__class__.__name__)
        _ast_handler(name_info.ast)

    with open(f'method_annotations_{module_name}.json', 'w') as fout:
        print(json.dumps(methods_dataset, sort_keys=True, indent=True), file=fout)

    with open(f'functions_annotations_{module_name}.json', 'w') as fout:
        print(json.dumps(functions_dataset, sort_keys=True, indent=True), file=fout)

    with open(f'assigns_{module_name}.json', 'w') as fout:
        print(json.dumps(assigns_dataset, sort_keys=True, indent=True), file=fout)

    with open(f'ann_assigns_{module_name}.json', 'w') as fout:
        print(json.dumps(ann_assigns_dataset, sort_keys=True, indent=True), file=fout)


class StubFileCollector:
    def __init__(self, dataset_directory: str):
        self.methods_dataset: dict[str, Any] = defaultdict(list)
        self.functions_dataset: dict[str, Any] = defaultdict(list)
        self.classes_dataset: dict[str, Any] = defaultdict(list)
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
                json_data = AstClassEncoder().default(ast_)
                self.classes_dataset[ast_.name].append(json_data)
                for method in json_data['methods']:
                    self.methods_dataset[method['name']].append({
                        'className': ast_.name,
                        'module': module,
                        'method': method,
                    })
            elif isinstance(ast_, (ast.FunctionDef, ast.AsyncFunctionDef)):
                json_data = AstFunctionDefEncoder().default(ast_)
                self.functions_dataset[ast_.name].append({
                    'function': json_data,
                    'module': module,
                })
            elif isinstance(ast_, ast.AnnAssign):
                if isinstance(ast_.annotation, ast.Name) and isinstance(ast_.target, ast.Name):
                    self.ann_assigns_dataset[ast_.target.id].append({
                        'annotation': ast_.annotation.id,
                        'value': None if ast_.value is None else astor.code_gen.to_source(ast_.value)
                    })
            elif isinstance(ast_, ast.Assign):
                if isinstance(ast_.value, ast.Name):
                    for target in ast_.targets:
                        if isinstance(target, ast.Name):
                            self.assigns_dataset[target.id].append({
                                'link': ast_.value.id
                            })
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
        with open(f'{self.dataset_directory}/assigns_annotations.json', 'w') as fout:
            print(json.dumps(defaultdict_to_array(self.assigns_dataset), sort_keys=True, indent=True), file=fout)

        with open(f'{self.dataset_directory}/ann_assigns_annotations.json', 'w') as fout:
            print(json.dumps(defaultdict_to_array(self.ann_assigns_dataset), sort_keys=True, indent=True), file=fout)

        with open(f'{self.dataset_directory}/classes_annotations.json', 'w') as fout:
            print(json.dumps(defaultdict_to_array(self.classes_dataset), sort_keys=True, indent=True), file=fout)

        with open(f'{self.dataset_directory}/method_annotations.json', 'w') as fout:
            print(json.dumps(defaultdict_to_array(self.methods_dataset), sort_keys=True, indent=True), file=fout)

        with open(f'{self.dataset_directory}/function_annotations.json', 'w') as fout:
            print(json.dumps(defaultdict_to_array(self.functions_dataset), sort_keys=True, indent=True), file=fout)


def defaultdict_to_array(dataset):
    return [
        {
            'name': name,
            'typeInfos': types,
        }
        for name, types in dataset.items()
    ]


if __name__ == '__main__':
    # create_method_table(BUILTIN_TYPES, 'builtins')
    # create_functions_table(BUILTIN_FUNCTIONS, 'builtins')
    # create_module_table('builtins')

    collector = StubFileCollector('stub_datasets')
    for module in tqdm.tqdm(MODULES):
        collector.create_module_table(module)
    collector.save_method_annotations()
