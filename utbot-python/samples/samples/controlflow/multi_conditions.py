def check_interval(x: float, left: float, right: float) -> str:
    if left < x < right or right < x < left:
        return "between"
    elif x < left and x < right:
        return "less"
    elif x > left and x > right:
        return "more"
    elif left == right:
        return "all equals"
    elif x == left:
        return "left"
    elif x == right:
        return "right"
    return "what?"
