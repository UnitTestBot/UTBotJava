from dataclasses import dataclass
import datetime
from typing import List


@dataclass
class Article:
    title: str
    author: str
    content: str
    created_at: datetime.datetime


def find_articles_with_author(articles: List[Article], author: str) -> List[Article]:
    return [
        article for article in articles
        if article.author == author
    ]


def f(x: List[int]):
    if len(x) == 0:
        return "Empty!"
    return sum(x)


if __name__ == '__main__':
    print(find_articles_with_author([
        Article('a', 'a1', 'jfls', datetime.datetime.today()),
        Article('b', 'a2', 'fjls', datetime.datetime.now())
    ], 'a1'))
