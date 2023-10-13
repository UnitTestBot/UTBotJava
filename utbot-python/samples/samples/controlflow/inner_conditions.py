def hard_function(x):
    if x % 100 == 0:
        return 1
    elif x + 100 < 400:
        return 2
    else:
        if x == complex(1, 2):
            return x
        elif len(str(x)) > 3:
            return 3
        else:
            return 4
