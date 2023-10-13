import typing as tp
import copy

from mypy.nodes import *
from mypy.traverser import *
import mypy.types

import utbot_mypy_runner.nodes as my_nodes


class MyTraverserVisitor(TraverserVisitor):
    def __init__(self, types, processor: tp.Callable[[int, int, int, int, mypy.types.Type, Any], None], definitions: tp.Dict[str, Any], annotation_node_dict: tp.Dict[str, Any], meta: my_nodes.Meta):
        self.types = types
        self.processor = processor
        self.meta: tp.Optional[my_nodes.Meta] = meta
        self.depth = 0
        self.definitions = definitions
        self.annotation_node_dict = annotation_node_dict

    def process_expression(self, o: Expression) -> None:
        if self.meta is not None and o in self.types.keys() and not isinstance(self.types[o], mypy.types.AnyType) \
            and o.end_line is not None and o.end_column is not None and o.line >= 0:
            self.processor(o.line, o.column, o.end_line, o.end_column, self.types[o], self.meta)

    def visit_name_expr(self, o: NameExpr) -> None:
        self.process_expression(o)
        super().visit_name_expr(o)

    def visit_member_expr(self, o: MemberExpr) -> None:
        self.process_expression(o)
        super().visit_member_expr(o)

    def visit_func(self, o: FuncItem) -> None:
        old_meta = self.meta
        definition: tp.Optional[my_nodes.Definition] = None
        
        containing_class: tp.Optional[my_nodes.AnnotationNode] = None
        if old_meta is not None and old_meta.containing_class is not None:
            containing_class = self.annotation_node_dict[old_meta.containing_class]
        
        if containing_class is not None and isinstance(containing_class, my_nodes.CompositeAnnotationNode):
            definition = next((x for x in containing_class.members if isinstance(x, my_nodes.FuncDef) and x.name == o.name), None)
        
        elif self.depth == 0 and o.name in self.definitions.keys():
            definition = self.definitions[o.name]

        if definition is not None and old_meta is not None:
            self.meta = copy.deepcopy(old_meta)
            self.meta.fullname_to_node_id[""] = definition.type.node_id
        else:
            self.meta = None

        self.depth += 1
        #print("META after func:", self.meta)
        #print("old meta:", self.meta)
        #print("name:", o.name)
        super().visit_func(o)
        self.depth -= 1
        self.meta = old_meta

    def visit_class_def(self, o: ClassDef) -> None:
        old_meta = self.meta
        self.meta = copy.copy(old_meta)

        if self.meta is not None and o.name in self.definitions.keys():
            self.meta.containing_class = self.definitions[o.name].type.node_id
        else:
            self.meta = None

        self.depth += 1
        super().visit_class_def(o)
        self.depth -= 1
        self.meta = old_meta


"""
    def visit_yield_expr(self, o: YieldExpr) -> None:
        self.process_expression(o)
        super().visit_yield_expr(o)

    def visit_call_expr(self, o: CallExpr) -> None:
        self.process_expression(o)
        super().visit_call_expr(o)

    def visit_op_expr(self, o: OpExpr) -> None:
        self.process_expression(o)
        super().visit_op_expr(o)

    def visit_comparison_expr(self, o: ComparisonExpr) -> None:
        self.process_expression(o)
        super().visit_comparison_expr(o)

    def visit_slice_expr(self, o: SliceExpr) -> None:
        self.process_expression(o)
        super().visit_slice_expr(o)

    def visit_cast_expr(self, o: CastExpr) -> None:
        self.process_expression(o)
        super().visit_cast_expr(o)

    def visit_assert_type_expr(self, o: AssertTypeExpr) -> None:
        self.process_expression(o)
        super().visit_assert_type_expr(o)

    def visit_reveal_expr(self, o: RevealExpr) -> None:
        self.process_expression(o)
        super().visit_reveal_expr(o)

    def visit_assignment_expr(self, o: AssignmentExpr) -> None:
        self.process_expression(o)
        super().visit_assignment_expr(o)

    def visit_unary_expr(self, o: UnaryExpr) -> None:
        self.process_expression(o)
        super().visit_unary_expr(o)

    def visit_list_expr(self, o: ListExpr) -> None:
        self.process_expression(o)
        super().visit_list_expr(o)

    def visit_tuple_expr(self, o: TupleExpr) -> None:
        self.process_expression(o)
        super().visit_tuple_expr(o)

    def visit_dict_expr(self, o: DictExpr) -> None:
        self.process_expression(o)
        super().visit_dict_expr(o)

    def visit_set_expr(self, o: SetExpr) -> None:
        self.process_expression(o)
        super().visit_set_expr(o)

    def visit_index_expr(self, o: IndexExpr) -> None:
        self.process_expression(o)
        super().visit_index_expr(o)

    def visit_generator_expr(self, o: GeneratorExpr) -> None:
        self.process_expression(o)
        super().visit_generator_expr(o)

    def visit_dictionary_comprehension(self, o: DictionaryComprehension) -> None:
        self.process_expression(o)
        super().visit_dictionary_comprehension(o)

    def visit_list_comprehension(self, o: ListComprehension) -> None:
        self.process_expression(o)
        super().visit_list_comprehension(o)

    def visit_set_comprehension(self, o: SetComprehension) -> None:
        self.process_expression(o)
        super().visit_set_comprehension(o)

    def visit_conditional_expr(self, o: ConditionalExpr) -> None: 
        self.process_expression(o)
        super().visit_conditional_expr(o)

    def visit_type_application(self, o: TypeApplication) -> None:
        self.process_expression(o)
        super().visit_type_application(o)

    def visit_lambda_expr(self, o: LambdaExpr) -> None:
        self.process_expression(o)
        super().visit_lambda_expr(o)

    def visit_star_expr(self, o: StarExpr) -> None:
        self.process_expression(o)
        super().visit_star_expr(o)

    def visit_backquote_expr(self, o: BackquoteExpr) -> None:
        self.process_expression(o)
        super().visit_backquote_expr(o)

    def visit_await_expr(self, o: AwaitExpr) -> None:
        self.process_expression(o)
        super().visit_await_expr(o)

    def visit_super_expr(self, o: SuperExpr) -> None:
        self.process_expression(o)
        super().visit_super_expr(o)
"""
