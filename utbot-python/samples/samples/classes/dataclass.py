from dataclasses import dataclass


@dataclass
class C:
    counter: int = 0

    def inc(self):
        self.counter += 1
