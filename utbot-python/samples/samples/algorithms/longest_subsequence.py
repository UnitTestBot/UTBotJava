from typing import List


def longest_subsequence(array: List[int]) -> List[int]:
    array_length = len(array)
    if array_length <= 1:
        return array
    pivot = array[0]
    is_found = False
    i = 1
    longest_subseq: List[int] = []
    while not is_found and i < array_length:
        if array[i] < pivot:
            is_found = True
            temp_array = [element for element in array[i:] if element >= array[i]]
            temp_array = longest_subsequence(temp_array)
            if len(temp_array) > len(longest_subseq):
                longest_subseq = temp_array
        else:
            i += 1

    temp_array = [element for element in array[1:] if element >= pivot]
    temp_array = [pivot] + longest_subsequence(temp_array)
    if len(temp_array) > len(longest_subseq):
        return temp_array
    else:
        return longest_subseq
