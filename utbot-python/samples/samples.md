## Соответствие файлов и сгенерированных тестов

Примеры в `/samples`, сгенерированный код в `/cli_utbot_tests`.

Команда по умолчанию
```bash
java -jar utbot-cli.jar generate_python samples/<filename>.py -p <python_path> -o cli_utbot_tests/<output_file>.py -s samples/ ----timeout-for-run 500 --timeout 10000 --visit-only-specified-source
```

| Пример                   | Тесты                                     | Дополнительные аргументы                  |
|--------------------------|-------------------------------------------|-------------------------------------------|
| `arithmetic.py`          | `generated_tests__arithmetic.py`          |                                           |
| `deep_equals.py`         | `generated_tests__deep_equals.py`         |                                           |
| `dicts.py`               | `generated_tests__dicts.py`               | `-c Dictionary -m translate`              |
| `deque.py`               | `generated_tests__deque.py`               |                                           |
| `dummy_with_eq.py`       | `generated_tests__dummy_with_eq.py`       | `-c Dummy -m propogate`                   |
| `dummy_without_eq.py`    | `generated_tests__dummy_without_eq.py`    | `-c Dummy -m propogate`                   |
| `graph.py`               | `generated_tests__graph.py`               |                                           |
| `lists.py`               | `generated_tests__lists.py`               |                                           |
| `list_of_datetime.py`    | `generated_tests__list_of_datetime.py`    |                                           |
| `longest_subsequence.py` | `generated_tests__longest_subsequence.py` |                                           |
| `matrix.py`              | `generated_tests__matrix.py`              | `-c Matrix -m __add__,__mul__,__matmul__` |
| `primitive_types.py`     | `generated_tests__primitive_types.py`     |                                           |
| `quick_sort.py`          | `generated_tests__quick_sort.py`          |                                           |
| `test_coverage.py`       | `generated_tests__test_coverage.py`       |                                           |
| `type_inhibition.py`     | `generated_tests__type_inhibition.py`     |                                           |
| `using_collections.py`   | `generated_tests__using_collections.py`   |                                           |
