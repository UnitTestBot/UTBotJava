#!/bin/bash

WORKDIR="."
TIME_LIMIT=${1}

while read prog; do
  echo "Starting features collection from $prog"
  prog="${prog%%[[:cntrl:]]}"
  while read selector; do
    echo "Starting features collection from $prog with $selector"
    selector="${selector%%[[:cntrl:]]}"
    $WORKDIR/scripts/ml/run_contest_estimator.sh "$prog" "$TIME_LIMIT" "$selector" true
  done <"$WORKDIR/scripts/ml/selector_list"
done <"$WORKDIR/scripts/ml/prog_list"
