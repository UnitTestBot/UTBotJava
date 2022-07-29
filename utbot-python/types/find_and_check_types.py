import types
from typing import Optional

import astor.code_gen
import os
import importlib, inspect
from modulefinder import ModuleFinder
from itertools import product


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


def run_mypy(code: str, filename: Optional[str] = None):
    # print(' --- Run Mypy --- ')
    # print(code)
    if filename is None:
        x = os.popen(f'python3 -m mypy -c """{code}"""')
    else:
        with open(filename, 'w') as fout:
            fout.write(code)
        x = os.popen(f'python3 -m mypy.dmypy check {filename}')
    # print(' --- Mypy output --- ')
    output = x.readlines()
    # print(output[-1])
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


def check_annotations(code, test_module_path, function_name, actual_annotations):
    current_annotations = {
        arg.arg: arg.annotation
        for arg in find_function_arguments(code, function_name)
    }
    count_without_annotation = sum(
        1 for arg, annotation in current_annotations.items()
        if annotation is None
    )

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


from parser import *
import functools

def get_const_from_node(arg_name, node):
    name_pat = name(equal(arg_name))

    const_pat = map1(const(apply_()), type)
    list_pat = map0(list_(), list)
    tuple_pat = map0(tuple_(), tuple)
    set_pat = map0(set_(), set)
    dict_pat = map0(dict_(), dict)
    values = [const_pat, list_pat, tuple_pat, set_pat, dict_pat]
    val_pat = functools.reduce(or_, values, reject())

    assign_pat = assign(
                   ftargets=any_(name_pat),
                   fvalue=val_pat)
    aug_assign_pat = aug_assign(
                       ftarget=name_pat,
                       fvalue=val_pat)
    bin_op_pat = or_(
                   bin_op(fleft=name_pat, fright=val_pat),
                   bin_op(fleft=val_pat, fright=name_pat))
    compare_pat = or_(
                    compare_(fleft=name_pat, fcomparators=any_(val_pat)),
                    compare_(fleft=val_pat, fcomparators=any_(name_pat)))

    patterns = [assign_pat, aug_assign_pat, bin_op_pat, compare_pat]
    pattern = functools.reduce(or_, patterns, reject())
    return parse(pattern, none, node, id_)
    

def find_types_for_arg(code, arg_name):
    class MyVisitor(ast.NodeVisitor):
        def __init__(self, *args, **kwargs):
            super().__init__(*args, **kwargs)
            self.types = set()

        def visit(self, node):
            cur_node_const = get_const_from_node(arg_name, node)
            if cur_node_const != None:
                self.types.add(cur_node_const)
            return super().visit(node)

    root = ast.parse(code)
    visitor = MyVisitor()
    visitor.visit(root)
    return visitor.types


def find_type_by_constants(code, function_name):
    arg_names = [arg.arg for arg in find_function_arguments(code, function_name)]
    for arg_name in arg_names:
        print(find_types_for_arg(code, arg_name))


def main(test_module_path, function_name):
    with open(test_module_path, 'r') as fin:
        code = ''.join(fin.readlines())
    print(code)

    find_type_by_constants(code, function_name)

    """
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

    check_annotations(code, test_module_path, function_name, actual_annotations)
    """
    

def report(filename, function_name, code, good_types, output):
    with open(output, 'w') as fout:
        print(f'File: {filename}', file=fout)
        print(f'Source code:\n {code}', file=fout)
        print(f'Function name:\n {function_name}', file=fout)
        print(f'Possible annotations:\n {good_types}', file=fout)


if __name__ == '__main__':
    main('test_module.py', 'f')
