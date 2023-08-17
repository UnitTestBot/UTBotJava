import typing as tp

from mypy.nodes import *
from mypy.traverser import *
import mypy.types


class MyTraverserVisitor(TraverserVisitor):
    def __init__(self, types, processor: tp.Callable[[int, int, int, int, mypy.types.Type], None]):
        self.types = types
        self.processor = processor

    def process_expression(self, o: Expression) -> None:
        if o in self.types.keys() and not isinstance(self.types[o], mypy.types.AnyType) \
            and o.end_line is not None and o.end_column is not None and o.line >= 0:
            self.processor(o.line, o.column, o.end_line, o.end_column, self.types[o])

    def visit_name_expr(self, o: NameExpr) -> None:
        self.process_expression(o)
        super().visit_name_expr(o)

    def visit_member_expr(self, o: MemberExpr) -> None:
        self.process_expression(o)
        super().visit_member_expr(o)

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
