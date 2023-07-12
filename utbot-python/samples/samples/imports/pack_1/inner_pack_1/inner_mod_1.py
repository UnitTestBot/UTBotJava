from ..inner_pack_2 import inner_mod_2


def inner_func_1(a: int):
    if a > 1:
        return inner_mod_2(a)
    return a ** 2