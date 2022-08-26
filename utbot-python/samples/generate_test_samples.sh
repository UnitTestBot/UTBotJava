# Usage:
# ./generate_test_samples.sh <absolute_python_path>

python_path=$0

java -jar utbot-cli.jar generate_python samples/arithmetic.py -p $python_path -o cli_utbot_tests/generated_tests__arithmetic.py -s samples/ --timeout-for-run 500 --visit-only-specified-source --timeout 10000
java -jar utbot-cli.jar generate_python samples/deep_equals.py -p $python_path -o cli_utbot_tests/generated_tests__deep_equals.py -s samples/ --timeout-for-run 500 --visit-only-specified-source --timeout 10000
java -jar utbot-cli.jar generate_python samples/dicts.py -p $python_path -o cli_utbot_tests/generated_tests__dicts.py -s samples/ --timeout-for-run 500 --visit-only-specified-source -c Dictionary -m translate --timeout 10000
java -jar utbot-cli.jar generate_python samples/lists.py -p $python_path -o cli_utbot_tests/generated_tests__lists.py -s samples/ --timeout-for-run 500 --visit-only-specified-source --timeout 10000
java -jar utbot-cli.jar generate_python samples/longest_subsequence.py -p $python_path -o cli_utbot_tests/generated_tests__longest_subsequence.py -s samples/ --timeout-for-run 500 --visit-only-specified-source --timeout 10000
java -jar utbot-cli.jar generate_python samples/matrix.py -p $python_path -o cli_utbot_tests/generated_tests__matrix.py -s samples/ --timeout-for-run 500 --visit-only-specified-source -c Matrix -m __add__,__mul__,__matmul__ --timeout 10000
java -jar utbot-cli.jar generate_python samples/primitive_types.py -p $python_path -o cli_utbot_tests/generated_tests__primitive_types.py -s samples/ --timeout-for-run 500 --visit-only-specified-source --timeout 10000
java -jar utbot-cli.jar generate_python samples/quick_sort.py -p $python_path -o cli_utbot_tests/generated_tests__quick_sort.py -s samples/ --timeout-for-run 500 --visit-only-specified-source --timeout 10000
java -jar utbot-cli.jar generate_python samples/test_coverage.py -p $python_path -o cli_utbot_tests/generated_tests__test_coverage.py -s samples/ --timeout-for-run 500 --visit-only-specified-source --timeout 10000
java -jar utbot-cli.jar generate_python samples/type_inhibition.py -p $python_path -o cli_utbot_tests/generated_tests__type_inhibition.py -s samples/ --timeout-for-run 500 --visit-only-specified-source --timeout 10000
java -jar utbot-cli.jar generate_python samples/using_collections.py -p $python_path -o cli_utbot_tests/generated_tests__using_collections.py -s samples/ --timeout-for-run 500 --visit-only-specified-source --timeout 10000
java -jar utbot-cli.jar generate_python samples/dummy_without_eq.py -p $python_path -o cli_utbot_tests/generated_tests__dummy_without_eq.py -s samples/ --timeout-for-run 500 --visit-only-specified-source --timeout 10000 -c Dummy -m propagate
java -jar utbot-cli.jar generate_python samples/dummy_with_eq.py -p $python_path -o cli_utbot_tests/generated_tests__dummy_with_eq.py -s samples/ --timeout-for-run 500 --visit-only-specified-source --timeout 10000 -c Dummy -m propagate
java -jar utbot-cli.jar generate_python samples/list_of_datetime.py -p $python_path -o cli_utbot_tests/generated_tests__list_of_datetime.py -s samples/ --timeout-for-run 500 --visit-only-specified-source --timeout 10000
