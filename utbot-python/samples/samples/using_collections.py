import collections


def generate_collections(collection: collections.Counter):
    collection[0] = 100
    elements = list(collection.items)
    return [
        collection,
        collection.Counter(collection),
        elements
    ]
