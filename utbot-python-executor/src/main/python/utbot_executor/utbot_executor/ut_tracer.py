import dis
import inspect
import logging
import os
import pathlib
import queue
import socket
import sys
import threading
import typing
from concurrent.futures import ThreadPoolExecutor


def _modname(path):
    base = os.path.basename(path)
    filename, _ = os.path.splitext(base)
    return filename


class UtCoverageSender:
    def __init__(self, coverage_id: str, host: str, port: int, use_thread: bool = False):
        self.coverage_id = coverage_id
        self.host = host
        self.port = port
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.message_queue = queue.Queue()

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


class UtTracer:
    def __init__(self, tested_file: pathlib.Path, ignore_dirs: typing.List[str], sender: UtCoverageSender):
        self.tested_file = tested_file
        self.counts = {}
        self.localtrace = self.localtrace_count
        self.globaltrace = self.globaltrace_lt
        self.ignore_dirs = ignore_dirs
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
        filename = frame.f_code.co_filename
        if pathlib.Path(filename) == self.tested_file:
            lineno = frame.f_lineno
            offset = 0
            if why == "opcode":
                offset = frame.f_lasti
            key = (lineno, offset)
            logging.debug(filename, key)
            if key not in self.counts:
                message = ":".join(map(str, key))
                try:
                    # self.sender.send_message(message)
                    self.sender.put_message(message)
                except Exception:
                    pass
            self.counts[key] = self.counts.get(key, 0) + 1
        return self.localtrace

    def globaltrace_lt(self, frame, why, arg):
        if why == 'call':
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


def f(x):
    if 0 < x < 10 and x % 2 == 0:
        return 1
    else:
        return [100,
                x**2,
                x + 1
                ]


if __name__ in "__main__":
    tracer = UtTracer(pathlib.Path(__file__), [], UtCoverageSender("1", "localhost", 0, use_thread=False))
    tracer.runfunc(f, 6)