import typing as tp


def bad_symbol(c: str) -> bool:
    return c.isspace() or c == '[' or c == '('


def get_borders(line: int, column: int, end_line: int, end_column: int, 
                file_content: tp.List[str]) -> tp.Tuple[int, int]:
    while bad_symbol(file_content[line - 1][column - 1]):
        line, column = inc_position(line, column, file_content)

    while bad_symbol(file_content[end_line - 1][end_column - 1]):
        end_line, end_column = dec_position(end_line, end_column, file_content)

    return get_offset(line, column, file_content), get_offset(end_line, end_column, file_content)


def get_offset(line: int, column: int, file_content: tp.List[str]) -> int:
    return sum([len(x) for x in file_content[:line-1]]) + column - 1


def inc_position(line: int, column: int, file_content: tp.List[str]) -> tp.Tuple[int, int]:
    if column == len(file_content[line - 1]):
        line += 1
        column = 1
    else:
        column += 1
    return line, column


def dec_position(line: int, column: int, file_content: tp.List[str]) -> tp.Tuple[int, int]:
    column -= 1
    if column == 0:
        line -= 1
        column = len(file_content[line - 1])
    return line, column
