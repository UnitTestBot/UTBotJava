import os
import sys
import json
import typing
import copy
from collections import defaultdict

import mypy_main
import mypy.nodes
import mypy.types


annotation_node_dict: typing.Dict[str, "AnnotationNode"] = {}
type_vars_of_node: typing.Dict[str, typing.List[str]] = defaultdict(list)


class Namespace:
    def __init__(self):
        self.fullname_to_node_id: typing.Dict[str, str] = {}


class AnnotationNode:
    def __init__(self, annotation_type, id_, namespace: Namespace):
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
    def __init__(self, function_like, id_, namespace: Namespace):
        super().__init__("Function", id_, namespace)
        self.namespace.fullname_to_node_id[''] = id_
        if isinstance(function_like, mypy.types.CallableType):
            self.positional: typing.List[Annotation] = [
                get_annotation(x, namespace=self.namespace)
                for x in function_like.arg_types[:function_like.min_args]
            ]
            self.return_type: Annotation = get_annotation(function_like.ret_type, self.namespace)
            self.type_vars: typing.List[str] = type_vars_of_node[id_]
        else:
            assert False, f"Some function type wasn't considered. Type: {type(function_like)}"

    def encode(self):
        superclass_dict = super().encode()
        subclass_dict = {
            "positional": [x.encode() for x in self.positional],
            "returnType": self.return_type.encode(),
            "typeVars": self.type_vars
        }
        return dict(superclass_dict, **subclass_dict)


class TypeVarNode(AnnotationNode):
    def __init__(self, type_var: mypy.types.TypeVarType, id_, namespace: Namespace):
        super().__init__("TypeVar", id_, namespace)
        self.name: str = type_var.name
        self.values: typing.List[Annotation] = [
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
    def __init__(self, type_name: str, symbol_node, id_, namespace: Namespace):
        if isinstance(symbol_node, mypy.nodes.TypeInfo):
            super().__init__(type_name, id_, namespace)
            self.namespace.fullname_to_node_id[symbol_node._fullname] = id_
            self.module: str = symbol_node.module_name
            self.simple_name: str = symbol_node._fullname.split('.')[-1]

            self.names: typing.Dict[str, Definition] = {}
            for name in symbol_node.names.keys():
                inner_symbol_node = symbol_node.names[name]
                definition = get_definition_from_node(inner_symbol_node, False, self.namespace)
                if definition is not None:
                    self.names[name] = definition

            self.type_vars: typing.List[Annotation] = [
                get_annotation(x, self.namespace) for x in symbol_node.defn.type_vars
            ]
            self.bases: typing.List[Annotation] = [
                get_annotation(x, self.namespace) for x in symbol_node.bases
            ]
        else:
            assert False, "Some SymbolNode wasn't considered"

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
    def __init__(self, symbol_node, id_, namespace: Namespace):
        assert not symbol_node.is_protocol
        super().__init__("Concrete", symbol_node, id_, namespace)

    def encode(self):
        return super().encode()
        

class ProtocolAnnotationNode(CompositeAnnotationNode):
    def __init__(self, symbol_node, id_, namespace: Namespace):
        assert symbol_node.is_protocol
        super().__init__("Protocol", symbol_node, id_, namespace)
        self.members: typing.List[str] = symbol_node.protocol_members

    def encode(self):
        superclass_dict = super().encode()
        subclass_dict = {"protocolMembers": self.members}
        return dict(superclass_dict, **subclass_dict)


class AnnotationNodeWithItems(AnnotationNode):
    def __init__(self, type_name: str, mypy_type, id_, namespace: Namespace):
        super().__init__(type_name, id_, namespace)
        self.items: typing.List[Annotation] = [
            get_annotation(x, self.namespace) for x in mypy_type.items
        ]

    def encode(self):
        superclass_dict = super().encode()
        subclass_dict = {"items": [x.encode() for x in self.items]}
        return dict(superclass_dict, **subclass_dict)



class Annotation:
    def __init__(self, node_id, args: typing.Optional[typing.List['Annotation']] = None):
        self.node_id = node_id
        self.args = args

    def encode(self):
        result = {"nodeId": str(self.node_id)}
        if self.args is not None:
            result["args"] = [x.encode() for x in self.args]
        return result


def get_annotation_node(mypy_type: mypy.types.Type, namespace: Namespace) -> AnnotationNode:
    if isinstance(mypy_type, mypy.types.Instance):
        id_ = str(id(mypy_type.type))
    elif isinstance(mypy_type, mypy.types.TypeVarType):
        if mypy_type.id.namespace not in namespace.fullname_to_node_id.keys():
            id_ = '0'
            mypy_type = mypy.types.Type()
        else:
            node = namespace.fullname_to_node_id[mypy_type.id.namespace]
            id_ = '.' + str(mypy_type.id.raw_id) + '.' + node
    else:
        id_ = str(id(mypy_type))

    if id_ in annotation_node_dict.keys():
        return annotation_node_dict[id_]

    result: AnnotationNode

    if isinstance(mypy_type, mypy.types.Instance):
        if mypy_type.type.is_protocol:
            result = ProtocolAnnotationNode(mypy_type.type, id_, namespace)
        else:
            result = ConcreteAnnotationNode(mypy_type.type, id_, namespace)
    
    elif isinstance(mypy_type, mypy.types.CallableType):
        result = FunctionNode(mypy_type, id_, namespace)

    elif isinstance(mypy_type, mypy.types.Overloaded):  # several signatures for one function
        result = AnnotationNodeWithItems("Overloaded", mypy_type, id_, namespace)
    
    elif isinstance(mypy_type, mypy.types.TypeVarType):
        result = TypeVarNode(mypy_type, id_, namespace)

    elif isinstance(mypy_type, mypy.types.AnyType):
        result = AnnotationNode("Any", id_, namespace)

    elif isinstance(mypy_type, mypy.types.TupleType):
        result = AnnotationNodeWithItems("Tuple", mypy_type, id_, namespace)

    elif isinstance(mypy_type, mypy.types.UnionType):
        result = AnnotationNodeWithItems("Union", mypy_type, id_, namespace)

    elif isinstance(mypy_type, mypy.types.NoneType):
        result = AnnotationNode("NoneType", id_, namespace)

    elif isinstance(mypy_type, mypy.types.TypeAliasType) and \
            mypy_type.alias is not None:
        return get_annotation_node(mypy_type.alias.target, namespace)

    else:
        id_ = '0'
        result = AnnotationNode("Unknown", id_, namespace)

    annotation_node_dict[id_] = result
    return result


def get_annotation(mypy_type: mypy.types.Type, namespace: Namespace) -> Annotation:
    cur_node = get_annotation_node(mypy_type, namespace)

    if isinstance(mypy_type, mypy.types.Instance):
        children = []
        for arg in mypy_type.args:
            children.append(get_annotation(arg, namespace))

        if len(children) == 0:
            return Annotation(cur_node.id_)
        else:
            return Annotation(cur_node.id_, children)

    # TODO: consider LiteralType
    
    else:
        return Annotation(cur_node.id_)


class Definition:
    def __init__(self, type_name: str):
        self.type_name = type_name

    def encode(self):
        return {"kind": self.type_name}


class TypeDefinition(Definition):
    def __init__(self, type_info: mypy.nodes.TypeInfo):
        super().__init__("Type")
        self.annotation: Annotation = get_annotation(
            mypy.types.Instance(type_info, []),
            # TODO: does this work for inner classes?
            Namespace()
        )

    def encode(self):
        superclass_dict = super().encode()
        subclass_dict = {"annotation": self.annotation.encode()}
        return dict(superclass_dict, **subclass_dict)


any_type_instance = mypy.types.AnyType(mypy.types.TypeOfAny.unannotated)


class VarDefinition(Definition):
    def __init__(self, var: typing.Union[mypy.nodes.Var, mypy.nodes.FuncBase], namespace: Namespace):
        super().__init__("Var")
        self.annotation: Annotation
        if var.type is None:
            self.annotation = get_annotation(any_type_instance, namespace)
        else:
            self.annotation = get_annotation(var.type, namespace)

    def encode(self):
        superclass_dict = super().encode()
        subclass_dict = {"annotation": self.annotation.encode()}
        return dict(superclass_dict, **subclass_dict)


def get_definition_from_node(
    table_node: mypy.nodes.SymbolTableNode,
    only_public: bool,
    namespace: Namespace
)-> typing.Optional[Definition]:
    if (only_public and not table_node.module_public) or table_node.node is None:
        return None

    node = table_node.node
    if isinstance(node, mypy.nodes.TypeInfo):
        return TypeDefinition(node)
    elif isinstance(node, mypy.nodes.Var) or isinstance(node, mypy.nodes.FuncBase):
        return VarDefinition(node, namespace)
    elif isinstance(node, mypy.nodes.Decorator):
        return VarDefinition(node.var, namespace)
    else:
        return None


def get_output_json(annotations: typing.Dict[str, typing.Dict[str, Definition]]):
    result: typing.Dict[str, typing.Any] = {}
    result['nodeStorage'] = {}
    for key in annotation_node_dict:
        result['nodeStorage'][str(key)] = annotation_node_dict[key].encode()
    result['definitions'] = {}
    for module in annotations.keys():
        result['definitions'][module] = {}
        for name in annotations[module].keys():
            result['definitions'][module][name] = annotations[module][name].encode()
    return json.dumps(result)


def main(mypy_config_file, source_paths):
    stdout, stderr, exit_status, build_result = mypy_main.run(
        source_paths + ["--config-file", mypy_config_file]
    )
    
    annotation_dict: typing.Dict[str, typing.Dict[str, Definition]] = {}
    for module in build_result.files.keys():
        annotation_dict[module] = {}
        for name in build_result.files[module].names.keys():
            symbol_table_node = build_result.files[module].names[name]
            definition = get_definition_from_node(symbol_table_node, True, Namespace())
            if definition is not None:
                annotation_dict[module][name] = definition

    print(get_output_json(annotation_dict))

    #module_name = os.path.basename(source_path)[:-3]
    #function_info = build_result.files[module_name].names[function_name].node

    #if function_info.type is None:
    #    sys.stderr.write("No annotation")
    #    exit(1)

    #arg_annotations = function_info.type.arg_types
    #annotation_list = [get_annotation(function_info.type)]
    #for x in arg_annotations:
    #    annotation_list.append(get_annotation(x))
    
    #print(get_output_json(annotation_list))


def get_args():
    mypy_config_file = sys.argv[1]
    source_paths = sys.argv[2:]
    return mypy_config_file, source_paths


if __name__ == '__main__':
    main(*get_args())
