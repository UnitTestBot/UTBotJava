import dataclasses
import datetime


@dataclasses.dataclass
class Article:
    title: str
    author: str
    content: str
    created_at: datetime.datetime


def find_articles_with_author(articles: list[Article], author: str) -> list[Article]:
    return [
        article for article in articles
        if article.author == author
    ]
