import json
import typing as tp
from collections import defaultdict

import mypy.nodes
import mypy.types

import utbot_mypy_runner.mypy_main as mypy_main
import utbot_mypy_runner.expression_traverser as expression_traverser
import utbot_mypy_runner.names
from utbot_mypy_runner.utils import get_borders
from utbot_mypy_runner.nodes import *


class ExpressionType:
    def __init__(self, start_offset: int, end_offset: int, line: int, type_: Annotation):
        self.start_offset = start_offset
        self.end_offset = end_offset
        self.line = line
        self.type_ = type_

    def encode(self):
        return {
            "startOffset": self.start_offset, 
            "endOffset": self.end_offset, 
            "line": self.line,
            "type": self.type_.encode()
        }


def get_output_json(annotations: tp.Dict[str, tp.Dict[str, Definition]], 
                    expression_types: tp.Dict[str, tp.List[ExpressionType]],
                    names_dict: tp.Dict[str, tp.List[utbot_mypy_runner.names.Name]],
                    indent: tp.Optional[int]):
    node_storage_key = 'nodeStorage'
    types_key = 'types'
    definitions_key = 'definitions'
    names_key = 'names'

    result: tp.Dict[str, tp.Any] = {node_storage_key: {}, types_key: {}}
    for key in annotation_node_dict:
        result[node_storage_key][str(key)] = annotation_node_dict[key].encode()

    result[definitions_key] = {}
    for module in annotations.keys():
        result[definitions_key][module] = {}
        for name in annotations[module].keys():
            result[definitions_key][module][name] = annotations[module][name].encode()

    for module in expression_types.keys():
        result[types_key][module] = [x.encode() for x in expression_types[module]]

    result[names_key] = {}
    for module in names_dict.keys():
        result[names_key][module] = [x.encode() for x in names_dict[module]]

    return json.dumps(result, indent=indent)


def skip_node(node: mypy.nodes.SymbolTableNode) -> bool:

    if isinstance(node.node, mypy.nodes.TypeInfo):
        x = node.node
        return x.is_named_tuple or (x.typeddict_type is not None) or x.is_newtype or x.is_intersection
    
    return False


def get_result_from_mypy_build(build_result: mypy_main.build.BuildResult, source_paths: tp.List[str],
                               module_for_types: tp.Optional[str], indent=None) -> str:
    annotation_dict: tp.Dict[str, tp.Dict[str, Definition]] = defaultdict(dict)
    names_dict: tp.Dict[str, tp.List[utbot_mypy_runner.names.Name]] = utbot_mypy_runner.names.get_names(build_result)
    for module in build_result.files.keys():
        mypy_file: mypy.nodes.MypyFile = build_result.files[module]

        for name in mypy_file.names.keys():
            symbol_table_node = build_result.files[module].names[name]

            if skip_node(symbol_table_node):
                continue

            only_types = mypy_file.path not in source_paths

            definition = get_definition_from_symbol_node(symbol_table_node, Meta(module), only_types)
            if definition is not None:
                annotation_dict[module][name] = definition

    expression_types: tp.Dict[str, tp.List[ExpressionType]] = defaultdict(list)
    if module_for_types is not None:
        mypy_file = build_result.files[module_for_types]
        with open(mypy_file.path, "r") as file:
            content = file.readlines()
            processor = lambda line, col, end_line, end_col, type_: \
                    expression_types[module_for_types].append( # TODO: proper Meta
                        ExpressionType(*get_borders(line, col, end_line, end_col, content), line, get_annotation(type_, Meta(module_for_types)))
                    )
            traverser = expression_traverser.MyTraverserVisitor(build_result.types, processor)
            traverser.visit_mypy_file(build_result.files[module_for_types])

    return get_output_json(annotation_dict, expression_types, names_dict, indent)
