import time


def check_interval(x: float, left: float, right: float) -> str:
    if left < x < right or right < x < left:
        print(1)
        time.sleep(4)
        return "between"
    elif x < left and x < right:
        print(1)
        time.sleep(4)
        return "less"
    elif x > left and x > right:
        print(1)
        time.sleep(4)
        return "more"
    elif left == right:
        print(1)
        time.sleep(4)
        return "all equals"
    elif x == left:
        print(1)
        time.sleep(4)
        return "left"
    elif x == right:
        print(1)
        time.sleep(4)
        return "right"
    print(1)
    time.sleep(4)
    return "what?"
