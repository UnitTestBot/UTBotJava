from typing import List, Dict, Optional


class Word:
    def __init__(self, translations: Dict[str, str]):
        self.translations = translations

    def __eq__(self, other):
        return self.translations == other.translations

    def keys(self):
        return list(self.translations.keys())


class Dictionary:
    def __init__(
            self,
            languages: List[str],
            words: List[Dict[str, str]],
    ):
        self.languages = languages
        self.words = [Word(translations) for translations in words]

    def __eq__(self, other):
        return self.languages == other.languages and self.words == other.words

    def translate(self, word: str, language: Optional[str]):
        if language is not None:
            for word_ in self.words:
                if word_.translations[language] == word:
                    return word_
        else:
            for word_ in self.words:
                if word in word_.translations.values():
                    return word_
