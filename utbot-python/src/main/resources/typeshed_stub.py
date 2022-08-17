import ast
import importlib
import json
import sys
import os

from contextlib import contextmanager
from collections import defaultdict
from typing import Any, Union, Optional

import astor
from typeshed_client import get_stub_names, get_search_context, OverloadedName, ImportedName


class AstClassEncoder(json.JSONEncoder):
    def default(self, o):
        if isinstance(o, ast.ClassDef):
            json_dump = {
                'className': o.name,
                'methods': [],
                'fields': [],
            }

            def _function_statements_handler(_statement):
                if isinstance(_statement, ast.FunctionDef):
                    method = AstFunctionDefEncoder().default(_statement)
                    is_property = method['is_property']
                    del method['is_property']
                    if is_property:
                        del method['args']
                        del method['kwonlyargs']

                        method['annotation'] = method['returns']
                        del method['returns']

                        json_dump['fields'].append(method)
                    else:
                        json_dump['methods'].append(method)
                if isinstance(_statement, ast.AnnAssign):
                    field = AstAnnAssignEncoder().default(_statement)
                    json_dump['fields'].append(field)

            for statement in o.body:
                _function_statements_handler(statement)

            return json_dump
        return json.JSONEncoder.default(self, o)


class AstAnnAssignEncoder(json.JSONEncoder):
    def default(self, o):
        if isinstance(o, ast.AnnAssign):
            json_dump = {
                'name': '...' if isinstance(o.target, type(Ellipsis)) else o.target.id,
                'annotation': transform_annotation(o.annotation),
            }
            return json_dump
        return json.JSONEncoder.default(self, o)


def find_init_method(function_ast: ast.ClassDef) -> Optional[ast.FunctionDef]:
    for statement in function_ast.body:
        if isinstance(statement, ast.FunctionDef) and statement.name == '__init__':
            return statement
    return None


class AstFunctionDefEncoder(json.JSONEncoder):
    def default(self, o):
        if isinstance(o, (ast.FunctionDef, ast.AsyncFunctionDef)):
            json_dump = {
                'name': o.name,
                'returns': transform_annotation(o.returns),
                'args': [
                    AstArgEncoder().default(arg)
                    for arg in o.args.args
                ],
                'kwonlyargs': [
                    AstArgEncoder().default(arg)
                    for arg in o.args.kwonlyargs
                ],
                'is_property': function_is_property(o),
            }
            return json_dump


def function_is_property(function: Union[ast.FunctionDef, ast.AsyncFunctionDef]) -> bool:
    return any(
        'property' == astor.code_gen.to_source(decorator).strip()
        for decorator in function.decorator_list
    )


class AstArgEncoder(json.JSONEncoder):
    def default(self, o):
        if isinstance(o, ast.arg):
            json_dump = {
                'arg': o.arg,
                'annotation': transform_annotation(o.annotation)
            }
            return json_dump


class AstConstantEncoder(json.JSONEncoder):
    def default(self, o):
        if isinstance(o, ast.Constant):
            json_dump = '...' if isinstance(o.value, type(Ellipsis)) else o.value

            return json_dump
        if isinstance(o, type(Ellipsis)):
            return '...'
        if o is None:
            return None


def transform_annotation(annotation: Optional[ast.expr]) -> Optional[str]:
    return '' if annotation is None else astor.code_gen.to_source(annotation).strip()


class StubFileCollector:
    def __init__(self, python_version: tuple[int, int]):
        self.methods_dataset: dict[str, Any] = defaultdict(list)
        self.fields_dataset: dict[str, Any] = defaultdict(list)
        self.functions_dataset: dict[str, Any] = defaultdict(list)
        self.classes_dataset: list[Any] = []
        self.assigns_dataset: dict[str, Any] = defaultdict(list)
        self.ann_assigns_dataset: dict[str, Any] = defaultdict(list)
        self.python_version = python_version
        self.visited_modules: list[str] = []

    def create_module_table(self, module_name: str):
        self.visited_modules.append(module_name)

        stub = get_stub_names(
            module_name,
            search_context=get_search_context(version=self.python_version)
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

            else:
                pass

        ast_nodes: set[str] = set()

        if stub is None:
            return

        for name, name_info in stub.items():
            ast_nodes.add(name_info.ast.__class__.__name__)
            _ast_handler(name_info.ast)

    def save_method_annotations(self):
        return json.dumps({
            'classAnnotations': self.classes_dataset,
            'fieldAnnotations': defaultdict_to_array(self.fields_dataset),
            'functionAnnotations': defaultdict_to_array(self.functions_dataset),
            'methodAnnotations': defaultdict_to_array(self.methods_dataset),
        },
            sort_keys=True,
            indent=True
        )


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
    print(collector_.visited_modules)
    try:
        submodules = [
            f'{module_name}.{submodule}' if module_name != 'builtins' else submodule
            for submodule in importlib.import_module(module_name).__dir__()
        ]
        for submodule in submodules:
            if type(eval(submodule)) == 'module' and submodule not in collector_.visited_modules:
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


def main():
    python_version = sys.version_info
    modules = sys.argv[1:]
    with suppress_stdout():
        collector = StubFileCollector((python_version.major, python_version.minor))
        for module in modules:
            parse_submodule(module, collector)
    result = collector.save_method_annotations()
    print(result, end='')


if __name__ == '__main__':
    main()
    sys.exit(0)
