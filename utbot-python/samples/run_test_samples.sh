# Usage:
# ./generate_test_samples.sh <absolute_python_path>

python_path=$1
java_path=$2

$java_path -jar utbot-cli.jar --verbosity DEBUG run_python cli_utbot_tests/generated_tests__arithmetic.py -p $python_path
$java_path -jar utbot-cli.jar --verbosity DEBUG run_python cli_utbot_tests/generated_tests__deep_equals.py -p $python_path
$java_path -jar utbot-cli.jar --verbosity DEBUG run_python cli_utbot_tests/generated_tests__dicts.py -p $python_path
$java_path -jar utbot-cli.jar --verbosity DEBUG run_python cli_utbot_tests/generated_tests__deque.py -p $python_path
$java_path -jar utbot-cli.jar --verbosity DEBUG run_python cli_utbot_tests/generated_tests__dummy_with_eq.py -p $python_path
$java_path -jar utbot-cli.jar --verbosity DEBUG run_python cli_utbot_tests/generated_tests__dummy_without_eq.py -p $python_path
$java_path -jar utbot-cli.jar --verbosity DEBUG run_python cli_utbot_tests/generated_tests__lists.py -p $python_path
$java_path -jar utbot-cli.jar --verbosity DEBUG run_python cli_utbot_tests/generated_tests__list_of_datetime.py -p $python_path
$java_path -jar utbot-cli.jar --verbosity DEBUG run_python cli_utbot_tests/generated_tests__longest_subsequence.py -p $python_path
$java_path -jar utbot-cli.jar --verbosity DEBUG run_python cli_utbot_tests/generated_tests__matrix.py -p $python_path
$java_path -jar utbot-cli.jar --verbosity DEBUG run_python cli_utbot_tests/generated_tests__primitive_types.py -p $python_path
$java_path -jar utbot-cli.jar --verbosity DEBUG run_python cli_utbot_tests/generated_tests__quick_sort.py -p $python_path
$java_path -jar utbot-cli.jar --verbosity DEBUG run_python cli_utbot_tests/generated_tests__test_coverage.py -p $python_path
$java_path -jar utbot-cli.jar --verbosity DEBUG run_python cli_utbot_tests/generated_tests__type_inference.py -p $python_path
$java_path -jar utbot-cli.jar --verbosity DEBUG run_python cli_utbot_tests/generated_tests__using_collections.py -p $python_path
$java_path -jar utbot-cli.jar --verbosity DEBUG run_python cli_utbot_tests/generated_tests__dummy_with_eq.py -p $python_path
$java_path -jar utbot-cli.jar --verbosity DEBUG run_python cli_utbot_tests/generated_tests__dummy_without_eq.py -p $python_path
$java_path -jar utbot-cli.jar --verbosity DEBUG run_python cli_utbot_tests/generated_tests__list_of_datetime.py -p $python_path