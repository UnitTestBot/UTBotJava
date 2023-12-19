import logging
import math
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
    def __init__(
            self,
            coverage_id: str,
            host: str,
            port: int,
            use_thread: bool = False,
            send_coverage: bool = True,
    ):
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
        super().__init__(
            "000000", "localhost", 0, use_thread=False, send_coverage=False
        )


class UtTracer:
    DEFAULT_LINE_FILTER = (-math.inf, math.inf)

    def __init__(
            self,
            tested_file: pathlib.Path,
            ignore_dirs: typing.List[str],
            sender: UtCoverageSender,
            mode: TraceMode = TraceMode.Instructions,
    ):
        self.tested_file = tested_file
        self.counts: dict[UtInstruction, int] = {}
        self.instructions: list[UtInstruction] = []
        self.localtrace = self.localtrace_count
        self.globaltrace = self.globaltrace_lt
        self.ignore_dirs = ignore_dirs
        self.sender = sender
        self.mode = mode
        self.line_filter = UtTracer.DEFAULT_LINE_FILTER
        self.f_code = None

    def runfunc(self, func, line_filter, /, *args, **kw):
        self.line_filter = line_filter
        result = None
        sys.settrace(self.globaltrace)
        self.f_code = func.__code__
        try:
            result = func(*args, **kw)
        finally:
            sys.settrace(None)
        return result

    def localtrace_count(self, frame, why, arg):
        filename = frame.f_code.co_filename
        lineno = frame.f_lineno
        if (
                pathlib.Path(filename) == self.tested_file
                and lineno is not None
                and self.line_filter[0] <= lineno <= self.line_filter[1]
        ):
            if self.mode == TraceMode.Instructions and frame.f_lasti is not None:
                offset = frame.f_lasti
            else:
                offset = 0
            key = UtInstruction(lineno, offset, frame.f_code == self.f_code)
            try:
                self.sender.put_message(key.serialize())
            except Exception:
                pass
            self.counts[key] = self.counts.get(key, 0) + 1
            self.instructions.append(key)
        return self.localtrace

    def globaltrace_lt(self, frame, why, arg):
        if why == "call":
            filename = frame.f_code.co_filename
            if filename and filename == str(self.tested_file.resolve()):
                if self.mode == TraceMode.Instructions:
                    frame.f_trace_opcodes = True
                    frame.f_trace_lines = False
                elif self.mode == TraceMode.Lines:
                    frame.f_trace_opcodes = False
                    frame.f_trace_lines = True

                modulename = _modname(filename)
                if modulename is not None:
                    return self.localtrace
            else:
                frame.f_trace_opcodes = False
                frame.f_trace_lines = False
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
    tracer.runfunc(f, tracer.DEFAULT_LINE_FILTER, 2)
    print(tracer.counts)
