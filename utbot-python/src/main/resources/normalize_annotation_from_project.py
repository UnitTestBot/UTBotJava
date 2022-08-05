import importlib.machinery
import inspect
import sys
import types
import mypy.fastparse


def main(annotation: str, project_root: str, path: str):
    def walk_mypy_type(mypy_type) -> str:
        try:
            source = inspect.getfile(eval(mypy_type.name))
            in_project = source.startswith(project_root)
        except:
            in_project = False

        modname = eval(mypy_type.name).__module__
        if modname != '' and not in_project:
            file_name = mypy_type.name.split('.')[-1]
            fullname = f'{modname}.{file_name}'
        else:
            fullname = mypy_type.name

        result = fullname
        if len(mypy_type.args) != 0:
            arg_strs = [
                walk_mypy_type(arg)
                for arg in mypy_type.args
            ]
            result += f"[{', '.join(arg_strs)}]"
        return result

    loader = importlib.machinery.SourceFileLoader('', path)
    mod = types.ModuleType(loader.name)
    loader.exec_module(mod)

    for name in dir(mod):
        globals()[name] = getattr(mod, name)

    mypy_type_ = mypy.fastparse.parse_type_string(annotation, annotation, -1, -1)
    print(walk_mypy_type(mypy_type_), end='')


def get_args():
    annotation = sys.argv[1]
    project_root = sys.argv[2]
    path = sys.argv[3]
    for extra_path in sys.argv[4:]:
        sys.path.append(extra_path)
    return annotation, project_root, path


if __name__ == '__main__':
    main(*get_args())
