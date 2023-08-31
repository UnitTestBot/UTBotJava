import os
import sys
import typing


def _modname(path):
    base = os.path.basename(path)
    filename, _ = os.path.splitext(base)
    return filename


class UtTracer:
    def __init__(self, sender: typing.Callable[[typing.Tuple[str, int]], None]):
        self.globaltrace = self.globaltrace_lt
        self.counts = {}
        self.localtrace = self.localtrace_count
        self.globaltrace = self.globaltrace_lt
        self.sender = sender

    def runfunc(self, func, /, *args, **kw):
        result = None
        sys.settrace(self.globaltrace)
        try:
            result = func(*args, **kw)
        finally:
            sys.settrace(None)
        return result

    def coverage(self, filename: str) -> typing.List[int]:
        filename = _modname(filename)
        return [line for file, line in self.counts.keys() if file == filename]

    def localtrace_count(self, frame, why, arg):
        if why == "line":
            filename = frame.f_code.co_filename
            lineno = frame.f_lineno
            key = filename, lineno
            if key not in self.counts:
                try:
                    self.sender(key)
                except Exception:
                    pass
            self.counts[key] = self.counts.get(key, 0) + 1
        return self.localtrace

    def globaltrace_lt(self, frame, why, arg):
        if why == 'call':
            filename = frame.f_globals.get('__file__', None)
            if filename:
                modulename = _modname(filename)
                if modulename is not None:
                    return self.localtrace
            else:
                return None


class PureTracer:
    def __init__(self):
        self.counts = []

    def runfunc(self, func, /, *args, **kw):
        return func(*args, **kw)
