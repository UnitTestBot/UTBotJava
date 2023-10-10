import argparse
import logging

from utbot_executor.listener import PythonExecuteServer
from utbot_executor.utils import TraceMode


def main(hostname: str, port: int, coverage_hostname: str, coverage_port: int, trace_mode: TraceMode, send_coverage: bool):
    server = PythonExecuteServer(hostname, port, coverage_hostname, coverage_port, trace_mode, send_coverage)
    server.run()


if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        prog="UtBot Python Executor",
        description="Listen socket stream and execute function value",
    )
    parser.add_argument("hostname")
    parser.add_argument("port", type=int)
    parser.add_argument("--logfile", default=None)
    parser.add_argument(
        "--loglevel",
        choices=["DEBUG", "INFO", "WARNING", "ERROR"],
        default="ERROR",
    )
    parser.add_argument("coverage_hostname")
    parser.add_argument("coverage_port", type=int)
    parser.add_argument(
        "--coverage_type", choices=["lines", "instructions"], default="instructions"
    )
    parser.add_argument(
        "--send_coverage", action=argparse.BooleanOptionalAction
    )
    args = parser.parse_args()

    loglevel = {
        "DEBUG": logging.DEBUG,
        "INFO": logging.INFO,
        "WARNING": logging.WARNING,
        "ERROR": logging.ERROR,
    }[args.loglevel]
    logging.basicConfig(
        filename=args.logfile,
        format="%(asctime)s | %(levelname)s | %(funcName)s - %(message)s",
        datefmt="%m/%d/%Y %H:%M:%S",
        level=loglevel,
    )
    trace_mode = TraceMode.Lines if args.coverage_type == "lines" else TraceMode.Instructions
    send_coverage = args.send_coverage
    main(args.hostname, args.port, args.coverage_hostname, args.coverage_port, trace_mode, send_coverage)
