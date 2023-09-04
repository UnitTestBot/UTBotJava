from mypy.main import *

import sys

from io import StringIO
from typing import List, Tuple, TextIO, Callable, Optional, cast


"""
Copy with some changes of function 'main' from here:
https://github.com/python/mypy/blob/v1.5.1/mypy/main.py
"""
def new_main(
    stdout: TextIO,
    stderr: TextIO,
    args: Optional[List[str]] = None,
    clean_exit: bool = False
) -> Optional[build.BuildResult]:
    """Main entry point to the type checker.

    Args:
        args: Custom command-line arguments.  If not given, sys.argv[1:] will
            be used.
        clean_exit: Don't hard kill the process on exit. This allows catching
            SystemExit.
    """
    util.check_python_version("mypy")
    t0 = time.time()
    # To log stat() calls: os.stat = stat_proxy
    sys.setrecursionlimit(2**14)
    if args is None:
        args = sys.argv[1:]

    fscache = FileSystemCache()
    sources, options = process_options(args, stdout=stdout, stderr=stderr, fscache=fscache)

    # CHANGE: export types of AST nodes
    options.preserve_asts = True
    options.export_types = True

    if clean_exit:
        options.fast_exit = False

    formatter = util.FancyFormatter(stdout, stderr, options.hide_error_codes)

    if options.install_types and (stdout is not sys.stdout or stderr is not sys.stderr):
        # Since --install-types performs user input, we want regular stdout and stderr.
        fail("error: --install-types not supported in this mode of running mypy", stderr, options)

    if options.non_interactive and not options.install_types:
        fail("error: --non-interactive is only supported with --install-types", stderr, options)

    if options.install_types and not options.incremental:
        fail(
            "error: --install-types not supported with incremental mode disabled", stderr, options
        )

    if options.install_types and options.python_executable is None:
        fail(
            "error: --install-types not supported without python executable or site packages",
            stderr,
            options,
        )

    # CHANGE
    # if options.install_types and not sources:
    #    install_types(formatter, options, non_interactive=options.non_interactive)
    #    return

    res, messages, blockers = run_build(sources, options, fscache, t0, stdout, stderr)

    if options.non_interactive:
        missing_pkgs = read_types_packages_to_install(options.cache_dir, after_run=True)
        if missing_pkgs:
            # Install missing type packages and rerun build.
            install_types(formatter, options, after_run=True, non_interactive=True)
            fscache.flush()
            print()
            res, messages, blockers = run_build(sources, options, fscache, t0, stdout, stderr)
        show_messages(messages, stderr, formatter, options)

    if MEM_PROFILE:
        from mypy.memprofile import print_memory_profile

        print_memory_profile()

    code = 0
    n_errors, n_notes, n_files = util.count_stats(messages)
    if messages and n_notes < len(messages):
        code = 2 if blockers else 1
    if options.error_summary:
        if n_errors:
            summary = formatter.format_error(
                n_errors, n_files, len(sources), blockers=blockers, use_color=options.color_output
            )
            stdout.write(summary + "\n")
        # Only notes should also output success
        elif not messages or n_notes == len(messages):
            stdout.write(formatter.format_success(len(sources), options.color_output) + "\n")
        stdout.flush()

    if options.install_types and not options.non_interactive:
        result = install_types(formatter, options, after_run=True, non_interactive=False)
        if result:
            print()
            print("note: Run mypy again for up-to-date results with installed types")
            code = 2

    return res


"""
Copy with some changes of mypy api functions from here:
https://github.com/python/mypy/blob/v0.971/mypy/api.py
"""
def _run(
    main_wrapper: Callable[[TextIO, TextIO], Optional[build.BuildResult]]
) -> Tuple[str, str, int, Optional[build.BuildResult]]:

    stdout = StringIO()
    stderr = StringIO()

    res = None
    try:
        res = main_wrapper(stdout, stderr)
        exit_status = 0
    except SystemExit as system_exit:
        exit_status = cast(int, system_exit.code)

    return stdout.getvalue(), stderr.getvalue(), exit_status, res


def run(args: List[str]) -> Tuple[str, str, int, Optional[build.BuildResult]]:
    args.append("--no-incremental")
    return _run(lambda stdout, stderr: new_main(args=args, stdout=stdout, stderr=stderr, clean_exit=True))


if __name__ == "__main__":
    import time
    start = time.time()
    stdout, stderr, exit_status, build_result = run(sys.argv[1:])
    print(f"Seconds passed: {time.time() - start}")
    print(stdout, stderr, exit_status, sep='\n')
    if build_result is None:
        print("BuildResult is None")
    else:
        pass
        #print(build_result.files['utbot_mypy_runner.nodes'].names['CompositeAnnotationNode'].node.names["module_key"].node.is_initialized_in_class)
        #print(build_result.files['builtins'].names['str'].node.names["count"].node.arguments[2].initializer)
        #print(build_result.files['dijkstra'].names['Dijkstra'].node.names['Node'].node.module_name)
        #print(build_result.files['numpy'].names['ndarray'].module_public)
        #print(build_result.files['_pytest.runner'].names['<subclass of "Node" and "tuple">'].node.is_intersection)
        #print(build_result.files['subtypes'].names['P'].node.names['f'].fullname)
        #print([build_result.files['subtypes'].names[x].fullname for x in build_result.files['subtypes'].names])
        #print(build_result.files['collections'].names['deque'].node.mro)
        #print(build_result.files['collections'].names['Counter'].node._promote)
        #for x in build_result.files['builtins'].names['set'].node.defn.type_vars:
        #    print(type(x))
