import collections


def generate_collections(collection):
    collection[0] = 100
    elements = list(collection.items())
    return [
        collection,
        collections.Counter(collection),
        elements
    ]


if __name__ == '__main__':
    print(generate_collections({1: 2}))
