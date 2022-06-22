#!/bin/bash

TIME_LIMIT=${1}
ITERATIONS=${2}
OUTPUT_DIR=${3}
PYTHON_COMMAND=${4}

WORKDIR="."
$WORKDIR/scripts/train_data.sh $TIME_LIMIT
for (( i=0; i < $ITERATIONS; i++ ))
do
  $PYTHON_COMMAND $WORKDIR/scripts/train.py --features_dir $WORKDIR/eval/features --output_dir $OUTPUT_DIR/linear/$i --prog_list $WORKDIR/scripts/prog_list --model linear
  $PYTHON_COMMAND $WORKDIR/scripts/train.py --features_dir $WORKDIR/eval/features --output_dir $OUTPUT_DIR/nn16/$i --prog_list $WORKDIR/scripts/prog_list --model nn16 --hidden_dim 16
  $PYTHON_COMMAND $WORKDIR/scripts/train.py --features_dir $WORKDIR/eval/features --output_dir $OUTPUT_DIR/nn32/$i --prog_list $WORKDIR/scripts/prog_list --model nn32 --hidden_dim 32
  $PYTHON_COMMAND $WORKDIR/scripts/train.py --features_dir $WORKDIR/eval/features --output_dir $OUTPUT_DIR/nn64/$i --prog_list $WORKDIR/scripts/prog_list --model nn64 --hidden_dim 64
  $PYTHON_COMMAND $WORKDIR/scripts/train.py --features_dir $WORKDIR/eval/features --output_dir $OUTPUT_DIR/nn128/$i --prog_list $WORKDIR/scripts/prog_list --model nn128  --hidden_dim 128
  while read prog; do
    prog="${prog%%[[:cntrl:]]}"
    $WORKDIR/scripts/run_contest_estimator.sh $prog $TIME_LIMIT "NN_REWARD_GUIDED_SELECTOR $OUTPUT_DIR/linear/$i LINEAR" "true eval/features/jlearch/linear$i/$prog"
    $WORKDIR/scripts/run_contest_estimator.sh $prog $TIME_LIMIT "NN_REWARD_GUIDED_SELECTOR $OUTPUT_DIR/nn16/$i BASE" "true eval/features/jlearch/nn16$i/$prog"
    $WORKDIR/scripts/run_contest_estimator.sh $prog $TIME_LIMIT "NN_REWARD_GUIDED_SELECTOR $OUTPUT_DIR/nn32/$i BASE" "true eval/features/jlearch/nn32$i/$prog"
    $WORKDIR/scripts/run_contest_estimator.sh $prog $TIME_LIMIT "NN_REWARD_GUIDED_SELECTOR $OUTPUT_DIR/nn64/$i BASE" "true eval/features/jlearch/nn64$i/$prog"
    $WORKDIR/scripts/run_contest_estimator.sh $prog $TIME_LIMIT "NN_REWARD_GUIDED_SELECTOR $OUTPUT_DIR/nn128/$i BASE" "true eval/features/jlearch/nn128$i/$prog"
  done <"$WORKDIR/scripts/prog_list"
done
