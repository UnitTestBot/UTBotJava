python_path=$1

rm -r utbot_coverage utbot_tests RUN_RESULT COVERAGE_RESULT
mkdir utbot_coverage
$python_path run_tests.py generate java ../../utbot-cli-python/build/libs/utbot-cli-python*.jar `pwd` -c test_configuration.json -p $python_path -o utbot_tests -i utbot_coverage

$python_path run_tests.py run -p $python_path  -c test_configuration.json -t utbot_tests > RUN_RESULT 2> RUN_RESULT

$python_path run_tests.py check_coverage -i utbot_coverage/ -c test_configuration.json > COVERAGE_RESULT 2> COVERAGE_RESULT
