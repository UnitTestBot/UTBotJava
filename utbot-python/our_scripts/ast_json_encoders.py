import ast
import json
from typing import Optional, Union

import astor.code_gen  # type: ignore
from typeshed_client import OverloadedName  # type: ignore


class AstNodeEncoder(json.JSONEncoder):
    def default(self, o):
        json_dump = {
            '_type': o.__class__.__name__
        }
        for attr in dir(o):
            if attr.startswith('_'):
                continue
            if any(key in attr for key in {'col_', 'lineno', 'ctx', 'type_comment'}):
                continue

            value = getattr(o, attr)

            if value is None:
                json_dump[attr] = value
            elif isinstance(value, (int, float, bool, str, bytes, bytearray)):
                json_dump[attr] = value
            elif isinstance(value, complex):
                json_dump[attr] = ComplexEncoder().encode(value)
            elif isinstance(value, list):
                json_dump[attr] = [
                    self.default(element) for element in value
                ]
            elif isinstance(value, type(Ellipsis)):
                json_dump[attr] = '...'
            elif isinstance(value, ast.AST):
                json_dump[attr] = self.default(value)
            else:
                raise Exception(f'Unknown type: {o}')
        return json_dump


class ComplexEncoder(json.JSONEncoder):
    def default(self, o):
        return {
            '_type': 'complex',
            'real': o.real,
            'imag': o.imag,
        }


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
