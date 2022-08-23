def pretty_print(x):
    if isinstance(x, int):
        return 'It is integer.\n' + 'Value ' + str(x)
    elif isinstance(x, str):
        return 'It is string.\n' + 'Value <<' + x + '>>'
    elif isinstance(x, complex):
        return 'It is complex.\n' + 'Value (' + str(x.real) + ' + ' + str(x.real) + 'i)'
    elif isinstance(x, list):
        return 'It is list.\n' + 'Value ' + str(x)
    else:
        return 'I do not have any variants'
