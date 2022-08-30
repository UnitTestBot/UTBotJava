#!/bin/bash

TIME_LIMIT=${1}
ITERATIONS=${2}
OUTPUT_DIR=${3}
PYTHON_COMMAND=${4}

declare -a models=("linear" "nn16" "nn32" "nn64" "nn128")

WORKDIR="."

echo "Start training data on heuristical based selectors"

$WORKDIR/scripts/ml/train_data.sh $TIME_LIMIT

echo "Start iterative learning of models"

for (( i=0; i < $ITERATIONS; i++ ))
do

  echo "Start $i iteration"

  for model in "${models[@]}"
  do
    EXTRA_ARGS=""
    if [[ $model == *"nn"* ]]; then
      EXTRA_ARGS="--hidden_dim $(echo $model | cut -c 3-)"
      echo "EXTRA_ARGS=$EXTRA_ARGS"
    fi

    COMMAND="$PYTHON_COMMAND $WORKDIR/scripts/ml/train.py --features_dir $WORKDIR/eval/features --output_dir $OUTPUT_DIR/$model/$i --prog_list $WORKDIR/scripts/prog_list --model $model $EXTRA_ARGS"
    echo "TRAINING COMMAND=$COMMAND"
    $COMMAND
  done

  while read prog; do
    prog="${prog%%[[:cntrl:]]}"

    for model in "${models[@]}"
    do
      PREDICTOR="BASE"

      if [[ $model == *"linear"* ]]; then
        PREDICTOR="LINEAR"
      fi

     $WORKDIR/scripts/ml/run_contest_estimator.sh $prog $TIME_LIMIT "NN_REWARD_GUIDED_SELECTOR $OUTPUT_DIR/$model/$i $PREDICTOR" "true eval/features/jlearch/$model$i/$prog"
    done
  done <"$WORKDIR/scripts/ml/prog_list"
done
