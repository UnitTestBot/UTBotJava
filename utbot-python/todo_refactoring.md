# Задачи

- [ ] importIfNeeded, добавление импортов внутри testFramework
- [ ] Переделать sys.path в import или package
- [ ] Посмотреть, где собирается existingVariableNames, и добавить туда свои
- [ ] Убрать if-ы из Cg... с Python -> (перенес логику из CgMethodConstructor в PythonCgMethodConstructor)
- [ ] Перенести Python модели из Api.kt в PythonApi???
- [ ] Все изменения положить в отдельные пакеты
- [ ] Обработка исключений (writeWarningAboutFunciton)
- [ ] Переместить вызов pythonDeepEquals в самый верх
- [ ] PythonTree <-> AssembleModel?

# Вопросы
 * Что делать с 
    ```python
    import sys
    sys.path.append('..')
    import user_module
    ```
   Можно добавить разделение `PythonImport` на две группы:
   * системные
   * пользовательского файла
   
    И сделать приоритет системных 1, `sys.path` 2, пользовательских 3
 * В каком месте создавать python пакет в `utbot-framework`?
 * Что делать с python-ветками в разных when?
 * Что делать с Domain.kt? Нужно ли убирать оттуда питоновские тестовые фремворки
 * Где искать superclass у тест?