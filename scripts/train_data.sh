#!/bin/bash

WORKDIR="."
TIME_LIMIT=${1}

while read prog; do
  prog="${prog%%[[:cntrl:]]}"
  while read selector; do
    selector="${selector%%[[:cntrl:]]}"
    $WORKDIR/scripts/run_contest_estimator.sh "$prog" "$TIME_LIMIT" "$selector" true
  done <"$WORKDIR/scripts/selector_list"
done <"$WORKDIR/scripts/prog_list"
