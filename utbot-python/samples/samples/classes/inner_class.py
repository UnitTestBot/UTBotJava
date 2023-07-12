class Outer:
    class Inner:
        a = 1

        def inc(self):
            self.a += 1

    def __init__(self):
        self.inner = Outer.Inner()

    def inc1(self):
        self.inner.inc()