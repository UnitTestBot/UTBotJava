import typing as tp
import mypy.nodes
import utbot_mypy_runner.mypy_main as mypy_main


class Name:
    def __init__(self, name, type_='Other'):
        self.name = name
        self.type_ = type_

    def encode(self):
        return {'name': self.name, 'kind': self.type_}


class ModuleName(Name):
    def __init__(self, name, fullname):
        super().__init__(name, 'Module')
        self.fullname = fullname
    
    def encode(self):
        superclass_dict = super().encode()
        subclass_dict = {'fullname': self.fullname}
        return dict(superclass_dict, **subclass_dict)


class LocalTypeName(Name):
    def __init__(self, name):
        super().__init__(name, 'LocalType')


class ImportedTypeName(Name):
    def __init__(self, name, fullname):
        super().__init__(name, 'ImportedType')
        self.fullname = fullname
    
    def encode(self):
        superclass_dict = super().encode()
        subclass_dict = {'fullname': self.fullname}
        return dict(superclass_dict, **subclass_dict)


def get_names_from_module(module_name: str, table: mypy.nodes.SymbolTable) -> tp.List[Name]:
    result: tp.List[Name] = []
    for name in table.keys():
        # TODO: remove synthetic names

        node = table[name].node

        if isinstance(node, mypy.nodes.TypeInfo):
            if node.is_intersection:
                continue

            if node._fullname.startswith(module_name):
                result.append(LocalTypeName(name))
            else:
                result.append(ImportedTypeName(name, node._fullname))

        elif isinstance(node, mypy.nodes.MypyFile):
            result.append(ModuleName(name, node._fullname))

        else:
            result.append(Name(name))

    return result


def get_names(build_result: mypy_main.build.BuildResult) -> tp.Dict[str, tp.List[Name]]:
    names_dict: tp.Dict[str, tp.List[Name]] = {}
    for module in build_result.files.keys():
        names_dict[module] = get_names_from_module(module, build_result.files[module].names)
    return names_dict