def pretty_print(x):
    if isinstance(x, int):
        return 'It is integer.\n' + f'Value {x}'
    elif isinstance(x, str):
        return 'It is string.\n' + f'Value <<{x}>>'
    elif isinstance(x, complex):
        return 'It is complex.\n' + f'Value ({x.real} + {x.real}i)'
    elif isinstance(x, list):
        return 'It is list.\n' + f'Value {x}'
    else:
        return 'I have any variants'
