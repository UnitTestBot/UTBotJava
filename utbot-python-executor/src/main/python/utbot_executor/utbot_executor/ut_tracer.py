import dis
import inspect
import logging
import os
import pathlib
import queue
import socket
import sys
import typing
from concurrent.futures import ThreadPoolExecutor

from utbot_executor.utils import TraceMode, UtInstruction


def _modname(path):
    base = os.path.basename(path)
    filename, _ = os.path.splitext(base)
    return filename


class UtCoverageSender:
    def __init__(self, coverage_id: str, host: str, port: int, use_thread: bool = False, send_coverage: bool = True):
        self.coverage_id = coverage_id
        self.host = host
        self.port = port
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.message_queue = queue.Queue()
        self.send_coverage = send_coverage

        self.use_thread = use_thread
        if use_thread:
            self.thread = ThreadPoolExecutor(max_workers=4)

    def send_loop(self):
        try:
            while True:
                self.send_message_thread()
        except Exception as _:
            self.send_loop()

    def send_message(self, message: bytes):
        if self.send_coverage:
            logging.debug(f"SEND {message}")
            self.sock.sendto(message, (self.host, self.port))

    def send_message_thread(self):
        message = self.message_queue.get()
        self.send_message(message)

    def put_message(self, key: str):
        message = bytes(f"{self.coverage_id}:{key}", encoding="utf-8")
        logging.debug(f"PUT {message}")
        if self.use_thread:
            self.message_queue.put((message, (self.host, self.port)))
            self.thread.submit(self.send_message_thread)
        else:
            self.send_message(message)


class PureSender(UtCoverageSender):
    def __init__(self):
        super().__init__("000000", "localhost", 0, use_thread=False, send_coverage=False)


class UtTracer:
    def __init__(
        self,
        tested_file: pathlib.Path,
        ignore_dirs: typing.List[str],
        sender: UtCoverageSender,
        mode: TraceMode = TraceMode.Instructions,
    ):
        self.tested_file = tested_file
        self.counts: dict[UtInstruction, int] = {}
        self.localtrace = self.localtrace_count
        self.globaltrace = self.globaltrace_lt
        self.ignore_dirs = ignore_dirs
        self.sender = sender
        self.mode = mode

    def runfunc(self, func, /, *args, **kw):
        result = None
        sys.settrace(self.globaltrace)
        self.f_code = func.__code__
        try:
            result = func(*args, **kw)
        finally:
            sys.settrace(None)
        return result

    def coverage(self, filename: str) -> typing.List[int]:
        filename = _modname(filename)
        return [line for file, line in self.counts.keys() if file == filename]

    def localtrace_count(self, frame, why, arg):
        filename = frame.f_code.co_filename
        lineno = frame.f_lineno
        if pathlib.Path(filename) == self.tested_file and lineno is not None:
            if self.mode == TraceMode.Instructions and frame.f_lasti is not None:
                offset = frame.f_lasti
            else:
                offset = 0
            key = UtInstruction(lineno, offset, frame.f_code == self.f_code)
            if key not in self.counts:
                message = key.serialize()
                try:
                    self.sender.put_message(message)
                except Exception:
                    pass
            self.counts[key] = self.counts.get(key, 0) + 1
        return self.localtrace

    def globaltrace_lt(self, frame, why, arg):
        if why == 'call':
            if self.mode == TraceMode.Instructions:
                frame.f_trace_opcodes = True
                frame.f_trace_lines = False
            filename = frame.f_globals.get('__file__', None)
            if filename and all(not filename.startswith(d + os.sep) for d in self.ignore_dirs):
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


def g1(x):
    return x * 2


def f(x):
    def g(x):
        xs = [[j for j in range(i)] for i in range(10)]
        return x * 2
    return g1(x) * g(x) + 2


if __name__ == "__main__":
    tracer = UtTracer(pathlib.Path(__file__), [], PureSender())
    tracer.runfunc(f, 2)
    print(tracer.counts)