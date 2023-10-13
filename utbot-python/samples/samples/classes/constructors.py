class EmptyClass:
    pass


class WithInitClass:
    def __init__(self, x: int):
        self.x = x


class EmptyInitClass:
    def __init__(self):
        pass


class BasicNewCass:
    def __new__(cls, *args, **kwargs):
        super().__new__(cls, *args, **kwargs)


class ParentEmptyInitClass(EmptyClass):
    pass


class ParentWithInitClass(WithInitClass):
    pass


class ParentEmptyClass(EmptyClass):
    pass


def func(a: EmptyClass, b: WithInitClass, c: EmptyInitClass, d: BasicNewCass, e: ParentEmptyClass,
         f: ParentWithInitClass, g: ParentEmptyInitClass):
    return a.__class__.__name__ + str(
        b.x) + c.__class__.__name__ + d.__class__.__name__ + e.__class__.__name__ + str(
        f.x) + g.__class__.__name__
