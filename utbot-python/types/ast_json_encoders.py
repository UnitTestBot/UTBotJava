import ast
import json
from typing import Optional

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
                'name': o.name,
                'methods': [],
                'attributes': [],
            }

            def _function_statements_handler(_statement):
                if isinstance(_statement, ast.FunctionDef):
                    json_dump['methods'].append(AstFunctionDefEncoder().default(_statement))
                if isinstance(_statement, ast.AnnAssign):
                    json_dump['attributes'].append(AstAnnAssignEncoder().default(_statement))

            for statement in o.body:
                _function_statements_handler(statement)

            return json_dump
        return json.JSONEncoder.default(self, o)


class AstAnnAssignEncoder(json.JSONEncoder):
    def default(self, o):
        if isinstance(o, ast.AnnAssign):
            json_dump = {
                'target': '...' if isinstance(o.target, type(Ellipsis)) else o.target.id,
                'annotation': split_annotations(o.annotation),
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
                'returns': split_annotations(o.returns),
                'args': [
                    AstArgEncoder().default(arg)
                    for arg in o.args.args
                ],
                'kwonlyargs': [
                    AstArgEncoder().default(arg)
                    for arg in o.args.kwonlyargs
                ],
            }
            return json_dump


class AstArgEncoder(json.JSONEncoder):
    def default(self, o):
        if isinstance(o, ast.arg):
            json_dump = {
                'arg': o.arg,
                'annotation': split_annotations(o.annotation)
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
            return 'null'


def split_annotations(annotation: Optional[ast.expr]) -> str:
    return 'null' if annotation is None else astor.code_gen.to_source(annotation).strip()
