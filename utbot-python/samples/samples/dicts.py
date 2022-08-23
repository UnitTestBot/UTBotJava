from typing import List, Dict


class Word:
    def __init__(self, translations: Dict[str, str]):
        self.translations = translations


class Dictionary:
    def __init__(
            self,
            languages: List[str],
            words: List[Word],
    ):
        self.languages = languages
        self.words = words

    def translate(self, word: str, language=None):
        if language is not None:
            for word_ in self.words:
                if word_.translations[language] == word:
                    return word_
        else:
            for word_ in self.words:
                if word in word_.translations.values():
                    return word_
