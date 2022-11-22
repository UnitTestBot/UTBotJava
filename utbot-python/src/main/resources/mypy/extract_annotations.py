import os
import sys
import json
import typing as tp
import copy
from collections import defaultdict

import mypy_main
import mypy.nodes
import mypy.types
import mypy.maptype


annotation_node_dict: tp.Dict[str, "AnnotationNode"] = {}
type_vars_of_node: tp.Dict[str, tp.List[str]] = defaultdict(list)
current_file: tp.Optional[tp.List[str]] = None
any_type_instance = mypy.types.AnyType(mypy.types.TypeOfAny.unannotated)


class Meta:
    def __init__(self):
        self.fullname_to_node_id: tp.Dict[str, str] = {}
        self.is_static: tp.Optional[bool] = None
        self.is_class: tp.Optional[bool] = None


class AnnotationNode:
    def __init__(self, annotation_type, id_, namespace: Meta):
        self.type = annotation_type
        self.id_ = id_
        annotation_node_dict[id_] = self
        self.namespace = copy.deepcopy(namespace)

    def encode(self):
        return {"type": self.type}

    def __eq__(self, other):
        return self.id_ == other.id_

    def __hash__(self):
        return hash(self.id_)


class FunctionNode(AnnotationNode):
    def __init__(self, function_like: tp.Union[mypy.types.CallableType, mypy.nodes.FuncItem],
            id_, namespace: Meta, is_static: tp.Optional[bool], is_class: tp.Optional[bool]):
        super().__init__("Function", id_, namespace)
        self.namespace.fullname_to_node_id[''] = id_
        self.is_class = is_class
        self.is_static = is_static
        self.positional: tp.List[Annotation]
        self.return_type: Annotation
        self.type_vars: tp.List[str]
        if isinstance(function_like, mypy.types.CallableType):
            self.positional = [
                get_annotation(x, meta=self.namespace)
                for x in function_like.arg_types[:function_like.min_args]
            ]
            self.return_type = get_annotation(function_like.ret_type, self.namespace)
            self.type_vars = type_vars_of_node[id_]
        elif isinstance(function_like, mypy.nodes.FuncItem):
            self.positional = [
                get_annotation(any_type_instance, meta=self.namespace)
                for _ in range(function_like.min_args)
            ]
            self.return_type = get_annotation(any_type_instance, meta=self.namespace)
            self.type_vars = []
        else:
            assert False, "Not reachable"


    def encode(self):
        superclass_dict = super().encode()
        subclass_dict = {
            "positional": [x.encode() for x in self.positional],
            "returnType": self.return_type.encode(),
            "typeVars": self.type_vars
        }
        if self.is_class:
            subclass_dict["isClass"] = True
        if self.is_static:
            subclass_dict["isStatic"] = True
        return dict(superclass_dict, **subclass_dict)


class TypeVarNode(AnnotationNode):
    def __init__(self, type_var: mypy.types.TypeVarType, id_, namespace: Meta):
        super().__init__("TypeVar", id_, namespace)
        self.name: str = type_var.name
        self.values: tp.List[Annotation] = [
            get_annotation(x, self.namespace)
            for x in type_var.values
        ]
        self.def_id: str = self.namespace.fullname_to_node_id[type_var.id.namespace]
        type_vars_of_node[self.def_id].append(id_)
        self.upper_bound: Annotation = get_annotation(type_var.upper_bound, self.namespace)
        self.variance: str
        if type_var.variance == mypy.nodes.COVARIANT:
            self.variance = "COVARIANT"
        elif type_var.variance == mypy.nodes.CONTRAVARIANT:
            self.variance = "CONTRAVARIANT"
        else:
            self.variance = "INVARIANT"

    def encode(self):
        superclass_dict = super().encode()
        subclass_dict = {
            "varName": self.name,
            "values": [x.encode() for x in self.values],
            "upperBound": self.upper_bound.encode(),
            "def": self.def_id,
            "variance": self.variance
        }
        return dict(superclass_dict, **subclass_dict)


class CompositeAnnotationNode(AnnotationNode):
    def __init__(self, type_name: str, symbol_node: mypy.nodes.TypeInfo, id_, namespace: Meta):
        super().__init__(type_name, id_, namespace)
        self.namespace.fullname_to_node_id[symbol_node._fullname] = id_
        self.module: str = symbol_node.module_name
        self.simple_name: str = symbol_node._fullname.split('.')[-1]

        self.names: tp.Dict[str, Definition] = {}
        for name in symbol_node.names.keys():
            inner_symbol_node = symbol_node.names[name]
            defs: tp.List[NodeInfo] = get_infos_of_nodes(symbol_node.defn.defs.body)
            definition = get_definition_from_node(inner_symbol_node, False, self.namespace, defs, name)
            if definition is not None:
                self.names[name] = definition

        self.raw_type_vars: tp.Sequence[mypy.types.Type] = symbol_node.defn.type_vars
        self.type_vars: tp.List[Annotation] = [
            get_annotation(x, self.namespace) for x in self.raw_type_vars
        ]
        self.bases = [get_annotation(x, self.namespace) for x in symbol_node.bases]

    def encode(self):
        superclass_dict = super().encode()
        subclass_dict = {
            "module": self.module,
            "simpleName": self.simple_name,
            "names": {},
            "typeVars": [x.encode() for x in self.type_vars],
            "bases": [x.encode() for x in self.bases]
        }
        for name in self.names.keys():
            subclass_dict["names"][name] = self.names[name].encode()
        return dict(superclass_dict, **subclass_dict)


class ConcreteAnnotationNode(CompositeAnnotationNode):
    def __init__(self, symbol_node: mypy.nodes.TypeInfo, id_, namespace: Meta):
        assert not symbol_node.is_protocol
        super().__init__("Concrete", symbol_node, id_, namespace)

    def encode(self):
        return super().encode()
        

class ProtocolAnnotationNode(CompositeAnnotationNode):
    def __init__(self, symbol_node: mypy.nodes.TypeInfo, id_, namespace: Meta):
        assert symbol_node.is_protocol
        super().__init__("Protocol", symbol_node, id_, namespace)
        self.members: tp.List[str] = symbol_node.protocol_members

    def encode(self):
        superclass_dict = super().encode()
        subclass_dict = {"protocolMembers": self.members}
        return dict(superclass_dict, **subclass_dict)


class AnnotationNodeWithItems(AnnotationNode):
    def __init__(self, type_name: str, mypy_type, id_, namespace: Meta):
        super().__init__(type_name, id_, namespace)
        self.items: tp.List[Annotation] = [
            get_annotation(x, self.namespace) for x in mypy_type.items
        ]

    def encode(self):
        superclass_dict = super().encode()
        subclass_dict = {"items": [x.encode() for x in self.items]}
        return dict(superclass_dict, **subclass_dict)



class Annotation:
    def __init__(self, node_id, args: tp.Optional[tp.List['Annotation']] = None):
        self.node_id = node_id
        self.args = args

    def encode(self):
        result = {"nodeId": str(self.node_id)}
        if self.args is not None:
            result["args"] = [x.encode() for x in self.args]
        return result


def get_annotation_node(mypy_type: mypy.types.Type, meta: Meta) -> AnnotationNode:
    is_static = meta.is_static
    is_class = meta.is_class
    meta.is_static = None
    meta.is_class = None

    if isinstance(mypy_type, mypy.types.Instance):
        id_ = str(id(mypy_type.type))
    elif isinstance(mypy_type, mypy.types.TypeVarType):
        if mypy_type.id.namespace not in meta.fullname_to_node_id.keys():
            id_ = '0'
            mypy_type = mypy.types.Type()
        else:
            node = meta.fullname_to_node_id[mypy_type.id.namespace]
            id_ = '.' + str(mypy_type.id.raw_id) + '.' + node
    else:
        id_ = str(id(mypy_type))

    if id_ in annotation_node_dict.keys():
        return annotation_node_dict[id_]

    result: AnnotationNode

    if isinstance(mypy_type, mypy.types.Instance):
        if mypy_type.type.is_protocol:
            result = ProtocolAnnotationNode(mypy_type.type, id_, meta)
        else:
            result = ConcreteAnnotationNode(mypy_type.type, id_, meta)
    
    elif isinstance(mypy_type, mypy.types.CallableType):
        result = FunctionNode(mypy_type, id_, meta, is_static, is_class)

    elif isinstance(mypy_type, mypy.types.Overloaded):  # several signatures for one function
        result = AnnotationNodeWithItems("Overloaded", mypy_type, id_, meta)
    
    elif isinstance(mypy_type, mypy.types.TypeVarType):
        result = TypeVarNode(mypy_type, id_, meta)

    elif isinstance(mypy_type, mypy.types.AnyType):
        result = AnnotationNode("Any", id_, meta)

    elif isinstance(mypy_type, mypy.types.TupleType):
        result = AnnotationNodeWithItems("Tuple", mypy_type, id_, meta)

    elif isinstance(mypy_type, mypy.types.UnionType):
        result = AnnotationNodeWithItems("Union", mypy_type, id_, meta)

    elif isinstance(mypy_type, mypy.types.NoneType):
        result = AnnotationNode("NoneType", id_, meta)

    elif isinstance(mypy_type, mypy.types.TypeAliasType) and \
            mypy_type.alias is not None:
        return get_annotation_node(mypy_type.alias.target, meta)

    else:
        id_ = '0'
        result = AnnotationNode("Unknown", id_, meta)

    annotation_node_dict[id_] = result
    return result


def get_annotation(mypy_type: mypy.types.Type, meta: Meta) -> Annotation:
    cur_node = get_annotation_node(mypy_type, meta)

    if isinstance(mypy_type, mypy.types.Instance):
        children = []
        for arg in mypy_type.args:
            children.append(get_annotation(arg, meta))

        if len(children) == 0:
            return Annotation(cur_node.id_)
        else:
            return Annotation(cur_node.id_, children)

    # TODO: consider LiteralType
    
    else:
        return Annotation(cur_node.id_)


class Definition:
    def __init__(self, type_name: str, line: int, column: int):
        self.type_name = type_name
        self.line = line
        self.column = column

    def encode(self):
        return {"kind": self.type_name}


class TypeDefinition(Definition):
    def __init__(self, type_info: mypy.nodes.TypeInfo, line: int, column: int):
        super().__init__("Type", line, column)
        self.annotation: Annotation = get_annotation(
            mypy.types.Instance(type_info, []),
            # TODO: does this work for inner classes?
            Meta()
        )

    def encode(self):
        superclass_dict = super().encode()
        subclass_dict = {"annotation": self.annotation.encode()}
        return dict(superclass_dict, **subclass_dict)


def get_function_default_annotation(node: mypy.nodes.FuncItem, meta: Meta) -> Annotation:
    id_ = str(id(node))
    annotation_node_dict[id_] = FunctionNode(node, id_, meta, meta.is_static, meta.is_class)
    meta.is_static = None
    meta.is_class = None
    return Annotation(id_)


class VarDefinition(Definition):
    def __init__(self, var: tp.Union[mypy.nodes.Var, mypy.nodes.FuncBase], meta: Meta, line: int, column: int):
        super().__init__("Var", line, column)

        if isinstance(var, mypy.nodes.FuncBase):
            meta.is_class = var.is_class
            meta.is_static = var.is_static

        self.annotation: Annotation
        if var.type is None:
            if isinstance(var, mypy.nodes.FuncItem):
                self.annotation = get_function_default_annotation(var, meta)
            else:
                self.annotation = get_annotation(any_type_instance, meta)
        else:
            self.annotation = get_annotation(var.type, meta)

    def encode(self):
        superclass_dict = super().encode()
        subclass_dict = {"annotation": self.annotation.encode()}
        return dict(superclass_dict, **subclass_dict)


def get_definition_from_node(
    table_node: mypy.nodes.SymbolTableNode,
    only_public: bool,
    namespace: Meta,
    defined_nodes: tp.List["NodeInfo"],
    name: str
)-> tp.Optional[Definition]:
    if (only_public and not table_node.module_public) or table_node.node is None \
            or not any(name == x.name for x in defined_nodes) \
            or not isinstance(table_node.node, mypy.nodes.Node):
        return None

    node = table_node.node
    info = next(filter(lambda x: x.name == name, defined_nodes))
    #sys.stderr.write(f"{info.line} {info.column} {info.end_line} {info.end_column}\n")

    if isinstance(node, mypy.nodes.TypeInfo):
        return TypeDefinition(node, info.line, info.column)
    elif isinstance(node, mypy.nodes.Var) or isinstance(node, mypy.nodes.FuncBase):
        return VarDefinition(node, namespace, info.line, info.column)
    elif isinstance(node, mypy.nodes.Decorator):
        return VarDefinition(node.var, namespace, info.line, info.column)
    else:
        return None


def get_output_json(annotations: tp.Dict[str, tp.Dict[str, Definition]]):
    result: tp.Dict[str, tp.Any] = {'nodeStorage': {}}
    for key in annotation_node_dict:
        result['nodeStorage'][str(key)] = annotation_node_dict[key].encode()
    result['definitions'] = {}
    for module in annotations.keys():
        result['definitions'][module] = {}
        for name in annotations[module].keys():
            result['definitions'][module][name] = annotations[module][name].encode()
    return json.dumps(result)


class NodeInfo:
    def __init__(self, name: str, node: mypy.nodes.Node):
        self.name = name
        self.line = node.line
        self.column = node.column
        self.end_line = node.end_line
        self.end_column = node.end_column

    def set_end(self, line, column):
        self.end_line = line
        self.end_column = column


def get_info_of_node(node) -> tp.Optional[NodeInfo]:
    if isinstance(node, mypy.nodes.SymbolNode) or isinstance(node, mypy.nodes.ClassDef):
        return NodeInfo(node.name, node)

    return None


#def _decrease_pos(line, number)


def get_infos_of_nodes(nodes: tp.List) -> tp.List[NodeInfo]:
    result = []
    for node in nodes:
        name = get_info_of_node(node)
        if name is not None:
            result.append(name)
    return result


def main(mypy_config_file, source_paths):
    stdout, stderr, exit_status, build_result = mypy_main.run(
        source_paths + ["--config-file", mypy_config_file]
    )

    annotation_dict: tp.Dict[str, tp.Dict[str, Definition]] = {}
    for module in build_result.files.keys():
        annotation_dict[module] = {}
        mypy_file: mypy.nodes.MypyFile = build_result.files[module]

        defs: tp.List[NodeInfo]
        if mypy_file.path in source_paths:
            with open(mypy_file.path, "r") as file:
                current_file = file.readlines()
            defs = get_infos_of_nodes(mypy_file.defs)
        else:
            current_file = None
            defs = get_infos_of_nodes([x.node for x in mypy_file.names.values()])

        for name in mypy_file.names.keys():
            symbol_table_node = build_result.files[module].names[name]
            definition = get_definition_from_node(symbol_table_node, True, Meta(), defs, name)
            if definition is not None:
                annotation_dict[module][name] = definition

    print(get_output_json(annotation_dict))


def get_args():
    mypy_config_file = sys.argv[1]
    source_paths = sys.argv[2:]
    return mypy_config_file, source_paths


if __name__ == '__main__':
    main(*get_args())
