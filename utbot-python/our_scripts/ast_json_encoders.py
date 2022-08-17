import ast
import json
from typing import Optional, Union

import astor.code_gen  # type: ignore
from typeshed_client import OverloadedName  # type: ignore


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
