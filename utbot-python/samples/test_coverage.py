from contextlib import contextmanager

from general import test_call

@contextmanager
def suppress_stdout():
    with open(os.devnull, "w") as devnull:
        old_stdout = sys.stdout
        sys.stdout = devnull
        try:
            yield
        finally:
            sys.stdout = old_stdout



def coverage_collector(func, args, fullpath):
    import inspect
    from coverage import Coverage

    def get_function_line_numbers(func):
        rows, start_row = inspect.getsourcelines(func)
        return start_row, start_row + len(rows)

    cov = Coverage()
    cov.start()

    result = func(*args)

    cov.stop()
    cov.save()
    coverage_lines = cov.get_data().lines(fullpath)
    first, last = get_function_line_numbers(func)
    return result, [line for line in coverage_lines if first <= line < last]


print(coverage_collector(test_call, [100], '/home/vyacheslav/Desktop/utbot/UTBotJava/utbot-python/samples/general.py'))

