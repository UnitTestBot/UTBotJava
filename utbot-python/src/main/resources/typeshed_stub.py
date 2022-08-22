import ast
import importlib
import json
import sys
import os

import mypy.fastparse

from contextlib import contextmanager
from collections import defaultdict

import astor
from typeshed_client import get_stub_names, get_search_context, OverloadedName


def normalize_annotation(annotation, module_of_annotation):
    def walk_mypy_type(mypy_type):
        try:
            prefix = f'{module_of_annotation}.' if len(module_of_annotation) > 0 else ''

            if mypy_type.name[:len(prefix)] == prefix:
                name = mypy_type.name[len(prefix):]
            else:
                name = mypy_type.name

            if eval(name) is None:
                result = "types.NoneType"
            else:
                modname = eval(name).__module__
                result = f'{modname}.{name}'

        except Exception as e:
            result = 'typing.Any'

        if hasattr(mypy_type, 'args') and len(mypy_type.args) != 0:
            arg_strs = [
                walk_mypy_type(arg)
                for arg in mypy_type.args
            ]
            result += f"[{', '.join(arg_strs)}]"

        return result

    mod = importlib.import_module(module_of_annotation)

    for name in dir(mod):
        globals()[name] = getattr(mod, name)

    mypy_type_ = mypy.fastparse.parse_type_string(annotation, annotation, -1, -1)
    return walk_mypy_type(mypy_type_)


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


def find_init_method(function_ast):
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


def function_is_property(function):
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


def transform_annotation(annotation):
    return '' if annotation is None else astor.code_gen.to_source(annotation).strip()


def recursive_normalize_annotations(json_data, module_name):
    if 'annotation' in json_data:
        json_data['annotation'] = normalize_annotation(
            annotation=json_data['annotation'],
            module_of_annotation=module_name
        )
    elif 'returns' in json_data:
        json_data['returns'] = normalize_annotation(
            annotation=json_data['returns'],
            module_of_annotation=module_name
        )
        json_data['args'] = [
            recursive_normalize_annotations(arg, module_name)
            for arg in json_data['args']
        ]
        json_data['kwonlyargs'] = [
            recursive_normalize_annotations(arg, module_name)
            for arg in json_data['kwonlyargs']
        ]
    elif 'className' in json_data:
        for key, value in json_data.items():
            if key in {'methods', 'fields'}:
                json_data[key] = [
                    recursive_normalize_annotations(elem, module_name)
                    for elem in value
                ]
    else:
        for key, value in json_data.items():
            json_data[key] = [
                recursive_normalize_annotations(elem, module_name)
                for elem in value
            ]

    return json_data


class StubFileCollector:
    def __init__(self, python_version):
        self.methods_dataset = defaultdict(list)
        self.fields_dataset = defaultdict(list)
        self.functions_dataset = defaultdict(list)
        self.classes_dataset = []
        self.assigns_dataset = defaultdict(list)
        self.ann_assigns_dataset = defaultdict(list)
        self.python_version = python_version
        self.visited_modules = []

    def create_module_table(self, module_name):
        self.visited_modules.append(module_name)

        stub = get_stub_names(
            module_name,
            search_context=get_search_context(version=self.python_version)
        )

        def _ast_handler(ast_):
            if isinstance(ast_, OverloadedName):
                for definition in ast_.definitions:
                    _ast_handler(definition)
            else:
                if isinstance(ast_, ast.ClassDef):
                    json_data = AstClassEncoder().default(ast_)
                    recursive_normalize_annotations(json_data, module_name)

                    if not ast_.name.startswith('_'):
                        class_name = f'{module_name}.{ast_.name}'
                        json_data['className'] = class_name
                        self.classes_dataset.append(json_data)

                        for method in json_data['methods']:
                            method['className'] = class_name
                            self.methods_dataset[method['name']].append(method)

                        for field in json_data['fields']:
                            field['className'] = class_name
                            self.fields_dataset[field['name']].append(field)

                elif isinstance(ast_, (ast.FunctionDef, ast.AsyncFunctionDef)):
                    json_data = AstFunctionDefEncoder().default(ast_)
                    recursive_normalize_annotations(json_data, module_name)

                    function_name = f'{module_name}.{ast_.name}'
                    json_data['name'] = function_name
                    json_data['className'] = None
                    self.functions_dataset[ast_.name].append(json_data)

                else:
                    pass

        ast_nodes = set()

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
        })


def defaultdict_to_array(dataset):
    return [
        {
            'name': name,
            'definitions': types,
        }
        for name, types in dataset.items()
    ]


def parse_submodule(module_name, collector_):
    collector_.create_module_table(module_name)
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
    sys.stdout.write(result)


if __name__ == '__main__':
    main()
    sys.exit(0)
