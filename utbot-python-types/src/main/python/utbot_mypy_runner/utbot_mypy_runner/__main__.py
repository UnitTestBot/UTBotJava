import argparse
import os

import utbot_mypy_runner.mypy_main as mypy_main
import utbot_mypy_runner.extract_annotations as extraction


parser = argparse.ArgumentParser()
parser.add_argument('--config', required=True)
parser.add_argument('--sources', required=True, nargs='+')
parser.add_argument('--modules', required=True, nargs='+')
parser.add_argument('--annotations_out')
parser.add_argument('--mypy_stdout')
parser.add_argument('--mypy_stderr')
parser.add_argument('--mypy_exit_status')
parser.add_argument('--module_for_types')
parser.add_argument('--indent', type=int)

args = parser.parse_args()

if len(args.sources) != len(args.modules):
    print("Sources must correspond to modules")
    exit(10)

mypy_args = ["--config-file", args.config]
for module_name in args.modules:
    mypy_args += ["-m", module_name]

stdout, stderr, exit_status, build_result = mypy_main.run(mypy_args)

if args.mypy_stdout is not None:
    with open(args.mypy_stdout, "w") as file:
        file.write(stdout)
    print("Wrote mypy stdout to", args.mypy_stdout)

if args.mypy_stderr is not None:
    with open(args.mypy_stderr, "w") as file:
        file.write(stderr)
    print("Wrote mypy stderr to", args.mypy_stderr)

if args.mypy_exit_status is not None:
    with open(args.mypy_exit_status, "w") as file:
        file.write(str(exit_status))
    print("Wrote mypy exit status to", args.mypy_exit_status)

if args.annotations_out is not None:
    if build_result is not None:
        with open(args.annotations_out, "w") as file:
            sources = [os.path.abspath(x) for x in args.sources]
            file.write(extraction.get_result_from_mypy_build(build_result, sources, args.module_for_types, args.indent))
        print("Extracted annotations and wrote to", args.annotations_out)
    else:
        print("For some reason BuildResult is None")
        exit(11)