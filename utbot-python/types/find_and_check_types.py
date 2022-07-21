import ast
import types
import astor.code_gen
import sys
import os
import importlib, inspect
from modulefinder import ModuleFinder
from itertools import product
from collections import defaultdict
from pprint import pprint


def find_all_classes_in_file(filename: str, prefix: str = '') -> list[str]:
    return [
        f'{prefix}{name}'
        for name, _ in inspect.getmembers(importlib.import_module(filename), inspect.isclass)
    ]


def find_module_types(module_name: types.ModuleType) -> list[str]:
    def eval_name(name, module_name):
        return name if 'builtin' in module_name.__name__ else f'{module_name.__name__}.{name}'

    res = []
    for name in dir(module_name):
        if isinstance(eval(eval_name(name, module_name)), type):
            res.append(name)
    return res


def find_import_types():
    return [
        (name, val) for name, val in globals().items()
        if isinstance(val, types.ModuleType)
    ]


def find_import_types_from_file(filename):
    finder = ModuleFinder()
    finder.run_script(filename)
    for name, val in finder.modules.items():
        print(name)
        if name != '__main__':
            print(name)
            print(find_module_types(val))
            print()


def import_to_module(import_str):
    print(import_str)
    return import_str.split('.')[0]


def find_function_arguments(code, function_name):
    class MyVisitor(ast.NodeVisitor):
        def __init__(self, *args, **kwargs):
            super().__init__(*args, **kwargs)
            self.arguments = []

        def visit(self, node):
            if isinstance(node, ast.FunctionDef) and node.name == function_name:
                self.arguments = node.args.args
            return super().visit(node)

    root = ast.parse(code)
    visitor = MyVisitor()
    visitor.visit(root)

    return visitor.arguments


def find_imports(code):
    class MyVisitor(ast.NodeVisitor):
        def __init__(self, *args, **kwargs):
            super().__init__(*args, **kwargs)
            self.imports = []

        def visit(self, node):
            if isinstance(node, ast.Import):
                self.imports += [import_to_module(name.name) for name in node.names]
            if isinstance(node, ast.ImportFrom):
                self.imports.append(import_to_module(node.module))
            return super().visit(node)

    root = ast.parse(code)
    visitor = MyVisitor()
    visitor.visit(root)

    return visitor.imports


def add_annotations(code, function_name, annotations):
    class MyVisitor(ast.NodeVisitor):
        def visit(self, node):
            if isinstance(node, ast.FunctionDef):
                if node.name == function_name:
                    node.args.args = [
                        ast.arg(arg, annotation)
                        for arg, annotation in annotations.items()
                    ]
            return super().visit(node)
    root = ast.parse(code)
    visitor = MyVisitor()
    visitor.visit(root)
    return astor.code_gen.to_source(root)


def run_mypy(code):
    # print(' --- Run Mypy --- ')
    # print(code)
    x = os.popen(f'mypy -c """{code}"""')
    # print(' --- Mypy output --- ')
    output = x.readlines()
    print(output[-1])
    return output[-1]


def find_all_types(code, test_module_path):
    all_classes = []

    all_classes += find_all_classes_in_file(test_module_path)

    imports = find_imports(code)
    for module in imports:
        all_classes += find_all_classes_in_file(module, f'{module}.')
    all_classes += find_module_types(__builtins__)

    print(f'Possible types (count = {len(all_classes)}):')
    print(all_classes)
    return all_classes


def main(test_module_path, function_name):
    with open(test_module_path, 'r') as fin:
        code = ''.join(fin.readlines())
    print(code)

    current_annotations = {
        arg.arg: arg.annotation
        for arg in find_function_arguments(code, function_name)
    }
    count_without_annotation = sum(
        1 for arg, annotation in current_annotations.items()
        if annotation is None
    )

    all_classes = find_all_types(code, test_module_path[:-3])

    actual_annotations = [
        annotation for annotation in all_classes
        if all([err not in annotation for err in [
            'Error', 'Exit', 'Warning', 'StopIteration',
            'StopAsyncIteration', 'KeyboardInterrupt', 
            'staticmethod', 'property', 'type', 'super',
            '._',
        ]])
    ]
    print(len(actual_annotations))

    default_res = run_mypy(code)
    print(default_res)
    good_types = []
    for new_types in product(actual_annotations, repeat=count_without_annotation):
        print(new_types)
        new_annotations = {}
        i = 0
        for arg, annotation in current_annotations.items():
            if annotation is None:
                new_annotations[arg] = new_types[i]
                i += 1
            else:
                new_annotations[arg] = annotation
        res = run_mypy(
            add_annotations(code, function_name, new_annotations)
        )
        if default_res == res:
            print('OK')
            good_types.append(new_annotations)

    # for arg, annotation in current_annotations.items():
    #     if annotation is None:
    #         for new_annotation in actual_annotations:
    #             new = current_annotations.copy()
    #             new[arg] = new_annotation
    #             res = run_mypy(
    #                 add_annotations(code, function_name, new)
    #             )
    #             if default_res == res:
    #                 good_types[arg].add(new_annotation)
    #     else:
    #         good_types[arg].add(annotation)

    print(good_types)
    report(test_module_path, function_name, code, good_types, f'{test_module_path}_{function_name}_report.txt')


def report(filename, function_name, code, good_types, output):
    with open(output, 'w') as fout:
        print(f'File: {filename}', file=fout)
        print(f'Source code:\n {code}', file=fout)
        print(f'Function name:\n {function_name}', file=fout)
        print(f'Possible annotations:\n {good_types}', file=fout)


if __name__ == '__main__':
    main('test5.py', 'f')
