import ast

class T:
    def go(node, x):
        pass


class NonMatch(Exception):
    pass


def parse(tpat, on_error, node, x):
    try:
        return tpat.go(node, x)
    except NonMatch:
        return on_error(node)


def make_T(pattern):
    def wrapper(*args, **kwargs):
        res = T()
        res.go = pattern(*args, **kwargs)
        return res
    return wrapper


def id_(x):
    return x


def none(x):
    return


@make_T
def map0(tpat, a):
    def go(node, x):
        return tpat.go(node, x(a))
    return go


@make_T
def map1(tpat, f):
    def go(node, x):
        return tpat.go(node, lambda a: x(f(a)))
    return go


@make_T
def or_(tpat1, tpat2):
    def go(node, x):
        try:
            return tpat1.go(node, x)
        except NonMatch:
            return tpat2.go(node, x)
    return go


@make_T
def apply_():
    def go(node, x):
        return x(node)
    return go


@make_T
def drop():
    def go(node, x):
        return x
    return go


@make_T
def const(fvalue):
    def go(node, x):
        if not isinstance(node, ast.Constant):
            raise NonMatch()
        return fvalue.go(node.value, x)
    return go


@make_T
def list_(felts=drop()):
    def go(node, x):
        if not isinstance(node, ast.List):
            raise NonMatch()
        return felts.go(node.elts, x)
    return go


@make_T
def tuple_(felts=drop()):
    def go(node, x):
        if not isinstance(node, ast.Tuple):
            raise NonMatch()
        return felts.go(node.elts, x)
    return go


@make_T
def set_(felts=drop()):
    def go(node, x):
        if not isinstance(node, ast.Set):
            raise NonMatch()
        return felts.go(node.elts, x)
    return go


@make_T
def dict_(fkeys=drop(), fvalues=drop()):
    def go(node, x):
        if not isinstance(node, ast.Dict):
            raise NonMatch()
        x1 = fkeys.go(node.keys, x)
        x2 = fvalues.go(node.values, x1)
        return x2
    return go


@make_T
def assign(ftargets, fvalue):
    def go(node, x):
        if not isinstance(node, ast.Assign):
            raise NonMatch()
        ret = ftargets.go(node.targets, x)
        return fvalue.go(node.value, ret)
    return go


@make_T
def aug_assign(ftarget, fvalue):
    def go(node, x):
        if not isinstance(node, ast.AugAssign):
            raise NonMatch()
        ret = ftarget.go(node.target, x)
        return fvalue.go(node.value, ret)
    return go


@make_T
def bin_op(fleft=drop(), fop=drop(), fright=drop()):
    def go(node, x):
        if not isinstance(node, ast.BinOp):
            raise NonMatch()
        return fright.go(node.right, fop.go(node.op, fleft.go(node.left, x)))
    return go


@make_T
def compare_(fleft=drop(), fops=drop(), fcomparators=drop()):
    def go(node, x):
        if not isinstance(node, ast.Compare):
            raise NonMatch()
        x1 = fleft.go(node.left, x)
        x2 = fops.go(node.ops, x1)
        x3 = fcomparators.go(node.comparators, x2)
        return x3
    return go


@make_T
def any_(felem):
    def go(node, x):
        if not isinstance(node, list):
            raise NonMatch()
        for elem in node:
            try:
                ret = felem.go(elem, x)
                return ret
            except NonMatch:
                continue
        raise NonMatch()
    return go


@make_T
def reject():
    def go(node, x):
        raise NonMatch()
    return go


@make_T
def name(fid):
    def go(node, x):
        if not isinstance(node, ast.Name):
            raise NonMatch()
        ret = fid.go(node.id, x)
        return ret
    return go


@make_T
def equal(val):
    def go(node, x):
        if node != val:
            raise NonMatch()
        return x
    return go
