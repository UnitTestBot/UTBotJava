def inhibition(number, string, string_sep, list_of_number, dict_str_to_list):
    new_string = '_' + string + '_'
    for i in range(number // 2):
        new_string = new_string[0] + string_sep + new_string[::-1]

    if len(list_of_number) < len(new_string):
        list_of_number += [0] * (len(new_string) - len(list_of_number))

    dict_str_to_list[new_string] = list_of_number
    dict_str_to_list[string] = []
    for key in dict_str_to_list.keys():
        list_of_number.append(key)

    return list_of_number


if __name__ == '__main__':
    print(inhibition(5, 'fjsl', '|', [1, 2, 3], {'fjls': [1, 2]}))