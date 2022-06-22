# ./scripts/train_iteratively.sh 120 6 models/guava python3

# ./scripts/run_with_coverage.sh guava-26.0 120 "NN_REWARD_GUIDED_SELECTOR models/guava true 4" nn4_120 true
# ./scripts/quality_analysis.sh guava-26.0 cpi_120,fork_120,random_120,random_path_120,subpath_120,nn4_120

# ./scripts/run_with_coverage.sh seata-core-0.5.0 120 "NN_REWARD_GUIDED_SELECTOR models/guava true 4" nn4_120 true
# ./scripts/quality_analysis.sh  seata-core-0.5.0 cpi_120,fork_120,random_120,random_path_120,subpath_120,nn4_120

# ./scripts/run_with_coverage.sh spoon-core-7.0.0 120 RANDOM_SELECTOR random_120 true
# ./scripts/run_with_coverage.sh spoon-core-7.0.0 120 RANDOM_PATH_SELECTOR random_path_120 true
# ./scripts/run_with_coverage.sh spoon-core-7.0.0 120 "SUBPATH_GUIDED_SELECTOR skip true 4" subpath_120 true
# ./scripts/run_with_coverage.sh spoon-core-7.0.0 120 "NN_REWARD_GUIDED_SELECTOR models/guava true 4" nn4_120 true
# ./scripts/quality_analysis.sh  spoon-core-7.0.0 cpi_120,fork_120,random_120,random_path_120,subpath_120,nn4_120
# ./scripts/run_with_coverage.sh seata-core-0.5.0 120 RANDOM_SELECTOR random_120
# ./scripts/run_with_coverage.sh seata-core-0.5.0 120 "SUBPATH_GUIDED_SELECTOR skip true 4" subpath_120
# ./scripts/run_with_coverage.sh seata-core-0.5.0 120 INHERITORS_SELECTOR inheritors_120
# ./scripts/run_with_coverage.sh seata-core-0.5.0 120 CPI_SELECTOR cpi_120
# ./scripts/run_with_coverage.sh seata-core-0.5.0 120 FORK_DEPTH_SELECTOR fork_120
# ./scripts/run_with_coverage.sh seata-core-0.5.0 120 "NN_REWARD_GUIDED_SELECTOR models/guava true 4" nn4_120 
# ./scripts/run_with_coverage.sh seata-core-0.5.0 120 "NN_REWARD_GUIDED_SELECTOR models/guava true 6" nn6_120 
# ./scripts/run_with_coverage.sh seata-core-0.5.0 120 "NN_REWARD_GUIDED_SELECTOR models/guava/0" nn0_120 
# ./scripts/run_with_coverage.sh seata-core-0.5.0 120 "NN_REWARD_GUIDED_SELECTOR models/guava/5" nn5_120
# ./scripts/quality_analysis.sh  seata-core-0.5.0 cpi_120,fork_120,random_120,inheritors_120,subpath_120,nn4_120,nn6_120,nn0_120,nn5_120

./scripts/run_with_coverage.sh pdfbox 120 RANDOM_SELECTOR random_120
./scripts/run_with_coverage.sh pdfbox 120 "SUBPATH_GUIDED_SELECTOR skip true 4" subpath_120
./scripts/run_with_coverage.sh pdfbox 120 INHERITORS_SELECTOR inheritors_120
./scripts/run_with_coverage.sh pdfbox 120 CPI_SELECTOR cpi_120
./scripts/run_with_coverage.sh pdfbox 120 FORK_DEPTH_SELECTOR fork_120
./scripts/run_with_coverage.sh pdfbox 120 "NN_REWARD_GUIDED_SELECTOR models/guava true 4" nn4_120 
./scripts/run_with_coverage.sh pdfbox 120 "NN_REWARD_GUIDED_SELECTOR models/guava true 6" nn6_120 
./scripts/run_with_coverage.sh pdfbox 120 "NN_REWARD_GUIDED_SELECTOR models/guava/0" nn0_120 
./scripts/run_with_coverage.sh pdfbox 120 "NN_REWARD_GUIDED_SELECTOR models/guava/5" nn5_120
./scripts/quality_analysis.sh  pdfbox cpi_120,fork_120,random_120,inheritors_120,subpath_120,nn4_120,nn6_120,nn0_120,nn5_120